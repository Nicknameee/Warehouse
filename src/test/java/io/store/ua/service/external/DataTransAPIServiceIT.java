package io.store.ua.service.external;

import io.store.ua.AbstractIT;
import io.store.ua.entity.RegularUser;
import io.store.ua.entity.cache.CurrencyRate;
import io.store.ua.enums.Role;
import io.store.ua.enums.Status;
import io.store.ua.exceptions.HealthCheckException;
import io.store.ua.models.api.data.DataTransTransaction;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("external")
@TestPropertySource(properties = {
        "transaction.incoming.merchantId=${DATA_TRANS_MERCHANT_ID}",
        "transaction.incoming.merchantPassword=${DATA_TRANS_MERCHANT_PASSWORD}"
})
@Disabled("IT for real DataTrans API invocation for testing service setup flow")
class DataTransAPIServiceIT extends AbstractIT {
    @Autowired
    private DataTransAPIService dataTransAPIService;
    @Value("${transaction.incoming.reference.length}")
    private int referenceLength;

    @BeforeEach
    void setup() {
        currencyRateRepository.saveAll(List.of(
                CurrencyRate.builder()
                        .currencyCode(DataTransAPIService.Constants.Currency.USD)
                        .baseCurrencyCode(DataTransAPIService.Constants.Currency.USD)
                        .rate(new BigDecimal("1.00"))
                        .expiryTime(TimeUnit.DAYS.toSeconds(1))
                        .build(),
                CurrencyRate.builder()
                        .currencyCode(DataTransAPIService.Constants.Currency.CHF)
                        .baseCurrencyCode(DataTransAPIService.Constants.Currency.USD)
                        .rate(new BigDecimal("1.25"))
                        .expiryTime(TimeUnit.DAYS.toSeconds(1))
                        .build(),
                CurrencyRate.builder()
                        .currencyCode(DataTransAPIService.Constants.Currency.EUR)
                        .baseCurrencyCode(DataTransAPIService.Constants.Currency.USD)
                        .rate(new BigDecimal("0.91"))
                        .expiryTime(TimeUnit.DAYS.toSeconds(1))
                        .build()
        ));

        var user = RegularUser.builder()
                .username(RandomStringUtils.secure().nextAlphanumeric(333))
                .role(Role.OWNER)
                .status(Status.ACTIVE)
                .build();

        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void initialisePayment_realApi_invokesSuccessfully() {
        dataTransAPIService.setHealth(true);
        BigDecimal amount = BigDecimal.valueOf(1000);

        DataTransTransaction transaction = dataTransAPIService.initialisePayment(DataTransAPIService.Constants.Currency.CHF, amount, false);

        assertNotNull(transaction);
        assertNotNull(transaction.getTransactionId());
        assertFalse(transaction.getTransactionId().isBlank());
        assertNotNull(transaction.getReference());
        assertEquals(referenceLength, transaction.getReference().length());
    }

    @Test
    void initialisePayment_realApi_failsWithFalseHealth() {
        dataTransAPIService.setHealth(false);
        assertThatThrownBy(() -> dataTransAPIService.initialisePayment(DataTransAPIService.Constants.Currency.CHF, BigDecimal.valueOf(1000), false))
                .isInstanceOf(HealthCheckException.class);
    }

    @Test
    void authorizePayment_realApi_invokesSuccessfully() {
        dataTransAPIService.setHealth(true);
        BigDecimal amount = BigDecimal.valueOf(1000);

        DataTransTransaction transaction = dataTransAPIService.authorizePayment(DataTransAPIService.Constants.Currency.CHF, amount);

        assertNotNull(transaction);
        assertNotNull(transaction.getTransactionId());
        assertFalse(transaction.getTransactionId().isBlank());
        assertNotNull(transaction.getReference());
        assertFalse(transaction.getReference().isBlank());
        assertNotNull(transaction.getAcquirerAuthorizationCode());
        assertFalse(transaction.getAcquirerAuthorizationCode().isBlank());
        assertEquals(referenceLength, transaction.getReference().length());
    }

    @Test
    void authorizePayment_realApi_failsWithFalseHealth() {
        dataTransAPIService.setHealth(false);
        assertThatThrownBy(() -> dataTransAPIService.authorizePayment(DataTransAPIService.Constants.Currency.CHF, BigDecimal.valueOf(1000)))
                .isInstanceOf(HealthCheckException.class);
    }

    @Test
    void settlePayment_realApi_invokesSuccessfully() {
        dataTransAPIService.setHealth(true);
        BigDecimal amount = BigDecimal.valueOf(1000);

        DataTransTransaction authorized =
                dataTransAPIService.authorizePayment(DataTransAPIService.Constants.Currency.CHF, amount);

        assertNotNull(authorized);
        assertNotNull(authorized.getTransactionId());
        assertFalse(authorized.getTransactionId().isBlank());
        assertNotNull(authorized.getReference());
        assertFalse(authorized.getReference().isBlank());
        assertNotNull(authorized.getAcquirerAuthorizationCode());
        assertFalse(authorized.getAcquirerAuthorizationCode().isBlank());
        assertEquals(referenceLength, authorized.getReference().length());

        DataTransTransaction settled = dataTransAPIService.settlePayment(
                DataTransAPIService.Constants.Currency.CHF,
                amount,
                authorized.getTransactionId(),
                authorized.getReference()
        );

        assertNotNull(settled);
        assertEquals(authorized.getTransactionId(), settled.getTransactionId());
        assertEquals(authorized.getReference(), settled.getReference());
    }

    @Test
    void settlePayment_realApi_failsWithFalseHealth() {
        dataTransAPIService.setHealth(false);
        assertThatThrownBy(() ->
                dataTransAPIService.settlePayment(
                        DataTransAPIService.Constants.Currency.CHF,
                        BigDecimal.valueOf(1000),
                        "any-tx-id",
                        "any-ref"
                ))
                .isInstanceOf(HealthCheckException.class);
    }
}

