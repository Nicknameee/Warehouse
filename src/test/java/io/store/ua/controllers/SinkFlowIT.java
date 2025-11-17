package io.store.ua.controllers;

import io.store.ua.AbstractIT;
import io.store.ua.entity.*;
import io.store.ua.enums.*;
import io.store.ua.models.dto.QueueResponseDTO;
import io.store.ua.models.dto.ShipmentDTO;
import io.store.ua.models.dto.TransactionDTO;
import io.store.ua.producers.SinkProducer;
import io.store.ua.service.ShipmentService;
import io.store.ua.utility.CodeGenerator;
import org.apache.commons.lang3.RandomUtils;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.math.BigInteger;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles({"actuator", "database", "external", "kafka", "redis", "default"})
class SinkFlowIT extends AbstractIT {
    @ServiceConnection
    static final KafkaContainer kafka =
            new KafkaContainer(DockerImageName.parse("apache/kafka-native:3.8.0"))
                    .withExposedPorts(9092)
                    .withStartupTimeout(Duration.ofMinutes(1));
    @Autowired
    private ShipmentService shipmentService;
    private Warehouse senderWarehouse;
    private Warehouse recipientWarehouse;
    private StockItem senderStockItem;


    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        kafka.start();
        Awaitility.await()
                .atMost(Duration.ofSeconds(15))
                .until(kafka::isRunning);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @BeforeEach
    void setUp() {
        StockItemGroup group = stockItemGroupRepository.save(StockItemGroup.builder()
                .code(GENERATOR.nextAlphanumeric(12))
                .name(GENERATOR.nextAlphabetic(10))
                .isActive(true)
                .build());

        Product product = productRepository.save(Product.builder()
                .code(GENERATOR.nextAlphanumeric(24))
                .title(GENERATOR.nextAlphabetic(12))
                .description(GENERATOR.nextAlphanumeric(40))
                .price(BigInteger.valueOf(RandomUtils.secure().randomLong(500, 50_000)))
                .currency(Currency.EUR.name())
                .weight(BigInteger.valueOf(RandomUtils.secure().randomLong(100, 5_000)))
                .length(BigInteger.valueOf(RandomUtils.secure().randomLong(5, 100)))
                .width(BigInteger.valueOf(RandomUtils.secure().randomLong(5, 100)))
                .height(BigInteger.valueOf(RandomUtils.secure().randomLong(5, 100)))
                .build());

        senderWarehouse = warehouseRepository.save(generateWarehouse());
        recipientWarehouse = warehouseRepository.save(generateWarehouse());
        senderStockItem = stockItemRepository.save(StockItem.builder()
                .code(CodeGenerator.StockCodeGenerator.generate())
                .batchVersion(stockItemRepository.countStockItemByProductIdAndWarehouseId(product.getId(), senderWarehouse.getId()) + 1)
                .productId(product.getId())
                .stockItemGroupId(group.getId())
                .warehouseId(senderWarehouse.getId())
                .expiryDate(LocalDate.now().plusDays(RandomUtils.secure().randomInt(30, 365)))
                .availableQuantity(BigInteger.valueOf(RandomUtils.secure().randomInt(30, 200)))
                .status(StockItemStatus.AVAILABLE)
                .isActive(true)
                .build());
    }

    private HttpHeaders generateHeaders() {
        HttpHeaders httpHeaders = generateAuthenticationHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);

        return httpHeaders;
    }

    @Test
    @DisplayName("enqueueShipment_success: POST /sink/shipments → Kafka → @KafkaListener → PLANNED → SENT")
    void enqueueShipment_success() {
        Shipment shipment = shipmentService.save(ShipmentDTO.builder()
                .senderCode(senderWarehouse.getCode())
                .recipientCode(recipientWarehouse.getCode())
                .stockItemId(senderStockItem.getId())
                .stockItemQuantity(3L)
                .shipmentDirection(ShipmentDirection.OUTCOMING.name())
                .build());

        assertThat(shipment.getStatus())
                .isEqualTo(ShipmentStatus.PLANNED);

        ShipmentDTO shipmentDTO = ShipmentDTO.builder()
                .id(shipment.getId())
                .status(ShipmentStatus.SENT.name())
                .build();

        ResponseEntity<QueueResponseDTO> response = restClient.exchange("/api/v1/sink/shipments",
                HttpMethod.POST,
                new HttpEntity<>(shipmentDTO, generateHeaders()),
                QueueResponseDTO.class);

        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.ACCEPTED);
        assertThat(response.getBody())
                .isNotNull();
        assertThat(response.getBody().getKey())
                .isNotBlank();
        assertThat(response.getBody().getTopic())
                .isEqualTo(SinkProducer.SHIPMENT_TOPIC);
        assertThat(response.getBody().isEnqueued())
                .isTrue();

        Awaitility.await()
                .atMost(Duration.ofSeconds(30))
                .pollInterval(Duration.ofMillis(500))
                .untilAsserted(() -> {
                    Shipment refreshed = shipmentRepository.findById(shipment.getId()).orElseThrow();

                    assertThat(refreshed.getStatus())
                            .isEqualTo(ShipmentStatus.SENT);
                });
    }

    @Test
    @DisplayName("enqueueShipment_fails: validation POST without id → 4xx")
    void enqueueShipment_fails() {
        ShipmentDTO shipmentDTO = ShipmentDTO.builder()
                .status(ShipmentStatus.SENT.name())
                .build();

        ResponseEntity<String> response = restClient.exchange("/api/v1/sink/shipments",
                HttpMethod.POST,
                new HttpEntity<>(shipmentDTO, generateHeaders()),
                String.class);

        assertThat(response.getStatusCode().is4xxClientError())
                .isTrue();
    }

    @Test
    @DisplayName("enqueueTransactions_success: CREDIT (CASH) message hits TransactionService.synchroniseTransaction")
    void enqueueTransactions_success() {
        TransactionDTO transactionDTO = TransactionDTO.builder()
                .purpose(TransactionPurpose.STOCK_OUTBOUND_REVENUE.name())
                .flow(TransactionFlowType.CREDIT.name())
                .amount(BigInteger.valueOf(100_000))
                .currency(Currency.UAH.name())
                .receiverFinancialAccountId(generateBeneficiary().getId())
                .paymentProvider(PaymentProvider.CASH.name())
                .build();

        ResponseEntity<QueueResponseDTO> response = restClient.exchange("/api/v1/sink/transactions",
                HttpMethod.POST,
                new HttpEntity<>(transactionDTO, generateHeaders()),
                QueueResponseDTO.class);

        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.ACCEPTED);
        assertThat(response.getBody())
                .isNotNull();
        assertThat(response.getBody().getKey())
                .isNotBlank();
        assertThat(response.getBody().getTopic())
                .isEqualTo(SinkProducer.TRANSACTION_TOPIC);
        assertThat(response.getBody().isEnqueued())
                .isTrue();

        Awaitility.await()
                .atMost(Duration.ofSeconds(30))
                .pollInterval(Duration.ofMillis(500))
                .untilAsserted(() -> {
                    List<Transaction> transactions = transactionRepository.findAll();

                    assertThat(transactions).isNotEmpty();
                    assertThat(transactions.getFirst())
                            .isNotNull();
                    assertThat(transactions.getFirst().getPaymentProvider())
                            .isEqualTo(PaymentProvider.CASH);
                });
    }

    @Test
    @DisplayName("enqueueTransactions_fails: non-CASH provider eventually reaches service and fails validation")
    void enqueueTransactions_fails() {
        TransactionDTO transactionDTO = TransactionDTO.builder()
                .purpose(TransactionPurpose.STOCK_OUTBOUND_REVENUE.name())
                .flow(TransactionFlowType.CREDIT.name())
                .amount(BigInteger.valueOf(100_000))
                .currency(Currency.UAH.name())
                .receiverFinancialAccountId(generateBeneficiary().getId())
                .paymentProvider(PaymentProvider.LIQ_PAY.name())
                .build();

        ResponseEntity<String> response = restClient.exchange("/api/v1/sink/transactions",
                HttpMethod.POST,
                new HttpEntity<>(transactionDTO, generateHeaders()),
                String.class);

        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.ACCEPTED);
    }
}
