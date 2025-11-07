package io.store.ua.service;

import io.store.ua.entity.immutable.StockItemHistory;
import io.store.ua.enums.StockItemStatus;
import io.store.ua.exceptions.NotFoundException;
import io.store.ua.models.dto.StockItemHistoryDTO;
import io.store.ua.repository.StockItemHistoryRepository;
import io.store.ua.repository.StockItemRepository;
import io.store.ua.validations.FieldValidator;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.validation.ValidationException;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Validated
@PreAuthorize("isAuthenticated()")
public class StockItemHistoryService {
    private final StockItemHistoryRepository stockItemHistoryRepository;
    private final StockItemRepository stockItemRepository;
    private final FieldValidator fieldValidator;
    private final EntityManager entityManager;

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

    public List<StockItemHistory> findBy(Long stockItemId,
                                         LocalDateTime from,
                                         LocalDateTime to,
                                         @Min(value = 1, message = "Size of page can't be less than 1") int pageSize,
                                         @Min(value = 1, message = "A page number can't be less than 1") int page) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<StockItemHistory> criteriaQuery = criteriaBuilder.createQuery(StockItemHistory.class);
        Root<StockItemHistory> root = criteriaQuery.from(StockItemHistory.class);

        List<Predicate> predicates = new ArrayList<>();

        if (stockItemId != null) {
            predicates.add(criteriaBuilder.equal(root.get(StockItemHistory.Fields.stockItemId), stockItemId));
        }

        if (from != null) {
            predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get(StockItemHistory.Fields.oldExpiration), from.toLocalDate()));
        }

        if (to != null) {
            predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get(StockItemHistory.Fields.newExpiration), to.toLocalDate()));
        }

        criteriaQuery.where(predicates.toArray(Predicate[]::new));
        criteriaQuery.orderBy(criteriaBuilder.desc(root.get(StockItemHistory.Fields.id)));

        criteriaQuery.select(root).where(criteriaBuilder.and(predicates.toArray(new Predicate[0])));

        return entityManager.createQuery(criteriaQuery)
                .setFirstResult((page - 1) * pageSize)
                .setMaxResults(pageSize)
                .getResultList();
    }

    public StockItemHistory save(@NotNull StockItemHistoryDTO stockItemHistoryDTO) {
        fieldValidator.validate(stockItemHistoryDTO, StockItemHistoryDTO.Fields.stockItemId, true);
        if (!stockItemRepository.existsById(stockItemHistoryDTO.getStockItemId())) {
            throw new NotFoundException("Stock item with ID '%s' was not found".formatted(stockItemHistoryDTO.getStockItemId()));
        }

        StockItemHistory.StockItemHistoryBuilder stockItemHistoryBuilder = StockItemHistory.builder();
        stockItemHistoryBuilder.stockItemId(stockItemHistoryDTO.getStockItemId());

        if (stockItemHistoryDTO.getOldWarehouseId() != null && stockItemHistoryDTO.getNewWarehouseId() != null) {
            fieldValidator.validate(stockItemHistoryDTO, true, StockItemHistoryDTO.Fields.oldWarehouseId, StockItemHistoryDTO.Fields.newWarehouseId);
            stockItemHistoryBuilder
                    .oldWarehouseId(stockItemHistoryDTO.getOldWarehouseId())
                    .newWarehouseId(stockItemHistoryDTO.getNewWarehouseId());
        }

        if (stockItemHistoryDTO.getQuantityBefore() != null && stockItemHistoryDTO.getQuantityAfter() != null && stockItemHistoryDTO.getQuantityAfter().compareTo(stockItemHistoryDTO.getQuantityBefore()) != 0) {
            fieldValidator.validate(stockItemHistoryDTO, true, StockItemHistoryDTO.Fields.quantityBefore, StockItemHistoryDTO.Fields.quantityAfter);
            stockItemHistoryBuilder
                    .quantityBefore(stockItemHistoryDTO.getQuantityBefore())
                    .quantityAfter(stockItemHistoryDTO.getQuantityAfter());
        }

        if (stockItemHistoryDTO.getOldExpiration() != null && stockItemHistoryDTO.getNewExpiration() != null) {
            fieldValidator.validate(stockItemHistoryDTO, true, StockItemHistoryDTO.Fields.oldExpiration, StockItemHistoryDTO.Fields.newExpiration);
            stockItemHistoryBuilder
                    .oldExpiration(stockItemHistoryDTO.getOldExpiration())
                    .newExpiration(stockItemHistoryDTO.getNewExpiration());
        }

        if (stockItemHistoryDTO.getOldStatus() != null && stockItemHistoryDTO.getNewStatus() != null) {
            fieldValidator.validate(stockItemHistoryDTO, true, StockItemHistoryDTO.Fields.oldStatus, StockItemHistoryDTO.Fields.newStatus);
            stockItemHistoryBuilder
                    .oldStatus(parseEnumOrThrow(stockItemHistoryDTO.getOldStatus(), StockItemStatus.class, StockItemHistoryDTO.Fields.oldStatus))
                    .newStatus(parseEnumOrThrow(stockItemHistoryDTO.getNewStatus(), StockItemStatus.class, StockItemHistoryDTO.Fields.newStatus));
        }

        if (stockItemHistoryDTO.getOldActivity() != null && stockItemHistoryDTO.getNewActivity() != null) {
            fieldValidator.validate(stockItemHistoryDTO, StockItemHistoryDTO.Fields.oldActivity, true);
            stockItemHistoryBuilder
                    .oldActivity(stockItemHistoryDTO.getOldActivity())
                    .newActivity(stockItemHistoryDTO.getNewActivity());
        }

        return stockItemHistoryRepository.save(stockItemHistoryBuilder.build());
    }
}
