package io.store.ua.controllers;

import io.store.ua.AbstractIT;
import io.store.ua.entity.*;
import io.store.ua.enums.*;
import io.store.ua.models.data.Address;
import io.store.ua.models.dto.ShipmentDTO;
import io.store.ua.utility.CodeGenerator;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.*;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ShipmentControllerIT extends AbstractIT {
    private static final String MANAGER = "manager";
    private HttpHeaders ownerAuthenticationHeaders;
    private HttpHeaders managerAuthenticationHeaders;
    private Warehouse senderWarehouse;
    private Warehouse recipientWarehouse;
    private StockItem stockItem;

    @BeforeAll
    void setupAuthentication() {
        ownerAuthenticationHeaders = generateAuthenticationHeaders();
        managerAuthenticationHeaders = generateAuthenticationHeaders(MANAGER, MANAGER);
    }

    @BeforeEach
    void setUp() {
        userRepository.save(User.builder()
                .username(MANAGER)
                .password(passwordEncoder.encode(MANAGER))
                .email("%s@example.com".formatted(MANAGER))
                .role(UserRole.MANAGER)
                .status(UserStatus.ACTIVE)
                .timezone("UTC")
                .build());

        StockItemGroup stockItemGroup = stockItemGroupRepository.save(StockItemGroup.builder()
                .code(RandomStringUtils.secure().nextAlphanumeric(16))
                .name(RandomStringUtils.secure().nextAlphabetic(10))
                .isActive(true)
                .build());

        Product product = productRepository.save(Product.builder()
                .code(RandomStringUtils.secure().nextAlphanumeric(24))
                .title(RandomStringUtils.secure().nextAlphabetic(12))
                .description(RandomStringUtils.secure().nextAlphanumeric(40))
                .price(BigInteger.valueOf(RandomUtils.secure().randomLong(500, 50000)))
                .currency(Currency.EUR.name())
                .weight(BigInteger.valueOf(RandomUtils.secure().randomLong(100, 5000)))
                .length(BigInteger.valueOf(RandomUtils.secure().randomLong(5, 100)))
                .width(BigInteger.valueOf(RandomUtils.secure().randomLong(5, 100)))
                .height(BigInteger.valueOf(RandomUtils.secure().randomLong(5, 100)))
                .build());

        senderWarehouse = warehouseRepository.save(generateWarehouse());
        recipientWarehouse = warehouseRepository.save(generateWarehouse());

        stockItem = stockItemRepository.save(StockItem.builder()
                .batchVersion(1L)
                .code(CodeGenerator.StockCodeGenerator.generate())
                .productId(product.getId())
                .stockItemGroupId(stockItemGroup.getId())
                .warehouseId(senderWarehouse.getId())
                .expiryDate(LocalDate.now().plusDays(RandomUtils.secure().randomInt(30, 365)))
                .availableQuantity(BigInteger.valueOf(RandomUtils.secure().randomInt(5, 500)))
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

    private HttpHeaders allHeaders(HttpHeaders baseHeaders) {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.putAll(baseHeaders);
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        return httpHeaders;
    }

    private ShipmentDTO outgoingWithRecipient() {
        return ShipmentDTO.builder()
                .senderCode(senderWarehouse.getCode())
                .recipientCode(recipientWarehouse.getCode())
                .stockItemId(stockItem.getId())
                .stockItemQuantity(3L)
                .shipmentDirection(ShipmentDirection.OUTCOMING.name())
                .status(ShipmentStatus.PLANNED.name())
                .build();
    }

    private ShipmentDTO outgoingWithAddress() {
        return ShipmentDTO.builder()
                .senderCode(senderWarehouse.getCode())
                .address(randomAddress())
                .stockItemId(stockItem.getId())
                .stockItemQuantity(2L)
                .shipmentDirection(ShipmentDirection.OUTCOMING.name())
                .build();
    }

    private Shipment postShipment(ShipmentDTO shipmentDTO, HttpHeaders authenticationHeaders) {
        ResponseEntity<Shipment> responseEntity = restClient.exchange(
                "/api/v1/shipments",
                HttpMethod.POST,
                new HttpEntity<>(shipmentDTO, allHeaders(authenticationHeaders)),
                Shipment.class
        );
        assertThat(responseEntity.getStatusCode())
                .isEqualTo(HttpStatus.OK);
        assertThat(responseEntity.getBody())
                .isNotNull();
        return responseEntity.getBody();
    }

    @Nested
    @DisplayName("GET /api/v1/shipments/findBy")
    class FindByTests {
        @Test
        @DisplayName("findBy_success_filtersByAllFields")
        void findBy_success_filtersByAllFields() {
            Shipment matched = postShipment(ShipmentDTO.builder()
                    .senderCode(senderWarehouse.getCode())
                    .recipientCode(recipientWarehouse.getCode())
                    .stockItemId(stockItem.getId())
                    .stockItemQuantity(2L)
                    .shipmentDirection(ShipmentDirection.OUTCOMING.name())
                    .status(ShipmentStatus.INITIATED.name())
                    .build(), ownerAuthenticationHeaders);

            Warehouse otherRecipientWarehouse = warehouseRepository.save(generateWarehouse());
            postShipment(ShipmentDTO.builder()
                    .senderCode(senderWarehouse.getCode())
                    .recipientCode(otherRecipientWarehouse.getCode())
                    .stockItemId(stockItem.getId())
                    .stockItemQuantity(1L)
                    .shipmentDirection(ShipmentDirection.OUTCOMING.name())
                    .status(ShipmentStatus.INITIATED.name())
                    .build(), ownerAuthenticationHeaders);

            postShipment(ShipmentDTO.builder()
                    .senderCode(senderWarehouse.getCode())
                    .recipientCode(recipientWarehouse.getCode())
                    .stockItemId(stockItem.getId())
                    .stockItemQuantity(1L)
                    .shipmentDirection(ShipmentDirection.INCOMING.name())
                    .status(ShipmentStatus.INITIATED.name())
                    .build(), ownerAuthenticationHeaders);

            postShipment(ShipmentDTO.builder()
                    .senderCode(senderWarehouse.getCode())
                    .recipientCode(recipientWarehouse.getCode())
                    .stockItemId(stockItem.getId())
                    .stockItemQuantity(1L)
                    .shipmentDirection(ShipmentDirection.OUTCOMING.name())
                    .status(ShipmentStatus.SENT.name())
                    .build(), ownerAuthenticationHeaders);

            String url = UriComponentsBuilder.fromPath("/api/v1/shipments/findBy")
                    .queryParam("warehouseIdSender", senderWarehouse.getId())
                    .queryParam("warehouseIdRecipient", recipientWarehouse.getId())
                    .queryParam("stockItemId", stockItem.getId())
                    .queryParam("status", ShipmentStatus.INITIATED.name())
                    .queryParam("shipmentDirection", ShipmentDirection.OUTCOMING.name())
                    .queryParam("from", java.time.LocalDateTime.now().minusHours(1))
                    .queryParam("to", java.time.LocalDateTime.now().plusHours(1))
                    .queryParam("pageSize", 50)
                    .queryParam("page", 1)
                    .build(true)
                    .toUriString();

            ResponseEntity<List<Shipment>> responseEntity = restClient.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(ownerAuthenticationHeaders),
                    new ParameterizedTypeReference<>() {
                    }
            );

            assertThat(responseEntity.getStatusCode())
                    .isEqualTo(HttpStatus.OK);
            assertThat(responseEntity.getBody())
                    .isNotNull();
            assertThat(responseEntity.getBody())
                    .extracting(Shipment::getId)
                    .contains(matched.getId());
            assertThat(responseEntity.getBody())
                    .allSatisfy(s -> assertThat(s.getShipmentDirection())
                            .isEqualTo(ShipmentDirection.OUTCOMING));
            assertThat(responseEntity.getBody())
                    .allSatisfy(s -> assertThat(s.getStatus())
                            .isEqualTo(ShipmentStatus.INITIATED));
            assertThat(responseEntity.getBody())
                    .allSatisfy(s -> assertThat(s.getWarehouseIdRecipient())
                            .isEqualTo(recipientWarehouse.getId()));
        }

        @Test
        @DisplayName("findBy_success_statusOnly")
        void findBy_success_statusOnly() {
            Shipment planned = postShipment(outgoingWithAddress(), ownerAuthenticationHeaders);
            postShipment(ShipmentDTO.builder()
                    .senderCode(senderWarehouse.getCode())
                    .address(randomAddress())
                    .stockItemId(stockItem.getId())
                    .stockItemQuantity(1L)
                    .shipmentDirection(ShipmentDirection.OUTCOMING.name())
                    .status(ShipmentStatus.SENT.name())
                    .build(), ownerAuthenticationHeaders);

            String url = UriComponentsBuilder.fromPath("/api/v1/shipments/findBy")
                    .queryParam("status", ShipmentStatus.PLANNED.name())
                    .queryParam("pageSize", 50)
                    .queryParam("page", 1)
                    .build(true)
                    .toUriString();

            ResponseEntity<List<Shipment>> responseEntity = restClient.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(ownerAuthenticationHeaders),
                    new ParameterizedTypeReference<>() {
                    }
            );

            assertThat(responseEntity.getStatusCode())
                    .isEqualTo(HttpStatus.OK);
            assertThat(responseEntity.getBody())
                    .isNotNull();
            assertThat(responseEntity.getBody())
                    .extracting(Shipment::getStatus)
                    .containsOnly(ShipmentStatus.PLANNED);
            assertThat(responseEntity.getBody())
                    .extracting(Shipment::getId)
                    .contains(planned.getId());
        }

        @Test
        @DisplayName("findBy_success_directionOnly")
        void findBy_success_directionOnly() {
            Shipment outgoing = postShipment(outgoingWithRecipient(), ownerAuthenticationHeaders);
            Warehouse incomingRecipient = warehouseRepository.save(generateWarehouse());
            postShipment(ShipmentDTO.builder()
                    .recipientCode(incomingRecipient.getCode())
                    .stockItemId(stockItem.getId())
                    .stockItemQuantity(1L)
                    .shipmentDirection(ShipmentDirection.INCOMING.name())
                    .build(), ownerAuthenticationHeaders);

            String url = UriComponentsBuilder.fromPath("/api/v1/shipments/findBy")
                    .queryParam("shipmentDirection", ShipmentDirection.OUTCOMING.name())
                    .queryParam("pageSize", 50)
                    .queryParam("page", 1)
                    .build(true)
                    .toUriString();

            ResponseEntity<List<Shipment>> responseEntity = restClient.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(ownerAuthenticationHeaders),
                    new ParameterizedTypeReference<>() {
                    }
            );

            assertThat(responseEntity.getStatusCode())
                    .isEqualTo(HttpStatus.OK);
            assertThat(responseEntity.getBody())
                    .isNotNull();
            assertThat(responseEntity.getBody())
                    .extracting(Shipment::getShipmentDirection)
                    .containsOnly(ShipmentDirection.OUTCOMING);
            assertThat(responseEntity.getBody())
                    .extracting(Shipment::getId)
                    .contains(outgoing.getId());
        }

        @Test
        @DisplayName("findBy_success_dateRangeInclusive")
        void findBy_success_dateRangeInclusive() {
            Shipment createdShipment = postShipment(outgoingWithAddress(), ownerAuthenticationHeaders);

            String insideUrl = UriComponentsBuilder.fromPath("/api/v1/shipments/findBy")
                    .queryParam("from", java.time.LocalDateTime.now().minusMinutes(5))
                    .queryParam("to", java.time.LocalDateTime.now().plusMinutes(5))
                    .queryParam("pageSize", 50)
                    .queryParam("page", 1)
                    .build(true)
                    .toUriString();

            ResponseEntity<List<Shipment>> insideResponse = restClient.exchange(
                    insideUrl,
                    HttpMethod.GET,
                    new HttpEntity<>(ownerAuthenticationHeaders),
                    new ParameterizedTypeReference<>() {
                    }
            );

            assertThat(insideResponse.getStatusCode())
                    .isEqualTo(HttpStatus.OK);
            assertThat(insideResponse.getBody())
                    .isNotNull();
            assertThat(insideResponse.getBody())
                    .extracting(Shipment::getId)
                    .contains(createdShipment.getId());

            String futureUrl = UriComponentsBuilder.fromPath("/api/v1/shipments/findBy")
                    .queryParam("from", java.time.LocalDateTime.now().plusDays(1))
                    .queryParam("to", java.time.LocalDateTime.now().plusDays(2))
                    .queryParam("pageSize", 50)
                    .queryParam("page", 1)
                    .build(true)
                    .toUriString();

            ResponseEntity<List<Shipment>> futureResponse = restClient.exchange(
                    futureUrl,
                    HttpMethod.GET,
                    new HttpEntity<>(ownerAuthenticationHeaders),
                    new ParameterizedTypeReference<>() {
                    }
            );

            assertThat(futureResponse.getStatusCode())
                    .isEqualTo(HttpStatus.OK);
            assertThat(futureResponse.getBody())
                    .isNotNull();
            assertThat(futureResponse.getBody())
                    .isEmpty();
        }

        @Test
        @DisplayName("findBy_success_pagination")
        void findBy_success_pagination() {
            for (int i = 0; i < 5; i++) {
                postShipment(outgoingWithAddress(), ownerAuthenticationHeaders);
            }

            String page1Url = UriComponentsBuilder.fromPath("/api/v1/shipments/findBy")
                    .queryParam("pageSize", 2)
                    .queryParam("page", 1)
                    .build(true)
                    .toUriString();

            String page2Url = UriComponentsBuilder.fromPath("/api/v1/shipments/findBy")
                    .queryParam("pageSize", 2)
                    .queryParam("page", 2)
                    .build(true)
                    .toUriString();

            ResponseEntity<List<Shipment>> page1 = restClient.exchange(
                    page1Url,
                    HttpMethod.GET,
                    new HttpEntity<>(ownerAuthenticationHeaders),
                    new ParameterizedTypeReference<>() {
                    }
            );

            ResponseEntity<List<Shipment>> page2 = restClient.exchange(
                    page2Url,
                    HttpMethod.GET,
                    new HttpEntity<>(ownerAuthenticationHeaders),
                    new ParameterizedTypeReference<>() {
                    }
            );

            assertThat(page1.getStatusCode())
                    .isEqualTo(HttpStatus.OK);
            assertThat(page2.getStatusCode())
                    .isEqualTo(HttpStatus.OK);
            assertThat(page1.getBody())
                    .isNotNull()
                    .hasSizeBetween(0, 2);
            assertThat(page2.getBody())
                    .isNotNull()
                    .hasSizeBetween(0, 2);
        }

        @Test
        @DisplayName("findBy_fail_invalidPagination_returns4xx")
        void findBy_fail_invalidPagination_returns4xx() {
            String url = UriComponentsBuilder.fromPath("/api/v1/shipments/findBy")
                    .queryParam("pageSize", 0)
                    .queryParam("page", 0)
                    .build(true)
                    .toUriString();

            ResponseEntity<String> responseEntity = restClient.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(ownerAuthenticationHeaders),
                    String.class
            );

            assertThat(responseEntity.getStatusCode().is4xxClientError())
                    .isTrue();
        }

        @Test
        @DisplayName("findBy_fail_invalidStatus_returns4xx")
        void findBy_fail_invalidStatus_returns4xx() {
            String url = UriComponentsBuilder.fromPath("/api/v1/shipments/findBy")
                    .queryParam("status", "NOT_A_STATUS")
                    .queryParam("pageSize", 10)
                    .queryParam("page", 1)
                    .build(true)
                    .toUriString();

            ResponseEntity<String> responseEntity = restClient.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(ownerAuthenticationHeaders),
                    String.class
            );

            assertThat(responseEntity.getStatusCode().is4xxClientError())
                    .isTrue();
        }

        @Test
        @DisplayName("findBy_fail_invalidDirection_returns4xx")
        void findBy_fail_invalidDirection_returns4xx() {
            String url = UriComponentsBuilder.fromPath("/api/v1/shipments/findBy")
                    .queryParam("shipmentDirection", "NOT_A_DIRECTION")
                    .queryParam("pageSize", 10)
                    .queryParam("page", 1)
                    .build(true)
                    .toUriString();

            ResponseEntity<String> responseEntity = restClient.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(ownerAuthenticationHeaders),
                    String.class
            );

            assertThat(responseEntity.getStatusCode().is4xxClientError())
                    .isTrue();
        }
    }

    @Nested
    @DisplayName("POST /api/v1/shipments")
    class SaveTests {
        @Test
        @DisplayName("save_success_outgoing_withRecipient")
        void save_success_outgoing_withRecipient() {
            ShipmentDTO shipmentDTO = outgoingWithRecipient();

            ResponseEntity<Shipment> responseEntity = restClient.exchange(
                    "/api/v1/shipments",
                    HttpMethod.POST,
                    new HttpEntity<>(shipmentDTO, allHeaders(ownerAuthenticationHeaders)),
                    Shipment.class
            );

            assertThat(responseEntity.getStatusCode())
                    .isEqualTo(HttpStatus.OK);
            assertThat(responseEntity.getBody())
                    .isNotNull();
            assertThat(responseEntity.getBody().getShipmentDirection())
                    .isEqualTo(ShipmentDirection.OUTCOMING);
            assertThat(responseEntity.getBody().getWarehouseIdSender())
                    .isEqualTo(senderWarehouse.getId());
            assertThat(responseEntity.getBody().getWarehouseIdRecipient())
                    .isEqualTo(recipientWarehouse.getId());
            assertThat(responseEntity.getBody().getStatus())
                    .isEqualTo(ShipmentStatus.PLANNED);
        }

        @Test
        @DisplayName("save_success_outgoing_withAddressOnly_defaultsPlanned")
        void save_success_outgoing_withAddressOnly_defaultsPlanned() {
            ShipmentDTO shipmentDTO = outgoingWithAddress();

            ResponseEntity<Shipment> responseEntity = restClient.exchange(
                    "/api/v1/shipments",
                    HttpMethod.POST,
                    new HttpEntity<>(shipmentDTO, allHeaders(managerAuthenticationHeaders)),
                    Shipment.class
            );

            assertThat(responseEntity.getStatusCode())
                    .isEqualTo(HttpStatus.OK);
            assertThat(responseEntity.getBody())
                    .isNotNull();
            assertThat(responseEntity.getBody().getWarehouseIdSender())
                    .isEqualTo(senderWarehouse.getId());
            assertThat(responseEntity.getBody().getWarehouseIdRecipient())
                    .isNull();
            assertThat(responseEntity.getBody().getStatus())
                    .isEqualTo(ShipmentStatus.PLANNED);
        }

        @Test
        @DisplayName("save_fail_outgoing_missingSenderOrDestination_returns4xx")
        void save_fail_outgoing_missingSenderOrDestination_returns4xx() {
            ShipmentDTO shipmentDTO = ShipmentDTO.builder()
                    .stockItemId(stockItem.getId())
                    .stockItemQuantity(1L)
                    .shipmentDirection(ShipmentDirection.OUTCOMING.name())
                    .build();

            ResponseEntity<String> responseEntity = restClient.exchange(
                    "/api/v1/shipments",
                    HttpMethod.POST,
                    new HttpEntity<>(shipmentDTO, allHeaders(ownerAuthenticationHeaders)),
                    String.class
            );

            assertThat(responseEntity.getStatusCode().is4xxClientError())
                    .isTrue();
        }

        @Test
        @DisplayName("save_fail_bothRecipientAndAddress_returns4xx")
        void save_fail_bothRecipientAndAddress_returns4xx() {
            ShipmentDTO shipmentDTO = ShipmentDTO.builder()
                    .senderCode(senderWarehouse.getCode())
                    .recipientCode(recipientWarehouse.getCode())
                    .address(randomAddress())
                    .stockItemId(stockItem.getId())
                    .stockItemQuantity(2L)
                    .shipmentDirection(ShipmentDirection.OUTCOMING.name())
                    .build();

            ResponseEntity<String> responseEntity = restClient.exchange(
                    "/api/v1/shipments",
                    HttpMethod.POST,
                    new HttpEntity<>(shipmentDTO, allHeaders(ownerAuthenticationHeaders)),
                    String.class
            );

            assertThat(responseEntity.getStatusCode().is4xxClientError())
                    .isTrue();
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/shipments")
    class UpdateTests {
        @Test
        @DisplayName("update_success_quantityAndStatus")
        void update_success_quantityAndStatus() {
            Shipment createdShipment = postShipment(outgoingWithRecipient(), ownerAuthenticationHeaders);

            ShipmentDTO updateShipmentDTO = ShipmentDTO.builder()
                    .id(createdShipment.getId())
                    .stockItemQuantity(10L)
                    .status(ShipmentStatus.SENT.name())
                    .build();

            ResponseEntity<Shipment> responseEntity = restClient.exchange(
                    "/api/v1/shipments",
                    HttpMethod.PUT,
                    new HttpEntity<>(updateShipmentDTO, allHeaders(ownerAuthenticationHeaders)),
                    Shipment.class
            );

            assertThat(responseEntity.getStatusCode())
                    .isEqualTo(HttpStatus.OK);
            assertThat(responseEntity.getBody())
                    .isNotNull();
            assertThat(responseEntity.getBody().getId())
                    .isEqualTo(createdShipment.getId());
            assertThat(responseEntity.getBody().getStockItemQuantity())
                    .isEqualTo(10L);
            assertThat(responseEntity.getBody().getStatus())
                    .isEqualTo(ShipmentStatus.SENT);
        }

        @Test
        @DisplayName("update_fail_sameSenderAndRecipient_returns4xx")
        void update_fail_sameSenderAndRecipient_returns4xx() {
            Shipment createdShipment = postShipment(outgoingWithRecipient(), ownerAuthenticationHeaders);

            ShipmentDTO updateShipmentDTO = ShipmentDTO.builder()
                    .id(createdShipment.getId())
                    .senderCode(senderWarehouse.getCode())
                    .recipientCode(senderWarehouse.getCode())
                    .build();

            ResponseEntity<String> responseEntity = restClient.exchange(
                    "/api/v1/shipments",
                    HttpMethod.PUT,
                    new HttpEntity<>(updateShipmentDTO, allHeaders(ownerAuthenticationHeaders)),
                    String.class
            );

            assertThat(responseEntity.getStatusCode().is4xxClientError())
                    .isTrue();
        }

        @Test
        @DisplayName("update_fail_invalidStatus_returns4xx")
        void update_fail_invalidStatus_returns4xx() {
            Shipment createdShipment = postShipment(outgoingWithAddress(), ownerAuthenticationHeaders);

            ShipmentDTO updateShipmentDTO = ShipmentDTO.builder()
                    .id(createdShipment.getId())
                    .status("WRONG")
                    .build();

            ResponseEntity<String> responseEntity = restClient.exchange(
                    "/api/v1/shipments",
                    HttpMethod.PUT,
                    new HttpEntity<>(updateShipmentDTO, allHeaders(ownerAuthenticationHeaders)),
                    String.class
            );

            assertThat(responseEntity.getStatusCode().is4xxClientError())
                    .isTrue();
        }

        @Test
        @DisplayName("update_fail_missingId_returns4xx")
        void update_fail_missingId_returns4xx() {
            ShipmentDTO updateShipmentDTO = ShipmentDTO.builder()
                    .status(ShipmentStatus.SENT.name())
                    .build();

            ResponseEntity<String> responseEntity = restClient.exchange(
                    "/api/v1/shipments",
                    HttpMethod.PUT,
                    new HttpEntity<>(updateShipmentDTO, allHeaders(ownerAuthenticationHeaders)),
                    String.class
            );

            assertThat(responseEntity.getStatusCode().is4xxClientError())
                    .isTrue();
        }
    }
}
