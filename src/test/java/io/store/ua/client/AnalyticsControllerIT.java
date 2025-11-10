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
import org.springframework.web.util.UriComponentsBuilder;

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

        return quantityBefore.compareTo(quantityAfter) > 0
                ? quantityBefore.subtract(quantityAfter) : BigInteger.ZERO;
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
        @DisplayName("aggregates by day within range for one item")
        void fetchItemSellingStatistic_success_aggregatesByDayWithinRangeForOneItem() {
            var stockItemGroup = generateStockItemGroup(true);
            var warehouse = generateWarehouse();
            var product = generateProduct();
            var item = generateStockItem(product.getId(), stockItemGroup.getId(), warehouse.getId());
            var otherItem = generateStockItem(generateProduct().getId(), stockItemGroup.getId(), warehouse.getId());

            LocalDate dateInPast = LocalDate.now().minusDays(3);
            LocalDate yesterday = LocalDate.now().minusDays(1);
            LocalDate today = LocalDate.now();

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
                    today);

            insertHistoryRow(otherItem.getId(),
                    BigInteger.valueOf(10),
                    BigInteger.valueOf(8),
                    BigInteger.valueOf(9),
                    yesterday);

            String requestUrl = UriComponentsBuilder.fromPath("/api/v1/analytics/itemSelling")
                    .queryParam("stock_item_id", item.getId())
                    .queryParam("from", dateInPast.format(DATE_DMY))
                    .queryParam("to", today.format(DATE_DMY))
                    .queryParam("pageSize", 50)
                    .queryParam("page", 1)
                    .build(true)
                    .toUriString();

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

            assertThat(response.getBody().stream().map(ItemSellingStatistic::getStartDate))
                    .contains(dateInPast, yesterday, today);
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
        @DisplayName("supports pagination by day")
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

            String firstURL = UriComponentsBuilder.fromPath("/api/v1/analytics/itemSelling")
                    .queryParam("stock_item_id", stockItem.getId())
                    .queryParam("pageSize", 1)
                    .queryParam("page", 1)
                    .build(true)
                    .toUriString();

            String otherURL = UriComponentsBuilder.fromPath("/api/v1/analytics/itemSelling")
                    .queryParam("stock_item_id", stockItem.getId())
                    .queryParam("pageSize", 1)
                    .queryParam("page", 2)
                    .build(true)
                    .toUriString();

            ResponseEntity<ItemSellingStatistic[]> firstResponse = restClient.exchange(firstURL,
                    HttpMethod.GET,
                    new HttpEntity<>(authenticationHeaders),
                    ItemSellingStatistic[].class);
            ResponseEntity<ItemSellingStatistic[]> secondResponse = restClient.exchange(otherURL,
                    HttpMethod.GET,
                    new HttpEntity<>(authenticationHeaders),
                    ItemSellingStatistic[].class);

            assertThat(firstResponse.getStatusCode())
                    .isEqualTo(HttpStatus.OK);
            assertThat(secondResponse.getStatusCode())
                    .isEqualTo(HttpStatus.OK);
            assertThat(firstResponse.getBody())
                    .isNotNull()
                    .hasSize(1);
            assertThat(secondResponse.getBody())
                    .isNotNull()
                    .hasSize(1);
            assertThat(firstResponse.getBody()[0].getStartDate())
                    .isNotEqualTo(secondResponse.getBody()[0].getStartDate());
        }

        @Test
        @DisplayName("missing stock_item_id returns 400")
        void fetchItemSellingStatistic_fails_missingStockItemIdReturns400() {
            String url = UriComponentsBuilder.fromPath("/api/v1/analytics/itemSelling")
                    .queryParam("pageSize", 10)
                    .queryParam("page", 1)
                    .build(true)
                    .toUriString();

            ResponseEntity<String> response = restClient.exchange(url,
                    HttpMethod.GET,
                    new HttpEntity<>(authenticationHeaders),
                    String.class);

            assertThat(response.getStatusCode())
                    .isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }

    @Nested
    @DisplayName("GET /api/v1/analytics/beneficiary/financialFlow")
    class BeneficiaryFinancialFlowTests {
        @Test
        @DisplayName("groups by currency for SETTLED only; inclusive date range")
        void fetchBeneficiaryFinancialStatistic_success_groupsByCurrencySettledOnlyInclusiveRange() {
            Beneficiary beneficiary = generateBeneficiary();
            LocalDate fromDate = LocalDate.now().minusDays(3);
            LocalDate toDate = LocalDate.now();

            var debitUsd = generateTransaction(beneficiary.getId(),
                    Currency.USD.name(),
                    BigInteger.valueOf(1000),
                    TransactionFlowType.DEBIT);
            var creditUsd = generateTransaction(beneficiary.getId(),
                    Currency.USD.name(),
                    BigInteger.valueOf(300),
                    TransactionFlowType.CREDIT);

            Transaction failedNoise = generateTransaction(beneficiary.getId(),
                    Currency.UAH.name(),
                    BigInteger.valueOf(999),
                    TransactionFlowType.CREDIT);
            failedNoise.setStatus(TransactionStatus.FAILED);

            transactionRepository.save(failedNoise);

            var creditEur = generateTransaction(beneficiary.getId(),
                    Currency.EUR.name(),
                    BigInteger.valueOf(300),
                    TransactionFlowType.CREDIT);

            String url = UriComponentsBuilder.fromPath("/api/v1/analytics/beneficiary/financialFlow")
                    .queryParam("beneficiary_id", beneficiary.getId())
                    .queryParam("from", fromDate.format(DATE_DMY))
                    .queryParam("to", toDate.format(DATE_DMY))
                    .queryParam("pageSize", 10)
                    .queryParam("page", 1)
                    .build(true)
                    .toUriString();

            ResponseEntity<BeneficiaryFinancialFlowStatistic> response = restClient.exchange(url,
                    HttpMethod.GET,
                    new HttpEntity<>(authenticationHeaders),
                    BeneficiaryFinancialFlowStatistic.class);

            assertThat(response.getStatusCode())
                    .isEqualTo(HttpStatus.OK);

            assertThat(response.getBody())
                    .isNotNull();
            assertThat(response.getBody().getBeneficiary())
                    .isNotNull();
            assertThat(response.getBody().getBeneficiary().getId())
                    .isEqualTo(beneficiary.getId());

            var statistic = response.getBody().getFinancialStatistic();

            assertThat(statistic)
                    .isNotNull()
                    .isNotEmpty();

            var usd = statistic.stream().filter(financialStatistic ->
                            Currency.USD.name().equals(financialStatistic.getCurrency()))
                    .findFirst()
                    .orElseThrow();

            var eur = statistic.stream().filter(financialStatistic ->
                            Currency.EUR.name().equals(financialStatistic.getCurrency()))
                    .findFirst()
                    .orElseThrow();

            assertThat(usd.getTotalDebit())
                    .isEqualTo(debitUsd.getAmount());
            assertThat(usd.getTotalCredit())
                    .isEqualTo(creditUsd.getAmount());
            assertThat(eur.getTotalDebit())
                    .isEqualTo(BigInteger.ZERO);
            assertThat(eur.getTotalCredit())
                    .isEqualTo(creditEur.getAmount());
        }

        @Test
        @DisplayName("missing beneficiary_id returns 400")
        void fetchBeneficiaryFinancialStatistic_fails_missingBeneficiaryIdReturns400() {
            String url = UriComponentsBuilder.fromPath("/api/v1/analytics/beneficiary/financialFlow")
                    .queryParam("pageSize", 10)
                    .queryParam("page", 1)
                    .build(true)
                    .toUriString();

            ResponseEntity<String> response = restClient.exchange(url,
                    HttpMethod.GET,
                    new HttpEntity<>(authenticationHeaders),
                    String.class);

            assertThat(response.getStatusCode())
                    .isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }
}
