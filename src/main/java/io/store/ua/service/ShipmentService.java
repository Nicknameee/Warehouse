package io.store.ua.service;

import io.store.ua.entity.Shipment;
import io.store.ua.exceptions.BusinessException;
import io.store.ua.models.dto.ShipmentDTO;
import io.store.ua.repository.ShipmentRepository;
import io.store.ua.validations.FieldValidator;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldNameConstants;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.Arrays;

@Service
@RequiredArgsConstructor
@Validated
@FieldNameConstants
public class ShipmentService {
    private final ShipmentRepository shipmentRepository;
    private final FieldValidator fieldValidator;
    private final WarehouseService warehouseService;

    public Shipment findById(@NotNull(message = "Shipment ID can't be null") Long id) {
        return shipmentRepository.findById(id)
                .orElse(null);
    }

    public Shipment save(@NotNull(message = "Shipment can't be null") ShipmentDTO shipmentDTO) {
        Shipment.ShipmentBuilder shipmentBuilder = Shipment.builder();

        var user = RegularUserService.getCurrentlyAuthenticatedUser()
                .orElseThrow(() -> new BusinessException("User is not authenticated"));
        shipmentBuilder.initiatorId(user.getId());

        fieldValidator.validate(shipmentDTO, ShipmentDTO.Fields.senderCode, true);
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

        fieldValidator.validate(shipmentDTO, ShipmentDTO.Fields.stockItemAmount, true);
        shipmentBuilder.stockItemAmount(shipmentDTO.getStockItemAmount());

        if (!StringUtils.isBlank(shipmentDTO.getStatus())) {
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
        Shipment entity = shipmentRepository.findById(shipmentDTO.getId())
                .orElseThrow(() -> new BusinessException("Shipment with ID '%s' was not found".formatted(shipmentDTO.getId())));

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

        if (shipmentDTO.getStockItemAmount() != null) {
            fieldValidator.validate(shipmentDTO, ShipmentDTO.Fields.stockItemAmount, true);
            entity.setStockItemAmount(shipmentDTO.getStockItemAmount());
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
