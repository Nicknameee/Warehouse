package io.store.ua.service.external;

import io.store.ua.AbstractIT;
import io.store.ua.entity.Beneficiary;
import io.store.ua.entity.RegularUser;
import io.store.ua.entity.cache.CurrencyRate;
import io.store.ua.enums.Currency;
import io.store.ua.enums.Role;
import io.store.ua.enums.Status;
import io.store.ua.exceptions.HealthCheckException;
import io.store.ua.models.api.external.request.LPInitiatePaymentRequestDTO;
import io.store.ua.models.api.external.response.LPResponse;
import lombok.SneakyThrows;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@TestPropertySource(properties = {
        "transaction.outcoming.publicKey=${LIQPAY_PUBLIC_KEY}",
        "transaction.outcoming.privateKey=${LIQPAY_PRIVATE_KEY}"
})
@Disabled("IT for real LiqPay API invocation for testing service setup flow")
class LiqPayAPIServiceIT extends AbstractIT {
    @Autowired
    private LiqPayAPIService liqPayAPIService;
    @Value("${transaction.outcoming.sandbox}")
    private boolean sandbox;

    @BeforeEach
    void setup() {
        currencyRateRepository.saveAll(List.of(
                CurrencyRate.builder()
                        .currencyCode(DataTransAPIService.Constants.Currency.USD)
                        .baseCurrencyCode(DataTransAPIService.Constants.Currency.USD)
                        .rate(BigDecimal.ONE)
                        .expiryTime(TimeUnit.DAYS.toSeconds(1))
                        .build(),
                CurrencyRate.builder()
                        .currencyCode(DataTransAPIService.Constants.Currency.CHF)
                        .baseCurrencyCode(DataTransAPIService.Constants.Currency.USD)
                        .rate(BigDecimal.valueOf(1.25))
                        .expiryTime(TimeUnit.DAYS.toSeconds(1))
                        .build(),
                CurrencyRate.builder()
                        .currencyCode(DataTransAPIService.Constants.Currency.EUR)
                        .baseCurrencyCode(DataTransAPIService.Constants.Currency.USD)
                        .rate(BigDecimal.valueOf(0.91))
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
    void initiateIncomingPayment_APICall_success() {
        liqPayAPIService.setHealth(true);

        assertNotNull(liqPayAPIService.initiateIncomingPaymentAPICall(
                LPInitiatePaymentRequestDTO.builder()
                        .orderId(String.valueOf(System.currentTimeMillis()))
                        .currency(Currency.UAH.name())
                        .amount(BigInteger.valueOf(RandomUtils.secure().randomInt(1_000, 1_000_000)))
                        .build()
        ));
    }

    @Test
    void initiateIncomingPayment_APICall_failsWithFalseHealth() {
        liqPayAPIService.setHealth(false);

        assertThatThrownBy(() -> liqPayAPIService.initiateIncomingPaymentAPICall(new LPInitiatePaymentRequestDTO()))
                .isInstanceOf(HealthCheckException.class);
    }

    @Test
    void checkPaymentStatus_APICall_success() {
        liqPayAPIService.setHealth(true);

        assertNotNull(liqPayAPIService.checkPaymentStatusAPICall("TEST-1761163416429"));
    }

    @Test
    void checkPaymentStatus_APICall_failsWithFalseHealth() {
        liqPayAPIService.setHealth(false);
        assertThatThrownBy(() -> liqPayAPIService.checkPaymentStatusAPICall(RandomStringUtils.secure().nextAlphanumeric(100)))
                .isInstanceOf(HealthCheckException.class);
    }

    @SneakyThrows
    @Test
    void initiateOutcomingPayment_APICall_success() {
        liqPayAPIService.setHealth(true);

        var result = liqPayAPIService.initiateOutcomingPaymentAPICall(
                LPInitiatePaymentRequestDTO.builder()
                        .orderId(RandomStringUtils.secure().nextAlphanumeric(33))
                        .currency("UAH")
                        .amount(BigInteger.valueOf(RandomUtils.secure().randomInt(1_000, 500_000_000)))
                        .beneficiaryID(beneficiaryRepository.save(Beneficiary.builder()
                                        .IBAN("UA123456789000000000000000000")
                                        .SWIFT("SWIFT9999")
                                        .name("Test Recipient")
                                        .card("4731195301524633")
                                        .isActive(true)
                                        .build())
                                .getId())
                        .build());
        assertNotNull(result);

        if (sandbox) {
            assertEquals(LPResponse.Status.SUCCESS, result.getStatus());
        }
    }

    @Test
    void initiateOutcomingPayment_APICall_fails_whenFalseHealth() {
        liqPayAPIService.setHealth(false);

        assertThatThrownBy(() -> liqPayAPIService.initiateOutcomingPaymentAPICall(new LPInitiatePaymentRequestDTO()))
                .isInstanceOf(HealthCheckException.class);
    }
}
