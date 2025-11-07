package io.store.ua.service;

import io.store.ua.AbstractIT;
import io.store.ua.entity.Product;
import io.store.ua.entity.StockItem;
import io.store.ua.entity.StockItemGroup;
import io.store.ua.entity.Warehouse;
import io.store.ua.entity.immutable.StockItemHistory;
import io.store.ua.enums.StockItemStatus;
import io.store.ua.exceptions.NotFoundException;
import io.store.ua.models.dto.StockItemHistoryDTO;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.ValidationException;
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
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StockItemHistoryServiceIT extends AbstractIT {
    @Autowired
    private StockItemHistoryService stockItemHistoryService;

    private Warehouse initialWarehouse;
    private Warehouse newWarehouse;
    private StockItem stockItem;
    private Product product;
    private StockItemGroup stockItemGroup;

    @BeforeEach
    void setUp() {
        initialWarehouse = generateWarehouse();
        newWarehouse = generateWarehouse();
        product = createProduct();
        stockItemGroup = createStockItemGroup();
        stockItem = createStockItem(product.getId(), stockItemGroup.getId(), initialWarehouse.getId());
    }

    @Nested
    @DisplayName("save(stockItemHistoryDTO: StockItemHistoryDTO)")
    class SaveTests {
        @Test
        @DisplayName("save_success_warehouseChange")
        void save_success_warehouseChange() {
            StockItemHistoryDTO stockItemHistoryDTO = StockItemHistoryDTO.builder()
                    .stockItemId(stockItem.getId())
                    .oldWarehouseId(initialWarehouse.getId())
                    .newWarehouseId(newWarehouse.getId())
                    .build();

            StockItemHistory result = stockItemHistoryService.save(stockItemHistoryDTO);

            assertThat(result.getId()).isNotNull();
            assertThat(result.getOldWarehouseId()).isEqualTo(initialWarehouse.getId());
            assertThat(result.getNewWarehouseId()).isEqualTo(newWarehouse.getId());
        }

        @Test
        @DisplayName("save_success_availableChange")
        void save_success_availableChange() {
            StockItemHistoryDTO stockItemHistoryDTO = StockItemHistoryDTO.builder()
                    .stockItemId(stockItem.getId())
                    .quantityBefore(BigInteger.valueOf(100))
                    .quantityAfter(BigInteger.valueOf(90))
                    .build();

            StockItemHistory result = stockItemHistoryService.save(stockItemHistoryDTO);

            assertThat(result.getQuantityBefore()).isEqualTo(BigInteger.valueOf(100));
            assertThat(result.getQuantityAfter()).isEqualTo(BigInteger.valueOf(90));
        }

        @Test
        @DisplayName("save_success_expirationChange")
        void save_success_expirationChange() {
            LocalDate oldDate = LocalDate.now().plusDays(1);
            LocalDate newDate = LocalDate.now().plusDays(30);

            StockItemHistoryDTO stockItemHistoryDTO = StockItemHistoryDTO.builder()
                    .stockItemId(stockItem.getId())
                    .oldExpiration(oldDate)
                    .newExpiration(newDate)
                    .build();

            StockItemHistory result = stockItemHistoryService.save(stockItemHistoryDTO);

            assertThat(result.getOldExpiration()).isEqualTo(oldDate);
            assertThat(result.getNewExpiration()).isEqualTo(newDate);
        }

        @Test
        @DisplayName("save_success_statusChange")
        void save_success_statusChange() {
            StockItemHistoryDTO dto = StockItemHistoryDTO.builder()
                    .stockItemId(stockItem.getId())
                    .oldStatus(StockItemStatus.AVAILABLE.name())
                    .newStatus(StockItemStatus.OUT_OF_STOCK.name())
                    .build();

            StockItemHistory result = stockItemHistoryService.save(dto);

            assertThat(result.getOldStatus()).isEqualTo(StockItemStatus.AVAILABLE);
            assertThat(result.getNewStatus()).isEqualTo(StockItemStatus.OUT_OF_STOCK);
        }

        @Test
        @DisplayName("save_success_activityChange")
        void save_success_activityChange() {
            StockItemHistoryDTO stockItemHistoryDTO = StockItemHistoryDTO.builder()
                    .stockItemId(stockItem.getId())
                    .oldActivity(true)
                    .newActivity(false)
                    .build();

            StockItemHistory result = stockItemHistoryService.save(stockItemHistoryDTO);

            assertThat(result.getOldActivity()).isTrue();
            assertThat(result.getNewActivity()).isFalse();
        }

        @Test
        @DisplayName("save_success_onlyRequired")
        void save_success_onlyRequired() {
            StockItemHistoryDTO stockItemHistoryDTO = StockItemHistoryDTO.builder()
                    .stockItemId(stockItem.getId())
                    .build();

            StockItemHistory result = stockItemHistoryService.save(stockItemHistoryDTO);

            assertThat(result.getId()).isNotNull();
            assertThat(result.getStockItemId()).isEqualTo(stockItem.getId());
        }

        @Test
        @DisplayName("save_fail_stockItemNotFound")
        void save_fail_stockItemNotFound() {
            StockItemHistoryDTO stockItemHistoryDTO = StockItemHistoryDTO.builder()
                    .stockItemId(99999L)
                    .build();

            assertThatThrownBy(() -> stockItemHistoryService.save(stockItemHistoryDTO))
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        @DisplayName("save_fail_invalidStatus")
        void save_fail_invalidStatus() {
            StockItemHistoryDTO stockItemHistoryDTO = StockItemHistoryDTO.builder()
                    .stockItemId(stockItem.getId())
                    .oldStatus(RandomStringUtils.secure().nextAlphabetic(6))
                    .newStatus(StockItemStatus.AVAILABLE.name())
                    .build();

            assertThatThrownBy(() -> stockItemHistoryService.save(stockItemHistoryDTO))
                    .isInstanceOf(ValidationException.class);
        }
    }

    @Nested
    @DisplayName("findBy(stockItemId: Long, pageSize: int, page: int)")
    class FindByTests {
        @Test
        @DisplayName("findBy_success_filter")
        void findBy_success_filter() {
            stockItemHistoryRepository.saveAll(List.of(StockItemHistory.builder()
                            .stockItemId(stockItem.getId())
                            .currentProductPrice(product.getPrice())
                            .build(),
                    StockItemHistory.builder().stockItemId(stockItem.getId())
                            .currentProductPrice(product.getPrice())
                            .build(),
                    StockItemHistory.builder()
                            .stockItemId(createStockItem(product.getId(), stockItemGroup.getId(), generateWarehouse().getId()).getId())
                            .currentProductPrice(product.getPrice())
                            .build()
            ));

            List<StockItemHistory> result = stockItemHistoryService.findBy(stockItem.getId(), null, null, 10, 1);

            assertThat(result).allMatch(stockItemHistory -> stockItemHistory.getStockItemId().equals(stockItem.getId()));
        }

        @Test
        @DisplayName("findBy_success_pagination")
        void findBy_success_pagination() {
            stockItemHistoryRepository.saveAll(List.of(
                    StockItemHistory.builder()
                            .stockItemId(stockItem.getId())
                            .currentProductPrice(product.getPrice())
                            .build(),
                    StockItemHistory.builder()
                            .stockItemId(stockItem.getId())
                            .currentProductPrice(product.getPrice())
                            .build(),
                    StockItemHistory.builder()
                            .stockItemId(stockItem.getId())
                            .currentProductPrice(product.getPrice())
                            .build(),
                    StockItemHistory.builder()
                            .stockItemId(stockItem.getId())
                            .currentProductPrice(product.getPrice())
                            .build()
            ));

            List<StockItemHistory> page = stockItemHistoryService.findBy(stockItem.getId(), null, null, 3, 1);
            List<StockItemHistory> histories = stockItemHistoryService.findBy(stockItem.getId(), null, null, 1, 4);

            assertThat(page).hasSize(3);
            assertThat(histories).hasSize(1);
            assertThat(page).doesNotContainAnyElementsOf(histories);
        }

        @Test
        @DisplayName("findBy_success_allWhenNull")
        void findBy_success_allWhenNull() {
            var history = stockItemHistoryRepository.saveAll(List.of(StockItemHistory.builder()
                    .stockItemId(stockItem.getId())
                    .currentProductPrice(product.getPrice())
                    .build()));

            List<StockItemHistory> result = stockItemHistoryService.findBy(null, null, null, 5, 1);

            assertThat(result).hasSize(history.size());
        }

        @Test
        @DisplayName("findBy_success_dateRange")
        void findBy_success_dateRange() {
            LocalDate d1 = LocalDate.now().plusDays(5);
            LocalDate d2 = LocalDate.now().plusDays(10);
            LocalDate d3 = LocalDate.now().plusDays(20);

            stockItemHistoryRepository.saveAll(List.of(StockItemHistory.builder().stockItemId(stockItem.getId())
                            .currentProductPrice(product.getPrice())
                            .oldExpiration(d1)
                            .newExpiration(d2)
                            .build(),
                    StockItemHistory.builder().stockItemId(stockItem.getId())
                            .currentProductPrice(product.getPrice())
                            .oldExpiration(d2)
                            .newExpiration(d3)
                            .build(),
                    StockItemHistory.builder().stockItemId(stockItem.getId())
                            .currentProductPrice(product.getPrice())
                            .oldExpiration(d3)
                            .newExpiration(d3)
                            .build()));

            LocalDateTime from = d2.atStartOfDay();
            LocalDateTime to = d3.atTime(23, 59);

            List<StockItemHistory> itemHistoryServiceBy = stockItemHistoryService.findBy(stockItem.getId(), from, to, 50, 1);

            assertThat(itemHistoryServiceBy).isNotEmpty();
            assertThat(itemHistoryServiceBy).allMatch(h ->
                    (h.getOldExpiration() == null || !h.getOldExpiration().isBefore(from.toLocalDate())) &&
                            (h.getNewExpiration() == null || !h.getNewExpiration().isAfter(to.toLocalDate()))
            );
        }

        @ParameterizedTest(name = "findBy_fail_invalidSize {0}")
        @ValueSource(ints = {0, -1})
        void findBy_fail_invalidSize(int size) {
            assertThatThrownBy(() -> stockItemHistoryService.findBy(null, null, null, size, 1))
                    .isInstanceOf(ConstraintViolationException.class);
        }

        @ParameterizedTest(name = "findBy_fail_invalidPage {0}")
        @ValueSource(ints = {0, -1})
        void findBy_fail_invalidPage(int page) {
            assertThatThrownBy(() -> stockItemHistoryService.findBy(null, null, null, 10, page))
                    .isInstanceOf(ConstraintViolationException.class);
        }
    }
}
