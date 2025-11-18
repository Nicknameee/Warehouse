package io.store.ua.service;

import io.store.ua.AbstractIT;
import io.store.ua.entity.Beneficiary;
import io.store.ua.entity.Product;
import io.store.ua.entity.StockItem;
import io.store.ua.entity.Transaction;
import io.store.ua.entity.immutable.StockItemHistory;
import io.store.ua.enums.TransactionFlowType;
import io.store.ua.models.data.BeneficiaryFinancialFlowStatistic;
import io.store.ua.models.data.FinancialStatistic;
import io.store.ua.models.data.ItemSellingStatistic;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigInteger;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AnalyticsServiceIT extends AbstractIT {
    @Autowired
    private AnalyticsService analyticsService;

    private Product product;
    private StockItem stockItem;
    private StockItem otherStockItem;
    private Beneficiary beneficiary;

    private static BigInteger calculateQuantity(ItemSellingStatistic statistic) {
        if (statistic == null || statistic.getStatistics() == null) {
            return BigInteger.ZERO;
        }

        return statistic.getStatistics().stream()
                .map(ItemSellingStatistic.Statistic::getSoldQuantity)
                .reduce(BigInteger.ZERO, BigInteger::add);
    }

    private static BigInteger calculateRevenue(ItemSellingStatistic statistic) {
        if (statistic == null || statistic.getStatistics() == null) {
            return BigInteger.ZERO;
        }

        return statistic.getStatistics().stream()
                .map(ItemSellingStatistic.Statistic::getTotalRevenueAmount)
                .reduce(BigInteger.ZERO, BigInteger::add);
    }

    private static BigInteger expectedQuantity(Stream<StockItemHistory> entries) {
        return entries.map(entry -> {
                    BigInteger before = entry.getQuantityBefore() == null
                            ? BigInteger.ZERO : entry.getQuantityBefore();
                    BigInteger after = entry.getQuantityAfter() == null
                            ? BigInteger.ZERO : entry.getQuantityAfter();
                    BigInteger difference = before.subtract(after);

                    return difference.signum() > 0
                            ? difference : BigInteger.ZERO;
                })
                .reduce(BigInteger.ZERO, BigInteger::add);
    }

    private static BigInteger expectedRevenue(Stream<StockItemHistory> history) {
        return history.map(entry -> {
                    BigInteger before = entry.getQuantityBefore() == null
                            ? BigInteger.ZERO : entry.getQuantityBefore();
                    BigInteger after = entry.getQuantityAfter() == null
                            ? BigInteger.ZERO : entry.getQuantityAfter();
                    BigInteger sold = before.subtract(after);

                    if (sold.signum() <= 0) {
                        return BigInteger.ZERO;
                    }

                    BigInteger price = entry.getCurrentProductPrice() == null
                            ? BigInteger.ZERO : entry.getCurrentProductPrice();

                    return sold.multiply(price);
                })
                .reduce(BigInteger.ZERO, BigInteger::add);
    }

    private static FinancialStatistic findByCurrency(BeneficiaryFinancialFlowStatistic statistic, String currency) {
        return statistic.getFinancialStatistic().stream()
                .filter(financialStatistic -> currency.equals(financialStatistic.getCurrency()))
                .findFirst()
                .orElse(null);
    }

    @BeforeEach
    void setupBeneficiary() {
        beneficiary = generateBeneficiary();
    }

    @BeforeEach
    void setUp() {
        product = generateProduct();
        var stockItemGroup = generateStockItemGroup(true);
        var warehouse = generateWarehouse();
        stockItem = generateStockItem(product.getId(), stockItemGroup.getId(), warehouse.getId());
        otherStockItem = generateStockItem(generateProduct().getId(), stockItemGroup.getId(), warehouse.getId());
    }

    @Nested
    @DisplayName("fetchItemSellingStatistic(stockItemId, from, to, pageSize, page)")
    class FetchItemSellingStatisticTests {

        @Test
        @DisplayName("fetchItemSellingStatistic_success: calculates and returns total revenue for stock item")
        void fetchItemSellingStatistic_success() {
            StockItemHistory firstEntry = insertHistoryRow(
                    stockItem.getId(), BigInteger.valueOf(10), BigInteger.valueOf(8), BigInteger.ONE, LocalDate.now());
            StockItemHistory extraEntry = insertHistoryRow(
                    stockItem.getId(), BigInteger.valueOf(8), BigInteger.valueOf(7), BigInteger.ONE, LocalDate.now());
            StockItemHistory thirdEntry = insertHistoryRow(
                    stockItem.getId(), BigInteger.valueOf(20), BigInteger.valueOf(25), BigInteger.ONE, LocalDate.now());

            ItemSellingStatistic statistic = analyticsService.fetchItemSellingStatistic(
                    stockItem.getId(), null, null, 100, 1);

            assertThat(statistic).isNotNull();
            assertThat(statistic.getStockItemId()).isEqualTo(stockItem.getId());
            assertThat(statistic.getStatistics()).isNotEmpty();

            assertThat(calculateQuantity(statistic))
                    .isEqualTo(expectedQuantity(Stream.of(firstEntry, extraEntry, thirdEntry)));
            assertThat(calculateRevenue(statistic))
                    .isEqualTo(expectedRevenue(Stream.of(firstEntry, extraEntry, thirdEntry)));

            long distinctDates = statistic.getStatistics().stream()
                    .map(ItemSellingStatistic.Statistic::getStartDate)
                    .distinct()
                    .count();

            assertThat(distinctDates).isGreaterThanOrEqualTo(1);
        }

        @Test
        @DisplayName("fetchItemSellingStatistic_success_filterByItem")
        void fetchItemSellingStatistic_success_filterByItem() {
            StockItemHistory stockItemHistory = insertHistoryRow(
                    stockItem.getId(), BigInteger.TEN, BigInteger.valueOf(7), BigInteger.ONE, LocalDate.now());
            insertHistoryRow(
                    otherStockItem.getId(), BigInteger.TEN, BigInteger.valueOf(5), BigInteger.ONE, LocalDate.now());

            ItemSellingStatistic statistic = analyticsService.fetchItemSellingStatistic(
                    stockItem.getId(), null, null, 50, 1);

            assertThat(statistic).isNotNull();
            assertThat(statistic.getStockItemId()).isEqualTo(stockItem.getId());

            assertThat(calculateQuantity(statistic))
                    .isEqualTo(expectedQuantity(Stream.of(stockItemHistory)));
            assertThat(calculateRevenue(statistic))
                    .isEqualTo(expectedRevenue(Stream.of(stockItemHistory)));
        }

        @Test
        @DisplayName("success_date_range")
        void success_date_range() {
            LocalDate today = LocalDate.now();
            StockItemHistory stockItemHistory = insertHistoryRow(
                    stockItem.getId(), BigInteger.TEN, BigInteger.valueOf(9), BigInteger.ONE, today);

            ItemSellingStatistic statistic = analyticsService.fetchItemSellingStatistic(
                    stockItem.getId(), today, today.plusDays(1), 100, 1);

            assertThat(statistic).isNotNull();
            assertThat(statistic.getStockItemId()).isEqualTo(stockItem.getId());

            assertThat(calculateQuantity(statistic))
                    .isEqualTo(expectedQuantity(Stream.of(stockItemHistory)));
            assertThat(calculateRevenue(statistic))
                    .isEqualTo(expectedRevenue(Stream.of(stockItemHistory)));

            assertThat(statistic.getStatistics()).allSatisfy(s ->
                    assertThat(s.getStartDate()).isEqualTo(today));
        }

        @Test
        @DisplayName("fetchItemSellingStatistic_success_pagination")
        void fetchItemSellingStatistic_success_pagination() {
            var today = LocalDate.now();
            var yesterday = today.minusDays(1);

            insertHistoryRow(stockItem.getId(), BigInteger.TEN, BigInteger.valueOf(9), BigInteger.ONE, yesterday);
            insertHistoryRow(stockItem.getId(), BigInteger.TEN, BigInteger.valueOf(9), BigInteger.ONE, today);

            ItemSellingStatistic firstPage = analyticsService.fetchItemSellingStatistic(
                    stockItem.getId(), null, null, 1, 1);
            ItemSellingStatistic otherPage = analyticsService.fetchItemSellingStatistic(
                    stockItem.getId(), null, null, 1, 2);

            assertThat(firstPage).isNotNull();
            assertThat(otherPage).isNotNull();

            assertThat(firstPage.getStatistics()).hasSize(1);
            assertThat(otherPage.getStatistics()).hasSize(1);

            LocalDate firstDate = firstPage.getStatistics().getFirst().getStartDate();
            LocalDate otherDate = otherPage.getStatistics().getFirst().getStartDate();

            assertThat(otherDate).isNotEqualTo(firstDate);
        }

        @ParameterizedTest(name = "fail_invalid_pageSize={0}")
        @ValueSource(ints = {0, -1})
        void fetchItemSellingStatistic_fails_invalidPageSize(int pageSize) {
            assertThatThrownBy(() -> analyticsService.fetchItemSellingStatistic(
                    stockItem.getId(), null, null, pageSize, 1))
                    .isInstanceOf(ConstraintViolationException.class);
        }

        @ParameterizedTest(name = "fail_invalid_page={0}")
        @ValueSource(ints = {0, -1})
        void fetchItemSellingStatistic_fails_invalidPage(int page) {
            assertThatThrownBy(() -> analyticsService.fetchItemSellingStatistic(
                    stockItem.getId(), null, null, 10, page))
                    .isInstanceOf(ConstraintViolationException.class);
        }

        @Test
        @DisplayName("fail_null_itemId")
        void fetchItemSellingStatistic_fails_invalidItemId() {
            assertThatThrownBy(() -> analyticsService.fetchItemSellingStatistic(
                    null, null, null, 10, 1))
                    .isInstanceOf(ConstraintViolationException.class);
        }
    }

    @Nested
    @DisplayName("fetchBeneficiaryFinancialStatistic(...)")
    class FetchBeneficiaryFinancialStatisticTest {
        @Test
        @DisplayName("calculates_totals_per_currency_for_beneficiary")
        void calculates_totals_per_currency_for_beneficiary() {
            List<Transaction> usdTx = List.of(
                    generateTransaction(beneficiary.getId(), "USD", BigInteger.valueOf(100), TransactionFlowType.DEBIT),
                    generateTransaction(beneficiary.getId(), "USD", BigInteger.valueOf(200), TransactionFlowType.DEBIT),
                    generateTransaction(beneficiary.getId(), "USD", BigInteger.valueOf(50), TransactionFlowType.CREDIT),
                    generateTransaction(beneficiary.getId(), "USD", BigInteger.valueOf(30), TransactionFlowType.CREDIT)
            );

            List<Transaction> eurTx = List.of(
                    generateTransaction(beneficiary.getId(), "EUR", BigInteger.valueOf(70), TransactionFlowType.DEBIT),
                    generateTransaction(beneficiary.getId(), "EUR", BigInteger.valueOf(10), TransactionFlowType.DEBIT),
                    generateTransaction(beneficiary.getId(), "EUR", BigInteger.valueOf(25), TransactionFlowType.CREDIT)
            );

            List<Transaction> uahTx = List.of(
                    generateTransaction(beneficiary.getId(), "UAH", BigInteger.valueOf(40), TransactionFlowType.CREDIT),
                    generateTransaction(beneficiary.getId(), "UAH", BigInteger.valueOf(60), TransactionFlowType.CREDIT)
            );

            var statistic = analyticsService.fetchBeneficiaryFinancialStatistic(beneficiary.getId(), null, null, 100, 1);

            assertThat(statistic).isNotNull();
            assertThat(statistic.getBeneficiary()).isNotNull();
            assertThat(statistic.getBeneficiary().getId()).isEqualTo(beneficiary.getId());
            assertThat(statistic.getFinancialStatistic()).hasSize(3);

            var usdExpectedDebit = usdTx.stream()
                    .filter(t -> t.getFlowType() == TransactionFlowType.DEBIT)
                    .map(Transaction::getAmount)
                    .reduce(BigInteger.ZERO, BigInteger::add);

            var usdExpectedCredit = usdTx.stream()
                    .filter(t -> t.getFlowType() == TransactionFlowType.CREDIT)
                    .map(Transaction::getAmount)
                    .reduce(BigInteger.ZERO, BigInteger::add);

            var eurExpectedDebit = eurTx.stream()
                    .filter(t -> t.getFlowType() == TransactionFlowType.DEBIT)
                    .map(Transaction::getAmount)
                    .reduce(BigInteger.ZERO, BigInteger::add);

            var eurExpectedCredit = eurTx.stream()
                    .filter(t -> t.getFlowType() == TransactionFlowType.CREDIT)
                    .map(Transaction::getAmount)
                    .reduce(BigInteger.ZERO, BigInteger::add);

            var uahExpectedDebit = uahTx.stream()
                    .filter(t -> t.getFlowType() == TransactionFlowType.DEBIT)
                    .map(Transaction::getAmount)
                    .reduce(BigInteger.ZERO, BigInteger::add);

            var uahExpectedCredit = uahTx.stream()
                    .filter(t -> t.getFlowType() == TransactionFlowType.CREDIT)
                    .map(Transaction::getAmount)
                    .reduce(BigInteger.ZERO, BigInteger::add);

            var usd = findByCurrency(statistic, "USD");
            var eur = findByCurrency(statistic, "EUR");
            var uah = findByCurrency(statistic, "UAH");

            assertThat(usd).isNotNull();
            assertThat(usd.getTotalDebit()).isEqualTo(usdExpectedDebit);
            assertThat(usd.getTotalCredit()).isEqualTo(usdExpectedCredit);

            assertThat(eur).isNotNull();
            assertThat(eur.getTotalDebit()).isEqualTo(eurExpectedDebit);
            assertThat(eur.getTotalCredit()).isEqualTo(eurExpectedCredit);

            assertThat(uah).isNotNull();
            assertThat(uah.getTotalDebit()).isEqualTo(uahExpectedDebit);
            assertThat(uah.getTotalCredit()).isEqualTo(uahExpectedCredit);
        }

        @ParameterizedTest(name = "fail_invalid_pageSize={0}")
        @ValueSource(ints = {0, -1})
        void fail_invalid_pageSize(int pageSize) {
            assertThatThrownBy(() -> analyticsService.fetchBeneficiaryFinancialStatistic(
                    beneficiary.getId(), null, null, pageSize, 1))
                    .isInstanceOf(ConstraintViolationException.class);
        }

        @ParameterizedTest(name = "fail_invalid_page={0}")
        @ValueSource(ints = {0, -1})
        void fail_invalid_page(int page) {
            assertThatThrownBy(() -> analyticsService.fetchBeneficiaryFinancialStatistic(
                    beneficiary.getId(), null, null, 10, page))
                    .isInstanceOf(ConstraintViolationException.class);
        }

        @Test
        @DisplayName("fail_from_after_to")
        void fail_from_after_to() {
            assertThatThrownBy(() -> analyticsService.fetchBeneficiaryFinancialStatistic(
                    beneficiary.getId(), LocalDate.now().plusDays(1), LocalDate.now(), 10, 1))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("'from' must not be after 'to'");
        }
    }
}
