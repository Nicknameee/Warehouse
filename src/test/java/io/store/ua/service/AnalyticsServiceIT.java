package io.store.ua.service;

import io.store.ua.AbstractIT;
import io.store.ua.entity.Product;
import io.store.ua.entity.StockItem;
import io.store.ua.entity.StockItemGroup;
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
    private StockItemGroup stockItemGroup;
    private StockItem stockItem;

    private static BigInteger sumSoldAmounts(List<ItemSellingStatistic> statistics) {
        return statistics.stream()
                .map(ItemSellingStatistic::getSoldQuantity)
                .reduce(BigInteger.ZERO, BigInteger::add);
    }

    private static BigInteger sumRevenue(List<ItemSellingStatistic> statistics) {
        return statistics.stream()
                .map(ItemSellingStatistic::getTotalRevenueAmount)
                .reduce(BigInteger.ZERO, BigInteger::add);
    }

    private static BigInteger expectedSold(Stream<StockItemHistory> entries) {
        return entries
                .map(entry -> {
                    BigInteger before = entry.getQuantityBefore() == null ? BigInteger.ZERO : entry.getQuantityBefore();
                    BigInteger after = entry.getQuantityAfter() == null ? BigInteger.ZERO : entry.getQuantityAfter();
                    BigInteger diff = before.subtract(after);
                    return diff.signum() > 0 ? diff : BigInteger.ZERO;
                })
                .reduce(BigInteger.ZERO, BigInteger::add);
    }

    private static BigInteger expectedRevenue(Stream<StockItemHistory> entries, BigInteger pricePerUnit) {
        return expectedSold(entries).multiply(pricePerUnit == null ? BigInteger.ZERO : pricePerUnit);
    }

    @BeforeEach
    void setUp() {
        product = createProduct();
        stockItemGroup = createStockItemGroup();
        stockItem = createStockItem(product.getId(), stockItemGroup.getId(), generateWarehouse().getId());
    }

    private StockItemHistory insertHistoryRow(Long stockItemId,
                                              BigInteger quantityBefore,
                                              BigInteger quantityAfter) {
        return stockItemHistoryRepository.save(
                StockItemHistory.builder()
                        .stockItemId(stockItemId)
                        .currentProductPrice(product.getPrice()) // cents
                        .quantityBefore(quantityBefore)
                        .quantityAfter(quantityAfter)
                        .build()
        );
    }

    @Nested
    @DisplayName("fetchItemSellingStatistic(stockItemId, from, to, pageSize, page)")
    class FetchTests {
        @Test
        @DisplayName("success_daily_aggregation")
        void success_daily_aggregation() {
            StockItemHistory firstEntry = insertHistoryRow(stockItem.getId(), BigInteger.valueOf(10), BigInteger.valueOf(8)); // +2
            StockItemHistory secondEntry = insertHistoryRow(stockItem.getId(), BigInteger.valueOf(8), BigInteger.valueOf(7)); // +1
            StockItemHistory thirdEntry = insertHistoryRow(stockItem.getId(), BigInteger.valueOf(20), BigInteger.valueOf(25)); // 0 (increase)

            List<ItemSellingStatistic> statistics = analyticsService.fetchItemSellingStatistic(stockItem.getId(), null, null, 100, 1);

            assertThat(statistics).isNotEmpty();
            assertThat(sumSoldAmounts(statistics))
                    .isEqualTo(expectedSold(Stream.of(firstEntry, secondEntry, thirdEntry)));
            assertThat(sumRevenue(statistics))
                    .isEqualTo(expectedRevenue(Stream.of(firstEntry, secondEntry, thirdEntry), product.getPrice()));
            assertThat(statistics.stream().map(ItemSellingStatistic::getStartDate).distinct().count())
                    .isGreaterThanOrEqualTo(1);
        }

        @Test
        @DisplayName("success_filter_by_item")
        void success_filter_by_item() {
            StockItem otherStockItem = createStockItem(product.getId(), stockItemGroup.getId(), generateWarehouse().getId());

            StockItemHistory primaryEntry = insertHistoryRow(stockItem.getId(), BigInteger.TEN, BigInteger.valueOf(7)); // +3
            insertHistoryRow(otherStockItem.getId(), BigInteger.TEN, BigInteger.valueOf(5)); // +5 (should be excluded)

            List<ItemSellingStatistic> statistics = analyticsService.fetchItemSellingStatistic(stockItem.getId(), null, null, 50, 1);

            assertThat(sumSoldAmounts(statistics)).isEqualTo(expectedSold(Stream.of(primaryEntry)));
            assertThat(sumRevenue(statistics)).isEqualTo(expectedRevenue(Stream.of(primaryEntry), product.getPrice()));
        }

        @Test
        @DisplayName("success_date_range")
        void success_date_range() {
            StockItemHistory includedEntry = insertHistoryRow(stockItem.getId(), BigInteger.TEN, BigInteger.valueOf(9)); // +1

            List<ItemSellingStatistic> statistics = analyticsService.fetchItemSellingStatistic(
                    stockItem.getId(),
                    LocalDate.now(),
                    LocalDate.now().plusDays(1),
                    100,
                    1
            );

            assertThat(sumSoldAmounts(statistics)).isEqualTo(expectedSold(Stream.of(includedEntry)));
            assertThat(sumRevenue(statistics)).isEqualTo(expectedRevenue(Stream.of(includedEntry), product.getPrice()));
            assertThat(statistics).allSatisfy(s -> assertThat(s.getStartDate()).isEqualTo(LocalDate.now()));
        }

        @Test
        @DisplayName("success_pagination")
        void success_pagination() {
            var otherItem = createStockItem(product.getId(), stockItemGroup.getId(), generateWarehouse().getId());

            for (int i = 0; i < 10; i++) {
                if (i % 2 == 0) {
                    insertHistoryRow(stockItem.getId(), BigInteger.TEN, BigInteger.valueOf(9));
                } else {
                    insertHistoryRow(otherItem.getId(), BigInteger.TEN, BigInteger.valueOf(9));
                }
            }

            var firstPage = analyticsService.fetchItemSellingStatistic(null, null, null, 1, 1);
            var otherPage = analyticsService.fetchItemSellingStatistic(null, null, null, 1, 2);

            assertThat(firstPage).hasSize(1);
            assertThat(otherPage).hasSize(1);
            assertThat(otherPage).doesNotContainAnyElementsOf(firstPage);
        }

        @ParameterizedTest(name = "fail_invalid_pageSize={0}")
        @ValueSource(ints = {0, -1})
        void fail_invalid_pageSize(int pageSize) {
            assertThatThrownBy(() -> analyticsService.fetchItemSellingStatistic(stockItem.getId(), null, null, pageSize, 1))
                    .isInstanceOf(ConstraintViolationException.class);
        }

        @ParameterizedTest(name = "fail_invalid_page={0}")
        @ValueSource(ints = {0, -1})
        void fail_invalid_page(int page) {
            assertThatThrownBy(() -> analyticsService.fetchItemSellingStatistic(stockItem.getId(), null, null, 10, page))
                    .isInstanceOf(ConstraintViolationException.class);
        }
    }
}
