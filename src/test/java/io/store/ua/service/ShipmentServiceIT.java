package io.store.ua.service;

import io.store.ua.AbstractIT;
import io.store.ua.entity.*;
import io.store.ua.enums.ShipmentDirection;
import io.store.ua.enums.ShipmentStatus;
import io.store.ua.enums.StockItemStatus;
import io.store.ua.exceptions.BusinessException;
import io.store.ua.exceptions.NotFoundException;
import io.store.ua.exceptions.AuthenticationException;
import io.store.ua.models.data.Address;
import io.store.ua.models.dto.ShipmentDTO;
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
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ShipmentServiceIT extends AbstractIT {
    @Autowired
    private ShipmentService shipmentService;
    @Autowired
    private StockItemService stockItemService;

    private Warehouse warehouse;
    private StockItem stockItem;

    @BeforeEach
    void setUp() {
        var stockItemGroup = stockItemGroupRepository.save(StockItemGroup.builder()
                .code(RandomStringUtils.secure().nextAlphanumeric(20))
                .name(RandomStringUtils.secure().nextAlphabetic(16))
                .isActive(true)
                .build());

        var product = productRepository.save(Product.builder()
                .code(RandomStringUtils.secure().nextAlphanumeric(32))
                .title(RandomStringUtils.secure().nextAlphabetic(12))
                .description(RandomStringUtils.secure().nextAlphanumeric(48))
                .price(BigInteger.valueOf(RandomUtils.secure().randomLong(500, 50_000)))
                .weight(BigInteger.valueOf(RandomUtils.secure().randomLong(100, 5_000)))
                .length(BigInteger.valueOf(RandomUtils.secure().randomLong(5, 100)))
                .width(BigInteger.valueOf(RandomUtils.secure().randomLong(5, 100)))
                .height(BigInteger.valueOf(RandomUtils.secure().randomLong(5, 100)))
                .build());

        warehouse = warehouseRepository.save(generateWarehouse());

        stockItem = stockItemRepository.save(StockItem.builder()
                .productId(product.getId())
                .stockItemGroupId(stockItemGroup.getId())
                .warehouseId(warehouse.getId())
                .expiryDate(LocalDate.now().plusDays(RandomUtils.secure().randomInt(30, 365)))
                .availableQuantity(BigInteger.valueOf(RandomUtils.secure().randomInt(50, 500)))
                .status(StockItemStatus.AVAILABLE)
                .isActive(true)
                .build());
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
            ShipmentDTO firstShipmentDTO = ShipmentDTO.builder()
                    .senderCode(warehouse.getCode())
                    .address(randomAddress())
                    .stockItemId(stockItem.getId())
                    .stockItemQuantity(3L)
                    .shipmentDirection(ShipmentDirection.OUTCOMING.name())
                    .build();

            ShipmentDTO secondShipmentDTO = ShipmentDTO.builder()
                    .senderCode(warehouse.getCode())
                    .address(randomAddress())
                    .stockItemId(stockItem.getId())
                    .stockItemQuantity(5L)
                    .shipmentDirection(ShipmentDirection.OUTCOMING.name())
                    .build();

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
            ShipmentDTO shipmentDTO = ShipmentDTO.builder()
                    .senderCode(warehouse.getCode())
                    .address(randomAddress())
                    .stockItemId(stockItem.getId())
                    .stockItemQuantity(2L)
                    .shipmentDirection(ShipmentDirection.OUTCOMING.name())
                    .build();
            shipmentService.save(shipmentDTO);

            List<Shipment> pagedShipments = shipmentService.findAll(10, 99);

            assertThat(pagedShipments).isEmpty();
        }

        @Test
        @DisplayName("findAll_fail_invalidPageSize: throws ValidationException for invalid page size")
        void findAll_fail_invalidPageSize() {
            assertThatThrownBy(() -> shipmentService.findAll(0, 1))
                    .isInstanceOf(ValidationException.class);
        }

        @Test
        @DisplayName("findAll_fail_invalidPageNumber: throws ValidationException for invalid page number")
        void findAll_fail_invalidPageNumber() {
            assertThatThrownBy(() -> shipmentService.findAll(10, 0))
                    .isInstanceOf(ValidationException.class);
        }
    }

    @Nested
    @DisplayName("findById(id: Long)")
    class FindShipmentByIdTests {
        @Test
        @DisplayName("findById_success: returns shipment when it exists")
        void findById_success() {
            ShipmentDTO shipmentDTO = ShipmentDTO.builder()
                    .senderCode(warehouse.getCode())
                    .address(randomAddress())
                    .stockItemId(stockItem.getId())
                    .stockItemQuantity(3L)
                    .shipmentDirection(ShipmentDirection.OUTCOMING.name())
                    .build();

            Shipment createdShipment = shipmentService.save(shipmentDTO);

            Shipment foundShipment = shipmentService.findById(createdShipment.getId());

            assertThat(foundShipment).isNotNull();
            assertThat(foundShipment.getId()).isEqualTo(createdShipment.getId());
        }

        @Test
        @DisplayName("findById_fail_notFound: throws NotFoundException when shipment does not exist")
        void findById_fail_notFound() {
            assertThatThrownBy(() -> shipmentService.findById(9999L))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("Shipment with ID '9999' was not found");
        }

        @Test
        @DisplayName("findById_fail_null: throws ValidationException when id is null")
        void findById_fail_null() {
            assertThatThrownBy(() -> shipmentService.findById(null))
                    .isInstanceOf(ValidationException.class);
        }
    }

    @Nested
    @DisplayName("findBy(warehouseIdSender, warehouseIdRecipient, stockItemId, status, shipmentDirection, from, to, pageSize, page)")
    class FindByShipmentsCriteriaTests {
        @Test
        @DisplayName("findBy_success_filtersByAllFields")
        void findBy_success_filtersByAllFields() {
            Warehouse recipientWarehouse = warehouseRepository.save(generateWarehouse());
            ShipmentDTO matchDto = ShipmentDTO.builder()
                    .senderCode(warehouse.getCode())
                    .recipientCode(recipientWarehouse.getCode())
                    .stockItemId(stockItem.getId())
                    .stockItemQuantity(2L)
                    .status(ShipmentStatus.INITIATED.name())
                    .shipmentDirection(ShipmentDirection.OUTCOMING.name())
                    .build();
            Shipment matched = shipmentService.save(matchDto);
            Warehouse otherRecipient = warehouseRepository.save(generateWarehouse());
            ShipmentDTO noiseDifferentRecipient = ShipmentDTO.builder()
                    .senderCode(warehouse.getCode())
                    .recipientCode(otherRecipient.getCode())
                    .stockItemId(stockItem.getId())
                    .stockItemQuantity(1L)
                    .status(ShipmentStatus.INITIATED.name())
                    .shipmentDirection(ShipmentDirection.OUTCOMING.name())
                    .build();
            shipmentService.save(noiseDifferentRecipient);
            ShipmentDTO noiseDifferentStatus = ShipmentDTO.builder()
                    .senderCode(warehouse.getCode())
                    .recipientCode(recipientWarehouse.getCode())
                    .stockItemId(stockItem.getId())
                    .stockItemQuantity(1L)
                    .status(ShipmentStatus.SENT.name())
                    .shipmentDirection(ShipmentDirection.OUTCOMING.name())
                    .build();
            shipmentService.save(noiseDifferentStatus);
            ShipmentDTO noiseDifferentDirection = ShipmentDTO.builder()
                    .senderCode(warehouse.getCode())
                    .recipientCode(recipientWarehouse.getCode())
                    .stockItemId(stockItem.getId())
                    .stockItemQuantity(1L)
                    .status(ShipmentStatus.INITIATED.name())
                    .shipmentDirection(ShipmentDirection.INCOMING.name())
                    .build();
            shipmentService.save(noiseDifferentDirection);
            var from = java.time.LocalDateTime.now().minusHours(1);
            var to = java.time.LocalDateTime.now().plusHours(1);
            List<Shipment> result = shipmentService.findBy(
                    warehouse.getId(),
                    recipientWarehouse.getId(),
                    stockItem.getId(),
                    ShipmentStatus.INITIATED.name(),
                    ShipmentDirection.OUTCOMING.name(),
                    from,
                    to,
                    50,
                    1
            );
            assertThat(result)
                    .isNotNull()
                    .extracting(Shipment::getId)
                    .contains(matched.getId());
            assertThat(result)
                    .extracting(Shipment::getWarehouseIdRecipient)
                    .containsOnly(recipientWarehouse.getId());
            assertThat(result)
                    .allSatisfy(s -> assertThat(s.getStatus())
                            .isEqualTo(ShipmentStatus.INITIATED));
            assertThat(result)
                    .allSatisfy(s -> assertThat(s.getShipmentDirection())
                            .isEqualTo(ShipmentDirection.OUTCOMING));
        }

        @Test
        @DisplayName("findBy_success_directionOnly")
        void findBy_success_directionOnly() {
            ShipmentDTO outgoingDto = ShipmentDTO.builder()
                    .senderCode(warehouse.getCode())
                    .address(randomAddress())
                    .stockItemId(stockItem.getId())
                    .stockItemQuantity(1L)
                    .shipmentDirection(ShipmentDirection.OUTCOMING.name())
                    .build();
            Shipment outgoing = shipmentService.save(outgoingDto);
            Warehouse sender2 = warehouseRepository.save(generateWarehouse());
            ShipmentDTO incomingDto = ShipmentDTO.builder()
                    .recipientCode(sender2.getCode())
                    .stockItemId(stockItem.getId())
                    .stockItemQuantity(1L)
                    .shipmentDirection(ShipmentDirection.INCOMING.name())
                    .build();
            shipmentService.save(incomingDto);
            List<Shipment> onlyOutgoing = shipmentService.findBy(
                    null,
                    null,
                    null,
                    null,
                    ShipmentDirection.OUTCOMING.name(),
                    null,
                    null,
                    50,
                    1
            );
            assertThat(onlyOutgoing)
                    .isNotNull()
                    .extracting(Shipment::getShipmentDirection)
                    .containsOnly(ShipmentDirection.OUTCOMING);
            assertThat(onlyOutgoing)
                    .extracting(Shipment::getId)
                    .contains(outgoing.getId());
        }

        @Test
        @DisplayName("findBy_success_statusOnly")
        void findBy_success_statusOnly() {
            ShipmentDTO plannedDto = ShipmentDTO.builder()
                    .senderCode(warehouse.getCode())
                    .address(randomAddress())
                    .stockItemId(stockItem.getId())
                    .stockItemQuantity(1L)
                    .shipmentDirection(ShipmentDirection.OUTCOMING.name())
                    .build();
            Shipment planned = shipmentService.save(plannedDto);
            ShipmentDTO sentDto = ShipmentDTO.builder()
                    .senderCode(warehouse.getCode())
                    .address(randomAddress())
                    .stockItemId(stockItem.getId())
                    .stockItemQuantity(1L)
                    .status(ShipmentStatus.SENT.name())
                    .shipmentDirection(ShipmentDirection.OUTCOMING.name())
                    .build();
            shipmentService.save(sentDto);
            List<Shipment> onlyPlanned = shipmentService.findBy(
                    null,
                    null,
                    null,
                    ShipmentStatus.PLANNED.name(),
                    null,
                    null,
                    null,
                    50,
                    1
            );
            assertThat(onlyPlanned)
                    .isNotNull()
                    .extracting(Shipment::getStatus)
                    .containsOnly(ShipmentStatus.PLANNED);
            assertThat(onlyPlanned)
                    .extracting(Shipment::getId)
                    .contains(planned.getId());
        }

        @Test
        @DisplayName("findBy_success_dateRangeInclusive")
        void findBy_success_dateRangeInclusive() {
            ShipmentDTO dto = ShipmentDTO.builder()
                    .senderCode(warehouse.getCode())
                    .address(randomAddress())
                    .stockItemId(stockItem.getId())
                    .stockItemQuantity(1L)
                    .shipmentDirection(ShipmentDirection.OUTCOMING.name())
                    .build();
            Shipment created = shipmentService.save(dto);
            var from = java.time.LocalDateTime.now().minusMinutes(5);
            var to = java.time.LocalDateTime.now().plusMinutes(5);
            List<Shipment> within = shipmentService.findBy(
                    null,
                    null,
                    null,
                    null,
                    null,
                    from,
                    to,
                    50,
                    1
            );
            assertThat(within)
                    .isNotNull()
                    .extracting(Shipment::getId)
                    .contains(created.getId());
            var futureFrom = java.time.LocalDateTime.now().plusDays(1);
            var futureTo = java.time.LocalDateTime.now().plusDays(2);
            List<Shipment> none = shipmentService.findBy(
                    null,
                    null,
                    null,
                    null,
                    null,
                    futureFrom,
                    futureTo,
                    50,
                    1
            );
            assertThat(none)
                    .isNotNull()
                    .isEmpty();
        }

        @Test
        @DisplayName("findBy_success_pagination")
        void findBy_success_pagination() {
            for (int i = 0; i < 5; i++) {
                ShipmentDTO dto = ShipmentDTO.builder()
                        .senderCode(warehouse.getCode())
                        .address(randomAddress())
                        .stockItemId(stockItem.getId())
                        .stockItemQuantity(1L)
                        .shipmentDirection(ShipmentDirection.OUTCOMING.name())
                        .build();
                shipmentService.save(dto);
            }
            List<Shipment> page1 = shipmentService.findBy(
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    2,
                    1
            );
            List<Shipment> page2 = shipmentService.findBy(
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    2,
                    2
            );
            assertThat(page1)
                    .isNotNull()
                    .hasSizeBetween(0, 2);
            assertThat(page2)
                    .isNotNull()
                    .hasSizeBetween(0, 2);
        }

        @Test
        @DisplayName("findBy_fail_invalidPageSize")
        void findBy_fail_invalidPageSize() {
            assertThatThrownBy(() -> shipmentService.findBy(
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    0,
                    1
            )).isInstanceOf(ValidationException.class);
        }

        @Test
        @DisplayName("findBy_fail_invalidPageNumber")
        void findBy_fail_invalidPageNumber() {
            assertThatThrownBy(() -> shipmentService.findBy(
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    10,
                    0
            )).isInstanceOf(ValidationException.class);
        }

        @Test
        @DisplayName("findBy_fail_invalidStatusString")
        void findBy_fail_invalidStatusString() {
            assertThatThrownBy(() -> shipmentService.findBy(
                    null,
                    null,
                    null,
                    "NOT_A_STATUS",
                    null,
                    null,
                    null,
                    10,
                    1))
                    .isInstanceOf(ValidationException.class);
        }

        @Test
        @DisplayName("findBy_fail_invalidDirectionString")
        void findBy_fail_invalidDirectionString() {
            assertThatThrownBy(() -> shipmentService.findBy(
                    null,
                    null,
                    null,
                    null,
                    "NOT_A_DIRECTION",
                    null,
                    null,
                    10,
                    1))
                    .isInstanceOf(ValidationException.class);
        }
    }

    @Nested
    @DisplayName("save(shipmentDTO: ShipmentDTO)")
    class SaveShipmentTests {
        @Test
        @DisplayName("save_success_withRecipient: creates a new Shipment when valid sender and recipient are provided")
        void save_success_withRecipient() {
            Warehouse recipientWarehouse = warehouseRepository.save(generateWarehouse());

            ShipmentDTO shipmentDTO = ShipmentDTO.builder()
                    .senderCode(warehouse.getCode())
                    .recipientCode(recipientWarehouse.getCode())
                    .stockItemId(stockItem.getId())
                    .stockItemQuantity(5L)
                    .status(ShipmentStatus.INITIATED.name())
                    .shipmentDirection(ShipmentDirection.OUTCOMING.name())
                    .build();

            Shipment createdShipment = shipmentService.save(shipmentDTO);

            assertThat(createdShipment.getId()).isNotNull();
            assertThat(createdShipment.getWarehouseIdSender()).isEqualTo(warehouse.getId());
            assertThat(createdShipment.getWarehouseIdRecipient()).isEqualTo(recipientWarehouse.getId());
            assertThat(createdShipment.getStatus()).isEqualTo(ShipmentStatus.INITIATED);
        }

        @Test
        @DisplayName("save_success_withAddressOnly: creates a Shipment when address provided and recipientCode is null")
        void save_success_withAddressOnly() {
            ShipmentDTO shipmentDTO = ShipmentDTO.builder()
                    .senderCode(warehouse.getCode())
                    .address(randomAddress())
                    .stockItemId(stockItem.getId())
                    .stockItemQuantity(2L)
                    .shipmentDirection(ShipmentDirection.OUTCOMING.name())
                    .build();

            Shipment createdShipment = shipmentService.save(shipmentDTO);

            assertThat(createdShipment.getId()).isNotNull();
            assertThat(createdShipment.getWarehouseIdSender()).isEqualTo(warehouse.getId());
            assertThat(createdShipment.getWarehouseIdRecipient()).isNull();
            assertThat(createdShipment.getAddress()).isEqualTo(shipmentDTO.getAddress());
            assertThat(createdShipment.getStatus()).isEqualTo(ShipmentStatus.PLANNED);
        }

        @Test
        @DisplayName("save_success_defaultStatus_withAddress: sets PLANNED when status is blank")
        void save_success_defaultStatus_withAddress() {
            ShipmentDTO shipmentDTO = ShipmentDTO.builder()
                    .senderCode(warehouse.getCode())
                    .address(randomAddress())
                    .stockItemId(stockItem.getId())
                    .stockItemQuantity(1L)
                    .shipmentDirection(ShipmentDirection.OUTCOMING.name())
                    .build();

            Shipment createdShipment = shipmentService.save(shipmentDTO);

            assertThat(createdShipment.getStatus()).isEqualTo(ShipmentStatus.PLANNED);
        }

        @Test
        @DisplayName("save_fail_bothNull: throws BusinessException when sender+recipient+address are all null targets")
        void save_fail_bothNull() {
            ShipmentDTO shipmentDTO = ShipmentDTO.builder()
                    .shipmentDirection(ShipmentDirection.OUTCOMING.name())
                    .stockItemId(stockItem.getId())
                    .stockItemQuantity(1L)
                    .build();

            assertThatThrownBy(() -> shipmentService.save(shipmentDTO))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("save_fail_bothRecipientAndAddress: disallow both for OUTCOMING")
        void save_fail_bothNotNull() {
            Warehouse recipientWarehouse = warehouseRepository.save(generateWarehouse());

            ShipmentDTO shipmentDTO = ShipmentDTO.builder()
                    .senderCode(warehouse.getCode())
                    .recipientCode(recipientWarehouse.getCode())
                    .address(randomAddress())
                    .stockItemId(stockItem.getId())
                    .stockItemQuantity(3L)
                    .shipmentDirection(ShipmentDirection.OUTCOMING.name())
                    .build();

            assertThatThrownBy(() -> shipmentService.save(shipmentDTO))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("save_fail_sameSenderAndRecipient: throws BusinessException when sender and recipient are the same")
        void save_fail_sameSenderAndRecipient() {
            Warehouse secondWarehouse = warehouseRepository.save(generateWarehouse());

            ShipmentDTO shipmentDTO = ShipmentDTO.builder()
                    .senderCode(secondWarehouse.getCode())
                    .recipientCode(secondWarehouse.getCode())
                    .stockItemId(stockItem.getId())
                    .stockItemQuantity(2L)
                    .shipmentDirection(ShipmentDirection.OUTCOMING.name())
                    .build();

            assertThatThrownBy(() -> shipmentService.save(shipmentDTO))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("save_fail_unauthenticated: throws RegularAuthenticationException when user not authenticated")
        void save_fail_unauthenticated() {
            SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                    null, null, AuthorityUtils.createAuthorityList()));

            Warehouse recipientWarehouse = warehouseRepository.save(generateWarehouse());
            ShipmentDTO shipmentDTO = ShipmentDTO.builder()
                    .senderCode(warehouse.getCode())
                    .recipientCode(recipientWarehouse.getCode())
                    .stockItemId(stockItem.getId())
                    .stockItemQuantity(1L)
                    .shipmentDirection(ShipmentDirection.OUTCOMING.name())
                    .build();

            assertThatThrownBy(() -> shipmentService.save(shipmentDTO))
                    .isInstanceOf(AuthenticationException.class);
        }

        @ParameterizedTest(name = "save_fail_invalidStatus: status=''{0}'' is invalid")
        @ValueSource(strings = {"WRONG", "INVALID"})
        @DisplayName("save_fail_invalidStatus: throws BusinessException when shipment status is invalid")
        void save_fail_invalidStatus(String invalidStatus) {
            ShipmentDTO shipmentDTO = ShipmentDTO.builder()
                    .senderCode(warehouse.getCode())
                    .address(randomAddress())
                    .stockItemId(stockItem.getId())
                    .stockItemQuantity(1L)
                    .status(invalidStatus)
                    .shipmentDirection(ShipmentDirection.OUTCOMING.name())
                    .build();

            assertThatThrownBy(() -> shipmentService.save(shipmentDTO))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("synchronise(shipmentDTO: ShipmentDTO)")
    class SynchroniseShipmentTests {
        @Test
        @DisplayName("synchronise_success_whenEntityPlanned_andNewStatusNotPlanned_updatesEntity")
        void synchronise_success_whenEntityPlanned_andNewStatusNotPlanned_updatesEntity() {
            ShipmentDTO createShipmentDTO = ShipmentDTO.builder()
                    .senderCode(warehouse.getCode())
                    .address(randomAddress())
                    .stockItemId(stockItem.getId())
                    .stockItemQuantity(3L)
                    .shipmentDirection(ShipmentDirection.OUTCOMING.name())
                    .build();
            Shipment createdShipment = shipmentService.save(createShipmentDTO);

            long newQty = 9L;
            ShipmentDTO synchroniseShipmentDTO = ShipmentDTO.builder()
                    .id(createdShipment.getId())
                    .status(ShipmentStatus.SENT.name())
                    .stockItemQuantity(newQty)
                    .build();

            Shipment updatedShipment = shipmentService.synchroniseShipment(synchroniseShipmentDTO);

            assertThat(updatedShipment.getId()).isEqualTo(createdShipment.getId());
            assertThat(updatedShipment.getStatus()).isEqualTo(ShipmentStatus.SENT);
            assertThat(updatedShipment.getStockItemQuantity()).isEqualTo(newQty);
        }

        @Test
        @DisplayName("synchronise_success_SENT_reduces_sender_stock")
        void synchronise_success_SENT_reduces_sender_stock() {
            long plannedQty = Math.min(5L, stockItem.getAvailableQuantity().longValue());
            if (plannedQty == 0L) {
                plannedQty = 1L;
                stockItem.setAvailableQuantity(BigInteger.valueOf(10L));
                stockItemRepository.save(stockItem);
            }

            Shipment created = shipmentService.save(ShipmentDTO.builder()
                    .senderCode(warehouse.getCode())
                    .address(randomAddress())
                    .stockItemId(stockItem.getId())
                    .stockItemQuantity(plannedQty)
                    .shipmentDirection(ShipmentDirection.OUTCOMING.name())
                    .build());

            BigInteger before = stockItemRepository.findById(stockItem.getId()).orElseThrow().getAvailableQuantity();

            shipmentService.synchroniseShipment(ShipmentDTO.builder()
                    .id(created.getId())
                    .status(ShipmentStatus.SENT.name())
                    .build());

            BigInteger after = stockItemRepository.findById(stockItem.getId()).orElseThrow().getAvailableQuantity();
            assertThat(after).isEqualTo(before.subtract(BigInteger.valueOf(plannedQty)));
        }

        @Test
        @DisplayName("synchronise_then_update_DELIVERED_increments_existing_recipient_stock")
        void synchronise_then_update_DELIVERED_increments_existing_recipient_stock() {
            Warehouse recipient = warehouseRepository.save(generateWarehouse());

            StockItem recipientItem = stockItemRepository.save(StockItem.builder()
                    .productId(stockItem.getProductId())
                    .stockItemGroupId(stockItem.getStockItemGroupId())
                    .warehouseId(recipient.getId())
                    .availableQuantity(BigInteger.valueOf(7))
                    .status(StockItemStatus.AVAILABLE)
                    .isActive(true)
                    .build());

            long qty = 4L;

            Shipment created = shipmentService.save(ShipmentDTO.builder()
                    .senderCode(warehouse.getCode())
                    .recipientCode(recipient.getCode())
                    .stockItemId(stockItem.getId())
                    .stockItemQuantity(qty)
                    .shipmentDirection(ShipmentDirection.OUTCOMING.name())
                    .build());

            BigInteger recipientBefore = stockItemRepository.findById(recipientItem.getId())
                    .orElseThrow()
                    .getAvailableQuantity();

            Shipment afterSent = shipmentService.synchroniseShipment(ShipmentDTO.builder()
                    .id(created.getId())
                    .status(ShipmentStatus.SENT.name())
                    .build());
            assertThat(afterSent.getStatus()).isEqualTo(ShipmentStatus.SENT);

            Shipment afterDelivered = shipmentService.update(ShipmentDTO.builder()
                    .id(created.getId())
                    .status(ShipmentStatus.DELIVERED.name())
                    .build());
            assertThat(afterDelivered.getStatus()).isEqualTo(ShipmentStatus.DELIVERED);

            BigInteger recipientAfter = stockItemRepository.findById(recipientItem.getId())
                    .orElseThrow()
                    .getAvailableQuantity();

            assertThat(recipientAfter).isEqualTo(recipientBefore.add(BigInteger.valueOf(qty)));
        }


        @Test
        @DisplayName("synchronise_fail_whenStatusInvalid_throwsBusinessException")
        void synchronise_fail_whenStatusInvalid_throwsBusinessException() {
            ShipmentDTO shipmentDTO = ShipmentDTO.builder()
                    .id(1L)
                    .status("WRONG")
                    .build();
            assertThatThrownBy(() -> shipmentService.synchroniseShipment(shipmentDTO))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("synchronise_fail_whenEntityNotPlanned_throwsBusinessException")
        void synchronise_fail_whenEntityNotPlanned_throwsBusinessException() {
            ShipmentDTO createShipmentDTO = ShipmentDTO.builder()
                    .senderCode(warehouse.getCode())
                    .address(randomAddress())
                    .stockItemId(stockItem.getId())
                    .stockItemQuantity(2L)
                    .status(ShipmentStatus.SENT.name())
                    .shipmentDirection(ShipmentDirection.OUTCOMING.name())
                    .build();
            Shipment createdShipment = shipmentService.save(createShipmentDTO);

            ShipmentDTO synchroniseShipmentDTO = ShipmentDTO.builder()
                    .id(createdShipment.getId())
                    .status(ShipmentStatus.DELIVERED.name())
                    .build();

            assertThatThrownBy(() -> shipmentService.synchroniseShipment(synchroniseShipmentDTO))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("synchronise_fail_whenNewStatusPlanned_throwsBusinessException")
        void synchronise_fail_whenNewStatusPlanned_throwsBusinessException() {
            ShipmentDTO createShipmentDTO = ShipmentDTO.builder()
                    .senderCode(warehouse.getCode())
                    .address(randomAddress())
                    .stockItemId(stockItem.getId())
                    .stockItemQuantity(1L)
                    .shipmentDirection(ShipmentDirection.OUTCOMING.name())
                    .build();
            Shipment createdShipment = shipmentService.save(createShipmentDTO);

            ShipmentDTO synchroniseShipmentDTO = ShipmentDTO.builder()
                    .id(createdShipment.getId())
                    .status(ShipmentStatus.PLANNED.name())
                    .build();

            assertThatThrownBy(() -> shipmentService.synchroniseShipment(synchroniseShipmentDTO))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("synchronise_then_update_DELIVERED_creates_recipient_stock_if_missing")
        void synchronise_then_update_DELIVERED_creates_recipient_stock_if_missing() {
            Warehouse recipient = warehouseRepository.save(generateWarehouse());
            long qty = 3L;

            Shipment created = shipmentService.save(ShipmentDTO.builder()
                    .senderCode(warehouse.getCode())
                    .recipientCode(recipient.getCode())
                    .stockItemId(stockItem.getId())
                    .stockItemQuantity(qty)
                    .shipmentDirection(ShipmentDirection.OUTCOMING.name())
                    .build());

            List<StockItem> preRecipientItems = stockItemService.findBy(
                    List.of(recipient.getId()),
                    List.of(stockItem.getProductId()),
                    null, null, null, null, null,
                    1, 1
            );
            assertThat(preRecipientItems).isEmpty();

            Shipment afterSent = shipmentService.synchroniseShipment(ShipmentDTO.builder()
                    .id(created.getId())
                    .status(ShipmentStatus.SENT.name())
                    .build());
            assertThat(afterSent.getStatus()).isEqualTo(ShipmentStatus.SENT);

            Shipment afterDelivered = shipmentService.update(ShipmentDTO.builder()
                    .id(created.getId())
                    .status(ShipmentStatus.DELIVERED.name())
                    .build());
            assertThat(afterDelivered.getStatus()).isEqualTo(ShipmentStatus.DELIVERED);

            List<StockItem> postRecipientItems = stockItemService.findBy(
                    List.of(recipient.getId()),
                    List.of(stockItem.getProductId()),
                    null, null, null, null, null,
                    1, 1
            );
            assertThat(postRecipientItems).isNotEmpty();
            StockItem createdRecipient = postRecipientItems.getFirst();
            assertThat(createdRecipient.getAvailableQuantity()).isEqualTo(BigInteger.valueOf(qty));
            assertThat(createdRecipient.getWarehouseId()).isEqualTo(recipient.getId());
            assertThat(createdRecipient.getProductId()).isEqualTo(stockItem.getProductId());
            assertThat(createdRecipient.getStockItemGroupId()).isEqualTo(stockItem.getStockItemGroupId());
        }

        @Test
        @DisplayName("synchronise_fail_whenIdMissing_throwsValidationException")
        void synchronise_fail_whenIdMissing_throwsValidationException() {
            ShipmentDTO synchroniseShipmentDTO = ShipmentDTO.builder()
                    .status(ShipmentStatus.SENT.name())
                    .build();
            assertThatThrownBy(() -> shipmentService.synchroniseShipment(synchroniseShipmentDTO))
                    .isInstanceOf(jakarta.validation.ValidationException.class);
        }

        @Test
        @DisplayName("synchronise_fail_whenIdNotFound_throwsNotFoundException")
        void synchronise_fail_whenIdNotFound_throwsNotFoundException() {
            ShipmentDTO synchroniseShipmentDTO = ShipmentDTO.builder()
                    .id(9_999_999L)
                    .status(ShipmentStatus.SENT.name())
                    .build();
            assertThatThrownBy(() -> shipmentService.synchroniseShipment(synchroniseShipmentDTO))
                    .isInstanceOf(NotFoundException.class);
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

            ShipmentDTO createShipmentDTO = ShipmentDTO.builder()
                    .senderCode(warehouse.getCode())
                    .recipientCode(initialRecipientWarehouse.getCode())
                    .stockItemId(stockItem.getId())
                    .stockItemQuantity(3L)
                    .shipmentDirection(ShipmentDirection.OUTCOMING.name())
                    .build();
            Shipment createdShipment = shipmentService.save(createShipmentDTO);

            ShipmentDTO updateShipmentDTO = ShipmentDTO.builder()
                    .id(createdShipment.getId())
                    .recipientCode(newRecipientWarehouse.getCode())
                    .stockItemQuantity(7L)
                    .status(ShipmentStatus.SENT.name())
                    .build();

            Shipment updatedShipment = shipmentService.update(updateShipmentDTO);

            assertThat(updatedShipment.getId()).isEqualTo(createdShipment.getId());
            assertThat(updatedShipment.getWarehouseIdRecipient()).isEqualTo(newRecipientWarehouse.getId());
            assertThat(updatedShipment.getStockItemQuantity()).isEqualTo(7L);
            assertThat(updatedShipment.getStatus()).isEqualTo(ShipmentStatus.SENT);
            assertThat(updatedShipment.getInitiatorId()).isEqualTo(UserService.getCurrentlyAuthenticatedUserID());
        }

        @Test
        @DisplayName("update_success_partial: leaves unspecified fields unchanged")
        void update_success_partial() {
            ShipmentDTO createShipmentDTO = ShipmentDTO.builder()
                    .senderCode(warehouse.getCode())
                    .address(randomAddress())
                    .stockItemId(stockItem.getId())
                    .stockItemQuantity(3L)
                    .shipmentDirection(ShipmentDirection.OUTCOMING.name())
                    .build();
            Shipment createdShipment = shipmentService.save(createShipmentDTO);

            ShipmentDTO updateShipmentDTO = ShipmentDTO.builder()
                    .id(createdShipment.getId())
                    .status(ShipmentStatus.SENT.name())
                    .build();

            Shipment updatedShipment = shipmentService.update(updateShipmentDTO);

            assertThat(updatedShipment.getId()).isEqualTo(createdShipment.getId());
            assertThat(updatedShipment.getWarehouseIdSender()).isEqualTo(createdShipment.getWarehouseIdSender());
            assertThat(updatedShipment.getWarehouseIdRecipient()).isEqualTo(createdShipment.getWarehouseIdRecipient());
            assertThat(updatedShipment.getStockItemId()).isEqualTo(createdShipment.getStockItemId());
            assertThat(updatedShipment.getStockItemQuantity()).isEqualTo(createdShipment.getStockItemQuantity());
            assertThat(updatedShipment.getStatus()).isEqualTo(ShipmentStatus.SENT);
        }

        @Test
        @DisplayName("update_fail_notFound: throws NotFoundException when shipment ID does not exist")
        void update_fail_notFound() {
            ShipmentDTO updateShipmentDTO = ShipmentDTO.builder()
                    .id(999_999L)
                    .status(ShipmentStatus.SENT.name())
                    .build();

            assertThatThrownBy(() -> shipmentService.update(updateShipmentDTO))
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        @DisplayName("update_fail_invalidStatus: throws BusinessException when shipment status is invalid")
        void update_fail_invalidStatus() {
            ShipmentDTO createShipmentDTO = ShipmentDTO.builder()
                    .senderCode(warehouse.getCode())
                    .address(randomAddress())
                    .stockItemId(stockItem.getId())
                    .stockItemQuantity(3L)
                    .shipmentDirection(ShipmentDirection.OUTCOMING.name())
                    .build();
            Shipment createdShipment = shipmentService.save(createShipmentDTO);

            ShipmentDTO updateShipmentDTO = ShipmentDTO.builder()
                    .id(createdShipment.getId())
                    .status("WRONG")
                    .build();

            assertThatThrownBy(() -> shipmentService.update(updateShipmentDTO))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("update_fail_bothNotNull: disallow recipient & address together for OUTCOMING")
        void update_fail_bothNotNull() {
            ShipmentDTO createShipmentDTO = ShipmentDTO.builder()
                    .senderCode(warehouse.getCode())
                    .address(randomAddress())
                    .stockItemId(stockItem.getId())
                    .stockItemQuantity(3L)
                    .shipmentDirection(ShipmentDirection.OUTCOMING.name())
                    .build();
            Shipment createdShipment = shipmentService.save(createShipmentDTO);

            Warehouse recipientWarehouse = warehouseRepository.save(generateWarehouse());

            ShipmentDTO updateShipmentDTO = ShipmentDTO.builder()
                    .id(createdShipment.getId())
                    .recipientCode(recipientWarehouse.getCode())
                    .address(randomAddress())
                    .build();

            assertThatThrownBy(() -> shipmentService.update(updateShipmentDTO))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("update_fail_sameSenderAndRecipient: throws BusinessException when sender and recipient are the same")
        void update_fail_sameSenderAndRecipient() {
            Warehouse sameWarehouse = warehouseRepository.save(generateWarehouse());

            ShipmentDTO createShipmentDTO = ShipmentDTO.builder()
                    .senderCode(warehouse.getCode())
                    .recipientCode(sameWarehouse.getCode())
                    .stockItemId(stockItem.getId())
                    .stockItemQuantity(3L)
                    .shipmentDirection(ShipmentDirection.OUTCOMING.name())
                    .build();
            Shipment createdShipment = shipmentService.save(createShipmentDTO);

            ShipmentDTO updateShipmentDTO = ShipmentDTO.builder()
                    .id(createdShipment.getId())
                    .senderCode(sameWarehouse.getCode())
                    .recipientCode(sameWarehouse.getCode())
                    .build();

            assertThatThrownBy(() -> shipmentService.update(updateShipmentDTO))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("update_fail_missingId: throws ValidationException when id is null")
        void update_fail_missingId() {
            ShipmentDTO updateShipmentDTO = ShipmentDTO.builder()
                    .status(ShipmentStatus.SENT.name())
                    .build();

            assertThatThrownBy(() -> shipmentService.update(updateShipmentDTO))
                    .isInstanceOf(ValidationException.class);
        }
    }

    @Nested
    @DisplayName("stock movements on SENT/DELIVERED")
    class StockMovementTests {
        @Test
        @DisplayName("save_success_whenStatusSent_decrementsSenderQuantity")
        void save_success_whenStatusSent_decrementsSenderQuantity() {
            BigInteger initialQuantity = BigInteger.valueOf(50);
            stockItem.setAvailableQuantity(initialQuantity);
            stockItemRepository.save(stockItem);

            ShipmentDTO shipmentDTO = ShipmentDTO.builder()
                    .senderCode(warehouse.getCode())
                    .address(randomAddress())
                    .stockItemId(stockItem.getId())
                    .stockItemQuantity(7L)
                    .status(ShipmentStatus.SENT.name())
                    .shipmentDirection(ShipmentDirection.OUTCOMING.name())
                    .build();

            shipmentService.save(shipmentDTO);

            StockItem updatedSender = stockItemRepository.findById(stockItem.getId()).orElseThrow();
            assertThat(updatedSender.getAvailableQuantity())
                    .isEqualTo(initialQuantity.subtract(BigInteger.valueOf(7)));
        }

        @Test
        @DisplayName("save_fail_whenStatusSent_quantityGreaterThanAvailable_throwsBusinessException")
        void save_fail_whenStatusSent_quantityGreaterThanAvailable_throwsBusinessException() {
            BigInteger initialQuantity = BigInteger.valueOf(3);
            stockItem.setAvailableQuantity(initialQuantity);
            stockItemRepository.save(stockItem);

            ShipmentDTO shipmentDTO = ShipmentDTO.builder()
                    .senderCode(warehouse.getCode())
                    .address(randomAddress())
                    .stockItemId(stockItem.getId())
                    .stockItemQuantity(10L)
                    .status(ShipmentStatus.SENT.name())
                    .shipmentDirection(ShipmentDirection.OUTCOMING.name())
                    .build();

            assertThatThrownBy(() -> shipmentService.save(shipmentDTO))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("update_success_toDelivered_incrementsExistingRecipientItem")
        void update_success_toDelivered_incrementsExistingRecipientItem() {
            Warehouse recipientWarehouse = warehouseRepository.save(generateWarehouse());

            StockItem recipientItem = stockItemRepository.save(StockItem.builder()
                    .productId(stockItem.getProductId())
                    .stockItemGroupId(stockItem.getStockItemGroupId())
                    .warehouseId(recipientWarehouse.getId())
                    .expiryDate(stockItem.getExpiryDate())
                    .availableQuantity(BigInteger.valueOf(10))
                    .status(StockItemStatus.AVAILABLE)
                    .isActive(true)
                    .build());

            ShipmentDTO createShipmentDTO = ShipmentDTO.builder()
                    .senderCode(warehouse.getCode())
                    .recipientCode(recipientWarehouse.getCode())
                    .stockItemId(stockItem.getId())
                    .stockItemQuantity(4L)
                    .shipmentDirection(ShipmentDirection.OUTCOMING.name())
                    .build();

            Shipment createdShipment = shipmentService.save(createShipmentDTO);

            ShipmentDTO toSent = ShipmentDTO.builder()
                    .id(createdShipment.getId())
                    .status(ShipmentStatus.SENT.name())
                    .build();
            shipmentService.update(toSent);

            ShipmentDTO toDelivered = ShipmentDTO.builder()
                    .id(createdShipment.getId())
                    .status(ShipmentStatus.DELIVERED.name())
                    .build();
            shipmentService.update(toDelivered);

            StockItem updatedRecipient = stockItemRepository.findById(recipientItem.getId()).orElseThrow();
            assertThat(updatedRecipient.getAvailableQuantity())
                    .isEqualTo(BigInteger.valueOf(10).add(BigInteger.valueOf(4)));
        }

        @Test
        @DisplayName("update_success_toDelivered_createsRecipientItemWhenMissing")
        void update_success_toDelivered_createsRecipientItemWhenMissing() {
            Warehouse recipientWarehouse = warehouseRepository.save(generateWarehouse());

            ShipmentDTO createShipmentDTO = ShipmentDTO.builder()
                    .senderCode(warehouse.getCode())
                    .recipientCode(recipientWarehouse.getCode())
                    .stockItemId(stockItem.getId())
                    .stockItemQuantity(6L)
                    .shipmentDirection(ShipmentDirection.OUTCOMING.name())
                    .build();

            Shipment createdShipment = shipmentService.save(createShipmentDTO);

            ShipmentDTO toSent = ShipmentDTO.builder()
                    .id(createdShipment.getId())
                    .status(ShipmentStatus.SENT.name())
                    .build();
            shipmentService.update(toSent);

            ShipmentDTO toDelivered = ShipmentDTO.builder()
                    .id(createdShipment.getId())
                    .status(ShipmentStatus.DELIVERED.name())
                    .build();
            shipmentService.update(toDelivered);

            List<StockItem> recipientItems = stockItemRepository.findAll().stream()
                    .filter(si -> si.getWarehouseId().equals(recipientWarehouse.getId())
                            && si.getProductId().equals(stockItem.getProductId()))
                    .toList();

            assertThat(recipientItems)
                    .hasSize(1);
            assertThat(recipientItems.getFirst().getAvailableQuantity())
                    .isEqualTo(BigInteger.valueOf(6));
            assertThat(recipientItems.getFirst().getStockItemGroupId())
                    .isEqualTo(stockItem.getStockItemGroupId());
            assertThat(recipientItems.getFirst().getStatus())
                    .isEqualTo(StockItemStatus.AVAILABLE);
        }
    }
}
