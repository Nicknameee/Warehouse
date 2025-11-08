package io.store.ua;

import io.store.ua.entity.*;
import io.store.ua.enums.Role;
import io.store.ua.enums.Status;
import io.store.ua.enums.StockItemStatus;
import io.store.ua.models.data.Address;
import io.store.ua.models.data.WorkingHours;
import io.store.ua.models.dto.WarehouseDTO;
import io.store.ua.repository.*;
import io.store.ua.repository.cache.BlacklistedTokenRepository;
import io.store.ua.repository.cache.CurrencyRateRepository;
import io.store.ua.service.external.CloudinaryAPIService;
import io.store.ua.service.external.OpenExchangeRateAPIService;
import io.store.ua.utility.CodeGenerator;
import jakarta.persistence.EntityManager;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.TestExecutionEvent;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

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
    protected PasswordEncoder passwordEncoder;
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
    protected StockItemRepository stockItemRepository;
    @Autowired
    protected StockItemHistoryRepository stockItemHistoryRepository;
    @Autowired
    protected StorageSectionRepository storageSectionRepository;
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

    protected RegularUser user;

    @BeforeAll
    void setUp() {
        postgres.start();
        redis.start();
    }

    @BeforeEach
    void clearTables() {
        shipmentRepository.flush();
        productPhotoRepository.flush();
        stockItemHistoryRepository.flush();
        stockItemRepository.flush();
        storageSectionRepository.flush();
        transactionRepository.flush();
        beneficiaryRepository.flush();
        warehouseRepository.flush();
        productRepository.flush();
        tagRepository.flush();
        userRepository.flush();
        stockItemGroupRepository.flush();

        jdbcTemplate.execute("DELETE FROM product_tags");

        shipmentRepository.deleteAll();
        productPhotoRepository.deleteAll();
        stockItemHistoryRepository.deleteAll();
        stockItemRepository.deleteAll();
        storageSectionRepository.deleteAll();
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
            user = userRepository.save(RegularUser.builder()
                    .username(OWNER)
                    .password(passwordEncoder.encode(RandomStringUtils.secure().nextAlphanumeric(64)))
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

    protected StockItem generateStockItem(Long productId, Long stockItemGroupId, Long warehouseId) {
        return stockItemRepository.save(StockItem.builder()
                .productId(productId)
                .stockItemGroupId(stockItemGroupId)
                .warehouseId(warehouseId)
                .expiryDate(LocalDate.now().plusDays(RandomUtils.secure().randomInt(5, 50)))
                .availableQuantity(BigInteger.valueOf(RandomUtils.secure().randomInt(1, 500)))
                .status(StockItemStatus.AVAILABLE)
                .isActive(true)
                .build());
    }

    protected WarehouseDTO buildWarehouseDTO() {
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

    protected Warehouse generateWarehouse() {
        WarehouseDTO warehouseDTO = buildWarehouseDTO();
        String code = CodeGenerator.WarehouseCodeGenerator.generate(warehouseDTO);

        return warehouseRepository.save(Warehouse.builder()
                .code(code)
                .name(warehouseDTO.getName())
                .address(warehouseDTO.getAddress())
                .workingHours(warehouseDTO.getWorkingHours())
                .phones(warehouseDTO.getPhones())
                .managerId(user.getId())
                .isActive(Boolean.TRUE.equals(warehouseDTO.getIsActive()))
                .build());
    }

    protected Product generateProduct() {
        return productRepository.save(Product.builder()
                .code(RandomStringUtils.secure().nextAlphanumeric(24))
                .title(RandomStringUtils.secure().nextAlphabetic(10))
                .description(RandomStringUtils.secure().nextAlphanumeric(40))
                .price(BigInteger.valueOf(RandomUtils.secure().randomInt(10, 500)))
                .weight(BigInteger.valueOf(RandomUtils.secure().randomInt(1, 100)))
                .length(BigInteger.valueOf(RandomUtils.secure().randomInt(1, 100)))
                .width(BigInteger.valueOf(RandomUtils.secure().randomInt(1, 100)))
                .height(BigInteger.valueOf(RandomUtils.secure().randomInt(1, 100)))
                .build());

    }

    protected StockItemGroup generateStockItemGroup(boolean isActive) {
        return stockItemGroupRepository.save(StockItemGroup.builder()
                .code(RandomStringUtils.secure().nextAlphanumeric(16))
                .name(RandomStringUtils.secure().nextAlphabetic(12))
                .isActive(isActive)
                .build());
    }

    protected StorageSection generateStorageSection(long warehouseId) {
        return storageSectionRepository.save(StorageSection.builder()
                .warehouseId(warehouseId)
                .code(RandomStringUtils.secure().nextAlphanumeric(8))
                .build());
    }
}

