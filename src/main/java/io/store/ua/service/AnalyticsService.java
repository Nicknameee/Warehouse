package io.store.ua.service;

import io.store.ua.entity.immutable.StockItemHistory;
import io.store.ua.models.data.ItemSellingStatistic;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.*;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.math.BigInteger;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Validated
@PreAuthorize("isAuthenticated()")
public class AnalyticsService {
    private final EntityManager entityManager;

    public List<ItemSellingStatistic> fetchItemSellingStatistic(Long stockItemId,
                                                                LocalDate from,
                                                                LocalDate to,
                                                                @Min(value = 1, message = "Size of page can't be less than 1") int pageSize,
                                                                @Min(value = 1, message = "A page number can't be less than 1") int page) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<ItemSellingStatistic> criteriaQuery = criteriaBuilder.createQuery(ItemSellingStatistic.class);
        Root<StockItemHistory> root = criteriaQuery.from(StockItemHistory.class);

        Expression<BigInteger> quantityBefore = criteriaBuilder.<BigInteger>coalesce()
                .value(root.get(StockItemHistory.Fields.quantityBefore)).value(BigInteger.ZERO);
        Expression<BigInteger> quantityAfter = criteriaBuilder.<BigInteger>coalesce()
                .value(root.get(StockItemHistory.Fields.quantityAfter)).value(BigInteger.ZERO);

        Expression<BigInteger> delta = criteriaBuilder.diff(quantityBefore, quantityAfter);
        Expression<BigInteger> zero = criteriaBuilder.literal(BigInteger.ZERO);

        Path<Long> itemIdPath = root.get(StockItemHistory.Fields.stockItemId);
        Expression<LocalDate> startDate = criteriaBuilder.function("DATE", LocalDate.class,
                root.get(StockItemHistory.Fields.createdAt));

        List<Predicate> predicates = new ArrayList<>();

        if (stockItemId != null) {
            predicates.add(criteriaBuilder.equal(itemIdPath, stockItemId));
        }
        if (from != null) {
            predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                    root.get(StockItemHistory.Fields.createdAt), from.atStartOfDay()));
        }
        if (to != null) {
            predicates.add(criteriaBuilder.lessThan(
                    root.get(StockItemHistory.Fields.createdAt), to.plusDays(1).atStartOfDay()));
        }

        predicates.add(criteriaBuilder.greaterThan(root.get(StockItemHistory.Fields.quantityBefore),
                root.get(StockItemHistory.Fields.quantityAfter)));

        Expression<BigInteger> sold = criteriaBuilder.<BigInteger>selectCase()
                .when(criteriaBuilder.greaterThan(delta, zero), delta)
                .otherwise(zero);

        criteriaQuery.select(criteriaBuilder.construct(
                        ItemSellingStatistic.class,
                        itemIdPath,
                        startDate,
                        criteriaBuilder.sum(sold)
                ))
                .where(predicates.toArray(Predicate[]::new))
                .groupBy(itemIdPath, startDate)
                .orderBy(criteriaBuilder.asc(itemIdPath), criteriaBuilder.asc(startDate));

        return entityManager.createQuery(criteriaQuery)
                .setMaxResults(pageSize)
                .setFirstResult((page - 1) * pageSize)
                .getResultList();
    }

}
