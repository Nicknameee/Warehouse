package io.store.ua;

import io.store.ua.repository.*;
import io.store.ua.repository.cache.BlacklistedTokenRepository;
import io.store.ua.repository.cache.CurrencyRateRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(classes = io.store.ua.WarehouseStarter.class)
@ActiveProfiles({"actuator", "database", "users", "redis", "external", "default"})
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EnableRetry
@TestPropertySource(properties = {
        "exchange.url=${EXCHANGE_URL:https://openexchangerates.org/api/latest.json}",
        "exchange.appId=${EXCHANGE_APP_ID:any}",
        "cloudinary.cloud=${CLOUDINARY_CLOUD_NAME:any}",
        "cloudinary.credentials.apiKey=${CLOUDINARY_API_KEY:any}",
        "cloudinary.credentials.apiSecret=${CLOUDINARY_API_SECRET:any}",
        "transaction.merchantId=${DATA_TRANS_MERCHANT_ID:any}",
        "transaction.merchantPassword=${DATA_TRANS_MERCHANT_PASSWORD:any}",
        "transaction.url=${DATA_TRANS_URL:https://api.sandbox.datatrans.com/v1}"
})
public abstract class AbstractIT {
    @ServiceConnection
    protected static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withConnectTimeoutSeconds(1)
                    .withStartupTimeoutSeconds(10);

    @ServiceConnection
    protected static final GenericContainer<?> redis =
            new GenericContainer<>("redis:7-alpine")
                    .withExposedPorts(6379);
    @Autowired
    protected JdbcTemplate jdbcTemplate;
    @Autowired
    protected BlacklistedTokenRepository blacklistedTokenRepository;
    @Autowired
    protected CurrencyRateRepository currencyRateRepository;
    @Autowired
    protected ProductPhotoRepository productPhotoRepository;
    @Autowired
    protected ProductRepository productRepository;
    @Autowired
    protected RegularUserRepository userRepository;
    @Autowired
    protected StockItemGroupRepository stockItemGroupRepository;
    @Autowired
    protected StockItemLogRepository stockItemLogRepository;
    @Autowired
    protected StockItemRepository stockItemRepository;
    @Autowired
    protected TagRepository tagRepository;
    @Autowired
    protected WarehouseRepository warehouseRepository;
    @Autowired
    protected ShipmentRepository shipmentRepository;
    @Autowired
    protected TransactionRepository transactionRepository;

    @BeforeAll
    void setUp() {
        postgres.start();
        redis.start();
    }

    @BeforeEach
    void clearTables() {

        stockItemLogRepository.flush();
        shipmentRepository.flush();
        productPhotoRepository.flush();
        stockItemRepository.flush();
        transactionRepository.flush();
        warehouseRepository.flush();
        productRepository.flush();
        tagRepository.flush();
        userRepository.flush();
        stockItemGroupRepository.flush();

        jdbcTemplate.execute("DELETE FROM product_tags");

        stockItemLogRepository.deleteAll();
        shipmentRepository.deleteAll();
        productPhotoRepository.deleteAll();
        stockItemRepository.deleteAll();
        transactionRepository.deleteAll();
        warehouseRepository.deleteAll();
        productRepository.deleteAll();
        tagRepository.deleteAll();
        userRepository.deleteAll();
        stockItemGroupRepository.deleteAll();
        blacklistedTokenRepository.deleteAll();
        currencyRateRepository.deleteAll();
    }
}

