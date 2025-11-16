package io.store.ua.service;

import io.store.ua.entity.StockItem;
import io.store.ua.entity.StockItemGroup;
import io.store.ua.enums.StockItemStatus;
import io.store.ua.enums.WebSocketTopic;
import io.store.ua.exceptions.BusinessException;
import io.store.ua.exceptions.NotFoundException;
import io.store.ua.models.data.ItemOutOfStockMessage;
import io.store.ua.models.dto.StockItemDTO;
import io.store.ua.models.dto.StockItemHistoryDTO;
import io.store.ua.repository.StockItemRepository;
import io.store.ua.repository.StorageSectionRepository;
import io.store.ua.utility.SocketService;
import io.store.ua.validations.FieldValidator;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.*;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Validated
public class StockItemService {
    private final StockItemRepository stockItemRepository;
    private final StorageSectionRepository storageSectionRepository;
    private final FieldValidator fieldValidator;
    private final EntityManager entityManager;
    private final StockItemHistoryService stockItemHistoryService;
    private final SocketService socketService;

    public List<StockItem> findBy(List<@NotNull(message = "Warehouse ID can't be null") Long> warehouseIDs,
                                  List<@NotNull(message = "Product ID can't be null") Long> productIDs,
                                  List<@NotNull(message = "Stock item group ID can't be null") Long> stockItemGroupIDs,
                                  List<@NotNull(message = "Status can't be null") String> statuses,
                                  List<@NotNull(message = "Storage section ID can't be null") Long> storageSectionIDs,
                                  Boolean isItemActive,
                                  Boolean isItemGroupActive,
                                  @Min(value = 1, message = "Size of page can't be less than 1") int pageSize,
                                  @Min(value = 1, message = "A page number can't be less than 1") int page) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<StockItem> criteriaQuery = criteriaBuilder.createQuery(StockItem.class);
        Root<StockItem> root = criteriaQuery.from(StockItem.class);

        List<Predicate> predicates = new ArrayList<>();

        if (warehouseIDs != null && !warehouseIDs.isEmpty()) {
            predicates.add(root.get(StockItem.Fields.warehouseId).in(warehouseIDs));
        }

        if (productIDs != null && !productIDs.isEmpty()) {
            predicates.add(root.get(StockItem.Fields.productId).in(productIDs));
        }

        if (stockItemGroupIDs != null && !stockItemGroupIDs.isEmpty()) {
            predicates.add(root.get(StockItem.Fields.stockItemGroupId).in(stockItemGroupIDs));
        }

        if (statuses != null && !statuses.isEmpty()) {
            try {
                predicates.add(root.get(StockItem.Fields.status).in(statuses.stream().map(status ->
                        StockItemStatus.valueOf(status.toUpperCase())).toList()));
            } catch (IllegalArgumentException e) {
                throw new BusinessException("Invalid stock item status");
            }
        }

        if (storageSectionIDs != null && !storageSectionIDs.isEmpty()) {
            predicates.add(root.get(StockItem.Fields.storageSectionId).in(storageSectionIDs));
        }

        if (isItemActive != null) {
            predicates.add(criteriaBuilder.equal(root.get(StockItem.Fields.isActive), isItemActive));
        }

        if (isItemGroupActive != null) {
            Join<StockItem, StockItemGroup> groupJoin = root.join(StockItem.Fields.stockItemGroup, JoinType.INNER);
            predicates.add(criteriaBuilder.equal(groupJoin.get(StockItemGroup.Fields.isActive), isItemGroupActive));
        }

        criteriaQuery
                .select(root)
                .where(predicates.toArray(new Predicate[0]))
                .orderBy(criteriaBuilder.asc(root.get(StockItem.Fields.id)));


        return entityManager.createQuery(criteriaQuery)
                .setFirstResult((page - 1) * pageSize)
                .setMaxResults(pageSize)
                .getResultList();
    }

    public StockItem findById(@NotNull(message = "Stock item ID can't be null") Long ID) {
        return stockItemRepository.findById(ID)
                .orElseThrow(() -> new NotFoundException("StockItem with ID '%s' was not found".formatted(ID)));
    }

    public StockItem create(@NotNull(message = "StockItem payload can't be null") StockItemDTO stockItemDTO) {
        fieldValidator.validate(stockItemDTO, true,
                StockItem.Fields.productId,
                StockItem.Fields.stockItemGroupId,
                StockItem.Fields.warehouseId,
                StockItem.Fields.availableQuantity);
        fieldValidator.validate(stockItemDTO, false,
                StockItemDTO.Fields.expiryDate,
                StockItemDTO.Fields.isActive,
                StockItemDTO.Fields.storageSectionId);

        return stockItemRepository.save(StockItem.builder()
                .productId(stockItemDTO.getProductId())
                .stockItemGroupId(stockItemDTO.getStockItemGroupId())
                .warehouseId(stockItemDTO.getWarehouseId())
                .expiryDate(stockItemDTO.getExpiryDate())
                .availableQuantity(stockItemDTO.getAvailableQuantity())
                .status(stockItemDTO.isSwitchOff() ? StockItemStatus.OUT_OF_SERVICE
                        : determineStatus(stockItemDTO.getAvailableQuantity()))
                .isActive(stockItemDTO.getIsActive() == null || stockItemDTO.getIsActive())
                .storageSectionId(stockItemDTO.getStorageSectionId())
                .build());
    }

    @Transactional
    public StockItem update(@NotNull(message = "Stock item can't be null") StockItemDTO stockItemDTO) {
        fieldValidator.validate(stockItemDTO, StockItemDTO.Fields.stockItemId, true);
        StockItem current = stockItemRepository.findById(stockItemDTO.getStockItemId())
                .orElseThrow(() -> new NotFoundException("StockItem with ID '%s' was not found".formatted(stockItemDTO.getStockItemId())));

        StockItemHistoryDTO.StockItemHistoryDTOBuilder stockItemHistoryDTOBuilder = StockItemHistoryDTO.builder();
        stockItemHistoryDTOBuilder.stockItemId(stockItemDTO.getStockItemId());
        boolean outOfStock = false;

        if (stockItemDTO.getStockItemGroupId() != null) {
            fieldValidator.validate(stockItemDTO, StockItemDTO.Fields.stockItemGroupId, true);

            stockItemHistoryDTOBuilder.oldStockItemGroupId(current.getStockItemGroupId());
            stockItemHistoryDTOBuilder.newStockItemGroupId(stockItemDTO.getStockItemGroupId());

            current.setStockItemGroupId(stockItemDTO.getStockItemGroupId());
        }

        if (stockItemDTO.getWarehouseId() != null) {
            fieldValidator.validate(stockItemDTO, StockItemDTO.Fields.warehouseId, true);

            stockItemHistoryDTOBuilder.oldWarehouseId(current.getWarehouseId());
            stockItemHistoryDTOBuilder.newWarehouseId(stockItemDTO.getWarehouseId());

            current.setWarehouseId(stockItemDTO.getWarehouseId());
        }

        if (stockItemDTO.getExpiryDate() != null) {
            fieldValidator.validate(stockItemDTO, StockItemDTO.Fields.expiryDate, true);

            stockItemHistoryDTOBuilder.oldExpiration(current.getExpiryDate());
            stockItemHistoryDTOBuilder.newExpiration(stockItemDTO.getExpiryDate());
            stockItemHistoryDTOBuilder.changeExpiration(true);

            current.setExpiryDate(stockItemDTO.getExpiryDate());
        } else if (stockItemDTO.isNullifyExpiration()) {
            stockItemHistoryDTOBuilder.oldExpiration(current.getExpiryDate());
            stockItemHistoryDTOBuilder.newExpiration(null);
            stockItemHistoryDTOBuilder.changeExpiration(true);

            current.setExpiryDate(null);
        }

        if (current.getStorageSectionId() != null
                && !storageSectionRepository.existsByIdAndWarehouseId(current.getStorageSectionId(), current.getWarehouseId())) {
            throw new BusinessException("Storage section with ID '%s' does not belong to warehouse with ID '%s'"
                    .formatted(current.getStorageSectionId(), current.getWarehouseId()));
        }

        if (stockItemDTO.getAvailableQuantity() != null) {
            fieldValidator.validate(stockItemDTO, StockItemDTO.Fields.availableQuantity, true);

            stockItemHistoryDTOBuilder.quantityBefore(current.getAvailableQuantity());
            stockItemHistoryDTOBuilder.quantityAfter(stockItemDTO.getAvailableQuantity());

            current.setAvailableQuantity(stockItemDTO.getAvailableQuantity());

            if (current.getAvailableQuantity().compareTo(BigInteger.ZERO) == 0 && current.getStatus() == StockItemStatus.AVAILABLE) {
                current.setStatus(StockItemStatus.OUT_OF_STOCK);
                stockItemHistoryDTOBuilder.oldStatus(current.getStatus().name());
                stockItemHistoryDTOBuilder.newStatus(StockItemStatus.OUT_OF_STOCK.name());
                outOfStock = true;
            }
        }

        if (stockItemDTO.getIsActive() != null) {
            fieldValidator.validate(stockItemDTO, StockItemDTO.Fields.isActive, true);

            stockItemHistoryDTOBuilder.oldActivity(current.getIsActive());
            stockItemHistoryDTOBuilder.newActivity(stockItemDTO.getIsActive());

            current.setIsActive(stockItemDTO.getIsActive());
        }

        if (stockItemDTO.getStorageSectionId() != null) {
            fieldValidator.validate(stockItemDTO, StockItemDTO.Fields.storageSectionId, true);

            stockItemHistoryDTOBuilder.oldSectionId(current.getStorageSectionId());
            stockItemHistoryDTOBuilder.newSectionId(stockItemDTO.getStorageSectionId());
            stockItemHistoryDTOBuilder.changeSection(true);

            current.setStorageSectionId(stockItemDTO.getStorageSectionId());
        } else if (stockItemDTO.isNullifySection()) {
            stockItemHistoryDTOBuilder.oldSectionId(current.getStorageSectionId());
            stockItemHistoryDTOBuilder.newSectionId(null);
            stockItemHistoryDTOBuilder.changeSection(true);

            current.setStorageSectionId(null);
        }

        if (stockItemDTO.isSwitchOff() && current.getStatus() != StockItemStatus.OUT_OF_SERVICE) {
            stockItemHistoryDTOBuilder
                    .oldStatus(current.getStatus().name())
                    .newStatus(StockItemStatus.OUT_OF_SERVICE.name());

            current.setStatus(StockItemStatus.OUT_OF_SERVICE);
        }

        if (!stockItemDTO.isSwitchOff()) {
            stockItemHistoryDTOBuilder
                    .oldStatus(current.getStatus().name())
                    .newStatus(determineStatus(current.getAvailableQuantity()).name());

            current.setStatus(determineStatus(current.getAvailableQuantity()));
        }

        stockItemHistoryService.save(stockItemHistoryDTOBuilder.build());

        var stockItem = stockItemRepository.save(current);

        if (outOfStock) {
            socketService.pushToTopic(WebSocketTopic.STOCK_ITEM_OUT_OF_STOCK.getTopic(), ItemOutOfStockMessage.builder()
                    .stockItemId(stockItem.getId())
                    .message("Stock item with ID '%s' is out of stock".formatted(stockItem.getId()))
                    .build());
        }

        return stockItem;
    }

    private StockItemStatus determineStatus(BigInteger availableQuantity) {
        if (availableQuantity.compareTo(BigInteger.ZERO) == 0) {
            return StockItemStatus.OUT_OF_STOCK;
        } else {
            return StockItemStatus.AVAILABLE;
        }
    }
}
