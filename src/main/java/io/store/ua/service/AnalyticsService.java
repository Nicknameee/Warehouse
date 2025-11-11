package io.store.ua.service;

import io.store.ua.entity.Beneficiary;
import io.store.ua.entity.Transaction;
import io.store.ua.entity.immutable.StockItemHistory;
import io.store.ua.enums.TransactionFlowType;
import io.store.ua.enums.TransactionStatus;
import io.store.ua.models.data.BeneficiaryFinancialFlowStatistic;
import io.store.ua.models.data.FinancialStatistic;
import io.store.ua.models.data.ItemSellingStatistic;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Validated
public class AnalyticsService {
    private final EntityManager entityManager;

    public List<ItemSellingStatistic> fetchItemSellingStatistic(@NotNull(message = "Stock item ID can't be null")
                                                                @Min(value = 1, message = "Stock item ID can't be less than 1")
                                                                Long stockItemId,
                                                                LocalDate from,
                                                                LocalDate to,
                                                                @Min(value = 1, message = "Size of page can't be less than 1") int pageSize,
                                                                @Min(value = 1, message = "A page number can't be less than 1") int page) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("A 'from' must not be after 'to'");
        }

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
        Path<LocalDateTime> loggedAt = root.get(StockItemHistory.Fields.loggedAt);
        Expression<LocalDate> startDate = criteriaBuilder.function("DATE", LocalDate.class, loggedAt);

        List<Predicate> predicates = new ArrayList<>();
        predicates.add(criteriaBuilder.equal(itemIdPath, stockItemId));

        if (from != null) {
            predicates.add(criteriaBuilder.greaterThanOrEqualTo(loggedAt, from.atStartOfDay()));
        }
        if (to != null) {
            predicates.add(criteriaBuilder.lessThan(loggedAt, to.plusDays(1).atStartOfDay()));
        }

        predicates.add(criteriaBuilder.greaterThan(root.get(StockItemHistory.Fields.quantityBefore),
                root.get(StockItemHistory.Fields.quantityAfter)));

        Expression<BigInteger> quantity = criteriaBuilder.<BigInteger>selectCase()
                .when(criteriaBuilder.greaterThan(delta, zero), delta)
                .otherwise(zero);

        Path<BigInteger> currentProductPrice = root.get(StockItemHistory.Fields.currentProductPrice);
        Expression<BigInteger> revenue = criteriaBuilder.prod(currentProductPrice, quantity);

        criteriaQuery.select(criteriaBuilder.construct(ItemSellingStatistic.class,
                        criteriaBuilder.literal(stockItemId),
                        startDate,
                        criteriaBuilder.sum(quantity),
                        criteriaBuilder.sum(revenue)))
                .where(predicates.toArray(Predicate[]::new))
                .groupBy(startDate)
                .orderBy(criteriaBuilder.asc(startDate));

        return entityManager.createQuery(criteriaQuery)
                .setMaxResults(pageSize)
                .setFirstResult((page - 1) * pageSize)
                .getResultList();
    }

    public BeneficiaryFinancialFlowStatistic fetchBeneficiaryFinancialStatistic(@NotNull(message = "Beneficiary ID can't be null")
                                                                                @Min(value = 1, message = "Beneficiary ID can't be less than 1")
                                                                                Long beneficiaryId,
                                                                                LocalDate from,
                                                                                LocalDate to,
                                                                                @Min(value = 1, message = "Size of page can't be less than 1") int pageSize,
                                                                                @Min(value = 1, message = "A page number can't be less than 1") int page) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("A 'from' must not be after 'to'");
        }

        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<FinancialStatistic> criteriaQuery =
                criteriaBuilder.createQuery(FinancialStatistic.class);
        Root<Transaction> root = criteriaQuery.from(Transaction.class);

        Path<Long> beneficiaryPath = root.get(Transaction.Fields.beneficiaryId);
        Path<String> currencyPath = root.get(Transaction.Fields.currency);
        Path<BigInteger> amountPath = root.get(Transaction.Fields.amount);
        Path<TransactionFlowType> flowTypePath = root.get(Transaction.Fields.flowType);
        Path<LocalDateTime> createdAtPath = root.get(Transaction.Fields.createdAt);
        Path<TransactionStatus> statusPath = root.get(Transaction.Fields.status);

        List<Predicate> predicates = new ArrayList<>();
        predicates.add(criteriaBuilder.equal(beneficiaryPath, beneficiaryId));
        predicates.add(criteriaBuilder.equal(statusPath, TransactionStatus.SETTLED));

        if (from != null) {
            predicates.add(criteriaBuilder.greaterThanOrEqualTo(createdAtPath, from.atStartOfDay()));
        }
        if (to != null) {
            predicates.add(criteriaBuilder.lessThan(createdAtPath, to.plusDays(1).atStartOfDay()));
        }

        Expression<BigInteger> zero = criteriaBuilder.literal(BigInteger.ZERO);

        Expression<BigInteger> debitCase = criteriaBuilder.<BigInteger>selectCase()
                .when(criteriaBuilder.equal(flowTypePath, TransactionFlowType.DEBIT), amountPath)
                .otherwise(zero);

        Expression<BigInteger> creditCase = criteriaBuilder.<BigInteger>selectCase()
                .when(criteriaBuilder.equal(flowTypePath, TransactionFlowType.CREDIT), amountPath)
                .otherwise(zero);

        Expression<BigInteger> totalDebit = criteriaBuilder.sum(debitCase);
        Expression<BigInteger> totalCredit = criteriaBuilder.sum(creditCase);
        criteriaQuery.select(
                        criteriaBuilder.construct(
                                FinancialStatistic.class,
                                currencyPath,
                                totalDebit,
                                totalCredit
                        )
                )
                .where(predicates.toArray(new Predicate[0]))
                .groupBy(currencyPath)
                .orderBy(criteriaBuilder.asc(currencyPath));

        TypedQuery<FinancialStatistic> query = entityManager.createQuery(criteriaQuery)
                .setFirstResult(Math.max(0, page - 1) * pageSize)
                .setMaxResults(pageSize);

        List<FinancialStatistic> rows = query.getResultList();

        List<FinancialStatistic> stats = new ArrayList<>(rows.size());

        for (FinancialStatistic row : rows) {
            stats.add(FinancialStatistic.builder()
                    .currency(row.getCurrency())
                    .totalDebit(row.getTotalDebit() != null ? row.getTotalDebit() : BigInteger.ZERO)
                    .totalCredit(row.getTotalCredit() != null ? row.getTotalCredit() : BigInteger.ZERO)
                    .build());
        }

        Beneficiary beneficiary = entityManager.find(Beneficiary.class, beneficiaryId);

        return BeneficiaryFinancialFlowStatistic.builder()
                .beneficiary(beneficiary)
                .financialStatistic(stats)
                .build();
    }
}
