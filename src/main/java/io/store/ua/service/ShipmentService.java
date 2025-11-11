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
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
@Validated
public class ShipmentService {
    private final ShipmentRepository shipmentRepository;
    private final StockItemService stockItemService;
    private final FieldValidator fieldValidator;
    private final EntityManager entityManager;
    private final WarehouseService warehouseService;

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

    public List<Shipment> findAll(@Min(value = 1, message = "Size of page can't be less than 1") int pageSize,
                                  @Min(value = 1, message = "A page number can't be less than 1") int page) {
        return shipmentRepository.findAll(Pageable.ofSize(pageSize).withPage(page - 1)).getContent();
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

        criteriaQuery.select(shipmentRoot)
                .where(predicates.toArray(new Predicate[0]))
                .orderBy(criteriaBuilder.desc(shipmentRoot.get(Shipment.Fields.createdAt)));

        return entityManager.createQuery(criteriaQuery)
                .setFirstResult((page - 1) * pageSize)
                .setMaxResults(pageSize)
                .getResultList();
    }

    public Shipment findById(@NotNull(message = "Shipment ID can't be null") Long shipmentId) {
        return shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new NotFoundException("Shipment with ID '%s' was not found".formatted(shipmentId)));
    }

    @Transactional
    public Shipment save(@NotNull(message = "Shipment can't be null") ShipmentDTO shipmentDTO) {
        if (ObjectUtils.allNull(shipmentDTO.getSenderCode(), shipmentDTO.getRecipientCode(), shipmentDTO.getAddress())) {
            throw new BusinessException("Sender, recipient code and address can't be null at the same time, unknown where to or where from to send shipment");
        }

        Shipment.ShipmentBuilder shipmentBuilder = Shipment.builder();
        shipmentBuilder.code(CodeGenerator.ShipmentCodeGenerator.generate());
        shipmentBuilder.initiatorId(UserService.getCurrentlyAuthenticatedUserID());

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
            throw new BusinessException("Only planned shipment can be synchronised after reconnecting");
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

            if (shipmentStatus != entity.getStatus()) {
                if (!ShipmentStatus.canTransitionTo(entity.getStatus(), shipmentStatus)) {
                    throw new BusinessException("Can't transition shipment from '%s' to '%s'".formatted(entity.getStatus(), shipmentStatus));
                }

                entity.setStatus(shipmentStatus);
            }
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

        processStockItemChange(entity);

        return shipmentRepository.save(entity);
    }

    private void processStockItemChange(Shipment shipment) {
        StockItem stockItem = stockItemService.findById(shipment.getStockItemId());
        BigInteger shipmentQuantity = BigInteger.valueOf(shipment.getStockItemQuantity());

        if (shipment.getStatus() == ShipmentStatus.SENT) {
            if (stockItem.getAvailableQuantity().compareTo(shipmentQuantity) < 0) {
                throw new BusinessException("Shipment quantity can't be greater than available quantity");
            }
            StockItemDTO updateSender = StockItemDTO.builder()
                    .stockItemId(stockItem.getId())
                    .availableQuantity(stockItem.getAvailableQuantity().subtract(shipmentQuantity))
                    .build();
            stockItemService.update(updateSender);
        } else if (shipment.getStatus() == ShipmentStatus.DELIVERED && shipment.getWarehouseIdRecipient() != null) {
            List<StockItem> recipientItems = stockItemService.findBy(
                    List.of(shipment.getWarehouseIdRecipient()),
                    List.of(stockItem.getProductId()),
                    null, null, null, null, null,
                    1, 1
            );
            if (recipientItems.isEmpty()) {
                StockItemDTO createRecipient = StockItemDTO.builder()
                        .productId(stockItem.getProductId())
                        .stockItemGroupId(stockItem.getStockItemGroupId())
                        .warehouseId(shipment.getWarehouseIdRecipient())
                        .availableQuantity(shipmentQuantity)
                        .isActive(true)
                        .build();
                stockItemService.create(createRecipient);
            } else {
                StockItem recipientItem = recipientItems.getFirst();
                StockItemDTO updateRecipient = StockItemDTO.builder()
                        .stockItemId(recipientItem.getId())
                        .availableQuantity(recipientItem.getAvailableQuantity().add(shipmentQuantity))
                        .build();
                stockItemService.update(updateRecipient);
            }
        }
    }
}
