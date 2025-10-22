package io.store.ua.service;

import io.store.ua.entity.StockItem;
import io.store.ua.exceptions.BusinessException;
import io.store.ua.exceptions.NotFoundException;
import io.store.ua.models.dto.StockItemDTO;
import io.store.ua.repository.StockItemRepository;
import io.store.ua.validations.FieldValidator;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Validated
@PreAuthorize("isAuthenticated()")
public class StockItemService {
    private final StockItemRepository stockItemRepository;
    private final FieldValidator fieldValidator;
    private final EntityManager entityManager;

    public List<StockItem> findAll(@Min(value = 1, message = "Size of page can't be less than 1") int pageSize,
                                   @Min(value = 1, message = "A page number can't be less than 1") int page) {
        return stockItemRepository.findAll(Pageable.ofSize(pageSize).withPage(page - 1)).getContent();
    }

    public List<StockItem> findBy(List<@NotNull(message = "Warehouse ID can't be null") Long> warehouseIDs,
                                  List<@NotNull(message = "Product ID can't be null") Long> productIDs,
                                  List<@NotNull(message = "Stock item group ID can't be null") Long> stockItemGroupIDs,
                                  List<@NotNull(message = "Status can't be null") String> statuses,
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
                        StockItem.Status.valueOf(status.toUpperCase())).toList()));
            } catch (IllegalArgumentException e) {
                throw new BusinessException("Invalid stock item status");
            }
        }

        criteriaQuery.select(root);

        if (!predicates.isEmpty()) {
            criteriaQuery.where(criteriaBuilder.and(predicates.toArray(Predicate[]::new)));
        }

        criteriaQuery.orderBy(criteriaBuilder.desc(root.get(StockItem.Fields.id)));

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
                StockItem.Fields.availableQuantity,
                StockItem.Fields.reservedQuantity);

        return stockItemRepository.save(StockItem.builder()
                .productId(stockItemDTO.getProductId())
                .stockItemGroupId(stockItemDTO.getStockItemGroupId())
                .warehouseId(stockItemDTO.getWarehouseId())
                .expiryDate(stockItemDTO.getExpiryDate())
                .availableQuantity(stockItemDTO.getAvailableQuantity())
                .reservedQuantity(stockItemDTO.getReservedQuantity())
                .status(determineStatus(stockItemDTO.getAvailableQuantity().longValue(), stockItemDTO.getReservedQuantity().longValue()))
                .build());
    }

    public StockItem update(@NotNull(message = "Stock item can't be null") StockItemDTO stockItemDTO) {
        fieldValidator.validate(stockItemDTO, StockItemDTO.Fields.id, true);
        StockItem current = stockItemRepository.findById(stockItemDTO.getId())
                .orElseThrow(() -> new NotFoundException("StockItem with ID '%s' was not found".formatted(stockItemDTO.getId())));

        if (stockItemDTO.getStockItemGroupId() != null) {
            fieldValidator.validate(stockItemDTO, StockItemDTO.Fields.stockItemGroupId, true);
            current.setStockItemGroupId(stockItemDTO.getStockItemGroupId());
        }

        if (stockItemDTO.getWarehouseId() != null) {
            fieldValidator.validate(stockItemDTO, StockItemDTO.Fields.warehouseId, true);
            current.setWarehouseId(stockItemDTO.getWarehouseId());
        }

        if (stockItemDTO.getExpiryDate() != null) {
            fieldValidator.validate(stockItemDTO, StockItemDTO.Fields.expiryDate, true);
            current.setExpiryDate(stockItemDTO.getExpiryDate());
        }

        if (stockItemDTO.getAvailableQuantity() != null) {
            fieldValidator.validate(stockItemDTO, StockItemDTO.Fields.availableQuantity, true);
            current.setAvailableQuantity(stockItemDTO.getAvailableQuantity());

        }

        if (stockItemDTO.getReservedQuantity() != null) {
            fieldValidator.validate(stockItemDTO, StockItemDTO.Fields.reservedQuantity, true);
            current.setReservedQuantity(stockItemDTO.getReservedQuantity());
        }

        StockItem.Status requestedStatus = null;

        if (stockItemDTO.getStatus() != null) {
            fieldValidator.validate(stockItemDTO, StockItemDTO.Fields.status, true);
            try {
                requestedStatus = StockItem.Status.valueOf(stockItemDTO.getStatus().trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new BusinessException("Invalid stock item status");
            }
        }

        if (requestedStatus == StockItem.Status.OUT_OF_SERVICE) {
            current.setStatus(StockItem.Status.OUT_OF_SERVICE);
        } else {
            current.setStatus(determineStatus(
                    current.getAvailableQuantity().longValue(),
                    current.getReservedQuantity().longValue()
            ));
        }

        return stockItemRepository.save(current);
    }

    private StockItem.Status determineStatus(long availableQuantity, long reservedQuantity) {
        if (availableQuantity == 0 && reservedQuantity == 0) {
            return StockItem.Status.OUT_OF_STOCK;
        } else if (availableQuantity == 0) {
            return StockItem.Status.RESERVED;
        } else {
            return StockItem.Status.AVAILABLE;
        }
    }
}
