package io.store.ua.service;

import io.store.ua.entity.Shipment;
import io.store.ua.enums.ShipmentDirection;
import io.store.ua.enums.ShipmentStatus;
import io.store.ua.exceptions.BusinessException;
import io.store.ua.exceptions.NotFoundException;
import io.store.ua.models.dto.ShipmentDTO;
import io.store.ua.repository.ShipmentRepository;
import io.store.ua.utility.CodeGenerator;
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
        if (ObjectUtils.allNull(shipmentDTO.getSenderCode(), shipmentDTO.getRecipientCode(), shipmentDTO.getAddress())) {
            throw new BusinessException("Sender, recipient code and address can't be null at the same time, unknown where to or where from to send shipment");
        }

        Shipment.ShipmentBuilder shipmentBuilder = Shipment.builder();
        shipmentBuilder.code(CodeGenerator.ShipmentCodeGenerator.generate());
        shipmentBuilder.initiatorId(RegularUserService.getCurrentlyAuthenticatedUserID());

        if (StringUtils.isNoneBlank(shipmentDTO.getSenderCode(), shipmentDTO.getRecipientCode())) {
            fieldValidator.validate(shipmentDTO, true, ShipmentDTO.Fields.senderCode, ShipmentDTO.Fields.recipientCode);

            if (shipmentDTO.getSenderCode().equals(shipmentDTO.getRecipientCode())) {
                throw new BusinessException("Sender and recipient can't be the same");
            }
        }

        if (shipmentDTO.getSenderCode() != null) {
            fieldValidator.validate(shipmentDTO, ShipmentDTO.Fields.senderCode, true);
            shipmentBuilder.warehouseIdSender(warehouseService.findByCode(shipmentDTO.getSenderCode()).getId());
        }

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
            var shipmentStatus = Arrays.stream(ShipmentStatus.values())
                    .filter(status -> status.name().equalsIgnoreCase(shipmentDTO.getStatus()))
                    .findAny()
                    .orElseThrow(() -> new BusinessException("Invalid shipment status"));
            shipmentBuilder.status(shipmentStatus);
        } else {
            shipmentBuilder.status(ShipmentStatus.PLANNED);
        }

        if (ObjectUtils.allNotNull(shipmentDTO.getRecipientCode(), shipmentDTO.getAddress())
                || ObjectUtils.allNotNull(shipmentDTO.getSenderCode(), shipmentDTO.getRecipientCode()) && shipmentDTO.getSenderCode().equals(shipmentDTO.getRecipientCode())) {
            throw new BusinessException("Incorrect shipment locations, sender and recipient can't be the same and only one of recipient or address can be set");
        }

        fieldValidator.validate(shipmentDTO, ShipmentDTO.Fields.shipmentDirection, true);
        var shipmentDirection = Arrays.stream(ShipmentDirection.values())
                .filter(status -> status.name().equalsIgnoreCase(shipmentDTO.getShipmentDirection()))
                .findAny()
                .orElseThrow(() -> new BusinessException("Invalid shipment direction"));

        if (shipmentDirection == ShipmentDirection.INCOMING && shipmentDTO.getRecipientCode() == null) {
            throw new BusinessException("Recipient code is required for incoming shipment");
        } else if (shipmentDirection == ShipmentDirection.OUTCOMING
                && (shipmentDTO.getSenderCode() == null || (shipmentDTO.getRecipientCode() == null && shipmentDTO.getAddress() == null))) {
            throw new BusinessException("Sender code and recipient code or address are required for outgoing shipment");
        }

        shipmentBuilder.shipmentDirection(shipmentDirection);

        return shipmentRepository.save(shipmentBuilder.build());
    }

    public Shipment update(@NotNull(message = "Shipment can't be null") ShipmentDTO shipmentDTO) {
        fieldValidator.validate(shipmentDTO, ShipmentDTO.Fields.id, true);
        Shipment entity = shipmentRepository.findById(shipmentDTO.getId())
                .orElseThrow(() -> new NotFoundException("Shipment with ID '%s' was not found".formatted(shipmentDTO.getId())));

        if (StringUtils.isNoneBlank(shipmentDTO.getSenderCode(), shipmentDTO.getRecipientCode())) {
            fieldValidator.validate(shipmentDTO, true, ShipmentDTO.Fields.senderCode, ShipmentDTO.Fields.recipientCode);
            if (shipmentDTO.getSenderCode().equals(shipmentDTO.getRecipientCode())) {
                throw new BusinessException("Sender and recipient can't be the same");
            }
        }

        var actualSender = entity.getWarehouseIdSender();
        var actualRecipient = entity.getWarehouseIdRecipient();
        var actualAddress = entity.getAddress();

        if (shipmentDTO.getSenderCode() != null) {
            fieldValidator.validate(shipmentDTO, ShipmentDTO.Fields.senderCode, true);
            var warehouseSender = warehouseService.findByCode(shipmentDTO.getSenderCode());
            entity.setWarehouseIdSender(warehouseSender.getId());
            actualSender = warehouseSender.getId();
        }

        if (shipmentDTO.getRecipientCode() != null) {
            fieldValidator.validate(shipmentDTO, ShipmentDTO.Fields.recipientCode, true);
            var warehouseRecipient = warehouseService.findByCode(shipmentDTO.getRecipientCode());
            entity.setWarehouseIdRecipient(warehouseRecipient.getId());
            actualRecipient = warehouseRecipient.getId();
        }

        if (shipmentDTO.getAddress() != null) {
            fieldValidator.validateObject(shipmentDTO, ShipmentDTO.Fields.address, true);
            entity.setAddress(shipmentDTO.getAddress());
            actualAddress = shipmentDTO.getAddress();
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
            fieldValidator.validate(shipmentDTO, ShipmentDTO.Fields.status, true);
            var shipmentStatus = Arrays.stream(ShipmentStatus.values())
                    .filter(status -> status.name().equalsIgnoreCase(shipmentDTO.getStatus()))
                    .findAny()
                    .orElseThrow(() -> new BusinessException("Invalid shipment status"));

            if (!ShipmentStatus.canTransitionTo(entity.getStatus(), shipmentStatus)) {
                throw new BusinessException("Can't transition shipment from '%s' to '%s'".formatted(entity.getStatus(), shipmentStatus));
            }

            entity.setStatus(shipmentStatus);
        }

        if (ObjectUtils.allNotNull(actualRecipient, actualAddress)
                || ObjectUtils.allNotNull(actualSender, actualRecipient) && actualSender.equals(actualRecipient)) {
            throw new BusinessException("Incorrect shipment locations, sender and recipient can't be the same and only one of recipient or address can be set");
        }

        if (!StringUtils.isBlank(shipmentDTO.getShipmentDirection())) {
            fieldValidator.validate(shipmentDTO, ShipmentDTO.Fields.shipmentDirection, true);
            var shipmentDirection = Arrays.stream(ShipmentDirection.values())
                    .filter(status -> status.name().equalsIgnoreCase(shipmentDTO.getShipmentDirection()))
                    .findAny()
                    .orElseThrow(() -> new BusinessException("Invalid shipment direction"));

            if (shipmentDirection == ShipmentDirection.INCOMING && actualRecipient == null) {
                throw new BusinessException("Recipient code is required for incoming shipment");
            } else if (shipmentDirection == ShipmentDirection.OUTCOMING
                    && (actualSender == null || (actualRecipient == null && actualAddress == null))) {
                throw new BusinessException("Sender code and recipient code or address are required for outgoing shipment");
            }

            entity.setShipmentDirection(shipmentDirection);
        }

        return shipmentRepository.save(entity);
    }
}
