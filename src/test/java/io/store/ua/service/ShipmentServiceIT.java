package io.store.ua.service;

import io.store.ua.AbstractIT;
import io.store.ua.entity.*;
import io.store.ua.enums.Role;
import io.store.ua.enums.Status;
import io.store.ua.exceptions.BusinessException;
import io.store.ua.exceptions.RegularAuthenticationException;
import io.store.ua.models.data.Address;
import io.store.ua.models.data.WorkingHours;
import io.store.ua.models.dto.ShipmentDTO;
import io.store.ua.models.dto.WarehouseDTO;
import io.store.ua.utility.CodeGenerator;
import jakarta.validation.ValidationException;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ShipmentServiceIT extends AbstractIT {
    @Autowired
    private ShipmentService shipmentService;

    private Warehouse warehouse;
    private RegularUser user;
    private StockItem stockItem;

    @BeforeEach
    void setUp() {
        StockItemGroup stockItemGroup = stockItemGroupRepository.save(StockItemGroup.builder()
                .code(RandomStringUtils.secure().nextAlphanumeric(20))
                .name(RandomStringUtils.secure().nextAlphabetic(16))
                .isActive(true)
                .build());

        Product product = productRepository.save(Product.builder()
                .code(RandomStringUtils.secure().nextAlphanumeric(32))
                .title(RandomStringUtils.secure().nextAlphabetic(12))
                .description(RandomStringUtils.secure().nextAlphanumeric(48))
                .price(BigInteger.valueOf(RandomUtils.secure().randomLong(500, 50_000)))
                .weight(BigInteger.valueOf(RandomUtils.secure().randomLong(100, 5_000)))
                .length(BigInteger.valueOf(RandomUtils.secure().randomLong(5, 100)))
                .width(BigInteger.valueOf(RandomUtils.secure().randomLong(5, 100)))
                .height(BigInteger.valueOf(RandomUtils.secure().randomLong(5, 100)))
                .build());

        user = userRepository.save(RegularUser.builder()
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

        warehouse = warehouseRepository.save(generateWarehouse());

        stockItem = stockItemRepository.save(StockItem.builder()
                .productId(product.getId())
                .stockItemGroupId(stockItemGroup.getId())
                .warehouseId(warehouse.getId())
                .expiryDate(LocalDate.now().plusDays(RandomUtils.secure().randomInt(30, 365)))
                .availableQuantity(BigInteger.valueOf(RandomUtils.secure().randomInt(1, 500)))
                .reservedQuantity(BigInteger.valueOf(RandomUtils.secure().randomInt(0, 100)))
                .status(StockItem.Status.AVAILABLE)
                .build());
    }

    private Warehouse generateWarehouse() {
        WarehouseDTO warehouseDTO = WarehouseDTO.builder()
                .name(RandomStringUtils.secure().nextAlphabetic(9))
                .address(Address.builder()
                        .country(RandomStringUtils.secure().nextAlphabetic(2).toUpperCase())
                        .state(RandomStringUtils.secure().nextAlphabetic(8))
                        .city(RandomStringUtils.secure().nextAlphabetic(8))
                        .street(RandomStringUtils.secure().nextAlphabetic(10))
                        .building(RandomStringUtils.secure().nextNumeric(3))
                        .postalCode(RandomStringUtils.secure().nextNumeric(5))
                        .latitude(new BigDecimal("50.4501"))
                        .longitude(new BigDecimal("30.5234"))
                        .build())
                .workingHours(WorkingHours.builder()
                        .timezone("UTC")
                        .days(List.of(
                                WorkingHours.DayHours.builder()
                                        .day(DayOfWeek.MONDAY)
                                        .open(List.of(
                                                WorkingHours.TimeRange.builder()
                                                        .from(LocalTime.of(9, 0))
                                                        .to(LocalTime.of(18, 0))
                                                        .build()
                                        ))
                                        .build()
                        ))
                        .build())
                .phones(List.of("+" + RandomStringUtils.secure().nextNumeric(11)))
                .isActive(true)
                .build();

        String generatedCode = CodeGenerator.WarehouseCodeGenerator.generate(warehouseDTO);

        return Warehouse.builder()
                .code(generatedCode)
                .name(warehouseDTO.getName())
                .address(warehouseDTO.getAddress())
                .workingHours(warehouseDTO.getWorkingHours())
                .phones(warehouseDTO.getPhones())
                .managerId(user.getId())
                .isActive(Boolean.TRUE.equals(warehouseDTO.getIsActive()))
                .build();
    }

    private Address randomAddress() {
        return Address.builder()
                .country(RandomStringUtils.secure().nextAlphabetic(2).toUpperCase())
                .state(RandomStringUtils.secure().nextAlphabetic(6))
                .city(RandomStringUtils.secure().nextAlphabetic(6))
                .street(RandomStringUtils.secure().nextAlphabetic(10))
                .building(RandomStringUtils.secure().nextNumeric(3))
                .postalCode(RandomStringUtils.secure().nextNumeric(5))
                .latitude(new BigDecimal("50.4501"))
                .longitude(new BigDecimal("30.5234"))
                .build();
    }

    @Nested
    @DisplayName("findAll(pageSize: int, page: int)")
    class FindAllShipmentsTests {
        @Test
        @DisplayName("findAll_success: returns shipments when page exists and size is valid")
        void findAll_success() {
            ShipmentDTO firstShipmentDTO = new ShipmentDTO();
            firstShipmentDTO.setSenderCode(warehouse.getCode());
            firstShipmentDTO.setAddress(randomAddress());
            firstShipmentDTO.setStockItemId(stockItem.getId());
            firstShipmentDTO.setStockItemAmount(3L);

            ShipmentDTO secondShipmentDTO = new ShipmentDTO();
            secondShipmentDTO.setSenderCode(warehouse.getCode());
            secondShipmentDTO.setAddress(randomAddress());
            secondShipmentDTO.setStockItemId(stockItem.getId());
            secondShipmentDTO.setStockItemAmount(5L);

            Shipment firstCreatedShipment = shipmentService.save(firstShipmentDTO);
            Shipment secondCreatedShipment = shipmentService.save(secondShipmentDTO);

            List<Shipment> pagedShipments = shipmentService.findAll(10, 1);

            assertThat(pagedShipments).isNotEmpty();
            assertThat(pagedShipments)
                    .extracting(Shipment::getId)
                    .contains(firstCreatedShipment.getId(), secondCreatedShipment.getId());
        }

        @Test
        @DisplayName("findAll_success_empty: returns empty list when page has no results")
        void findAll_success_empty() {
            ShipmentDTO shipmentDTO = new ShipmentDTO();
            shipmentDTO.setSenderCode(warehouse.getCode());
            shipmentDTO.setAddress(randomAddress());
            shipmentDTO.setStockItemId(stockItem.getId());
            shipmentDTO.setStockItemAmount(2L);
            shipmentService.save(shipmentDTO);

            List<Shipment> pagedShipments = shipmentService.findAll(10, 99);

            assertThat(pagedShipments).isEmpty();
        }

        @Test
        @DisplayName("findAll_fail_invalidPageSize: throws ValidationException for invalid page size")
        void findAll_fail_invalidPageSize() {
            assertThatThrownBy(() -> shipmentService.findAll(0, 1))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("Size of page can't be less than 1");
        }

        @Test
        @DisplayName("findAll_fail_invalidPageNumber: throws ValidationException for invalid page number")
        void findAll_fail_invalidPageNumber() {
            assertThatThrownBy(() -> shipmentService.findAll(10, 0))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("A page number can't be less than 1");
        }
    }

    @Nested
    @DisplayName("findById(id: Long)")
    class FindShipmentByIdTests {

        @Test
        @DisplayName("findById_success: returns shipment when it exists")
        void findById_success() {
            ShipmentDTO shipmentDTO = new ShipmentDTO();
            shipmentDTO.setSenderCode(warehouse.getCode());
            shipmentDTO.setAddress(randomAddress());
            shipmentDTO.setStockItemId(stockItem.getId());
            shipmentDTO.setStockItemAmount(2L);

            Shipment createdShipment = shipmentService.save(shipmentDTO);

            Shipment foundShipment = shipmentService.findById(createdShipment.getId());

            assertThat(foundShipment).isNotNull();
            assertThat(foundShipment.getId()).isEqualTo(createdShipment.getId());
        }

        @Test
        @DisplayName("findById_fail_notFound: throws NotFoundException when shipment does not exist")
        void findById_fail_notFound() {
            assertThatThrownBy(() -> shipmentService.findById(9999L))
                    .isInstanceOf(io.store.ua.exceptions.NotFoundException.class)
                    .hasMessageContaining("Shipment with ID '9999' was not found");
        }

        @Test
        @DisplayName("findById_fail_null: throws ValidationException when id is null")
        void findById_fail_null() {
            assertThatThrownBy(() -> shipmentService.findById(null))
                    .isInstanceOf(jakarta.validation.ValidationException.class);
        }
    }

    @Nested
    @DisplayName("save(shipmentDTO: ShipmentDTO)")
    class SaveShipmentTests {
        @Test
        @DisplayName("save_success_withRecipient: creates a new Shipment when valid sender and recipient are provided")
        void save_success_withRecipient() {
            Warehouse recipientWarehouse = warehouseRepository.save(generateWarehouse());

            ShipmentDTO shipmentDTO = new ShipmentDTO();
            shipmentDTO.setSenderCode(warehouse.getCode());
            shipmentDTO.setRecipientCode(recipientWarehouse.getCode());
            shipmentDTO.setStockItemId(stockItem.getId());
            shipmentDTO.setStockItemAmount(5L);
            shipmentDTO.setStatus(Shipment.ShipmentStatus.INITIATED.name());

            Shipment createdShipment = shipmentService.save(shipmentDTO);

            assertThat(createdShipment.getId()).isNotNull();
            assertThat(createdShipment.getWarehouseIdSender()).isEqualTo(warehouse.getId());
            assertThat(createdShipment.getWarehouseIdRecipient()).isEqualTo(recipientWarehouse.getId());
            assertThat(createdShipment.getStatus()).isEqualTo(Shipment.ShipmentStatus.INITIATED);
        }

        @Test
        @DisplayName("save_success_withAddressOnly: creates a Shipment when address provided and recipientCode is null")
        void save_success_withAddressOnly() {
            ShipmentDTO shipmentDTO = new ShipmentDTO();
            shipmentDTO.setSenderCode(warehouse.getCode());
            shipmentDTO.setAddress(randomAddress());
            shipmentDTO.setStockItemId(stockItem.getId());
            shipmentDTO.setStockItemAmount(2L);

            Shipment createdShipment = shipmentService.save(shipmentDTO);

            assertThat(createdShipment.getId()).isNotNull();
            assertThat(createdShipment.getWarehouseIdSender()).isEqualTo(warehouse.getId());
            assertThat(createdShipment.getWarehouseIdRecipient()).isNull();
            assertThat(createdShipment.getAddress()).isEqualTo(shipmentDTO.getAddress());
            assertThat(createdShipment.getStatus()).isEqualTo(Shipment.ShipmentStatus.INITIATED);
        }

        @Test
        @DisplayName("save_success_defaultStatus_withAddress: sets INITIATED when status is blank and address is used")
        void save_success_defaultStatus_withAddress() {
            ShipmentDTO shipmentDTO = new ShipmentDTO();
            shipmentDTO.setSenderCode(warehouse.getCode());
            shipmentDTO.setAddress(randomAddress());
            shipmentDTO.setStockItemId(stockItem.getId());
            shipmentDTO.setStockItemAmount(1L);

            Shipment createdShipment = shipmentService.save(shipmentDTO);

            assertThat(createdShipment.getStatus()).isEqualTo(Shipment.ShipmentStatus.INITIATED);
        }

        @Test
        @DisplayName("save_fail_bothNull: throws BusinessException when both recipient and address are null")
        void save_fail_bothNull() {
            ShipmentDTO shipmentDTO = new ShipmentDTO();
            shipmentDTO.setSenderCode(warehouse.getCode());
            shipmentDTO.setStockItemId(stockItem.getId());
            shipmentDTO.setStockItemAmount(1L);

            assertThatThrownBy(() -> shipmentService.save(shipmentDTO))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Recipient code and address can't be null at the same time, unknown where to send shipment");
        }

        @Test
        @DisplayName("save_fail_bothNotNull: throws BusinessException when both recipient and address are provided")
        void save_fail_bothNotNull() {
            Warehouse recipientWarehouse = warehouseRepository.save(generateWarehouse());

            ShipmentDTO shipmentDTO = new ShipmentDTO();
            shipmentDTO.setSenderCode(warehouse.getCode());
            shipmentDTO.setRecipientCode(recipientWarehouse.getCode());
            shipmentDTO.setAddress(randomAddress());
            shipmentDTO.setStockItemId(stockItem.getId());
            shipmentDTO.setStockItemAmount(3L);

            assertThatThrownBy(() -> shipmentService.save(shipmentDTO))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Recipient code and address can't be not null at the same time, unknown where to send shipment");
        }

        @Test
        @DisplayName("save_fail_sameSenderAndRecipient: throws BusinessException when sender and recipient are the same")
        void save_fail_sameSenderAndRecipient() {
            Warehouse secondWarehouse = warehouseRepository.save(generateWarehouse());

            ShipmentDTO shipmentDTO = new ShipmentDTO();
            shipmentDTO.setSenderCode(secondWarehouse.getCode());
            shipmentDTO.setRecipientCode(secondWarehouse.getCode());
            shipmentDTO.setStockItemId(stockItem.getId());
            shipmentDTO.setStockItemAmount(2L);

            assertThatThrownBy(() -> shipmentService.save(shipmentDTO))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Sender and recipient can't be the same");
        }

        @Test
        @DisplayName("save_fail_unauthenticated: throws BusinessException when user not authenticated")
        void save_fail_unauthenticated() {
            SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(null,
                    null,
                    AuthorityUtils.createAuthorityList()));

            Warehouse recipientWarehouse = warehouseRepository.save(generateWarehouse());
            ShipmentDTO shipmentDTO = new ShipmentDTO();
            shipmentDTO.setSenderCode(warehouse.getCode());
            shipmentDTO.setRecipientCode(recipientWarehouse.getCode());
            shipmentDTO.setStockItemId(stockItem.getId());
            shipmentDTO.setStockItemAmount(1L);

            assertThatThrownBy(() -> shipmentService.save(shipmentDTO))
                    .isInstanceOf(RegularAuthenticationException.class);
        }

        @ParameterizedTest(name = "save_fail_invalidStatus: status=''{0}'' is invalid")
        @ValueSource(strings = {"WRONG", "INVALID"})
        @DisplayName("save_fail_invalidStatus: throws BusinessException when shipment status is invalid")
        void save_fail_invalidStatus(String invalidStatus) {
            ShipmentDTO shipmentDTO = new ShipmentDTO();
            shipmentDTO.setSenderCode(warehouse.getCode());
            shipmentDTO.setAddress(randomAddress());
            shipmentDTO.setStockItemId(stockItem.getId());
            shipmentDTO.setStockItemAmount(1L);
            shipmentDTO.setStatus(invalidStatus);

            assertThatThrownBy(() -> shipmentService.save(shipmentDTO))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Invalid shipment status");
        }
    }

    @Nested
    @DisplayName("update(shipmentDTO: ShipmentDTO)")
    class UpdateShipmentTests {
        @Test
        @DisplayName("update_success: updates only provided fields when shipment exists")
        void update_success() {
            Warehouse initialRecipientWarehouse = warehouseRepository.save(generateWarehouse());
            Warehouse newRecipientWarehouse = warehouseRepository.save(generateWarehouse());

            ShipmentDTO createShipmentDTO = new ShipmentDTO();
            createShipmentDTO.setSenderCode(warehouse.getCode());
            createShipmentDTO.setRecipientCode(initialRecipientWarehouse.getCode());
            createShipmentDTO.setStockItemId(stockItem.getId());
            createShipmentDTO.setStockItemAmount(3L);
            Shipment createdShipment = shipmentService.save(createShipmentDTO);

            ShipmentDTO updateShipmentDTO = new ShipmentDTO();
            updateShipmentDTO.setId(createdShipment.getId());
            updateShipmentDTO.setRecipientCode(newRecipientWarehouse.getCode());
            updateShipmentDTO.setStockItemAmount(7L);
            updateShipmentDTO.setStatus("SENT");

            Shipment updatedShipment = shipmentService.update(updateShipmentDTO);

            assertThat(updatedShipment.getId()).isEqualTo(createdShipment.getId());
            assertThat(updatedShipment.getWarehouseIdRecipient()).isEqualTo(newRecipientWarehouse.getId());
            assertThat(updatedShipment.getStockItemAmount()).isEqualTo(7L);
            assertThat(updatedShipment.getStatus()).isEqualTo(Shipment.ShipmentStatus.SENT);
            assertThat(updatedShipment.getInitiatorId()).isEqualTo(RegularUserService.getCurrentlyAuthenticatedUserID());
        }

        @Test
        @DisplayName("update_success_partial: leaves unspecified fields unchanged")
        void update_success_partial() {
            ShipmentDTO createShipmentDTO = new ShipmentDTO();
            createShipmentDTO.setSenderCode(warehouse.getCode());
            createShipmentDTO.setAddress(randomAddress());
            createShipmentDTO.setStockItemId(stockItem.getId());
            createShipmentDTO.setStockItemAmount(2L);
            Shipment createdShipment = shipmentService.save(createShipmentDTO);

            ShipmentDTO updateShipmentDTO = new ShipmentDTO();
            updateShipmentDTO.setId(createdShipment.getId());
            updateShipmentDTO.setStatus("DELIVERED");

            Shipment updatedShipment = shipmentService.update(updateShipmentDTO);

            assertThat(updatedShipment.getId()).isEqualTo(createdShipment.getId());
            assertThat(updatedShipment.getWarehouseIdSender()).isEqualTo(createdShipment.getWarehouseIdSender());
            assertThat(updatedShipment.getWarehouseIdRecipient()).isEqualTo(createdShipment.getWarehouseIdRecipient());
            assertThat(updatedShipment.getStockItemId()).isEqualTo(createdShipment.getStockItemId());
            assertThat(updatedShipment.getStockItemAmount()).isEqualTo(createdShipment.getStockItemAmount());
            assertThat(updatedShipment.getStatus()).isEqualTo(Shipment.ShipmentStatus.DELIVERED);
        }

        @Test
        @DisplayName("update_fail_notFound: throws BusinessException when shipment ID does not exist")
        void update_fail_notFound() {
            ShipmentDTO updateShipmentDTO = new ShipmentDTO();
            updateShipmentDTO.setId(999_999L);
            updateShipmentDTO.setStatus("SENT");

            assertThatThrownBy(() -> shipmentService.update(updateShipmentDTO))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Shipment with ID");
        }

        @Test
        @DisplayName("update_fail_invalidStatus: throws BusinessException when shipment status is invalid")
        void update_fail_invalidStatus() {
            ShipmentDTO createShipmentDTO = new ShipmentDTO();
            createShipmentDTO.setSenderCode(warehouse.getCode());
            createShipmentDTO.setAddress(randomAddress());
            createShipmentDTO.setStockItemId(stockItem.getId());
            createShipmentDTO.setStockItemAmount(3L);
            Shipment createdShipment = shipmentService.save(createShipmentDTO);

            ShipmentDTO updateShipmentDTO = new ShipmentDTO();
            updateShipmentDTO.setId(createdShipment.getId());
            updateShipmentDTO.setStatus("WRONG");

            assertThatThrownBy(() -> shipmentService.update(updateShipmentDTO))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Invalid shipment status");
        }

        @Test
        @DisplayName("update_fail_bothNotNull: throws BusinessException when both recipient and address are provided")
        void update_fail_bothNotNull() {
            ShipmentDTO createShipmentDTO = new ShipmentDTO();
            createShipmentDTO.setSenderCode(warehouse.getCode());
            createShipmentDTO.setAddress(randomAddress());
            createShipmentDTO.setStockItemId(stockItem.getId());
            createShipmentDTO.setStockItemAmount(2L);
            Shipment createdShipment = shipmentService.save(createShipmentDTO);

            Warehouse recipientWarehouse = warehouseRepository.save(generateWarehouse());

            ShipmentDTO updateShipmentDTO = new ShipmentDTO();
            updateShipmentDTO.setId(createdShipment.getId());
            updateShipmentDTO.setRecipientCode(recipientWarehouse.getCode());
            updateShipmentDTO.setAddress(randomAddress());

            assertThatThrownBy(() -> shipmentService.update(updateShipmentDTO))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Recipient code and address can't be not null at the same time, unknown where to send shipment");
        }

        @Test
        @DisplayName("update_fail_sameSenderAndRecipient: throws BusinessException when sender and recipient are the same")
        void update_fail_sameSenderAndRecipient() {
            Warehouse sameWarehouse = warehouseRepository.save(generateWarehouse());

            ShipmentDTO createShipmentDTO = new ShipmentDTO();
            createShipmentDTO.setSenderCode(warehouse.getCode());
            createShipmentDTO.setRecipientCode(sameWarehouse.getCode());
            createShipmentDTO.setStockItemId(stockItem.getId());
            createShipmentDTO.setStockItemAmount(3L);
            Shipment createdShipment = shipmentService.save(createShipmentDTO);

            ShipmentDTO updateShipmentDTO = new ShipmentDTO();
            updateShipmentDTO.setId(createdShipment.getId());
            updateShipmentDTO.setSenderCode(sameWarehouse.getCode());
            updateShipmentDTO.setRecipientCode(sameWarehouse.getCode());

            assertThatThrownBy(() -> shipmentService.update(updateShipmentDTO))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Sender and recipient can't be the same");
        }

        @Test
        @DisplayName("update_fail_missingId: throws ValidationException when id is null")
        void update_fail_missingId() {
            ShipmentDTO updateShipmentDTO = new ShipmentDTO();
            updateShipmentDTO.setStatus("SENT");

            assertThatThrownBy(() -> shipmentService.update(updateShipmentDTO))
                    .isInstanceOf(ValidationException.class);
        }
    }
}
