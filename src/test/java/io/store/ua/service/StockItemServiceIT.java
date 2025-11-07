package io.store.ua.service;

import io.store.ua.AbstractIT;
import io.store.ua.entity.*;
import io.store.ua.enums.StockItemStatus;
import io.store.ua.exceptions.BusinessException;
import io.store.ua.exceptions.NotFoundException;
import io.store.ua.models.data.Address;
import io.store.ua.models.data.WorkingHours;
import io.store.ua.models.dto.StockItemDTO;
import jakarta.validation.ValidationException;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
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

    private StorageSection sectionA1;
    private StorageSection sectionA2;
    private StorageSection sectionB1;

    @BeforeEach
    void setUp() {
        stockItemGroupA = stockItemGroupRepository.save(StockItemGroup.builder()
                .code(RandomStringUtils.secure().nextAlphanumeric(16))
                .name(RandomStringUtils.secure().nextAlphabetic(12))
                .isActive(true)
                .build());

        stockItemGroupB = stockItemGroupRepository.save(StockItemGroup.builder()
                .code(RandomStringUtils.secure().nextAlphanumeric(16))
                .name(RandomStringUtils.secure().nextAlphabetic(12))
                .isActive(true)
                .build());

        stockItemGroupC = stockItemGroupRepository.save(StockItemGroup.builder()
                .code(RandomStringUtils.secure().nextAlphanumeric(16))
                .name(RandomStringUtils.secure().nextAlphabetic(12))
                .isActive(false)
                .build());

        productA = productRepository.save(Product.builder()
                .code(RandomStringUtils.secure().nextAlphanumeric(24))
                .title(RandomStringUtils.secure().nextAlphabetic(10))
                .description(RandomStringUtils.secure().nextAlphanumeric(40))
                .price(BigInteger.valueOf(RandomUtils.secure().randomInt(10, 500)))
                .weight(BigInteger.valueOf(RandomUtils.secure().randomInt(1, 100)))
                .length(BigInteger.valueOf(RandomUtils.secure().randomInt(1, 100)))
                .width(BigInteger.valueOf(RandomUtils.secure().randomInt(1, 100)))
                .height(BigInteger.valueOf(RandomUtils.secure().randomInt(1, 100)))
                .build());

        productB = productRepository.save(Product.builder()
                .code(RandomStringUtils.secure().nextAlphanumeric(24))
                .title(RandomStringUtils.secure().nextAlphabetic(10))
                .description(RandomStringUtils.secure().nextAlphanumeric(40))
                .price(BigInteger.valueOf(RandomUtils.secure().randomInt(10, 500)))
                .weight(BigInteger.valueOf(RandomUtils.secure().randomInt(1, 100)))
                .length(BigInteger.valueOf(RandomUtils.secure().randomInt(1, 100)))
                .width(BigInteger.valueOf(RandomUtils.secure().randomInt(1, 100)))
                .height(BigInteger.valueOf(RandomUtils.secure().randomInt(1, 100)))
                .build());

        productC = productRepository.save(Product.builder()
                .code(RandomStringUtils.secure().nextAlphanumeric(24))
                .title(RandomStringUtils.secure().nextAlphabetic(10))
                .description(RandomStringUtils.secure().nextAlphanumeric(40))
                .price(BigInteger.valueOf(RandomUtils.secure().randomInt(10, 500)))
                .weight(BigInteger.valueOf(RandomUtils.secure().randomInt(1, 100)))
                .length(BigInteger.valueOf(RandomUtils.secure().randomInt(1, 100)))
                .width(BigInteger.valueOf(RandomUtils.secure().randomInt(1, 100)))
                .height(BigInteger.valueOf(RandomUtils.secure().randomInt(1, 100)))
                .build());

        warehouseA = warehouseRepository.save(Warehouse.builder()
                .code(RandomStringUtils.secure().nextAlphanumeric(12))
                .name(RandomStringUtils.secure().nextAlphabetic(8))
                .address(defaultAddress())
                .workingHours(defaultWorkingHours())
                .phones(List.of("+" + RandomStringUtils.secure().nextNumeric(11)))
                .managerId(user.getId())
                .isActive(true)
                .build());

        warehouseB = warehouseRepository.save(Warehouse.builder()
                .code(RandomStringUtils.secure().nextAlphanumeric(12))
                .name(RandomStringUtils.secure().nextAlphabetic(8))
                .address(defaultAddress())
                .workingHours(defaultWorkingHours())
                .phones(List.of("+" + RandomStringUtils.secure().nextNumeric(11)))
                .managerId(user.getId())
                .isActive(true)
                .build());

        warehouseC = warehouseRepository.save(Warehouse.builder()
                .code(RandomStringUtils.secure().nextAlphanumeric(12))
                .name(RandomStringUtils.secure().nextAlphabetic(8))
                .address(defaultAddress())
                .workingHours(defaultWorkingHours())
                .phones(List.of("+" + RandomStringUtils.secure().nextNumeric(11)))
                .managerId(user.getId())
                .isActive(true)
                .build());

        sectionA1 = storageSectionRepository.save(StorageSection.builder()
                .warehouseId(warehouseA.getId())
                .code("A1-" + RandomStringUtils.secure().nextAlphanumeric(5))
                .build());

        sectionA2 = storageSectionRepository.save(StorageSection.builder()
                .warehouseId(warehouseA.getId())
                .code("A2-" + RandomStringUtils.secure().nextAlphanumeric(5))
                .build());

        sectionB1 = storageSectionRepository.save(StorageSection.builder()
                .warehouseId(warehouseB.getId())
                .code("B1-" + RandomStringUtils.secure().nextAlphanumeric(5))
                .build());
    }

    private StockItem createStockItem(Product product,
                                      StockItemGroup group,
                                      Warehouse warehouse,
                                      boolean isActive,
                                      int available) {
        return createStockItem(product, group, warehouse, isActive, available, null);
    }

    private StockItem createStockItem(Product product,
                                      StockItemGroup group,
                                      Warehouse warehouse,
                                      boolean isActive,
                                      int available,
                                      Long storageSectionId) {
        StockItemDTO stockItemDTO = new StockItemDTO();
        stockItemDTO.setProductId(product.getId());
        stockItemDTO.setStockItemGroupId(group.getId());
        stockItemDTO.setWarehouseId(warehouse.getId());
        stockItemDTO.setExpiryDate(LocalDate.now().plusDays(RandomUtils.secure().randomInt(15, 120)));
        stockItemDTO.setAvailableQuantity(BigInteger.valueOf(available));
        stockItemDTO.setIsActive(isActive);
        stockItemDTO.setStorageSectionId(storageSectionId);
        return stockItemService.create(stockItemDTO);
    }

    private Address defaultAddress() {
        return Address.builder()
                .country("UA")
                .state(RandomStringUtils.secure().nextAlphabetic(6))
                .city(RandomStringUtils.secure().nextAlphabetic(6))
                .street(RandomStringUtils.secure().nextAlphabetic(10))
                .building(RandomStringUtils.secure().nextNumeric(3))
                .postalCode("01001")
                .latitude(new BigDecimal("50.4501"))
                .longitude(new BigDecimal("30.5234"))
                .build();
    }

    private WorkingHours defaultWorkingHours() {
        return WorkingHours.builder()
                .timezone("UTC")
                .days(List.of())
                .build();
    }

    @Nested
    @DisplayName("findAll(pageSize: int, page: int)")
    class FindAllTests {
        @Test
        @DisplayName("findAll_success: returns first page with created items")
        void findAll_success() {
            StockItem availableStockItem = createStockItem(productA,
                    stockItemGroupA,
                    warehouseA,
                    RandomUtils.secure().randomBoolean(),
                    10);
            StockItem reservedStockItem = createStockItem(productB,
                    stockItemGroupB,
                    warehouseB,
                    RandomUtils.secure().randomBoolean(),
                    0);
            StockItem outOfStockItem = createStockItem(productC,
                    stockItemGroupB,
                    warehouseC,
                    RandomUtils.secure().randomBoolean(),
                    0);

            List<StockItem> result = stockItemService.findAll(3, 1);

            assertThat(result).hasSize(3);
            assertThat(result).extracting(StockItem::getId)
                    .contains(availableStockItem.getId(), reservedStockItem.getId(), outOfStockItem.getId());
        }

        @Test
        @DisplayName("findAll_success_empty: returns empty out of bounds page")
        void findAll_success_empty() {
            createStockItem(productA, stockItemGroupA, warehouseA, RandomUtils.secure().randomBoolean(), 3);

            List<StockItem> result = stockItemService.findAll(10, 99);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("findAll_fail_invalidPageSize: throws ValidationException when page size is invalid")
        void findAll_fail_invalidPageSize() {
            assertThatThrownBy(() -> stockItemService.findAll(0, 1))
                    .isInstanceOf(ValidationException.class);
        }

        @Test
        @DisplayName("findAll_fail_invalidPage: throws ValidationException when page is invalid")
        void findAll_fail_invalidPage() {
            assertThatThrownBy(() -> stockItemService.findAll(10, 0))
                    .isInstanceOf(ValidationException.class);
        }

        @Test
        @DisplayName("findBy_success_filtersByActivity")
        void findBy_success_onlyActive() {
            StockItem target = createStockItem(productA, stockItemGroupA, warehouseA, true, 5);
            StockItem inInactiveGroup = createStockItem(productB, stockItemGroupC, warehouseA, true, 5);
            StockItem inactiveItem = createStockItem(productC, stockItemGroupA, warehouseA, false, 5);

            List<StockItem> result = stockItemService.findBy(
                    List.of(warehouseA.getId()),
                    List.of(productA.getId()),
                    List.of(stockItemGroupA.getId()),
                    List.of(StockItemStatus.AVAILABLE.name()),
                    null,
                    true,
                    true,
                    50,
                    1);

            assertThat(result).extracting(StockItem::getId)
                    .containsExactly(target.getId());
            assertThat(result).extracting(StockItem::getId)
                    .doesNotContain(inInactiveGroup.getId(), inactiveItem.getId());
        }
    }

    @Nested
    @DisplayName("findBy(...)")
    class FindByTests {
        @Test
        @DisplayName("findBy_success_filtersByWarehouseAndStatusLists")
        void findBy_success_filtersByWarehouseAndStatusLists() {
            StockItem availableStockItem = createStockItem(productA, stockItemGroupA, warehouseA, true, 10);
            StockItem anotherWarehouseStockItem = createStockItem(productB, stockItemGroupB, warehouseB, true, 0);

            List<Long> warehouseIds = List.of(warehouseA.getId());
            List<String> statuses = List.of(StockItemStatus.AVAILABLE.name());

            List<StockItem> result = stockItemService.findBy(
                    warehouseIds,
                    null,
                    null,
                    statuses,
                    null,
                    null,
                    null,
                    20,
                    1);

            assertThat(result).extracting(StockItem::getId)
                    .contains(availableStockItem.getId())
                    .doesNotContain(anotherWarehouseStockItem.getId());
        }

        @Test
        @DisplayName("findBy_success_allFiltersInLists (with sections null)")
        void findBy_success_allFiltersInLists() {
            StockItem target = createStockItem(productA, stockItemGroupA, warehouseA, true, 7);

            createStockItem(productB, stockItemGroupA, warehouseA, RandomUtils.secure().randomBoolean(), 1);
            createStockItem(productA, stockItemGroupA, warehouseC, RandomUtils.secure().randomBoolean(), 1);

            List<StockItem> result = stockItemService.findBy(
                    List.of(warehouseA.getId()),
                    List.of(productA.getId()),
                    List.of(stockItemGroupA.getId()),
                    List.of(StockItemStatus.AVAILABLE.name()),
                    null,
                    true,
                    true,
                    10,
                    1);

            assertThat(result).extracting(StockItem::getId).containsExactly(target.getId());
        }

        @Test
        @DisplayName("findBy_success_filterByStorageSectionIds")
        void findBy_success_filterByStorageSectionIds() {
            StockItem inA1 = createStockItem(productA, stockItemGroupA, warehouseA, true, 3, sectionA1.getId());
            createStockItem(productA, stockItemGroupA, warehouseC, true, 3, sectionA2.getId());
            createStockItem(productA, stockItemGroupA, warehouseB, true, 3, sectionB1.getId());

            var result = stockItemService.findBy(
                    null,
                    List.of(productA.getId()),
                    List.of(stockItemGroupA.getId()),
                    List.of(StockItemStatus.AVAILABLE.name()),
                    List.of(sectionA1.getId()),
                    true,
                    true,
                    50,
                    1);

            assertThat(result).extracting(StockItem::getId).containsExactly(inA1.getId());
            assertThat(result).allMatch(si -> sectionA1.getId().equals(si.getStorageSectionId()));
        }

        @Test
        @DisplayName("findBy_fail_statusNullInList: ValidationException when invalid status in filter list")
        void findBy_fail_statusNullInList() {
            assertThatThrownBy(() -> stockItemService.findBy(
                    null,
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
        @DisplayName("findBy_fail_invalidStatusString: BusinessException on bad enum value")
        void findBy_fails_whenInvalidStatusString() {
            assertThatThrownBy(() -> stockItemService.findBy(
                    null,
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
            StockItem created = createStockItem(productA, stockItemGroupA, warehouseA, RandomUtils.secure().randomBoolean(), 4);
            StockItem found = stockItemService.findById(created.getId());
            assertThat(found.getId()).isEqualTo(created.getId());
        }

        @Test
        @DisplayName("findById_fails_whenIdIsNull: ValidationException")
        void findById_fails_whenIdIsNull() {
            assertThatThrownBy(() -> stockItemService.findById(null))
                    .isInstanceOf(ValidationException.class);
        }

        @Test
        @DisplayName("findById_fail_notFound: NotFoundException")
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
            StockItemDTO dto = StockItemDTO.builder()
                    .productId(productA.getId())
                    .stockItemGroupId(stockItemGroupA.getId())
                    .warehouseId(warehouseA.getId())
                    .expiryDate(LocalDate.now().plusDays(30))
                    .availableQuantity(BigInteger.TEN)
                    .isActive(true)
                    .storageSectionId(sectionA1.getId())
                    .build();

            StockItem created = stockItemService.create(dto);

            assertThat(created).isNotNull();
            assertThat(created.getStatus()).isEqualTo(StockItemStatus.AVAILABLE);
            assertThat(created.getStorageSectionId()).isEqualTo(sectionA1.getId());
        }

        @Test
        @DisplayName("create_success_setsStatusOUT_OF_STOCK_whenQtyIsZero")
        void create_success_whenQuantitiesAreZero_thenStatusIsOutOfStock() {
            StockItemDTO dto = StockItemDTO.builder()
                    .productId(productA.getId())
                    .stockItemGroupId(stockItemGroupA.getId())
                    .warehouseId(warehouseA.getId())
                    .expiryDate(LocalDate.now().plusDays(10))
                    .availableQuantity(BigInteger.ZERO)
                    .isActive(true)
                    .storageSectionId(sectionA2.getId())
                    .build();

            StockItem created = stockItemService.create(dto);

            assertThat(created.getStatus()).isEqualTo(StockItemStatus.OUT_OF_STOCK);
            assertThat(created.getStorageSectionId()).isEqualTo(sectionA2.getId());
        }

        @Test
        @DisplayName("create_fails_whenNullPayload: ValidationException")
        void create_fails_whenNullPayload() {
            assertThatThrownBy(() -> stockItemService.create(null)).isInstanceOf(ValidationException.class);
        }
    }

    @Nested
    @DisplayName("update(stockItemDTO: StockItemDTO)")
    class UpdateTests {
        @Test
        @DisplayName("update_success_recomputesStatus_onQtyChange")
        void update_success_whenQuantitiesChange_thenStatusRecomputing() {
            StockItem initial = createStockItem(productA, stockItemGroupA, warehouseA, true, 5);

            StockItemDTO updateDTO = new StockItemDTO();
            updateDTO.setId(initial.getId());
            updateDTO.setAvailableQuantity(BigInteger.ZERO);
            updateDTO.setIsActive(false);

            StockItem updated = stockItemService.update(updateDTO);

            assertThat(updated.getAvailableQuantity()).isEqualByComparingTo(BigInteger.ZERO);
            assertThat(updated.getStatus()).isEqualTo(StockItemStatus.OUT_OF_STOCK);
            assertThat(updated.getIsActive()).isFalse();
        }

        @Test
        @DisplayName("update_success_changingStatus_toOutOfService")
        void update_success_changingStatus() {
            StockItem initial = createStockItem(productA, stockItemGroupA, warehouseA, true, 8);

            StockItemDTO updateDTO = new StockItemDTO();
            updateDTO.setId(initial.getId());
            updateDTO.setStatus(StockItemStatus.OUT_OF_SERVICE.name());

            StockItem updated = stockItemService.update(updateDTO);

            assertThat(updated.getStatus()).isEqualTo(StockItemStatus.OUT_OF_SERVICE);
        }

        @Test
        @DisplayName("update_success_changeStorageSection")
        void update_success_changeStorageSection() {
            StockItem initial = createStockItem(productA, stockItemGroupA, warehouseA, true, 3, sectionA1.getId());

            StockItemDTO updateDTO = new StockItemDTO();
            updateDTO.setId(initial.getId());
            updateDTO.setStorageSectionId(sectionA2.getId());

            StockItem updated = stockItemService.update(updateDTO);

            assertThat(updated.getStorageSectionId()).isEqualTo(sectionA2.getId());
        }

        @Test
        @DisplayName("update_fails_whenInvalidStatusString: ValidationException")
        void update_fails_whenInvalidStatus() {
            StockItem initial = createStockItem(productA, stockItemGroupA, warehouseA, true, 8);

            StockItemDTO updateDTO = new StockItemDTO();
            updateDTO.setId(initial.getId());
            updateDTO.setStatus("WRONG_STATUS");

            assertThatThrownBy(() -> stockItemService.update(updateDTO)).isInstanceOf(ValidationException.class);
        }

        @Test
        @DisplayName("update_fails_whenIdIsAbsentInRequest: ValidationException")
        void update_fails_whenIdIsAbsentInRequest() {
            StockItemDTO updateDTO = new StockItemDTO();
            updateDTO.setAvailableQuantity(BigInteger.TEN);

            assertThatThrownBy(() -> stockItemService.update(updateDTO)).isInstanceOf(ValidationException.class);
        }

        @Test
        @DisplayName("update_fails_whenItemWasNotFound: NotFoundException")
        void update_fails_whenItemWasNotFound() {
            StockItemDTO updateDTO = new StockItemDTO();
            updateDTO.setId(999_999L);
            updateDTO.setAvailableQuantity(BigInteger.ONE);

            assertThatThrownBy(() -> stockItemService.update(updateDTO)).isInstanceOf(NotFoundException.class);
        }
    }
}
