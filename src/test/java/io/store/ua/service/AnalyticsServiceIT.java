package io.store.ua.service;

import io.store.ua.AbstractIT;
import io.store.ua.entity.Beneficiary;
import io.store.ua.entity.Product;
import io.store.ua.entity.StockItem;
import io.store.ua.entity.Transaction;
import io.store.ua.entity.immutable.StockItemHistory;
import io.store.ua.enums.PaymentProvider;
import io.store.ua.enums.TransactionFlowType;
import io.store.ua.enums.TransactionPurpose;
import io.store.ua.enums.TransactionStatus;
import io.store.ua.models.data.BeneficiaryFinancialFlowStatistic;
import io.store.ua.models.data.FinancialStatistic;
import io.store.ua.models.data.ItemSellingStatistic;
import jakarta.validation.ConstraintViolationException;
import org.apache.commons.lang3.RandomStringUtils;
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
import java.util.UUID;
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

    private static BigInteger calculateQuantity(List<ItemSellingStatistic> statistics) {
        return statistics.stream()
                .map(ItemSellingStatistic::getSoldQuantity)
                .reduce(BigInteger.ZERO, BigInteger::add);
    }

    private static BigInteger calculateRevenue(List<ItemSellingStatistic> statistics) {
        return statistics.stream()
                .map(ItemSellingStatistic::getTotalRevenueAmount)
                .reduce(BigInteger.ZERO, BigInteger::add);
    }

    private static BigInteger expectedQuantity(Stream<StockItemHistory> entries) {
        return entries.map(entry -> {
                    BigInteger before = entry.getQuantityBefore() == null ? BigInteger.ZERO : entry.getQuantityBefore();
                    BigInteger after = entry.getQuantityAfter() == null ? BigInteger.ZERO : entry.getQuantityAfter();
                    BigInteger difference = before.subtract(after);

                    return difference.signum() > 0 ? difference : BigInteger.ZERO;
                })
                .reduce(BigInteger.ZERO, BigInteger::add);
    }

    private static BigInteger expectedRevenue(Stream<StockItemHistory> history, BigInteger price) {
        return expectedQuantity(history).multiply(price == null ? BigInteger.ZERO : price);
    }

    private static FinancialStatistic findByCurrency(BeneficiaryFinancialFlowStatistic statistic, String currency) {
        return statistic.getFinancialStatistic().stream()
                .filter(financialStatistic -> currency.equals(financialStatistic.getCurrency()))
                .findFirst()
                .orElse(null);
    }

    private StockItemHistory insertHistoryRow(Long stockItemId, BigInteger quantityBefore, BigInteger quantityAfter) {
        return stockItemHistoryRepository.save(StockItemHistory.builder()
                .stockItemId(stockItemId)
                .currentProductPrice(product.getPrice())
                .quantityBefore(quantityBefore)
                .quantityAfter(quantityAfter)
                .build());
    }

    @BeforeEach
    void setupBeneficiary() {
        beneficiary = beneficiaryRepository.save(Beneficiary.builder()
                .name(RandomStringUtils.secure().nextAlphabetic(10))
                .IBAN("UA" + RandomStringUtils.secure().nextNumeric(27))
                .SWIFT(RandomStringUtils.secure().nextAlphabetic(8).toUpperCase())
                .card(RandomStringUtils.secure().nextNumeric(16))
                .isActive(true)
                .build());
    }

    private Transaction generateTransaction(Long beneficiaryId,
                                            String currency,
                                            BigInteger amount,
                                            TransactionFlowType flow) {
        return transactionRepository.save(Transaction.builder()
                .transactionId(UUID.randomUUID().toString())
                .reference(UUID.randomUUID().toString())
                .flowType(flow)
                .purpose(TransactionPurpose.STOCK_OUTBOUND_REVENUE)
                .status(TransactionStatus.SETTLED)
                .amount(amount)
                .currency(currency)
                .beneficiaryId(beneficiaryId)
                .paymentProvider(PaymentProvider.CASH)
                .build());
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
        @DisplayName("success_daily_aggregation")
        void success_daily_aggregation() {
            StockItemHistory firstEntry = insertHistoryRow(stockItem.getId(),
                    BigInteger.valueOf(10),
                    BigInteger.valueOf(8));

            StockItemHistory extraEntry = insertHistoryRow(stockItem.getId(),
                    BigInteger.valueOf(8),
                    BigInteger.valueOf(7));

            StockItemHistory thirdEntry = insertHistoryRow(stockItem.getId(),
                    BigInteger.valueOf(20),
                    BigInteger.valueOf(25));

            List<ItemSellingStatistic> statistics = analyticsService.fetchItemSellingStatistic(stockItem.getId(),
                    null,
                    null,
                    100,
                    1);

            assertThat(statistics)
                    .isNotEmpty();
            assertThat(calculateQuantity(statistics))
                    .isEqualTo(expectedQuantity(Stream.of(firstEntry, extraEntry, thirdEntry)));
            assertThat(calculateRevenue(statistics))
                    .isEqualTo(expectedRevenue(Stream.of(firstEntry, extraEntry, thirdEntry), product.getPrice()));
            assertThat(statistics.stream().map(ItemSellingStatistic::getStartDate).distinct().count())
                    .isGreaterThanOrEqualTo(1);
        }

        @Test
        @DisplayName("success_filter_by_item")
        void success_filter_by_item() {
            StockItemHistory stockItemHistory = insertHistoryRow(stockItem.getId(), BigInteger.TEN, BigInteger.valueOf(7));
            insertHistoryRow(otherStockItem.getId(), BigInteger.TEN, BigInteger.valueOf(5));

            List<ItemSellingStatistic> statistics = analyticsService.fetchItemSellingStatistic(stockItem.getId(),
                    null,
                    null,
                    50,
                    1);

            assertThat(calculateQuantity(statistics))
                    .isEqualTo(expectedQuantity(Stream.of(stockItemHistory)));
            assertThat(calculateRevenue(statistics))
                    .isEqualTo(expectedRevenue(Stream.of(stockItemHistory), product.getPrice()));
        }

        @Test
        @DisplayName("success_date_range")
        void success_date_range() {
            StockItemHistory stockItemHistory = insertHistoryRow(stockItem.getId(),
                    BigInteger.TEN,
                    BigInteger.valueOf(9));

            List<ItemSellingStatistic> statistics = analyticsService.fetchItemSellingStatistic(stockItem.getId(),
                    LocalDate.now(),
                    LocalDate.now().plusDays(1),
                    100,
                    1);

            assertThat(calculateQuantity(statistics))
                    .isEqualTo(expectedQuantity(Stream.of(stockItemHistory)));
            assertThat(calculateRevenue(statistics))
                    .isEqualTo(expectedRevenue(Stream.of(stockItemHistory), product.getPrice()));
            assertThat(statistics)
                    .allSatisfy(itemSellingStatistic -> assertThat(itemSellingStatistic.getStartDate())
                            .isEqualTo(LocalDate.now()));
        }

        @Test
        @DisplayName("success_pagination")
        void success_pagination() {
            for (int i = 0; i < 5; i++) {
                insertHistoryRow(stockItem.getId(), BigInteger.TEN, BigInteger.valueOf(9));
            }

            for (int i = 0; i < 5; i++) {
                insertHistoryRow(otherStockItem.getId(), BigInteger.TEN, BigInteger.valueOf(9));
            }

            var firstPage = analyticsService.fetchItemSellingStatistic(null,
                    null,
                    null,
                    1,
                    1);
            var otherPage = analyticsService.fetchItemSellingStatistic(null,
                    null,
                    null,
                    1,
                    2);

            assertThat(firstPage)
                    .hasSize(1);
            assertThat(otherPage)
                    .hasSize(1);
            assertThat(otherPage)
                    .doesNotContainAnyElementsOf(firstPage);
        }

        @ParameterizedTest(name = "fail_invalid_pageSize={0}")
        @ValueSource(ints = {0, -1})
        void fail_invalid_pageSize(int pageSize) {
            assertThatThrownBy(() -> analyticsService.fetchItemSellingStatistic(stockItem.getId(),
                    null,
                    null,
                    pageSize,
                    1))
                    .isInstanceOf(ConstraintViolationException.class);
        }

        @ParameterizedTest(name = "fail_invalid_page={0}")
        @ValueSource(ints = {0, -1})
        void fail_invalid_page(int page) {
            assertThatThrownBy(() -> analyticsService.fetchItemSellingStatistic(stockItem.getId(),
                    null,
                    null,
                    10,
                    page))
                    .isInstanceOf(ConstraintViolationException.class);
        }
    }

    @Nested
    @DisplayName("")
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

            var statistic = analyticsService.fetchBeneficiaryFinancialStatistic(
                    beneficiary.getId(), null, null, 100, 1);

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
