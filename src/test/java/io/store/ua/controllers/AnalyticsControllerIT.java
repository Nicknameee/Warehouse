package io.store.ua.controllers;

import io.store.ua.AbstractIT;
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
import org.springframework.http.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigInteger;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;

class AnalyticsControllerIT extends AbstractIT {
    private static final DateTimeFormatter DATE_DMY = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private HttpHeaders authenticationHeaders;

    private static BigInteger calculateSoldQuantity(StockItemHistory history) {
        BigInteger quantityBefore = history.getQuantityBefore() == null
                ? BigInteger.ZERO : history.getQuantityBefore();
        BigInteger quantityAfter = history.getQuantityAfter() == null
                ? BigInteger.ZERO : history.getQuantityAfter();

        return quantityBefore.compareTo(quantityAfter) > 0
                ? quantityBefore.subtract(quantityAfter) : BigInteger.ZERO;
    }

    private static BigInteger calculateRevenue(StockItemHistory history) {
        BigInteger currentPrice = history.getCurrentProductPrice() == null
                ? BigInteger.ZERO : history.getCurrentProductPrice();

        return calculateSoldQuantity(history).multiply(currentPrice);
    }

    @BeforeAll
    void setUp() {
        authenticationHeaders = generateAuthenticationHeaders();
    }

    @Nested
    @DisplayName("GET /api/v1/analytics/fetchItemSellingStatistic")
    class FetchItemSellingStatisticTests {
        @Test
        @DisplayName("fetchItemSellingStatistic_success_allParams: aggregates by day within range for one item")
        void fetchItemSellingStatistic_success_allParams() {
            var stockItemGroup = generateStockItemGroup(true);
            var warehouse = generateWarehouse();
            var product = generateProduct();
            var item = generateStockItem(product.getId(), stockItemGroup.getId(), warehouse.getId());
            var otherItem = generateStockItem(generateProduct().getId(), stockItemGroup.getId(), warehouse.getId());

            var dateInPast = LocalDate.now().minusDays(3);
            var yesterday = LocalDate.now().minusDays(1);
            var today = LocalDate.now();

            StockItemHistory historyForPast = insertHistoryRow(
                    item.getId(),
                    BigInteger.valueOf(10),
                    BigInteger.valueOf(7),
                    BigInteger.valueOf(5),
                    dateInPast
            );
            StockItemHistory historyForYesterday = insertHistoryRow(
                    item.getId(),
                    BigInteger.valueOf(7),
                    BigInteger.valueOf(6),
                    BigInteger.valueOf(5),
                    yesterday
            );
            StockItemHistory historyForToday = insertHistoryRow(
                    item.getId(),
                    BigInteger.valueOf(9),
                    BigInteger.valueOf(3),
                    BigInteger.valueOf(5),
                    today
            );

            insertHistoryRow(
                    otherItem.getId(),
                    BigInteger.valueOf(10),
                    BigInteger.valueOf(8),
                    BigInteger.valueOf(9),
                    yesterday
            );

            String url = UriComponentsBuilder
                    .fromPath("/api/v1/analytics/fetchItemSellingStatistic")
                    .queryParam("stockItemId", item.getId())
                    .queryParam("from", dateInPast.format(DATE_DMY))
                    .queryParam("to", today.format(DATE_DMY))
                    .queryParam("pageSize", 50)
                    .queryParam("page", 1)
                    .build(true)
                    .toUriString();

            ResponseEntity<ItemSellingStatistic> response = restClient.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(authenticationHeaders),
                    ItemSellingStatistic.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody())
                    .isNotNull();

            ItemSellingStatistic body = response.getBody();
            assertThat(body.getStockItemId()).isEqualTo(item.getId());
            assertThat(body.getStatistics())
                    .isNotNull()
                    .isNotEmpty();

            BigInteger expectedTotalQuantity = calculateSoldQuantity(historyForPast)
                    .add(calculateSoldQuantity(historyForYesterday))
                    .add(calculateSoldQuantity(historyForToday));
            BigInteger expectedTotalRevenue = calculateRevenue(historyForPast)
                    .add(calculateRevenue(historyForYesterday))
                    .add(calculateRevenue(historyForToday));

            assertThat(body.getStatistics()
                    .stream()
                    .map(ItemSellingStatistic.Statistic::getStartDate))
                    .contains(dateInPast, yesterday, today);

            BigInteger actualTotalQuantity = body.getStatistics()
                    .stream()
                    .map(ItemSellingStatistic.Statistic::getSoldQuantity)
                    .reduce(BigInteger.ZERO, BigInteger::add);

            BigInteger actualTotalRevenue = body.getStatistics()
                    .stream()
                    .map(ItemSellingStatistic.Statistic::getTotalRevenueAmount)
                    .reduce(BigInteger.ZERO, BigInteger::add);

            assertThat(actualTotalQuantity).isEqualTo(expectedTotalQuantity);
            assertThat(actualTotalRevenue).isEqualTo(expectedTotalRevenue);
        }
    }

    @Nested
    @DisplayName("GET /api/v1/analytics/fetchBeneficiaryFinancialStatistic")
    class BeneficiaryFinancialFlowTests {
        @Test
        @DisplayName("fetchBeneficiaryFinancialStatistic_success_allParams: groups by currency for SETTLED only; inclusive date range")
        void fetchBeneficiaryFinancialStatistic_success_allParams() {
            var beneficiary = generateBeneficiary();
            var fromDate = LocalDate.now().minusDays(3);
            var toDate = LocalDate.now();

            var debitUsd = generateTransaction(
                    beneficiary.getId(),
                    Currency.USD.name(),
                    BigInteger.valueOf(1000),
                    TransactionFlowType.DEBIT
            );
            var creditUsd = generateTransaction(
                    beneficiary.getId(),
                    Currency.USD.name(),
                    BigInteger.valueOf(300),
                    TransactionFlowType.CREDIT
            );

            var failedUah = generateTransaction(
                    beneficiary.getId(),
                    Currency.UAH.name(),
                    BigInteger.valueOf(999),
                    TransactionFlowType.CREDIT
            );
            failedUah.setStatus(TransactionStatus.FAILED);
            transactionRepository.save(failedUah);

            var creditEur = generateTransaction(
                    beneficiary.getId(),
                    Currency.EUR.name(),
                    BigInteger.valueOf(300),
                    TransactionFlowType.CREDIT
            );

            String url = UriComponentsBuilder
                    .fromPath("/api/v1/analytics/fetchBeneficiaryFinancialStatistic")
                    .queryParam("beneficiaryId", beneficiary.getId())
                    .queryParam("from", fromDate.format(DATE_DMY))
                    .queryParam("to", toDate.format(DATE_DMY))
                    .queryParam("pageSize", 10)
                    .queryParam("page", 1)
                    .build(true)
                    .toUriString();

            ResponseEntity<BeneficiaryFinancialFlowStatistic> response = restClient.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(authenticationHeaders),
                    BeneficiaryFinancialFlowStatistic.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getBeneficiary())
                    .isNotNull();
            assertThat(response.getBody().getBeneficiary().getId())
                    .isEqualTo(beneficiary.getId());

            var statistic = response.getBody().getFinancialStatistic();

            assertThat(statistic)
                    .isNotNull()
                    .isNotEmpty();

            var usdStatistic = statistic.stream()
                    .filter(financialStatistic -> Currency.USD.name().equals(financialStatistic.getCurrency()))
                    .findFirst()
                    .orElseThrow();

            var eurStatistic = statistic.stream()
                    .filter(financialStatistic -> Currency.EUR.name().equals(financialStatistic.getCurrency()))
                    .findFirst()
                    .orElseThrow();

            assertThat(usdStatistic.getTotalDebit())
                    .isEqualTo(debitUsd.getAmount());
            assertThat(usdStatistic.getTotalCredit())
                    .isEqualTo(creditUsd.getAmount());
            assertThat(eurStatistic.getTotalDebit())
                    .isEqualTo(BigInteger.ZERO);
            assertThat(eurStatistic.getTotalCredit())
                    .isEqualTo(creditEur.getAmount());
        }
    }
}
