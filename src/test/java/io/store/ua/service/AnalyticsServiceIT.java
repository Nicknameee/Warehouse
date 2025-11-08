package io.store.ua.service;

import io.store.ua.AbstractIT;
import io.store.ua.entity.Product;
import io.store.ua.entity.StockItem;
import io.store.ua.entity.immutable.StockItemHistory;
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

    private StockItemHistory insertHistoryRow(Long stockItemId, BigInteger quantityBefore, BigInteger quantityAfter) {
        return stockItemHistoryRepository.save(StockItemHistory.builder()
                .stockItemId(stockItemId)
                .currentProductPrice(product.getPrice())
                .quantityBefore(quantityBefore)
                .quantityAfter(quantityAfter)
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
    class FetchTests {
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
}
