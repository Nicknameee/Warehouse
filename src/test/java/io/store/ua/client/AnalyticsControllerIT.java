package io.store.ua.client;

import io.store.ua.AbstractIT;
import io.store.ua.entity.Beneficiary;
import io.store.ua.entity.StockItem;
import io.store.ua.entity.Transaction;
import io.store.ua.entity.immutable.StockItemHistory;
import io.store.ua.enums.Currency;
import io.store.ua.enums.TransactionFlowType;
import io.store.ua.enums.TransactionStatus;
import io.store.ua.models.data.BeneficiaryFinancialFlowStatistic;
import io.store.ua.models.data.ItemSellingStatistic;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;

import java.math.BigInteger;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AnalyticsControllerIT extends AbstractIT {
    private static final DateTimeFormatter DATE_DMY = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private HttpHeaders authenticationHeaders;

    private static BigInteger computeSoldQuantity(StockItemHistory history) {
        BigInteger quantityBefore = history.getQuantityBefore() == null
                ? BigInteger.ZERO : history.getQuantityBefore();
        BigInteger quantityAfter = history.getQuantityAfter() == null
                ? BigInteger.ZERO : history.getQuantityAfter();

        return quantityBefore.compareTo(quantityAfter) > 0 ? quantityBefore.subtract(quantityAfter) : BigInteger.ZERO;
    }

    private static BigInteger computeRevenue(StockItemHistory history) {
        BigInteger currentPrice = history.getCurrentProductPrice() == null
                ? BigInteger.ZERO : history.getCurrentProductPrice();

        return computeSoldQuantity(history).multiply(currentPrice);
    }

    @BeforeAll
    void setUp() {
        authenticationHeaders = generateAuthenticationHeaders();
    }

    @Nested
    @DisplayName("GET /api/v1/analytics/itemSelling")
    class ItemSellingTests {
        @Test
        @DisplayName("fetchItemSellingStatistic_success_aggregatesByDayWithinRangeForOneItem: aggregates by day within range for one item")
        void fetchItemSellingStatistic_success_aggregatesByDayWithinRangeForOneItem() {
            var stockItemGroup = generateStockItemGroup(true);
            var warehouse = generateWarehouse();
            var product = generateProduct();
            var item = generateStockItem(product.getId(), stockItemGroup.getId(), warehouse.getId());
            var otherItem = generateStockItem(generateProduct().getId(), stockItemGroup.getId(), warehouse.getId());

            LocalDate dateInPast = LocalDate.now().minusDays(3);
            LocalDate yesterday = LocalDate.now().minusDays(1);
            LocalDate now = LocalDate.now();

            StockItemHistory historyForPast = insertHistoryRow(item.getId(),
                    BigInteger.valueOf(10),
                    BigInteger.valueOf(7),
                    BigInteger.valueOf(5),
                    dateInPast);
            StockItemHistory historyForYesterday = insertHistoryRow(item.getId(),
                    BigInteger.valueOf(7),
                    BigInteger.valueOf(6),
                    BigInteger.valueOf(5),
                    yesterday);
            StockItemHistory historyForToday = insertHistoryRow(item.getId(),
                    BigInteger.valueOf(9),
                    BigInteger.valueOf(3),
                    BigInteger.valueOf(5),
                    now);
            insertHistoryRow(otherItem.getId(), BigInteger.valueOf(10),
                    BigInteger.valueOf(8),
                    BigInteger.valueOf(9),
                    yesterday);

            String requestUrl = "/api/v1/analytics/itemSelling?stock_item_id=%d&from=%s&to=%s&pageSize=50&page=1"
                    .formatted(item.getId(), dateInPast.format(DATE_DMY), now.format(DATE_DMY));

            ResponseEntity<List<ItemSellingStatistic>> response = restClient.exchange(requestUrl,
                    HttpMethod.GET,
                    new HttpEntity<>(authenticationHeaders),
                    new ParameterizedTypeReference<>() {
                    });

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull().isNotEmpty();

            BigInteger expectedTotalQuantity = computeSoldQuantity(historyForPast)
                    .add(computeSoldQuantity(historyForYesterday))
                    .add(computeSoldQuantity(historyForToday));
            BigInteger expectedTotalRevenue = computeRevenue(historyForPast)
                    .add(computeRevenue(historyForYesterday))
                    .add(computeRevenue(historyForToday));

            assertThat(response.getBody().stream()
                    .map(ItemSellingStatistic::getStartDate))
                    .contains(dateInPast, yesterday, now);
            assertThat(response.getBody().stream()
                    .map(ItemSellingStatistic::getSoldQuantity)
                    .reduce(BigInteger.ZERO, BigInteger::add))
                    .isEqualTo(expectedTotalQuantity);
            assertThat(response.getBody().stream()
                    .map(ItemSellingStatistic::getTotalRevenueAmount)
                    .reduce(BigInteger.ZERO, BigInteger::add))
                    .isEqualTo(expectedTotalRevenue);
        }

        @Test
        @DisplayName("fetchItemSellingStatistic_success_supportsPaginationByDay: supports pagination by day")
        void fetchItemSellingStatistic_success_supportsPaginationByDay() {
            var stockItemGroup = generateStockItemGroup(true);
            var warehouse = generateWarehouse();
            StockItem stockItem = generateStockItem(generateProduct().getId(), stockItemGroup.getId(), warehouse.getId());

            insertHistoryRow(stockItem.getId(),
                    BigInteger.valueOf(10),
                    BigInteger.valueOf(9),
                    BigInteger.ONE,
                    LocalDate.now().minusDays(3));
            insertHistoryRow(stockItem.getId(),
                    BigInteger.valueOf(9),
                    BigInteger.valueOf(7),
                    BigInteger.ONE,
                    LocalDate.now().minusDays(2));
            insertHistoryRow(stockItem.getId(),
                    BigInteger.valueOf(7),
                    BigInteger.valueOf(6),
                    BigInteger.ONE,
                    LocalDate.now().minusDays(1));

            ResponseEntity<ItemSellingStatistic[]> firstPageResponse = restClient.exchange(
                    "/api/v1/analytics/itemSelling?stock_item_id=%d&pageSize=1&page=1".formatted(stockItem.getId()),
                    HttpMethod.GET,
                    new HttpEntity<>(authenticationHeaders),
                    ItemSellingStatistic[].class);

            ResponseEntity<ItemSellingStatistic[]> secondPageResponse = restClient.exchange(
                    "/api/v1/analytics/itemSelling?stock_item_id=%d&pageSize=1&page=2".formatted(stockItem.getId()),
                    HttpMethod.GET,
                    new HttpEntity<>(authenticationHeaders),
                    ItemSellingStatistic[].class);

            assertThat(firstPageResponse.getStatusCode())
                    .isEqualTo(HttpStatus.OK);
            assertThat(secondPageResponse.getStatusCode())
                    .isEqualTo(HttpStatus.OK);
            assertThat(firstPageResponse.getBody())
                    .isNotNull()
                    .hasSize(1);
            assertThat(secondPageResponse.getBody())
                    .isNotNull()
                    .hasSize(1);
            assertThat(firstPageResponse.getBody()[0].getStartDate())
                    .isNotEqualTo(secondPageResponse.getBody()[0].getStartDate());
        }

        @Test
        @DisplayName("missing stock_item_id returns 400")
        void fetchItemSellingStatistic_fails_missingStockItemIdReturns400() {
            ResponseEntity<String> response = restClient.exchange(
                    "/api/v1/analytics/itemSelling?pageSize=10&page=1",
                    HttpMethod.GET,
                    new HttpEntity<>(authenticationHeaders),
                    String.class);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }

    @Nested
    @DisplayName("GET /api/v1/analytics/beneficiary/financialFlow")
    class BeneficiaryFinancialFlowTests {
        @Test
        @DisplayName("fetchBeneficiaryFinancialStatistic_success_groupsByCurrencySettledOnlyInclusiveRange: groups by currency for SETTLED only; inclusive date range")
        void fetchBeneficiaryFinancialStatistic_success_groupsByCurrencySettledOnlyInclusiveRange() {
            Beneficiary beneficiary = generateBeneficiary();
            LocalDate fromDate = LocalDate.now().minusDays(3);
            LocalDate toDate = LocalDate.now();

            var firstTransaction = generateTransaction(beneficiary.getId(),
                    Currency.USD.name(),
                    BigInteger.valueOf(1000),
                    TransactionFlowType.DEBIT);
            var otherTransaction = generateTransaction(beneficiary.getId(),
                    Currency.USD.name(),
                    BigInteger.valueOf(300),
                    TransactionFlowType.CREDIT);

            Transaction failedNoise = generateTransaction(beneficiary.getId(),
                    Currency.UAH.name(),
                    BigInteger.valueOf(999),
                    TransactionFlowType.CREDIT);
            failedNoise.setStatus(TransactionStatus.FAILED);
            transactionRepository.save(failedNoise);

            var anotherTransaction = generateTransaction(beneficiary.getId(),
                    Currency.EUR.name(),
                    BigInteger.valueOf(300),
                    TransactionFlowType.CREDIT);

            ResponseEntity<BeneficiaryFinancialFlowStatistic> response = restClient.exchange(
                    "/api/v1/analytics/beneficiary/financialFlow?beneficiary_id=%d&from=%s&to=%s&pageSize=10&page=1"
                            .formatted(beneficiary.getId(), fromDate.format(DATE_DMY), toDate.format(DATE_DMY)),
                    HttpMethod.GET,
                    new HttpEntity<>(authenticationHeaders),
                    BeneficiaryFinancialFlowStatistic.class);

            assertThat(response.getStatusCode())
                    .isEqualTo(HttpStatus.OK);

            BeneficiaryFinancialFlowStatistic responseBody = response.getBody();

            assertThat(responseBody)
                    .isNotNull();
            assertThat(responseBody.getBeneficiary())
                    .isNotNull();
            assertThat(responseBody.getBeneficiary().getId())
                    .isEqualTo(beneficiary.getId());

            var financialStatistics = responseBody.getFinancialStatistic();

            assertThat(financialStatistics).isNotNull().isNotEmpty();

            var usdStatistic = financialStatistics.stream()
                    .filter(statistic -> Currency.USD.name().equals(statistic.getCurrency()))
                    .findFirst()
                    .orElseThrow();

            var eurStatistic = financialStatistics.stream()
                    .filter(statistic -> Currency.EUR.name().equals(statistic.getCurrency()))
                    .findFirst()
                    .orElseThrow();

            assertThat(usdStatistic.getTotalDebit())
                    .isEqualTo(firstTransaction.getAmount());
            assertThat(usdStatistic.getTotalCredit())
                    .isEqualTo(otherTransaction.getAmount());
            assertThat(eurStatistic.getTotalDebit())
                    .isEqualTo(BigInteger.ZERO);
            assertThat(eurStatistic.getTotalCredit())
                    .isEqualTo(anotherTransaction.getAmount());
        }

        @Test
        @DisplayName("fetchBeneficiaryFinancialStatistic_fails_missingBeneficiaryIdReturns400: missing beneficiary_id returns 400")
        void fetchBeneficiaryFinancialStatistic_fails_missingBeneficiaryIdReturns400() {
            ResponseEntity<String> response = restClient.exchange("/api/v1/analytics/beneficiary/financialFlow?pageSize=10&page=1",
                    HttpMethod.GET,
                    new HttpEntity<>(authenticationHeaders),
                    String.class);
            assertThat(response.getStatusCode())
                    .isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }
}