package io.store.ua;

import io.store.ua.entity.RegularUser;
import io.store.ua.enums.Role;
import io.store.ua.enums.Status;
import io.store.ua.repository.*;
import io.store.ua.repository.cache.BlacklistedTokenRepository;
import io.store.ua.repository.cache.CurrencyRateRepository;
import io.store.ua.service.external.CloudinaryAPIService;
import io.store.ua.service.external.OpenExchangeRateAPIService;
import jakarta.persistence.EntityManager;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.security.test.context.support.TestExecutionEvent;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(classes = io.store.ua.WarehouseStarter.class)
@ActiveProfiles({"actuator", "database", "users", "redis", "external", "default"})
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EnableRetry
@WithUserDetails(value = AbstractIT.OWNER, userDetailsServiceBeanName = "regularUserDetailsService", setupBefore = TestExecutionEvent.TEST_EXECUTION)
public abstract class AbstractIT {
    public static final String OWNER = "owner";

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
    protected EntityManager entityManager;
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
    @Autowired
    protected BeneficiaryRepository beneficiaryRepository;
    @MockitoBean
    protected OpenExchangeRateAPIService openExchangeRateAPIService;
    @MockitoBean
    protected CloudinaryAPIService cloudinaryAPIService;

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
        beneficiaryRepository.flush();
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
        beneficiaryRepository.deleteAll();
        warehouseRepository.deleteAll();
        productRepository.deleteAll();
        tagRepository.deleteAll();
        userRepository.deleteAll();
        stockItemGroupRepository.deleteAll();
        blacklistedTokenRepository.deleteAll();
        currencyRateRepository.deleteAll();

        if (!userRepository.existsByUsername(OWNER)) {
            userRepository.save(RegularUser.builder()
                    .username(OWNER)
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
        }
    }
}

