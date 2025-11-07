package io.store.ua.service;

import io.store.ua.entity.Shipment;
import io.store.ua.exceptions.BusinessException;
import io.store.ua.exceptions.NotFoundException;
import io.store.ua.models.dto.ShipmentDTO;
import io.store.ua.repository.ShipmentRepository;
import io.store.ua.validations.FieldValidator;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
@Validated
@PreAuthorize("isAuthenticated()")
public class ShipmentService {
    private final ShipmentRepository shipmentRepository;
    private final FieldValidator fieldValidator;
    private final WarehouseService warehouseService;

    public List<Shipment> findAll(@Min(value = 1, message = "Size of page can't be less than 1") int pageSize,
                                  @Min(value = 1, message = "A page number can't be less than 1") int page) {
        return shipmentRepository.findAll(Pageable.ofSize(pageSize).withPage(page - 1)).getContent();
    }

    public Shipment findById(@NotNull(message = "Shipment ID can't be null") Long id) {
        return shipmentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Shipment with ID '%s' was not found".formatted(id)));
    }

    public Shipment save(@NotNull(message = "Shipment can't be null") ShipmentDTO shipmentDTO) {
        if (ObjectUtils.allNull(shipmentDTO.getRecipientCode(), shipmentDTO.getAddress())) {
            throw new BusinessException("Recipient code and address can't be null at the same time, unknown where to send shipment");
        }

        if (ObjectUtils.allNotNull(shipmentDTO.getRecipientCode(), shipmentDTO.getAddress())) {
            throw new BusinessException("Recipient code and address can't be not null at the same time, unknown where to send shipment");
        }

        Shipment.ShipmentBuilder shipmentBuilder = Shipment.builder();

        shipmentBuilder.initiatorId(RegularUserService.getCurrentlyAuthenticatedUserID());

        fieldValidator.validate(shipmentDTO, ShipmentDTO.Fields.senderCode, true);

        if (shipmentDTO.getSenderCode().equals(shipmentDTO.getRecipientCode())) {
            throw new BusinessException("Sender and recipient can't be the same");
        }

        var warehouseSender = warehouseService.findByCode(shipmentDTO.getSenderCode());
        shipmentBuilder.warehouseIdSender(warehouseSender.getId());

        if (shipmentDTO.getRecipientCode() != null) {
            fieldValidator.validate(shipmentDTO, ShipmentDTO.Fields.recipientCode, true);
            shipmentBuilder.warehouseIdRecipient(warehouseService.findByCode(shipmentDTO.getRecipientCode()).getId());
        }

        if (shipmentDTO.getAddress() != null) {
            fieldValidator.validateObject(shipmentDTO, ShipmentDTO.Fields.address, true);
            shipmentBuilder.address(shipmentDTO.getAddress());
        }

        fieldValidator.validate(shipmentDTO, ShipmentDTO.Fields.stockItemId, true);
        shipmentBuilder.stockItemId(shipmentDTO.getStockItemId());

        fieldValidator.validate(shipmentDTO, ShipmentDTO.Fields.stockItemQuantity, true);
        shipmentBuilder.stockItemQuantity(shipmentDTO.getStockItemQuantity());

        if (!StringUtils.isBlank(shipmentDTO.getStatus())) {
            fieldValidator.validate(shipmentDTO, ShipmentDTO.Fields.status, true);
            var shipmentStatus = Arrays.stream(Shipment.ShipmentStatus.values())
                    .filter(status -> status.name().equalsIgnoreCase(shipmentDTO.getStatus()))
                    .findAny()
                    .orElseThrow(() -> new BusinessException("Invalid shipment status"));
            shipmentBuilder.status(shipmentStatus);
        } else {
            shipmentBuilder.status(Shipment.ShipmentStatus.INITIATED);
        }

        return shipmentRepository.save(shipmentBuilder.build());
    }

    public Shipment update(@NotNull(message = "Shipment can't be null") ShipmentDTO shipmentDTO) {
        if (ObjectUtils.allNotNull(shipmentDTO.getRecipientCode(), shipmentDTO.getAddress())) {
            throw new BusinessException("Recipient code and address can't be not null at the same time, unknown where to send shipment");
        }

        fieldValidator.validate(shipmentDTO, ShipmentDTO.Fields.id, true);
        Shipment entity = shipmentRepository.findById(shipmentDTO.getId())
                .orElseThrow(() -> new BusinessException("Shipment with ID '%s' was not found".formatted(shipmentDTO.getId())));


        if (ObjectUtils.allNotNull(shipmentDTO.getSenderCode(), shipmentDTO.getRecipientCode())
                && shipmentDTO.getSenderCode().equals(shipmentDTO.getRecipientCode())) {
            throw new BusinessException("Sender and recipient can't be the same");
        }

        if (StringUtils.isNotBlank(shipmentDTO.getSenderCode())) {
            fieldValidator.validate(shipmentDTO, ShipmentDTO.Fields.senderCode, true);
            var warehouseSender = warehouseService.findByCode(shipmentDTO.getSenderCode());
            entity.setWarehouseIdSender(warehouseSender.getId());
        }

        if (shipmentDTO.getRecipientCode() != null) {
            fieldValidator.validate(shipmentDTO, ShipmentDTO.Fields.recipientCode, true);
            var warehouseRecipient = warehouseService.findByCode(shipmentDTO.getRecipientCode());
            entity.setWarehouseIdRecipient(warehouseRecipient.getId());
        }

        if (shipmentDTO.getAddress() != null) {
            fieldValidator.validateObject(shipmentDTO, ShipmentDTO.Fields.address, true);
            entity.setAddress(shipmentDTO.getAddress());
        }

        if (shipmentDTO.getStockItemId() != null) {
            fieldValidator.validate(shipmentDTO, ShipmentDTO.Fields.stockItemId, true);
            entity.setStockItemId(shipmentDTO.getStockItemId());
        }

        if (shipmentDTO.getStockItemQuantity() != null) {
            fieldValidator.validate(shipmentDTO, ShipmentDTO.Fields.stockItemQuantity, true);
            entity.setStockItemQuantity(shipmentDTO.getStockItemQuantity());
        }

        if (!StringUtils.isBlank(shipmentDTO.getStatus())) {
            var shipmentStatus = Arrays.stream(Shipment.ShipmentStatus.values())
                    .filter(status -> status.name().equalsIgnoreCase(shipmentDTO.getStatus()))
                    .findAny()
                    .orElseThrow(() -> new BusinessException("Invalid shipment status"));
            entity.setStatus(shipmentStatus);
        }

        return shipmentRepository.save(entity);
    }
}
