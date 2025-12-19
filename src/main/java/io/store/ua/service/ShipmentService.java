package io.store.ua.service;

import io.store.ua.entity.Shipment;
import io.store.ua.entity.StockItem;
import io.store.ua.enums.ShipmentDirection;
import io.store.ua.enums.ShipmentStatus;
import io.store.ua.exceptions.BusinessException;
import io.store.ua.exceptions.NotFoundException;
import io.store.ua.models.dto.ShipmentDTO;
import io.store.ua.models.dto.StockItemDTO;
import io.store.ua.repository.ShipmentRepository;
import io.store.ua.repository.WarehouseRepository;
import io.store.ua.utility.CodeGenerator;
import io.store.ua.validations.FieldValidator;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.transaction.Transactional;
import jakarta.validation.ValidationException;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Validated
public class ShipmentService {
    private final ShipmentRepository shipmentRepository;
    private final StockItemService stockItemService;
    private final FieldValidator fieldValidator;
    private final EntityManager entityManager;
    private final WarehouseRepository warehouseRepository;

    private static <E extends Enum<E>> E parseEnumOrThrow(String value, Class<E> type, String fieldName) {
        try {
            return Enum.valueOf(type, value.trim().toUpperCase());
        } catch (Exception e) {
            StringBuilder allowed = new StringBuilder();

            for (E v : type.getEnumConstants()) {
                if (!allowed.isEmpty()) {
                    allowed.append(", ");
                }

                allowed.append(v.name());
            }

            throw new ValidationException("Invalid %s '%s'. Allowed values: [%s]".formatted(fieldName, value, allowed));
        }
    }

    public List<Shipment> findBy(Long warehouseIdSender,
                                 Long warehouseIdRecipient,
                                 Long stockItemId,
                                 String status,
                                 String shipmentDirection,
                                 LocalDateTime from,
                                 LocalDateTime to,
                                 @Min(value = 1, message = "Size of page can't be less than 1") int pageSize,
                                 @Min(value = 1, message = "A page number can't be less than 1") int page) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Shipment> criteriaQuery = criteriaBuilder.createQuery(Shipment.class);
        Root<Shipment> shipmentRoot = criteriaQuery.from(Shipment.class);
        List<Predicate> predicates = new ArrayList<>();

        if (warehouseIdSender != null) {
            predicates.add(criteriaBuilder.equal(shipmentRoot.get(Shipment.Fields.warehouseIdSender), warehouseIdSender));
        }

        if (warehouseIdRecipient != null) {
            predicates.add(criteriaBuilder.equal(shipmentRoot.get(Shipment.Fields.warehouseIdRecipient), warehouseIdRecipient));
        }

        if (stockItemId != null) {
            predicates.add(criteriaBuilder.equal(shipmentRoot.get(Shipment.Fields.stockItemId), stockItemId));
        }

        if (status != null) {
            ShipmentStatus shipmentStatus = parseEnumOrThrow(status, ShipmentStatus.class, Shipment.Fields.status);
            predicates.add(criteriaBuilder.equal(shipmentRoot.get(Shipment.Fields.status), shipmentStatus));
        }

        if (shipmentDirection != null) {
            ShipmentDirection direction = parseEnumOrThrow(shipmentDirection, ShipmentDirection.class, Shipment.Fields.shipmentDirection);
            predicates.add(criteriaBuilder.equal(shipmentRoot.get(Shipment.Fields.shipmentDirection), direction));
        }

        if (from != null) {
            predicates.add(criteriaBuilder.greaterThanOrEqualTo(shipmentRoot.get(Shipment.Fields.createdAt), from));
        }

        if (to != null) {
            predicates.add(criteriaBuilder.lessThanOrEqualTo(shipmentRoot.get(Shipment.Fields.createdAt), to));
        }

        criteriaQuery
                .select(shipmentRoot)
                .where(predicates.toArray(new Predicate[0]))
                .orderBy(criteriaBuilder.desc(shipmentRoot.get(Shipment.Fields.createdAt)));

        return entityManager.createQuery(criteriaQuery)
                .setFirstResult((page - 1) * pageSize)
                .setMaxResults(pageSize)
                .getResultList();
    }

    @Transactional
    public Shipment save(@NotNull(message = "Shipment can't be null") ShipmentDTO shipmentDTO) {
        if (ObjectUtils.allNull(shipmentDTO.getSenderCode(), shipmentDTO.getRecipientCode(), shipmentDTO.getAddress())) {
            throw new BusinessException("Sender, recipient code and address can't be null at the same time, unknown where to or where from to send shipment");
        }

        var stockItem = stockItemService.findById(shipmentDTO.getStockItemId());

        Shipment.ShipmentBuilder shipmentBuilder = Shipment.builder();
        shipmentBuilder.code(CodeGenerator.ShipmentCodeGenerator.generate());
        shipmentBuilder.initiatorId(UserService.getCurrentlyAuthenticatedUserID());

        fieldValidator.validate(shipmentDTO, ShipmentDTO.Fields.stockItemId, true);
        shipmentBuilder.stockItemId(shipmentDTO.getStockItemId());

        fieldValidator.validate(shipmentDTO, ShipmentDTO.Fields.stockItemQuantity, true);
        shipmentBuilder.stockItemQuantity(shipmentDTO.getStockItemQuantity());

        if (StringUtils.isNoneBlank(shipmentDTO.getSenderCode(), shipmentDTO.getRecipientCode())) {
            fieldValidator.validate(shipmentDTO, true, ShipmentDTO.Fields.senderCode, ShipmentDTO.Fields.recipientCode);

            if (shipmentDTO.getSenderCode().equals(shipmentDTO.getRecipientCode())) {
                throw new BusinessException("Sender and recipient can't be the same");
            }
        }

        if (shipmentDTO.getSenderCode() != null) {
            fieldValidator.validate(shipmentDTO, ShipmentDTO.Fields.senderCode, true);
            var warehouse = warehouseRepository.findByCode(shipmentDTO.getSenderCode())
                    .orElseThrow(() -> new NotFoundException("Warehouse with code '%s' was not found".formatted(shipmentDTO.getSenderCode())));
            shipmentBuilder.warehouseIdSender(warehouse.getId());

            if (!Objects.equals(stockItem.getWarehouseId(), warehouse.getId())) {
                throw new BusinessException("Stock item '%s' does not belong to the sender warehouse '%s'".formatted(stockItem.getCode(), warehouse.getCode()));
            }
        }

        if (shipmentDTO.getRecipientCode() != null) {
            fieldValidator.validate(shipmentDTO, ShipmentDTO.Fields.recipientCode, true);
            var warehouse = warehouseRepository.findByCode(shipmentDTO.getRecipientCode())
                    .orElseThrow(() -> new NotFoundException("Warehouse with code '%s' was not found".formatted(shipmentDTO.getRecipientCode())));
            shipmentBuilder.warehouseIdRecipient(warehouse.getId());
        }

        if (shipmentDTO.getAddress() != null) {
            fieldValidator.validateObject(shipmentDTO, ShipmentDTO.Fields.address, true);
            shipmentBuilder.address(shipmentDTO.getAddress());
        }

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
                || ObjectUtils.allNotNull(shipmentDTO.getSenderCode(), shipmentDTO.getRecipientCode())
                && shipmentDTO.getSenderCode().equals(shipmentDTO.getRecipientCode())) {
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

        processStockItemChange(shipmentBuilder.build());

        return shipmentRepository.save(shipmentBuilder.build());
    }

    @Transactional
    public Shipment synchroniseShipment(@NotNull(message = "Shipment can't be null") ShipmentDTO shipmentDTO) {
        fieldValidator.validate(shipmentDTO, ShipmentDTO.Fields.status, true);
        var shipmentStatus = Arrays.stream(ShipmentStatus.values())
                .filter(status -> status.name().equalsIgnoreCase(shipmentDTO.getStatus()))
                .findAny()
                .orElseThrow(() -> new BusinessException("Invalid shipment status"));

        fieldValidator.validate(shipmentDTO, ShipmentDTO.Fields.id, true);
        Shipment entity = shipmentRepository.findById(shipmentDTO.getId())
                .orElseThrow(() -> new NotFoundException("Shipment with ID '%s' was not found".formatted(shipmentDTO.getId())));

        if (entity.getStatus() != ShipmentStatus.PLANNED || shipmentStatus == ShipmentStatus.PLANNED) {
            throw new BusinessException("Only planned shipment can be synchronised after reconnecting, new status can't be the same as initial");
        }

        return update(shipmentDTO);
    }

    @Transactional
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
            var warehouseSender = warehouseRepository.findByCode(shipmentDTO.getSenderCode())
                    .orElseThrow(() -> new NotFoundException("Warehouse with code '%s' was not found".formatted(shipmentDTO.getSenderCode())));
            entity.setWarehouseIdSender(warehouseSender.getId());
            actualSender = warehouseSender.getId();
        }

        if (shipmentDTO.getRecipientCode() != null) {
            fieldValidator.validate(shipmentDTO, ShipmentDTO.Fields.recipientCode, true);
            var warehouseRecipient = warehouseRepository.findByCode(shipmentDTO.getRecipientCode())
                    .orElseThrow(() -> new NotFoundException("Warehouse with code '%s' was not found".formatted(shipmentDTO.getRecipientCode())));
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

            if (shipmentStatus != entity.getStatus()) {
                if (!ShipmentStatus.canTransitionTo(entity.getStatus(), shipmentStatus)) {
                    throw new BusinessException("Can't transition shipment from '%s' to '%s'".formatted(entity.getStatus(), shipmentStatus));
                }

                entity.setStatus(shipmentStatus);
            }
        }

        if (ObjectUtils.allNotNull(actualRecipient, actualAddress)
                || (ObjectUtils.allNotNull(actualSender, actualRecipient)
                && actualSender.equals(actualRecipient))) {
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

        processStockItemChange(entity);

        return shipmentRepository.save(entity);
    }

    private void processStockItemChange(Shipment shipment) {
        StockItem stockItem = stockItemService.findById(shipment.getStockItemId());
        BigInteger shipmentQuantity = BigInteger.valueOf(shipment.getStockItemQuantity());

        if (shipment.getStatus() == ShipmentStatus.SENT && shipment.getShipmentDirection() == ShipmentDirection.OUTCOMING) {
            if (stockItem.getAvailableQuantity().compareTo(shipmentQuantity) < 0) {
                throw new BusinessException("Shipment quantity can't be greater than available quantity");
            }

            StockItemDTO updateSender = StockItemDTO.builder()
                    .stockItemId(stockItem.getId())
                    .availableQuantity(stockItem.getAvailableQuantity().subtract(shipmentQuantity))
                    .build();

            stockItemService.update(updateSender);
        } else if (shipment.getStatus() == ShipmentStatus.DELIVERED) {
            if (shipment.getWarehouseIdRecipient() != null) {
                StockItemDTO stockItemDTO = StockItemDTO.builder()
                        .productId(stockItem.getProductId())
                        .stockItemGroupId(stockItem.getStockItemGroupId())
                        .warehouseId(shipment.getWarehouseIdRecipient())
                        .availableQuantity(shipmentQuantity)
                        .isActive(true)
                        .build();

                stockItemService.create(stockItemDTO);
            }
        } else if (shipment.getStatus() == ShipmentStatus.ROLLBACK && shipment.getShipmentDirection() == ShipmentDirection.OUTCOMING) {
            StockItemDTO updateSender = StockItemDTO.builder()
                    .stockItemId(stockItem.getId())
                    .availableQuantity(stockItem.getAvailableQuantity().add(shipmentQuantity))
                    .build();

            stockItemService.update(updateSender);
        }
    }
}
