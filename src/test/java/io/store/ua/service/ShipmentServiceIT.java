package io.store.ua.service;

import io.store.ua.AbstractIT;
import io.store.ua.entity.*;
import io.store.ua.enums.Role;
import io.store.ua.enums.Status;
import io.store.ua.exceptions.BusinessException;
import io.store.ua.models.data.Address;
import io.store.ua.models.data.WorkingHours;
import io.store.ua.models.dto.ShipmentDTO;
import io.store.ua.models.dto.WarehouseDTO;
import io.store.ua.utility.CodeGenerator;
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
        var group = stockItemGroupRepository.save(StockItemGroup.builder()
                .code(RandomStringUtils.secure().nextAlphanumeric(20))
                .name("Group " + RandomStringUtils.secure().nextAlphabetic(12))
                .isActive(true)
                .build());

        var product = productRepository.save(Product.builder()
                .code(RandomStringUtils.secure().nextAlphanumeric(32))
                .title(RandomStringUtils.secure().nextAlphabetic(10))
                .description(RandomStringUtils.secure().nextAlphanumeric(50))
                .price(BigInteger.valueOf(RandomUtils.secure().randomLong(500, 50_000)))
                .weight(BigInteger.valueOf(RandomUtils.secure().randomLong(100, 5_000)))
                .length(BigInteger.valueOf(RandomUtils.secure().randomLong(5, 100)))
                .width(BigInteger.valueOf(RandomUtils.secure().randomLong(5, 100)))
                .height(BigInteger.valueOf(RandomUtils.secure().randomLong(5, 100)))
                .build());

        user = userRepository.save(RegularUser.builder()
                .username(RandomStringUtils.secure().nextAlphanumeric(24))
                .password(RandomStringUtils.secure().nextAlphanumeric(32))
                .email(RandomStringUtils.secure().nextAlphabetic(8).toLowerCase() + "@example.com")
                .role(Role.OWNER)
                .status(Status.ACTIVE)
                .timezone("UTC")
                .build());

        warehouse = warehouseRepository.save(generateWarehouse());

        stockItem = stockItemRepository.save(StockItem.builder()
                .productId(product.getId())
                .stockItemGroupId(group.getId())
                .warehouseId(warehouse.getId())
                .expiryDate(LocalDate.now().plusDays(RandomUtils.secure().randomInt(30, 365)))
                .availableQuantity(BigDecimal.valueOf(RandomUtils.secure().randomDouble(1, 500)))
                .reservedQuantity(BigDecimal.valueOf(RandomUtils.secure().randomDouble(0, 100)))
                .status(StockItem.Status.AVAILABLE)
                .build());

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities())
        );
    }

    private WarehouseDTO buildWarehouseDTO() {
        Address address = Address.builder()
                .country("UA")
                .state("Kyiv")
                .city("Kyiv")
                .street("St." + RandomStringUtils.secure().nextAlphanumeric(5))
                .building(RandomStringUtils.secure().nextNumeric(3))
                .postalCode("01001")
                .latitude(new BigDecimal("50.4501"))
                .longitude(new BigDecimal("30.5234"))
                .build();

        WorkingHours workingHours = WorkingHours.builder()
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
                .build();

        return WarehouseDTO.builder()
                .name(RandomStringUtils.secure().nextAlphanumeric(9))
                .address(address)
                .workingHours(workingHours)
                .phones(List.of("+" + RandomStringUtils.secure().nextNumeric(11)))
                .isActive(true)
                .build();
    }

    private Warehouse generateWarehouse() {
        WarehouseDTO warehouseDTO = buildWarehouseDTO();
        String code = CodeGenerator.WarehouseCodeGenerator.generate(warehouseDTO);

        return Warehouse.builder()
                .code(code)
                .name(warehouseDTO.getName())
                .address(warehouseDTO.getAddress())
                .workingHours(warehouseDTO.getWorkingHours())
                .phones(warehouseDTO.getPhones())
                .managerId(user.getId())
                .isActive(Boolean.TRUE.equals(warehouseDTO.getIsActive()))
                .build();
    }

    @Nested
    @DisplayName("save(shipmentDTO: ShipmentDTO)")
    class SaveShipmentTests {
        @Test
        @DisplayName("save_success: creates a new Shipment when valid sender and recipient are provided")
        void save_success() {
            var recipient = warehouseRepository.save(generateWarehouse());

            var shipmentDTO = new ShipmentDTO();
            shipmentDTO.setSenderCode(warehouse.getCode());
            shipmentDTO.setRecipientCode(recipient.getCode());
            shipmentDTO.setStockItemId(stockItem.getId());
            shipmentDTO.setStockItemAmount(5L);
            shipmentDTO.setStatus(Shipment.ShipmentStatus.INITIATED.name());

            Shipment savedShipment = shipmentService.save(shipmentDTO);

            assertThat(savedShipment.getId()).isNotNull();
            assertThat(savedShipment.getWarehouseIdSender()).isEqualTo(warehouse.getId());
            assertThat(savedShipment.getWarehouseIdRecipient()).isEqualTo(recipient.getId());
            assertThat(savedShipment.getStockItemId()).isEqualTo(shipmentDTO.getStockItemId());
            assertThat(savedShipment.getStockItemAmount()).isEqualTo(shipmentDTO.getStockItemAmount());
            assertThat(savedShipment.getInitiatorId()).isEqualTo(user.getId());
            assertThat(savedShipment.getStatus()).isEqualTo(Shipment.ShipmentStatus.valueOf(shipmentDTO.getStatus()));
        }

        @ParameterizedTest(name = "save_fail_invalidStatus: status=''{0}'' is invalid")
        @ValueSource(strings = {"WRONG", "INVALID"})
        @DisplayName("save_fail_invalidStatus: throws BusinessException when shipment status is invalid")
        void save_fail_invalidStatus(String invalidStatus) {
            var shipmentDTO = new ShipmentDTO();
            shipmentDTO.setSenderCode(warehouse.getCode());
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

            var createShipmentDTO = new ShipmentDTO();
            createShipmentDTO.setSenderCode(warehouse.getCode());
            createShipmentDTO.setRecipientCode(initialRecipientWarehouse.getCode());
            createShipmentDTO.setStockItemId(stockItem.getId());
            createShipmentDTO.setStockItemAmount(3L);
            Shipment createdShipment = shipmentService.save(createShipmentDTO);

            var updateShipmentDTO = new ShipmentDTO();
            updateShipmentDTO.setId(createdShipment.getId());
            updateShipmentDTO.setRecipientCode(newRecipientWarehouse.getCode());
            updateShipmentDTO.setStockItemAmount(7L);
            updateShipmentDTO.setStatus("SENT");

            Shipment updatedShipment = shipmentService.update(updateShipmentDTO);

            assertThat(updatedShipment.getId()).isEqualTo(createdShipment.getId());
            assertThat(updatedShipment.getWarehouseIdRecipient()).isEqualTo(newRecipientWarehouse.getId());
            assertThat(updatedShipment.getStockItemAmount()).isEqualTo(7L);
            assertThat(updatedShipment.getStatus()).isEqualTo(Shipment.ShipmentStatus.SENT);
            assertThat(updatedShipment.getInitiatorId()).isEqualTo(user.getId());
        }

        @Test
        @DisplayName("update_fail_notFound: throws BusinessException when shipment ID does not exist")
        void update_fail_notFound() {
            var updateShipmentDTO = new ShipmentDTO();
            updateShipmentDTO.setId(999999L);
            updateShipmentDTO.setStatus("SENT");

            assertThatThrownBy(() -> shipmentService.update(updateShipmentDTO))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Shipment with ID");
        }

        @Test
        @DisplayName("update_fail_invalidStatus: throws BusinessException when shipment status is invalid")
        void update_fail_invalidStatus() {
            var createShipmentDTO = new ShipmentDTO();
            createShipmentDTO.setSenderCode(warehouse.getCode());
            createShipmentDTO.setStockItemId(stockItem.getId());
            createShipmentDTO.setStockItemAmount(3L);
            Shipment createdShipment = shipmentService.save(createShipmentDTO);

            var updateShipmentDTO = new ShipmentDTO();
            updateShipmentDTO.setId(createdShipment.getId());
            updateShipmentDTO.setStatus("WRONG");

            assertThatThrownBy(() -> shipmentService.update(updateShipmentDTO))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Invalid shipment status");
        }
    }

    @Nested
    @DisplayName("findById(id: Long)")
    class FindShipmentByIdTests {
        @Test
        @DisplayName("findById_success: returns shipment when it exists")
        void findById_success() {
            var shipmentDTO = new ShipmentDTO();
            shipmentDTO.setSenderCode(warehouse.getCode());
            shipmentDTO.setStockItemId(stockItem.getId());
            shipmentDTO.setStockItemAmount(2L);

            Shipment savedShipment = shipmentService.save(shipmentDTO);
            Shipment foundShipment = shipmentService.findById(savedShipment.getId());

            assertThat(foundShipment).isNotNull();
            assertThat(foundShipment.getId()).isEqualTo(savedShipment.getId());
        }

        @Test
        @DisplayName("findById_fail: returns null when shipment does not exist")
        void findById_fail() {
            assertThat(shipmentService.findById(9999L)).isNull();
        }
    }
}
