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
    private StockItemGroup stockItemGroupC;

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

    private StockItem createStockItem(Product product,
                                      StockItemGroup group,
                                      Warehouse warehouse,
                                      boolean isActive,
                                      int available,
                                      int reserved) {
        StockItemDTO stockItemDTO = new StockItemDTO();
        stockItemDTO.setProductId(product.getId());
        stockItemDTO.setStockItemGroupId(group.getId());
        stockItemDTO.setWarehouseId(warehouse.getId());
        stockItemDTO.setExpiryDate(LocalDate.now().plusDays(RandomUtils.secure().randomInt(15, 120)));
        stockItemDTO.setAvailableQuantity(BigInteger.valueOf(available));
        stockItemDTO.setReservedQuantity(BigInteger.valueOf(reserved));
        stockItemDTO.setIsActive(isActive);

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
                    10,
                    3);

            StockItem reservedStockItem = createStockItem(productB,
                    stockItemGroupB,
                    warehouseB,
                    RandomUtils.secure().randomBoolean(),
                    0,
                    0);

            StockItem outOfStockItem = createStockItem(productB,
                    stockItemGroupB,
                    warehouseB,
                    RandomUtils.secure().randomBoolean(),
                    0,
                    0);

            createStockItem(productB,
                    stockItemGroupB,
                    warehouseB,
                    RandomUtils.secure().randomBoolean(),
                    0,
                    0);

            List<StockItem> result = stockItemService.findAll(3, 1);

            assertThat(result).hasSize(3);
            assertThat(result).extracting(StockItem::getId).contains(availableStockItem.getId(),
                    reservedStockItem.getId(),
                    outOfStockItem.getId());
        }

        @Test
        @DisplayName("findAll_success_empty: returns empty out of bounds page")
        void findAll_success_empty() {
            createStockItem(productA,
                    stockItemGroupA,
                    warehouseA,
                    RandomUtils.secure().randomBoolean(),
                    3,
                    1);

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
            StockItem target = createStockItem(productA,
                    stockItemGroupA,
                    warehouseA,
                    true,
                    5,
                    0);
            StockItem inInactiveGroup = createStockItem(productA,
                    stockItemGroupC,
                    warehouseA,
                    true,
                    5,
                    0);
            StockItem inactiveItem = createStockItem(productA,
                    stockItemGroupA,
                    warehouseA,
                    false,
                    6,
                    0);

            List<StockItem> result = stockItemService.findBy(List.of(warehouseA.getId()),
                    List.of(productA.getId()),
                    List.of(stockItemGroupA.getId()),
                    List.of(StockItem.Status.AVAILABLE.name()),
                    true,
                    true,
                    50,
                    1
            );

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
            StockItem avaialbleStockItem = createStockItem(productA,
                    stockItemGroupA,
                    warehouseA,
                    true,
                    10,
                    0);
            StockItem reservedStockItem = createStockItem(productA,
                    stockItemGroupA,
                    warehouseA,
                    true,
                    0,
                    5);
            StockItem anotherWarehouseStockitem = createStockItem(productB,
                    stockItemGroupB,
                    warehouseB,
                    true,
                    3,
                    0);

            List<Long> warehouseIds = List.of(warehouseA.getId());
            List<String> statuses = List.of(StockItem.Status.AVAILABLE.name(), StockItem.Status.RESERVED.name());

            List<StockItem> result = stockItemService.findBy(warehouseIds,
                    null,
                    null,
                    statuses,
                    null,
                    null,
                    20,
                    1);

            assertThat(result).extracting(StockItem::getId)
                    .contains(avaialbleStockItem.getId(), reservedStockItem.getId())
                    .doesNotContain(anotherWarehouseStockitem.getId());
        }

        @Test
        @DisplayName("findBy_success_allFiltersInLists")
        void findBy_success_allFiltersInLists() {
            StockItem target = createStockItem(productA, stockItemGroupA, warehouseA, true, 7, 1);

            createStockItem(productB,
                    stockItemGroupA,
                    warehouseA,
                    RandomUtils.secure().randomBoolean(),
                    1,
                    0);
            createStockItem(productA,
                    stockItemGroupB,
                    warehouseA,
                    RandomUtils.secure().randomBoolean(),
                    1,
                    1);
            createStockItem(productA,
                    stockItemGroupA,
                    warehouseB,
                    RandomUtils.secure().randomBoolean(),
                    1,
                    0);

            List<StockItem> result = stockItemService.findBy(List.of(warehouseA.getId()),
                    List.of(productA.getId()),
                    List.of(stockItemGroupA.getId()),
                    List.of(StockItem.Status.AVAILABLE.name()),
                    true,
                    true,
                    10,
                    1);

            assertThat(result).extracting(StockItem::getId).containsExactly(target.getId());
        }

        @Test
        @DisplayName("findBy_fail_statusNullInList: throws ValidationException when invalid status in filter list")
        void findBy_fail_statusNullInList() {
            assertThatThrownBy(() -> stockItemService.findBy(null,
                    null,
                    null,
                    Arrays.asList(StockItem.Status.AVAILABLE.name(), null),
                    null,
                    null,
                    10,
                    1))
                    .isInstanceOf(ValidationException.class);
        }

        @Test
        @DisplayName("findBy_fail_invalidStatusString: BusinessException on bad enum value")
        void findBy_fails_whenInvalidStatusString() {
            assertThatThrownBy(() -> stockItemService.findBy(null,
                    null,
                    null,
                    List.of("WRONG_STATUS"),
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
        @DisplayName("findById_success: returns all entities")
        void findById_success() {
            StockItem created = createStockItem(productA,
                    stockItemGroupA,
                    warehouseA,
                    RandomUtils.secure().randomBoolean(),
                    4,
                    0);

            StockItem found = stockItemService.findById(created.getId());

            assertThat(found.getId()).isEqualTo(created.getId());
        }

        @Test
        @DisplayName("findById_fails_whenIdIsNull: throws ValidationException when item ID is invalid")
        void findById_fails_whenIdIsNull() {
            assertThatThrownBy(() -> stockItemService.findById(null))
                    .isInstanceOf(ValidationException.class);
        }

        @Test
        @DisplayName("findById_fail_notFound: throws NotFoundException when item was not found by ID")
        void findById_fail_notFound() {
            assertThatThrownBy(() -> stockItemService.findById(Long.MAX_VALUE))
                    .isInstanceOf(NotFoundException.class);
        }
    }

    @Nested
    @DisplayName("create(stockItemDTO: StockItemDTO)")
    class CreateTests {
        @Test
        @DisplayName("create_success_whenQuantitiesAvailable_thenStatusIsAvailable: automatically sets status to AVAILABLE if available quantity is greater than 0")
        void create_success_whenQuantitiesAvailable_thenStatusIsAvailable() {
            StockItemDTO stockItemDTO = StockItemDTO.builder()
                    .productId(productA.getId())
                    .stockItemGroupId(stockItemGroupA.getId())
                    .warehouseId(warehouseA.getId())
                    .expiryDate(LocalDate.now().plusDays(30))
                    .availableQuantity(BigInteger.TEN)
                    .reservedQuantity(BigInteger.ONE)
                    .isActive(true)
                    .build();

            StockItem created = stockItemService.create(stockItemDTO);

            assertThat(created).isNotNull();
            assertThat(created.getId()).isNotNull();
            assertThat(created.getProductId()).isEqualTo(stockItemDTO.getProductId());
            assertThat(created.getStockItemGroupId()).isEqualTo(stockItemDTO.getStockItemGroupId());
            assertThat(created.getWarehouseId()).isEqualTo(stockItemDTO.getWarehouseId());
            assertThat(created.getExpiryDate()).isEqualTo(stockItemDTO.getExpiryDate());
            assertThat(created.getAvailableQuantity()).isEqualByComparingTo(stockItemDTO.getAvailableQuantity());
            assertThat(created.getReservedQuantity()).isEqualByComparingTo(stockItemDTO.getReservedQuantity());
            assertThat(created.getStatus()).isEqualTo(StockItem.Status.AVAILABLE);
            assertThat(created.getIsActive()).isEqualTo(stockItemDTO.getIsActive());
        }

        @Test
        @DisplayName("create_success_whenQuantitiesReservedOnly_thenStatusIsReserved: automatically sets status to RESERVED if available quantity is 0 and reserved quantity is greater than 0")
        void create_success_whenQuantitiesReservedOnly_thenStatusIsReserved() {
            StockItemDTO stockItemDTO = StockItemDTO.builder()
                    .productId(productA.getId())
                    .stockItemGroupId(stockItemGroupA.getId())
                    .warehouseId(warehouseA.getId())
                    .expiryDate(LocalDate.now().plusDays(30))
                    .availableQuantity(BigInteger.ZERO)
                    .reservedQuantity(BigInteger.ONE)
                    .isActive(true)
                    .build();

            StockItem created = stockItemService.create(stockItemDTO);

            assertThat(created).isNotNull();
            assertThat(created.getId()).isNotNull();
            assertThat(created.getProductId()).isEqualTo(stockItemDTO.getProductId());
            assertThat(created.getStockItemGroupId()).isEqualTo(stockItemDTO.getStockItemGroupId());
            assertThat(created.getWarehouseId()).isEqualTo(stockItemDTO.getWarehouseId());
            assertThat(created.getExpiryDate()).isEqualTo(stockItemDTO.getExpiryDate());
            assertThat(created.getAvailableQuantity()).isEqualByComparingTo(stockItemDTO.getAvailableQuantity());
            assertThat(created.getReservedQuantity()).isEqualByComparingTo(stockItemDTO.getReservedQuantity());
            assertThat(created.getStatus()).isEqualTo(StockItem.Status.RESERVED);
            assertThat(created.getIsActive()).isEqualTo(stockItemDTO.getIsActive());
        }

        @Test
        @DisplayName("create_success_whenQuantitiesAreZero_thenStatusIsOutOfStock: automatically sets status to OUT_OF_STOCK if quantities are 0")
        void create_success_whenQuantitiesAreZero_thenStatusIsOutOfStock() {
            StockItemDTO stockItemDTO = StockItemDTO.builder()
                    .productId(productA.getId())
                    .stockItemGroupId(stockItemGroupA.getId())
                    .warehouseId(warehouseA.getId())
                    .expiryDate(LocalDate.now().plusDays(10))
                    .availableQuantity(BigInteger.ZERO)
                    .reservedQuantity(BigInteger.ZERO)
                    .isActive(true)
                    .build();

            StockItem created = stockItemService.create(stockItemDTO);

            assertThat(created).isNotNull();
            assertThat(created.getId()).isNotNull();
            assertThat(created.getProductId()).isEqualTo(stockItemDTO.getProductId());
            assertThat(created.getStockItemGroupId()).isEqualTo(stockItemDTO.getStockItemGroupId());
            assertThat(created.getWarehouseId()).isEqualTo(stockItemDTO.getWarehouseId());
            assertThat(created.getExpiryDate()).isEqualTo(stockItemDTO.getExpiryDate());
            assertThat(created.getAvailableQuantity()).isEqualByComparingTo(stockItemDTO.getAvailableQuantity());
            assertThat(created.getReservedQuantity()).isEqualByComparingTo(stockItemDTO.getReservedQuantity());
            assertThat(created.getStatus()).isEqualTo(StockItem.Status.OUT_OF_STOCK);
            assertThat(created.getIsActive()).isEqualTo(stockItemDTO.getIsActive());
        }

        @Test
        @DisplayName("create_fails_whenNullPayload: @NotNull triggers ValidationException")
        void create_fails_whenNullPayload() {
            assertThatThrownBy(() -> stockItemService.create(null)).isInstanceOf(ValidationException.class);
        }
    }

    @Nested
    @DisplayName("update(stockItemDTO: StockItemDTO)")
    class UpdateTests {
        @Test
        @DisplayName("update_success_whenQuantitiesChange_thenStatusRecomputing")
        void update_success_whenQuantitiesChange_thenStatusRecomputing() {
            StockItem initialStockItem = createStockItem(productA, stockItemGroupA, warehouseA, true, 5, 0);

            StockItemDTO updateDTO = new StockItemDTO();
            updateDTO.setId(initialStockItem.getId());
            updateDTO.setAvailableQuantity(BigInteger.ZERO);
            updateDTO.setReservedQuantity(BigInteger.ONE);
            updateDTO.setIsActive(false);

            StockItem updated = stockItemService.update(updateDTO);

            assertThat(updated.getAvailableQuantity()).isEqualByComparingTo(BigInteger.ZERO);
            assertThat(updated.getReservedQuantity()).isEqualByComparingTo(BigInteger.ONE);
            assertThat(updated.getStatus()).isEqualTo(StockItem.Status.RESERVED);
            assertThat(updated.getIsActive()).isEqualTo(false);
        }

        @Test
        @DisplayName("update_success_changingStatus")
        void update_success_changingStatus() {
            StockItem initialStockItem = createStockItem(productA,
                    stockItemGroupA,
                    warehouseA,
                    RandomUtils.secure().randomBoolean(),
                    8,
                    0);

            StockItemDTO updateDTO = new StockItemDTO();
            updateDTO.setId(initialStockItem.getId());
            updateDTO.setStatus(StockItem.Status.OUT_OF_SERVICE.name());

            StockItem updated = stockItemService.update(updateDTO);

            assertThat(updated.getStatus()).isEqualTo(StockItem.Status.OUT_OF_SERVICE);
        }

        @Test
        @DisplayName("update_fails_whenInvalidStatus: throws BusinessException when status can't be identified")
        void update_fails_whenInvalidStatus() {
            StockItem initialStockItem = createStockItem(productA,
                    stockItemGroupA,
                    warehouseA,
                    RandomUtils.secure().randomBoolean(),
                    8,
                    0);

            StockItemDTO updateDTO = new StockItemDTO();
            updateDTO.setId(initialStockItem.getId());
            updateDTO.setStatus("WRONG_STATUS");

            assertThatThrownBy(() -> stockItemService.update(updateDTO)).isInstanceOf(ValidationException.class);
        }

        @Test
        @DisplayName("update_fails_whenIdIsAbsentInRequest: throws ValidationException when ID is absent in request")
        void update_fails_whenIdIsAbsentInRequest() {
            StockItemDTO updateDTO = new StockItemDTO();
            updateDTO.setAvailableQuantity(BigInteger.TEN);

            assertThatThrownBy(() -> stockItemService.update(updateDTO)).isInstanceOf(ValidationException.class);
        }

        @Test
        @DisplayName("update_fails_whenItemWasNotFound: throws NotFoundException when item was not found by ID")
        void update_fails_whenItemWasNotFound() {
            StockItemDTO updateDTO = new StockItemDTO();
            updateDTO.setId(999_999L);
            updateDTO.setAvailableQuantity(BigInteger.ONE);

            assertThatThrownBy(() -> stockItemService.update(updateDTO)).isInstanceOf(NotFoundException.class);
        }
    }
}
