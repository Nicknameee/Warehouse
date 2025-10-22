package io.store.ua.service;

import io.store.ua.AbstractIT;
import io.store.ua.entity.*;
import io.store.ua.enums.Role;
import io.store.ua.enums.Status;
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
    private Product productA;
    private Product productB;
    private StockItemGroup stockItemGroupA;
    private StockItemGroup stockItemGroupB;

    @BeforeEach
    void setUp() {
        var user = userRepository.save(RegularUser.builder()
                .username(RandomStringUtils.secure().nextAlphanumeric(24))
                .password(RandomStringUtils.secure().nextAlphanumeric(32))
                .email(String.format(
                        "%s@%s.%s",
                        RandomStringUtils.secure().nextAlphabetic(8).toLowerCase(),
                        RandomStringUtils.secure().nextAlphabetic(6).toLowerCase(),
                        RandomStringUtils.secure().nextAlphabetic(3).toLowerCase()
                ))
                .role(Role.OWNER)
                .status(Status.ACTIVE)
                .timezone("UTC")
                .build());

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
    }

    private StockItem createPersistedItem(Product product,
                                          StockItemGroup group,
                                          Warehouse warehouse,
                                          int available,
                                          int reserved) {
        StockItemDTO stockItemDTO = new StockItemDTO();
        stockItemDTO.setProductId(product.getId());
        stockItemDTO.setStockItemGroupId(group.getId());
        stockItemDTO.setWarehouseId(warehouse.getId());
        stockItemDTO.setExpiryDate(LocalDate.now().plusDays(RandomUtils.secure().randomInt(15, 120)));
        stockItemDTO.setAvailableQuantity(BigInteger.valueOf(available));
        stockItemDTO.setReservedQuantity(BigInteger.valueOf(reserved));

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
            StockItem firstCreated = createPersistedItem(productA, stockItemGroupA, warehouseA, 10, 2);
            StockItem secondCreated = createPersistedItem(productB, stockItemGroupB, warehouseB, 5, 0);

            List<StockItem> result = stockItemService.findAll(10, 1);

            assertThat(result).extracting(StockItem::getId)
                    .contains(firstCreated.getId(), secondCreated.getId());
        }

        @Test
        @DisplayName("findAll_success_empty: empty page when out of range")
        void findAll_success_empty() {
            createPersistedItem(productA, stockItemGroupA, warehouseA, 3, 1);

            List<StockItem> result = stockItemService.findAll(10, 99);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("findAll_fail_invalidPageSize: @Min violation")
        void findAll_fail_invalidPageSize() {
            assertThatThrownBy(() -> stockItemService.findAll(0, 1))
                    .isInstanceOf(ValidationException.class);
        }

        @Test
        @DisplayName("findAll_fail_invalidPage: @Min violation")
        void findAll_fail_invalidPage() {
            assertThatThrownBy(() -> stockItemService.findAll(10, 0))
                    .isInstanceOf(ValidationException.class);
        }
    }

    @Nested
    @DisplayName("findBy(warehouseIDs: List<Long>, productIDs: List<Long>, stockItemGroupIDs: List<Long>, statuses: List<String>, pageSize: int, page: int)")
    class FindByTests {
        @Test
        @DisplayName("findBy_success_filtersByWarehouseAndStatusLists")
        void findBy_success_filtersByWarehouseAndStatusLists() {
            StockItem a1 = createPersistedItem(productA, stockItemGroupA, warehouseA, 10, 0);
            StockItem a2 = createPersistedItem(productA, stockItemGroupA, warehouseA, 0, 5);
            StockItem b1 = createPersistedItem(productB, stockItemGroupB, warehouseB, 2, 0);

            List<Long> warehouseIds = List.of(warehouseA.getId());
            List<String> statuses = List.of(StockItem.Status.AVAILABLE.name(), StockItem.Status.RESERVED.name());

            List<StockItem> result = stockItemService.findBy(warehouseIds,
                    null,
                    null,
                    statuses,
                    20,
                    1);

            assertThat(result).extracting(StockItem::getId)
                    .contains(a1.getId(), a2.getId())
                    .doesNotContain(b1.getId());
        }

        @Test
        @DisplayName("findBy_success_allFiltersInLists")
        void findBy_success_allFiltersInLists() {
            StockItem target = createPersistedItem(productA, stockItemGroupA, warehouseA, 7, 1);
            createPersistedItem(productB, stockItemGroupA, warehouseA, 1, 0);
            createPersistedItem(productA, stockItemGroupB, warehouseA, 1, 1);
            createPersistedItem(productA, stockItemGroupA, warehouseB, 1, 0);

            List<StockItem> result = stockItemService.findBy(List.of(warehouseA.getId()),
                    List.of(productA.getId()),
                    List.of(stockItemGroupA.getId()),
                    List.of(StockItem.Status.AVAILABLE.name()),
                    10,
                    1);

            assertThat(result).extracting(StockItem::getId).containsExactly(target.getId());
        }

        @Test
        @DisplayName("findBy_fail_statusNullInList: triggers ValidationException")
        void findBy_fail_statusNullInList() {
            assertThatThrownBy(() -> stockItemService.findBy(null,
                    null,
                    null,
                    Arrays.asList(StockItem.Status.AVAILABLE.name(), null),
                    10,
                    1))
                    .isInstanceOf(ValidationException.class);
        }

        @Test
        @DisplayName("findBy_fail_invalidStatusString: BusinessException on bad enum value")
        void findBy_fail_invalidStatusString() {
            createPersistedItem(productA, stockItemGroupA, warehouseA, 5, 0);
            assertThatThrownBy(() -> stockItemService.findBy(null,
                    null,
                    null,
                    List.of("WRONG_STATUS"),
                    10,
                    1))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("findById(id: Long)")
    class FindByIdTests {
        @Test
        @DisplayName("findById_success: returns entity when exists")
        void findById_success() {
            StockItem created = createPersistedItem(productA, stockItemGroupA, warehouseA, 4, 0);

            StockItem found = stockItemService.findById(created.getId());

            assertThat(found.getId()).isEqualTo(created.getId());
        }

        @Test
        @DisplayName("findById_fail_null: @NotNull triggers ValidationException")
        void findById_fail_null() {
            assertThatThrownBy(() -> stockItemService.findById(null))
                    .isInstanceOf(ValidationException.class);
        }

        @Test
        @DisplayName("findById_fail_notFound: throws NotFoundException")
        void findById_fail_notFound() {
            assertThatThrownBy(() -> stockItemService.findById(Long.MAX_VALUE))
                    .isInstanceOf(NotFoundException.class);
        }
    }

    @Nested
    @DisplayName("create(stockItemDTO: StockItemDTO)")
    class CreateTests {
        @Test
        @DisplayName("create_success_setsDerivedStatus_AVAILABLE_whenAvailableGreaterThanZero")
        void create_success_setsDerivedStatus_AVAILABLE_whenAvailableGreaterThanZero() {
            StockItemDTO stockItemDTO = new StockItemDTO();
            stockItemDTO.setProductId(productA.getId());
            stockItemDTO.setStockItemGroupId(stockItemGroupA.getId());
            stockItemDTO.setWarehouseId(warehouseA.getId());
            stockItemDTO.setExpiryDate(LocalDate.now().plusDays(60));
            stockItemDTO.setAvailableQuantity(BigInteger.valueOf(10));
            stockItemDTO.setReservedQuantity(BigInteger.ZERO);

            StockItem created = stockItemService.create(stockItemDTO);

            assertThat(created.getStatus()).isEqualTo(StockItem.Status.AVAILABLE);
        }

        @Test
        @DisplayName("create_success_setsDerivedStatus_RESERVED_whenAvailableZeroAndReservedPositive")
        void create_success_setsDerivedStatus_RESERVED_whenAvailableZeroAndReservedPositive() {
            StockItemDTO stockItemDTO = new StockItemDTO();
            stockItemDTO.setProductId(productA.getId());
            stockItemDTO.setStockItemGroupId(stockItemGroupA.getId());
            stockItemDTO.setWarehouseId(warehouseA.getId());
            stockItemDTO.setExpiryDate(LocalDate.now().plusDays(30));
            stockItemDTO.setAvailableQuantity(BigInteger.ZERO);
            stockItemDTO.setReservedQuantity(BigInteger.ONE);

            StockItem created = stockItemService.create(stockItemDTO);

            assertThat(created.getStatus()).isEqualTo(StockItem.Status.RESERVED);
        }

        @Test
        @DisplayName("create_success_setsDerivedStatus_OUT_OF_STOCK_whenBothZero")
        void create_success_setsDerivedStatus_OUT_OF_STOCK_whenBothZero() {
            StockItemDTO stockItemDTO = new StockItemDTO();
            stockItemDTO.setProductId(productA.getId());
            stockItemDTO.setStockItemGroupId(stockItemGroupA.getId());
            stockItemDTO.setWarehouseId(warehouseA.getId());
            stockItemDTO.setExpiryDate(LocalDate.now().plusDays(10));
            stockItemDTO.setAvailableQuantity(BigInteger.ZERO);
            stockItemDTO.setReservedQuantity(BigInteger.ZERO);

            StockItem created = stockItemService.create(stockItemDTO);

            assertThat(created.getStatus()).isEqualTo(StockItem.Status.OUT_OF_STOCK);
        }

        @Test
        @DisplayName("create_fail_nullPayload: @NotNull triggers ValidationException")
        void create_fail_nullPayload() {
            assertThatThrownBy(() -> stockItemService.create(null))
                    .isInstanceOf(ValidationException.class);
        }
    }

    @Nested
    @DisplayName("update(stockItemDTO: StockItemDTO)")
    class UpdateTests {
        @Test
        @DisplayName("update_success_patchQuantities_recomputesStatus")
        void update_success_patchQuantities_recomputesStatus() {
            StockItem initial = createPersistedItem(productA, stockItemGroupA, warehouseA, 5, 0);
            StockItemDTO updateDTO = new StockItemDTO();
            updateDTO.setId(initial.getId());
            updateDTO.setAvailableQuantity(BigInteger.ZERO);
            updateDTO.setReservedQuantity(BigInteger.ONE);

            StockItem updated = stockItemService.update(updateDTO);

            assertThat(updated.getAvailableQuantity()).isEqualByComparingTo(BigInteger.ZERO);
            assertThat(updated.getReservedQuantity()).isEqualByComparingTo(BigInteger.ONE);
            assertThat(updated.getStatus()).isEqualTo(StockItem.Status.RESERVED);
        }

        @Test
        @DisplayName("update_success_outOfService_override")
        void update_success_outOfService_override() {
            StockItem initial = createPersistedItem(productA, stockItemGroupA, warehouseA, 8, 0);

            StockItemDTO updateDTO = new StockItemDTO();
            updateDTO.setId(initial.getId());
            updateDTO.setStatus(StockItem.Status.OUT_OF_SERVICE.name());

            StockItem updated = stockItemService.update(updateDTO);

            assertThat(updated.getStatus()).isEqualTo(StockItem.Status.OUT_OF_SERVICE);
        }

        @Test
        @DisplayName("update_fail_invalidStatus: BusinessException")
        void update_fail_invalidStatus() {
            StockItem initial = createPersistedItem(productA, stockItemGroupA, warehouseA, 8, 0);

            StockItemDTO updateDTO = new StockItemDTO();
            updateDTO.setId(initial.getId());
            updateDTO.setStatus("WRONG_STATUS");

            assertThatThrownBy(() -> stockItemService.update(updateDTO))
                    .isInstanceOf(ValidationException.class);
        }

        @Test
        @DisplayName("update_fail_missingId: ValidationException")
        void update_fail_missingId() {
            StockItemDTO updateDTO = new StockItemDTO();
            updateDTO.setAvailableQuantity(BigInteger.TEN);

            assertThatThrownBy(() -> stockItemService.update(updateDTO))
                    .isInstanceOf(ValidationException.class);
        }

        @Test
        @DisplayName("update_fail_notFound: NotFoundException when ID absent")
        void update_fail_notFound() {
            StockItemDTO updateDTO = new StockItemDTO();
            updateDTO.setId(999_999L);
            updateDTO.setAvailableQuantity(BigInteger.ONE);

            assertThatThrownBy(() -> stockItemService.update(updateDTO))
                    .isInstanceOf(NotFoundException.class);
        }
    }
}
