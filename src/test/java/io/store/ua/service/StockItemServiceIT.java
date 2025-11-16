package io.store.ua.service;

import io.store.ua.AbstractIT;
import io.store.ua.entity.*;
import io.store.ua.entity.immutable.StockItemHistory;
import io.store.ua.enums.StockItemStatus;
import io.store.ua.exceptions.BusinessException;
import io.store.ua.exceptions.NotFoundException;
import io.store.ua.models.dto.StockItemDTO;
import jakarta.validation.ValidationException;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.math.BigInteger;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StockItemServiceIT extends AbstractIT {
    @Autowired
    private StockItemService stockItemService;

    private Warehouse warehouseA;
    private Warehouse warehouseB;
    private Warehouse warehouseC;
    private Product productA;
    private Product productB;
    private Product productC;
    private StockItemGroup stockItemGroupA;
    private StockItemGroup stockItemGroupB;
    private StockItemGroup stockItemGroupC;
    private StorageSection section0WarehouseA;
    private StorageSection section1WarehouseA;
    private StorageSection section0WarehouseB;

    @BeforeEach
    void setUp() {
        stockItemGroupA = generateStockItemGroup(true);
        stockItemGroupB = generateStockItemGroup(true);
        stockItemGroupC = generateStockItemGroup(false);
        productA = generateProduct();
        productB = generateProduct();
        productC = generateProduct();
        warehouseA = generateWarehouse();
        warehouseB = generateWarehouse();
        warehouseC = generateWarehouse();
        section0WarehouseA = generateStorageSection(warehouseA.getId());
        section1WarehouseA = generateStorageSection(warehouseA.getId());
        section0WarehouseB = generateStorageSection(warehouseB.getId());
    }

    private StockItem generateStockItem(Product product,
                                        StockItemGroup group,
                                        Warehouse warehouse,
                                        boolean isActive,
                                        int availableQuantity,
                                        Long sectionId) {
        return stockItemRepository.save(StockItem.builder()
                .productId(product.getId())
                .stockItemGroupId(group.getId())
                .warehouseId(warehouse.getId())
                .expiryDate(LocalDate.now().plusDays(RandomUtils.secure().randomInt(15, 120)))
                .availableQuantity(BigInteger.valueOf(availableQuantity))
                .status(availableQuantity > 0 ? StockItemStatus.AVAILABLE : StockItemStatus.OUT_OF_STOCK)
                .isActive(isActive)
                .storageSectionId(sectionId)
                .build());
    }

    private long fetchHistoryCount(Long stockItemId) {
        return stockItemHistoryRepository.countByStockItemId(stockItemId);
    }

    private StockItemHistory fetchLatestHistory(Long stockItemId) {
        return stockItemHistoryRepository.findByStockItemId(stockItemId, PageRequest.of(0, 1,
                        Sort.by(Sort.Direction.DESC, StockItemHistory.Fields.createdAt)))
                .getContent()
                .getFirst();
    }

    @Nested
    @DisplayName("findBy(...)")
    class FindByTests {
        @Test
        @DisplayName("findBy_success_filtersByActivity")
        void findBy_success_onlyActive() {
            StockItem stockItem = generateStockItem(productA,
                    stockItemGroupA,
                    warehouseA,
                    true,
                    5, null);
            StockItem stockItemEntityInInactiveGroup = generateStockItem(productB,
                    stockItemGroupC,
                    warehouseA,
                    true,
                    5, null);
            StockItem incativeStockItem = generateStockItem(productC, stockItemGroupA,
                    warehouseA,
                    false,
                    5, null);

            List<StockItem> stockItems = stockItemService.findBy(List.of(warehouseA.getId()),
                    List.of(productA.getId()),
                    List.of(stockItemGroupA.getId()),
                    List.of(StockItemStatus.AVAILABLE.name()),
                    null,
                    true,
                    true,
                    50,
                    1);

            assertThat(stockItems)
                    .extracting(StockItem::getId)
                    .containsExactly(stockItem.getId());
            assertThat(stockItems)
                    .extracting(StockItem::getId)
                    .doesNotContain(stockItemEntityInInactiveGroup.getId(), incativeStockItem.getId());
        }

        @Test
        @DisplayName("findBy_success_filtersByWarehouseAndStatusLists")
        void findBy_success_filtersByWarehouseAndStatusLists() {
            StockItem stockItem = generateStockItem(productA,
                    stockItemGroupA,
                    warehouseA,
                    true,
                    10, null);
            StockItem otherStockItem = generateStockItem(productB,
                    stockItemGroupB,
                    warehouseB,
                    true,
                    0, null);

            List<StockItem> stockItems = stockItemService.findBy(List.of(warehouseA.getId()),
                    null,
                    null,
                    List.of(StockItemStatus.AVAILABLE.name()),
                    null,
                    null,
                    null,
                    20,
                    1);

            assertThat(stockItems)
                    .extracting(StockItem::getId)
                    .contains(stockItem.getId())
                    .doesNotContain(otherStockItem.getId());
        }

        @Test
        @DisplayName("findBy_success_allFiltersInLists")
        void findBy_success_allFiltersInLists() {
            StockItem stockItem = generateStockItem(productA,
                    stockItemGroupA,
                    warehouseA,
                    true,
                    7, null);
            generateStockItem(productB,
                    stockItemGroupA,
                    warehouseA,
                    RandomUtils.secure().randomBoolean(),
                    1, null);
            generateStockItem(productA,
                    stockItemGroupA,
                    warehouseC,
                    RandomUtils.secure().randomBoolean(),
                    1, null);

            List<StockItem> stockItems = stockItemService.findBy(List.of(warehouseA.getId()),
                    List.of(productA.getId()),
                    List.of(stockItemGroupA.getId()),
                    List.of(StockItemStatus.AVAILABLE.name()),
                    null,
                    true,
                    true,
                    10,
                    1);

            assertThat(stockItems)
                    .extracting(StockItem::getId)
                    .containsExactly(stockItem.getId());
        }

        @Test
        @DisplayName("findBy_success_filterByStorageSectionIds")
        void findBy_success_filterByStorageSectionIds() {
            StockItem stockItem = generateStockItem(productA,
                    stockItemGroupA,
                    warehouseB,
                    true,
                    3,
                    section0WarehouseA.getId());
            generateStockItem(productC,
                    stockItemGroupA,
                    warehouseC,
                    true,
                    3,
                    section1WarehouseA.getId());
            generateStockItem(productB,
                    stockItemGroupA,
                    warehouseB,
                    true,
                    3,
                    section0WarehouseB.getId());

            List<StockItem> stockItems = stockItemService.findBy(null,
                    List.of(productA.getId()),
                    List.of(stockItemGroupA.getId()),
                    List.of(StockItemStatus.AVAILABLE.name()),
                    List.of(section0WarehouseA.getId()),
                    true,
                    true,
                    50,
                    1);

            assertThat(stockItems)
                    .extracting(StockItem::getId)
                    .containsExactly(stockItem.getId());
            assertThat(stockItems)
                    .allMatch(stockItemEntity -> section0WarehouseA.getId().equals(stockItemEntity.getStorageSectionId()));
        }

        @Test
        @DisplayName("findBy_fail_statusNullInList")
        void findBy_fail_statusNullInList() {
            assertThatThrownBy(() -> stockItemService.findBy(null,
                    null,
                    null,
                    Arrays.asList(StockItemStatus.AVAILABLE.name(), null),
                    null,
                    null,
                    null,
                    10,
                    1))
                    .isInstanceOf(ValidationException.class);
        }

        @Test
        @DisplayName("findBy_fail_invalidStatusString")
        void findBy_fails_whenInvalidStatusString() {
            assertThatThrownBy(() -> stockItemService.findBy(null,
                    null,
                    null,
                    List.of("WRONG_STATUS"),
                    null,
                    null,
                    null,
                    10,
                    1))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("findById(id: Long)")
    class FindByIdTests {
        @Test
        @DisplayName("findById_success")
        void findById_success() {
            StockItem stockItem = generateStockItem(productA,
                    stockItemGroupA,
                    warehouseA,
                    RandomUtils.secure().randomBoolean(),
                    4,
                    null);

            StockItem stockItemFetch = stockItemService.findById(stockItem.getId());

            assertThat(stockItemFetch.getId()).isEqualTo(stockItem.getId());
        }

        @Test
        @DisplayName("findById_fails_whenIdIsNull")
        void findById_fails_whenIdIsNull() {
            assertThatThrownBy(() -> stockItemService.findById(null))
                    .isInstanceOf(ValidationException.class);
        }

        @Test
        @DisplayName("findById_fail_notFound")
        void findById_fail_notFound() {
            assertThatThrownBy(() -> stockItemService.findById(Long.MAX_VALUE))
                    .isInstanceOf(NotFoundException.class);
        }
    }

    @Nested
    @DisplayName("create(stockItemDTO: StockItemDTO)")
    class CreateTests {
        @Test
        @DisplayName("create_success_setsStatusAVAILABLE_whenQty>0")
        void create_success_whenQuantitiesAvailable_thenStatusIsAvailable() {
            StockItemDTO stockItemDTO = StockItemDTO.builder()
                    .productId(productA.getId())
                    .stockItemGroupId(stockItemGroupA.getId())
                    .warehouseId(warehouseA.getId())
                    .expiryDate(LocalDate.now().plusDays(30))
                    .availableQuantity(BigInteger.TEN)
                    .isActive(true)
                    .storageSectionId(section0WarehouseA.getId())
                    .build();

            StockItem stockItem = stockItemService.create(stockItemDTO);

            assertThat(stockItem)
                    .isNotNull();
            assertThat(stockItem.getStatus())
                    .isEqualTo(StockItemStatus.AVAILABLE);
            assertThat(stockItem.getStorageSectionId())
                    .isEqualTo(section0WarehouseA.getId());
        }

        @Test
        @DisplayName("create_success_setsStatusOUT_OF_STOCK_whenQtyIsZero")
        void create_success_whenQuantitiesAreZero_thenStatusIsOutOfStock() {
            StockItemDTO stockItemDTO = StockItemDTO.builder()
                    .productId(productA.getId())
                    .stockItemGroupId(stockItemGroupA.getId())
                    .warehouseId(warehouseA.getId())
                    .expiryDate(LocalDate.now().plusDays(10))
                    .availableQuantity(BigInteger.ZERO)
                    .isActive(true)
                    .storageSectionId(section1WarehouseA.getId())
                    .build();

            StockItem stockItem = stockItemService.create(stockItemDTO);

            assertThat(stockItem.getStatus())
                    .isEqualTo(StockItemStatus.OUT_OF_STOCK);
            assertThat(stockItem.getStorageSectionId())
                    .isEqualTo(section1WarehouseA.getId());
        }

        @Test
        @DisplayName("create_fails_whenNullPayload")
        void create_fails_whenNullPayload() {
            assertThatThrownBy(() -> stockItemService.create(null))
                    .isInstanceOf(ValidationException.class);
        }
    }

    @Nested
    @DisplayName("update(stockItemDTO: StockItemDTO)")
    class UpdateTests {
        @Test
        @DisplayName("update_success_recomputesStatus_onQtyChange_and_historyCreated")
        void update_success_whenQuantitiesChange_thenStatusRecomputing_andHistory() {
            StockItem stockItem = generateStockItem(productA,
                    stockItemGroupA,
                    warehouseA,
                    true,
                    5,
                    null);

            long historyCountBefore = fetchHistoryCount(stockItem.getId());

            StockItemDTO stockItemDTO = StockItemDTO.builder()
                    .stockItemId(stockItem.getId())
                    .availableQuantity(BigInteger.ZERO)
                    .isActive(false)
                    .build();

            stockItem = stockItemService.update(stockItemDTO);

            StockItemHistory stockItemHistory = fetchLatestHistory(stockItem.getId());

            assertThat(stockItem.getAvailableQuantity())
                    .isEqualByComparingTo(BigInteger.ZERO);
            assertThat(stockItem.getStatus())
                    .isEqualTo(StockItemStatus.OUT_OF_STOCK);
            assertThat(stockItem.getIsActive())
                    .isFalse();
            assertThat(fetchHistoryCount(stockItem.getId()))
                    .isEqualTo(historyCountBefore + 1);
            assertThat(stockItemHistory.getQuantityBefore())
                    .isEqualTo(BigInteger.valueOf(5));
            assertThat(stockItemHistory.getQuantityAfter())
                    .isEqualTo(BigInteger.ZERO);
            assertThat(stockItemHistory.getOldStatus())
                    .isNotNull();
            assertThat(stockItemHistory.getNewStatus())
                    .isEqualTo(StockItemStatus.OUT_OF_STOCK);
        }

        @Test
        @DisplayName("update_success_changingStatus_toOutOfService_and_historyCreated")
        void update_success_changingStatus_andHistory() {
            StockItem stockItem = generateStockItem(productA,
                    stockItemGroupA,
                    warehouseA,
                    true,
                    8,
                    null);

            long historyCountBefore = fetchHistoryCount(stockItem.getId());

            StockItemDTO stockItemDTO = StockItemDTO.builder()
                    .stockItemId(stockItem.getId())
                    .switchOff(true)
                    .build();

            stockItem = stockItemService.update(stockItemDTO);

            StockItemHistory stockItemHistory = fetchLatestHistory(stockItem.getId());

            assertThat(stockItem.getStatus())
                    .isEqualTo(StockItemStatus.OUT_OF_SERVICE);
            assertThat(fetchHistoryCount(stockItem.getId()))
                    .isEqualTo(historyCountBefore + 1);
            assertThat(stockItemHistory.getOldStatus())
                    .isEqualTo(StockItemStatus.AVAILABLE);
            assertThat(stockItemHistory.getNewStatus())
                    .isEqualTo(StockItemStatus.OUT_OF_SERVICE);
        }

        @Test
        @DisplayName("update_success_changeStorageSection_and_historyCreated")
        void update_success_changeStorageSection_andHistory() {
            StockItem stockItem = generateStockItem(productA,
                    stockItemGroupA,
                    warehouseA,
                    true,
                    3,
                    section0WarehouseA.getId());

            long historyCountBefore = fetchHistoryCount(stockItem.getId());

            StockItemDTO stockItemDTO = StockItemDTO.builder()
                    .stockItemId(stockItem.getId())
                    .storageSectionId(section1WarehouseA.getId())
                    .build();

            stockItem = stockItemService.update(stockItemDTO);

            StockItemHistory stockItemHistory = fetchLatestHistory(stockItem.getId());

            assertThat(stockItem.getStorageSectionId())
                    .isEqualTo(section1WarehouseA.getId());
            assertThat(fetchHistoryCount(stockItem.getId()))
                    .isEqualTo(historyCountBefore + 1);
            assertThat(stockItemHistory.getOldSectionId())
                    .isEqualTo(section0WarehouseA.getId());
            assertThat(stockItemHistory.getNewSectionId())
                    .isEqualTo(section1WarehouseA.getId());
        }

        @Test
        @DisplayName("update_success_nullifySectionFlag_and_historyCreated")
        void update_success_nullifySectionFlag_andHistory() {
            StockItem stockItem = generateStockItem(productA,
                    stockItemGroupA,
                    warehouseA,
                    true,
                    3,
                    section0WarehouseA.getId());

            long historyCountBefore = fetchHistoryCount(stockItem.getId());

            StockItemDTO stockItemDTO = StockItemDTO.builder()
                    .stockItemId(stockItem.getId())
                    .nullifySection(true)
                    .build();

            stockItem = stockItemService.update(stockItemDTO);

            StockItemHistory stockItemHistory = fetchLatestHistory(stockItem.getId());

            assertThat(stockItem.getStorageSectionId())
                    .isNull();
            assertThat(fetchHistoryCount(stockItem.getId()))
                    .isEqualTo(historyCountBefore + 1);
            assertThat(stockItemHistory.getOldSectionId())
                    .isEqualTo(section0WarehouseA.getId());
            assertThat(stockItemHistory.getNewSectionId())
                    .isNull();
        }

        @Test
        @DisplayName("update_success_changeAndNullifyExpiration_with_historyCreated_each_step")
        void update_success_changeAndNullifyExpiration_withHistory() {
            StockItem stockItem = generateStockItem(productA,
                    stockItemGroupA,
                    warehouseA,
                    true,
                    5,
                    null);

            long historyCountBefore = fetchHistoryCount(stockItem.getId());

            LocalDate expiration = LocalDate.now().plusDays(RandomUtils.secure().randomInt(30, 90));

            StockItemDTO stockItemDTO = StockItemDTO.builder()
                    .stockItemId(stockItem.getId())
                    .expiryDate(expiration)
                    .build();

            stockItem = stockItemService.update(stockItemDTO);

            long historyCountAfter = fetchHistoryCount(stockItem.getId());

            StockItemHistory stockItemHistory = fetchLatestHistory(stockItem.getId());

            assertThat(stockItem.getExpiryDate())
                    .isEqualTo(expiration);
            assertThat(historyCountAfter)
                    .isEqualTo(historyCountBefore + 1);
            assertThat(stockItemHistory.getOldExpiration())
                    .isNotNull();
            assertThat(stockItemHistory.getNewExpiration())
                    .isEqualTo(expiration);

            stockItemDTO = StockItemDTO.builder()
                    .stockItemId(stockItem.getId())
                    .nullifyExpiration(true)
                    .build();

            stockItem = stockItemService.update(stockItemDTO);

            StockItemHistory stockItemHistoryEntityAfterNullify = fetchLatestHistory(stockItem.getId());

            assertThat(stockItem.getExpiryDate())
                    .isNull();
            assertThat(fetchHistoryCount(stockItem.getId()))
                    .isEqualTo(historyCountAfter + 1);
            assertThat(stockItemHistoryEntityAfterNullify.getOldExpiration())
                    .isEqualTo(expiration);
            assertThat(stockItemHistoryEntityAfterNullify.getNewExpiration())
                    .isNull();
        }

        @Test
        @DisplayName("update_fails_whenCurrentSectionDoesNotBelongToCurrentWarehouse")
        void update_fails_whenCurrentSectionDoesNotBelongToCurrentWarehouse() {
            StockItem stockItem = generateStockItem(productA,
                    stockItemGroupA,
                    warehouseA,
                    true,
                    3,
                    section0WarehouseA.getId());
            stockItem.setStorageSectionId(section0WarehouseB.getId());

            stockItem = stockItemRepository.save(stockItem);

            StockItemDTO stockItemDTO = StockItemDTO.builder()
                    .stockItemId(stockItem.getId())
                    .isActive(false)
                    .build();

            assertThatThrownBy(() -> stockItemService.update(stockItemDTO))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("update_fails_whenIdAbsent")
        void update_fails_whenIdIsAbsentInRequest() {
            StockItemDTO stockItemDTO = StockItemDTO.builder()
                    .availableQuantity(BigInteger.TEN)
                    .build();

            assertThatThrownBy(() -> stockItemService.update(stockItemDTO))
                    .isInstanceOf(ValidationException.class);
        }

        @Test
        @DisplayName("update_fails_whenItemNotFound")
        void update_fails_whenItemWasNotFound() {
            StockItemDTO stockItemDTO = StockItemDTO.builder()
                    .stockItemId(Long.MAX_VALUE)
                    .availableQuantity(BigInteger.ONE)
                    .build();

            assertThatThrownBy(() -> stockItemService.update(stockItemDTO))
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        @DisplayName("update_success_changeGroup_and_historyCreated")
        void update_success_changeGroup_andHistory() {
            StockItem stockItem = generateStockItem(productA,
                    stockItemGroupA,
                    warehouseA,
                    true,
                    9,
                    null);

            long historyCountBefore = fetchHistoryCount(stockItem.getId());

            StockItemDTO stockItemDTO = StockItemDTO.builder()
                    .stockItemId(stockItem.getId())
                    .stockItemGroupId(stockItemGroupB.getId())
                    .build();

            stockItem = stockItemService.update(stockItemDTO);

            long historyCountAfter = fetchHistoryCount(stockItem.getId());

            StockItemHistory stockItemHistory = fetchLatestHistory(stockItem.getId());

            assertThat(stockItem.getStockItemGroupId())
                    .isEqualTo(stockItemGroupB.getId());
            assertThat(historyCountAfter)
                    .isEqualTo(historyCountBefore + 1);
            assertThat(stockItemHistory.getOldGroupId())
                    .isEqualTo(stockItemGroupA.getId());
            assertThat(stockItemHistory.getNewGroupId())
                    .isEqualTo(stockItemGroupB.getId());
        }

        @Test
        @DisplayName("update_success_changeWarehouse_and_historyCreated")
        void update_success_changeWarehouse_andHistory() {
            StockItem stockItem = generateStockItem(productA,
                    stockItemGroupA,
                    warehouseA,
                    true,
                    6,
                    null);

            long historyCountBefore = fetchHistoryCount(stockItem.getId());

            StockItemDTO stockItemDTO = StockItemDTO.builder()
                    .stockItemId(stockItem.getId())
                    .warehouseId(warehouseB.getId())
                    .build();

            stockItem = stockItemService.update(stockItemDTO);

            StockItemHistory stockItemHistory = fetchLatestHistory(stockItem.getId());

            assertThat(stockItem.getWarehouseId())
                    .isEqualTo(warehouseB.getId());
            assertThat(fetchHistoryCount(stockItem.getId()))
                    .isEqualTo(historyCountBefore + 1);
            assertThat(stockItemHistory.getOldWarehouseId())
                    .isEqualTo(warehouseA.getId());
            assertThat(stockItemHistory.getNewWarehouseId())
                    .isEqualTo(warehouseB.getId());
        }

        @Test
        @DisplayName("update_success_changeWarehouse_and_setSectionFromAnotherWarehouse_current-behavior")
        void update_success_changeWarehouse_andSetSectionFromAnotherWarehouse_currentBehavior() {
            StockItem stockItem = generateStockItem(productA,
                    stockItemGroupA,
                    warehouseA,
                    true,
                    4,
                    null);

            long historyCountBefore = fetchHistoryCount(stockItem.getId());

            StockItemDTO stockItemDTO = StockItemDTO.builder()
                    .stockItemId(stockItem.getId())
                    .warehouseId(warehouseB.getId())
                    .storageSectionId(section0WarehouseB.getId())
                    .build();

            stockItem = stockItemService.update(stockItemDTO);

            StockItemHistory stockItemHistory = fetchLatestHistory(stockItem.getId());

            assertThat(stockItem.getWarehouseId())
                    .isEqualTo(warehouseB.getId());
            assertThat(stockItem.getStorageSectionId())
                    .isEqualTo(section0WarehouseB.getId());
            assertThat(fetchHistoryCount(stockItem.getId()))
                    .isEqualTo(historyCountBefore + 1);
            assertThat(stockItemHistory.getOldWarehouseId())
                    .isEqualTo(warehouseA.getId());
            assertThat(stockItemHistory.getNewWarehouseId())
                    .isEqualTo(warehouseB.getId());
            assertThat(stockItemHistory.getOldSectionId())
                    .isNull();
            assertThat(stockItemHistory.getNewSectionId())
                    .isEqualTo(section0WarehouseB.getId());
        }

        @Test
        @DisplayName("update_success_noop_fields_creates_statusSnapshot_only")
        void update_success_noop_createsStatusSnapshotOnly() {
            StockItem stockItem = generateStockItem(productA,
                    stockItemGroupA,
                    warehouseA,
                    true,
                    11,
                    null);

            long historyCountBefore = fetchHistoryCount(stockItem.getId());

            StockItemDTO stockItemDTO = StockItemDTO.builder()
                    .stockItemId(stockItem.getId())
                    .build();

            stockItem = stockItemService.update(stockItemDTO);

            StockItemHistory stockItemHistory = fetchLatestHistory(stockItem.getId());

            assertThat(stockItem.getStatus())
                    .isEqualTo(StockItemStatus.AVAILABLE);
            assertThat(fetchHistoryCount(stockItem.getId()))
                    .isEqualTo(historyCountBefore + 1);
            assertThat(stockItemHistory.getOldGroupId())
                    .isNull();
            assertThat(stockItemHistory.getNewGroupId())
                    .isNull();
            assertThat(stockItemHistory.getQuantityBefore())
                    .isNull();
            assertThat(stockItemHistory.getQuantityAfter())
                    .isNull();
            assertThat(stockItemHistory.getOldExpiration())
                    .isNull();
            assertThat(stockItemHistory.getNewExpiration())
                    .isNull();
            assertThat(stockItemHistory.getOldSectionId())
                    .isNull();
            assertThat(stockItemHistory.getNewSectionId())
                    .isNull();
            assertThat(stockItemHistory.getNewStatus())
                    .isEqualTo(StockItemStatus.AVAILABLE);
        }

        @Test
        @DisplayName("update_success_quantitySame_creates_history_without_quantity_fields")
        void update_success_quantitySame_historyWithoutQuantityFields() {
            StockItem stockItem = generateStockItem(productA,
                    stockItemGroupA,
                    warehouseA,
                    true,
                    13,
                    null);

            long historyCountBefore = fetchHistoryCount(stockItem.getId());

            StockItemDTO stockItemDTO = StockItemDTO.builder()
                    .stockItemId(stockItem.getId())
                    .availableQuantity(stockItem.getAvailableQuantity())
                    .build();

            stockItem = stockItemService.update(stockItemDTO);

            StockItemHistory stockItemHistory = fetchLatestHistory(stockItem.getId());

            assertThat(stockItem.getAvailableQuantity())
                    .isEqualByComparingTo(stockItem.getAvailableQuantity());
            assertThat(fetchHistoryCount(stockItem.getId()))
                    .isEqualTo(historyCountBefore + 1);
            assertThat(stockItemHistory.getQuantityBefore())
                    .isNull();
            assertThat(stockItemHistory.getQuantityAfter())
                    .isNull();
        }

        @Test
        @DisplayName("update_success_expirationEqual_still_records_expiration_using_flag")
        void update_success_expirationEqual_recordsWithFlag() {
            StockItem stockItem = generateStockItem(productA,
                    stockItemGroupA,
                    warehouseA,
                    true,
                    7,
                    null);

            LocalDate expiration = stockItem.getExpiryDate();

            long historyCountBefore = fetchHistoryCount(stockItem.getId());

            StockItemDTO stockItemDTO = StockItemDTO.builder()
                    .stockItemId(stockItem.getId())
                    .expiryDate(expiration)
                    .build();

            stockItem = stockItemService.update(stockItemDTO);

            StockItemHistory stockItemHistory = fetchLatestHistory(stockItem.getId());

            assertThat(stockItem.getExpiryDate())
                    .isEqualTo(expiration);
            assertThat(fetchHistoryCount(stockItem.getId()))
                    .isEqualTo(historyCountBefore + 1);
            assertThat(stockItemHistory.getOldExpiration())
                    .isEqualTo(expiration);
            assertThat(stockItemHistory.getNewExpiration())
                    .isEqualTo(expiration);
        }

        @Test
        @DisplayName("update_success_sectionPrecedence_setSection_wins_over_nullifySection")
        void update_success_sectionPrecedence_setWinsOverNullify() {
            StockItem stockItem = generateStockItem(productA,
                    stockItemGroupA,
                    warehouseA,
                    true,
                    5,
                    section0WarehouseA.getId());

            long historyCountBefore = fetchHistoryCount(stockItem.getId());

            StockItemDTO stockItemDTO = StockItemDTO.builder()
                    .stockItemId(stockItem.getId())
                    .storageSectionId(section1WarehouseA.getId())
                    .nullifySection(true)
                    .build();

            stockItem = stockItemService.update(stockItemDTO);

            StockItemHistory stockItemHistory = fetchLatestHistory(stockItem.getId());

            assertThat(stockItem.getStorageSectionId())
                    .isEqualTo(section1WarehouseA.getId());
            assertThat(fetchHistoryCount(stockItem.getId()))
                    .isEqualTo(historyCountBefore + 1);
            assertThat(stockItemHistory.getOldSectionId())
                    .isEqualTo(section0WarehouseA.getId());
            assertThat(stockItemHistory.getNewSectionId())
                    .isEqualTo(section1WarehouseA.getId());
        }

        @Test
        @DisplayName("update_success_expirationPrecedence_setDate_wins_over_nullifyExpiration")
        void update_success_expirationPrecedence_setWinsOverNullify() {
            StockItem stockItem = generateStockItem(productA,
                    stockItemGroupA,
                    warehouseA,
                    true,
                    6,
                    null);

            LocalDate expiration = LocalDate.now().plusDays(RandomUtils.secure().randomInt(20, 60));

            long historyCountBefore = fetchHistoryCount(stockItem.getId());

            StockItemDTO stockItemDTO = StockItemDTO.builder()
                    .stockItemId(stockItem.getId())
                    .expiryDate(expiration)
                    .nullifyExpiration(true)
                    .build();

            stockItem = stockItemService.update(stockItemDTO);

            StockItemHistory stockItemHistory = fetchLatestHistory(stockItem.getId());

            assertThat(stockItem.getExpiryDate())
                    .isEqualTo(expiration);
            assertThat(fetchHistoryCount(stockItem.getId()))
                    .isEqualTo(historyCountBefore + 1);
            assertThat(stockItemHistory.getOldExpiration())
                    .isNotNull();
            assertThat(stockItemHistory.getNewExpiration())
                    .isEqualTo(expiration);
        }

        @Test
        @DisplayName("update_success_qtyPositive_noExplicitStatus_statusRemainsAvailable_and_historySnapshot")
        void update_success_qtyPositive_noExplicitStatus_statusRemainsAvailable_andHistory() {
            StockItem stockItem = generateStockItem(productA,
                    stockItemGroupA,
                    warehouseA,
                    true,
                    14,
                    null);

            long historyCountBefore = fetchHistoryCount(stockItem.getId());

            StockItemDTO stockItemDTO = StockItemDTO.builder()
                    .stockItemId(stockItem.getId())
                    .availableQuantity(BigInteger.valueOf(20))
                    .build();

            stockItem = stockItemService.update(stockItemDTO);

            StockItemHistory stockItemHistory = fetchLatestHistory(stockItem.getId());

            assertThat(stockItem.getStatus())
                    .isEqualTo(StockItemStatus.AVAILABLE);
            assertThat(fetchHistoryCount(stockItem.getId()))
                    .isEqualTo(historyCountBefore + 1);
            assertThat(stockItemHistory.getNewStatus())
                    .isEqualTo(StockItemStatus.AVAILABLE);
        }

        @Test
        @DisplayName("update_success_changeGroup_and_quantityTogether_groupAndQuantityRecorded")
        void update_success_changeGroup_andQuantityTogether_groupAndQuantityRecorded() {
            StockItem stockItem = generateStockItem(productA,
                    stockItemGroupA,
                    warehouseA,
                    true,
                    3,
                    null);

            long historyCountBefore = fetchHistoryCount(stockItem.getId());

            StockItemDTO stockItemDTO = StockItemDTO.builder()
                    .stockItemId(stockItem.getId())
                    .stockItemGroupId(stockItemGroupB.getId())
                    .availableQuantity(BigInteger.valueOf(9))
                    .build();

            stockItem = stockItemService.update(stockItemDTO);

            StockItemHistory stockItemHistory = fetchLatestHistory(stockItem.getId());

            assertThat(stockItem.getStockItemGroupId())
                    .isEqualTo(stockItemGroupB.getId());
            assertThat(stockItem.getAvailableQuantity())
                    .isEqualByComparingTo(BigInteger.valueOf(9));
            assertThat(fetchHistoryCount(stockItem.getId()))
                    .isEqualTo(historyCountBefore + 1);
            assertThat(stockItemHistory.getOldGroupId())
                    .isEqualTo(stockItemGroupA.getId());
            assertThat(stockItemHistory.getNewGroupId())
                    .isEqualTo(stockItemGroupB.getId());
            assertThat(stockItemHistory.getQuantityBefore())
                    .isEqualTo(BigInteger.valueOf(3));
            assertThat(stockItemHistory.getQuantityAfter())
                    .isEqualTo(BigInteger.valueOf(9));
        }
    }
}
