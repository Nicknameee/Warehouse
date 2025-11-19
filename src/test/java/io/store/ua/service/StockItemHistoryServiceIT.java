package io.store.ua.service;

import io.store.ua.AbstractIT;
import io.store.ua.entity.*;
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
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StockItemHistoryServiceIT extends AbstractIT {
    @Autowired
    private StockItemHistoryService stockItemHistoryService;

    private Warehouse warehouseInitial;
    private Warehouse warehouseNew;
    private StorageSection sectionInitial;
    private StorageSection sectionNew;
    private StockItemGroup groupInitial;
    private StockItemGroup groupNew;
    private StockItem stockItem;
    private Product product;

    @BeforeEach
    void setUp() {
        warehouseInitial = generateWarehouse();
        warehouseNew = generateWarehouse();
        sectionInitial = generateStorageSection(warehouseInitial.getId());
        sectionNew = generateStorageSection(warehouseInitial.getId());
        product = generateProduct();
        groupInitial = generateStockItemGroup(true);
        groupNew = generateStockItemGroup(true);
        stockItem = generateStockItem(product.getId(), groupInitial.getId(), warehouseInitial.getId());
    }

    @Nested
    @DisplayName("findBy(stockItemId: Long, from: LocalDateTime, to: LocalDateTime, pageSize: int, page: int)")
    class FindByTests {
        @Test
        @DisplayName("findBy_success_filter")
        void findBy_success_filter() {
            stockItemHistoryRepository.saveAll(List.of(
                    StockItemHistory.builder()
                            .stockItemId(stockItem.getId())
                            .currentProductPrice(product.getPrice())
                            .currency(product.getCurrency())
                            .loggedAt(LocalDateTime.now(Clock.systemUTC()))
                            .build(),
                    StockItemHistory.builder()
                            .stockItemId(stockItem.getId())
                            .currentProductPrice(product.getPrice())
                            .currency(product.getCurrency())
                            .loggedAt(LocalDateTime.now(Clock.systemUTC()))
                            .build(),
                    StockItemHistory.builder()
                            .stockItemId(generateStockItem(product.getId(), groupInitial.getId(), generateWarehouse().getId()).getId())
                            .currentProductPrice(product.getPrice())
                            .currency(product.getCurrency())
                            .loggedAt(LocalDateTime.now(Clock.systemUTC()))
                            .build()
            ));

            var result = stockItemHistoryService.findBy(stockItem.getId(), null, null, 10, 1);

            assertThat(result).allMatch(stockItemHistory -> stockItemHistory.getStockItemId().equals(stockItem.getId()));
        }

        @Test
        @DisplayName("findBy_success_pagination")
        void findBy_success_pagination() {
            stockItemHistoryRepository.saveAll(List.of(
                    StockItemHistory.builder()
                            .stockItemId(stockItem.getId())
                            .currentProductPrice(product.getPrice())
                            .currency(product.getCurrency())
                            .loggedAt(LocalDateTime.now(Clock.systemUTC()))
                            .build(),
                    StockItemHistory.builder()
                            .stockItemId(stockItem.getId())
                            .currentProductPrice(product.getPrice())
                            .currency(product.getCurrency())
                            .loggedAt(LocalDateTime.now(Clock.systemUTC()))
                            .build(),
                    StockItemHistory.builder()
                            .stockItemId(stockItem.getId())
                            .currentProductPrice(product.getPrice())
                            .currency(product.getCurrency())
                            .loggedAt(LocalDateTime.now(Clock.systemUTC()))
                            .build(),
                    StockItemHistory.builder()
                            .stockItemId(stockItem.getId())
                            .currentProductPrice(product.getPrice())
                            .currency(product.getCurrency())
                            .loggedAt(LocalDateTime.now(Clock.systemUTC()))
                            .build()
            ));

            var page = stockItemHistoryService.findBy(stockItem.getId(), null, null, 3, 1);
            var tail = stockItemHistoryService.findBy(stockItem.getId(), null, null, 1, 4);

            assertThat(page).hasSize(3);
            assertThat(tail).hasSize(1);
            assertThat(page).doesNotContainAnyElementsOf(tail);
        }

        @Test
        @DisplayName("findBy_success_allWhenNull")
        void findBy_success_allWhenNull() {
            var saved = stockItemHistoryRepository.saveAll(List.of(StockItemHistory.builder()
                    .stockItemId(stockItem.getId())
                    .currentProductPrice(product.getPrice())
                    .currency(product.getCurrency())
                    .loggedAt(LocalDateTime.now(Clock.systemUTC()))
                    .build()));

            var result = stockItemHistoryService.findBy(null, null, null, 5, 1);

            assertThat(result).hasSize(saved.size());
        }

        @Test
        @DisplayName("findBy_success_dateRange")
        void findBy_success_dateRange() {
            var future5Days = LocalDate.now().plusDays(5);
            var future10Days = LocalDate.now().plusDays(10);
            var future30Days = LocalDate.now().plusDays(30);

            stockItemHistoryRepository.saveAll(List.of(StockItemHistory.builder()
                            .stockItemId(stockItem.getId())
                            .currentProductPrice(product.getPrice())
                            .currency(product.getCurrency())
                            .oldExpiration(future5Days)
                            .newExpiration(future10Days)
                            .loggedAt(LocalDateTime.now(Clock.systemUTC()))
                            .build(),
                    StockItemHistory.builder()
                            .stockItemId(stockItem.getId())
                            .currentProductPrice(product.getPrice())
                            .currency(product.getCurrency())
                            .oldExpiration(future10Days)
                            .newExpiration(future30Days)
                            .loggedAt(LocalDateTime.now(Clock.systemUTC()))
                            .build(),
                    StockItemHistory.builder()
                            .stockItemId(stockItem.getId())
                            .currentProductPrice(product.getPrice())
                            .currency(product.getCurrency())
                            .oldExpiration(future30Days)
                            .newExpiration(future30Days)
                            .loggedAt(LocalDateTime.now(Clock.systemUTC()))
                            .build()
            ));

            var from = LocalDateTime.now(Clock.systemUTC()).minusHours(1L);
            var to = future30Days.atTime(23, 59);

            var result = stockItemHistoryService.findBy(stockItem.getId(), from, to, 50, 1);

            assertThat(result)
                    .isNotEmpty();
            assertThat(result).allMatch(stockItemHistory ->
                    (stockItemHistory.getOldExpiration() == null || !stockItemHistory.getOldExpiration().isBefore(from.toLocalDate())) &&
                            (stockItemHistory.getNewExpiration() == null || !stockItemHistory.getNewExpiration().isAfter(to.toLocalDate()))
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

    @Nested
    @DisplayName("save(stockItemHistoryDTO: StockItemHistoryDTO)")
    class SaveTests {
        @Test
        @DisplayName("save_success_warehouseChange")
        void save_success_warehouseChange() {
            StockItemHistoryDTO stockItemHistoryDTO = StockItemHistoryDTO.builder()
                    .stockItemId(stockItem.getId())
                    .oldWarehouseId(warehouseInitial.getId())
                    .newWarehouseId(warehouseNew.getId())
                    .build();

            StockItemHistory stockItemHistory = stockItemHistoryService.save(stockItemHistoryDTO);

            assertThat(stockItemHistory.getId()).isNotNull();
            assertThat(stockItemHistory.getOldWarehouseId()).isEqualTo(warehouseInitial.getId());
            assertThat(stockItemHistory.getNewWarehouseId()).isEqualTo(warehouseNew.getId());
        }

        @Test
        @DisplayName("save_success_groupChange")
        void save_success_groupChange() {
            StockItemHistoryDTO stockItemHistoryDTO = StockItemHistoryDTO.builder()
                    .stockItemId(stockItem.getId())
                    .oldStockItemGroupId(groupInitial.getId())
                    .newStockItemGroupId(groupNew.getId())
                    .build();

            StockItemHistory stockItemHistory = stockItemHistoryService.save(stockItemHistoryDTO);

            assertThat(stockItemHistory.getOldGroupId()).isEqualTo(groupInitial.getId());
            assertThat(stockItemHistory.getNewGroupId()).isEqualTo(groupNew.getId());
        }

        @Test
        @DisplayName("save_success_availableChange")
        void save_success_availableChange() {
            StockItemHistoryDTO stockItemHistoryDTO = StockItemHistoryDTO.builder()
                    .stockItemId(stockItem.getId())
                    .quantityBefore(BigInteger.valueOf(100))
                    .quantityAfter(BigInteger.valueOf(90))
                    .build();

            StockItemHistory stockItemHistory = stockItemHistoryService.save(stockItemHistoryDTO);

            assertThat(stockItemHistory.getQuantityBefore()).isEqualTo(BigInteger.valueOf(100));
            assertThat(stockItemHistory.getQuantityAfter()).isEqualTo(BigInteger.valueOf(90));
        }

        @Test
        @DisplayName("save_success_expirationChange_withFlag")
        void save_success_expirationChange_withFlag() {
            LocalDate oldDate = LocalDate.now().plusDays(1);
            LocalDate newDate = LocalDate.now().plusDays(30);

            StockItemHistoryDTO stockItemHistoryDTO = StockItemHistoryDTO.builder()
                    .stockItemId(stockItem.getId())
                    .oldExpiration(oldDate)
                    .newExpiration(newDate)
                    .changeExpiration(true)
                    .build();

            StockItemHistory stockItemHistory = stockItemHistoryService.save(stockItemHistoryDTO);

            assertThat(stockItemHistory.getOldExpiration()).isEqualTo(oldDate);
            assertThat(stockItemHistory.getNewExpiration()).isEqualTo(newDate);
        }

        @Test
        @DisplayName("save_success_expirationNullify_withFlag")
        void save_success_expirationNullify_withFlag() {
            LocalDate localDate = LocalDate.now().plusDays(15);

            StockItemHistoryDTO stockItemHistoryDTO = StockItemHistoryDTO.builder()
                    .stockItemId(stockItem.getId())
                    .oldExpiration(localDate)
                    .newExpiration(null)
                    .changeExpiration(true)
                    .build();

            StockItemHistory stockItemHistory = stockItemHistoryService.save(stockItemHistoryDTO);

            assertThat(stockItemHistory.getOldExpiration()).isEqualTo(localDate);
            assertThat(stockItemHistory.getNewExpiration()).isNull();
        }

        @Test
        @DisplayName("save_ignores_expiration_withoutFlag")
        void save_ignores_expiration_withoutFlag() {
            LocalDate oldDate = LocalDate.now().plusDays(1);
            LocalDate newDate = LocalDate.now().plusDays(30);

            StockItemHistoryDTO stockItemHistoryDTO = StockItemHistoryDTO.builder()
                    .stockItemId(stockItem.getId())
                    .oldExpiration(oldDate)
                    .newExpiration(newDate)
                    .build();

            StockItemHistory stockItemHistory = stockItemHistoryService.save(stockItemHistoryDTO);

            assertThat(stockItemHistory.getOldExpiration()).isNull();
            assertThat(stockItemHistory.getNewExpiration()).isNull();
        }

        @Test
        @DisplayName("save_success_sectionChange_withFlag")
        void save_success_sectionChange_withFlag() {
            StockItemHistoryDTO stockItemHistoryDTO = StockItemHistoryDTO.builder()
                    .stockItemId(stockItem.getId())
                    .oldSectionId(sectionInitial.getId())
                    .newSectionId(sectionNew.getId())
                    .changeSection(true)
                    .build();

            StockItemHistory stockItemHistory = stockItemHistoryService.save(stockItemHistoryDTO);

            assertThat(stockItemHistory.getOldSectionId()).isEqualTo(sectionInitial.getId());
            assertThat(stockItemHistory.getNewSectionId()).isEqualTo(sectionNew.getId());
        }

        @Test
        @DisplayName("save_success_sectionNullify_withFlag")
        void save_success_sectionNullify_withFlag() {
            StockItemHistoryDTO stockItemHistoryDTO = StockItemHistoryDTO.builder()
                    .stockItemId(stockItem.getId())
                    .oldSectionId(sectionInitial.getId())
                    .newSectionId(null)
                    .changeSection(true)
                    .build();

            StockItemHistory stockItemHistory = stockItemHistoryService.save(stockItemHistoryDTO);

            assertThat(stockItemHistory.getOldSectionId()).isEqualTo(sectionInitial.getId());
            assertThat(stockItemHistory.getNewSectionId()).isNull();
        }

        @Test
        @DisplayName("save_ignores_section_withoutFlag")
        void save_ignores_section_withoutFlag() {
            StockItemHistoryDTO stockItemHistoryDTO = StockItemHistoryDTO.builder()
                    .stockItemId(stockItem.getId())
                    .oldSectionId(sectionInitial.getId())
                    .newSectionId(sectionNew.getId())
                    .build();

            StockItemHistory stockItemHistory = stockItemHistoryService.save(stockItemHistoryDTO);

            assertThat(stockItemHistory.getOldSectionId()).isNull();
            assertThat(stockItemHistory.getNewSectionId()).isNull();
        }

        @Test
        @DisplayName("save_success_statusChange")
        void save_success_statusChange() {
            StockItemHistoryDTO stockItemHistoryDTO = StockItemHistoryDTO.builder()
                    .stockItemId(stockItem.getId())
                    .oldStatus(StockItemStatus.AVAILABLE.name())
                    .newStatus(StockItemStatus.OUT_OF_STOCK.name())
                    .build();

            StockItemHistory stockItemHistory = stockItemHistoryService.save(stockItemHistoryDTO);

            assertThat(stockItemHistory.getOldStatus()).isEqualTo(StockItemStatus.AVAILABLE);
            assertThat(stockItemHistory.getNewStatus()).isEqualTo(StockItemStatus.OUT_OF_STOCK);
        }

        @Test
        @DisplayName("save_success_activityChange")
        void save_success_activityChange() {
            StockItemHistoryDTO stockItemHistoryDTO = StockItemHistoryDTO.builder()
                    .stockItemId(stockItem.getId())
                    .oldActivity(true)
                    .newActivity(false)
                    .build();

            StockItemHistory stockItemHistory = stockItemHistoryService.save(stockItemHistoryDTO);

            assertThat(stockItemHistory.getOldActivity()).isTrue();
            assertThat(stockItemHistory.getNewActivity()).isFalse();
        }

        @Test
        @DisplayName("save_success_onlyRequired")
        void save_success_onlyRequired() {
            StockItemHistoryDTO stockItemHistoryDTO = StockItemHistoryDTO.builder()
                    .stockItemId(stockItem.getId())
                    .build();

            StockItemHistory stockItemHistory = stockItemHistoryService.save(stockItemHistoryDTO);

            assertThat(stockItemHistory.getId()).isNotNull();
            assertThat(stockItemHistory.getStockItemId()).isEqualTo(stockItem.getId());
        }

        @Test
        @DisplayName("save_fail_stockItemNotFound")
        void save_fail_stockItemNotFound() {
            StockItemHistoryDTO stockItemHistoryDTO = StockItemHistoryDTO.builder()
                    .stockItemId(Long.MAX_VALUE)
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
}
