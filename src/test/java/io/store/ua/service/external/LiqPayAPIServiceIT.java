package io.store.ua.service.external;

import io.store.ua.AbstractIT;
import io.store.ua.entity.Beneficiary;
import io.store.ua.exceptions.HealthCheckException;
import io.store.ua.models.api.external.request.LPInitiatePaymentRequestDTO;
import io.store.ua.models.api.external.response.LPResponse;
import lombok.SneakyThrows;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ActiveProfiles("external")
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

    @Test
    void initiateIncomingPayment_success() {
        liqPayAPIService.setHealth(true);

        assertNotNull(liqPayAPIService.initiateIncomingPayment(
                LPInitiatePaymentRequestDTO.builder()
                        .orderId(String.valueOf(System.currentTimeMillis()))
                        .currency("UAH")
                        .amount(BigDecimal.valueOf(RandomUtils.secure().randomDouble(1, 1_000)))
                        .build()
        ));
    }

    @Test
    void initiateIncomingPayment_failsWithFalseHealth() {
        liqPayAPIService.setHealth(false);
        assertThatThrownBy(() -> liqPayAPIService.initiateIncomingPayment(new LPInitiatePaymentRequestDTO()))
                .isInstanceOf(HealthCheckException.class);
    }

    @Test
    void checkPaymentStatus_success() {
        liqPayAPIService.setHealth(true);

        assertNotNull(liqPayAPIService.checkPaymentStatus("TEST-1761163416429"));
    }

    @Test
    void checkPaymentStatus_failsWithFalseHealth() {
        liqPayAPIService.setHealth(false);
        assertThatThrownBy(() -> liqPayAPIService.checkPaymentStatus(RandomStringUtils.secure().nextAlphanumeric(100)))
                .isInstanceOf(HealthCheckException.class);
    }

    @SneakyThrows
    @Test
    void initiateOutcomingPayment_success() {
        liqPayAPIService.setHealth(true);

        var result = liqPayAPIService.initiateOutcomingPayment(
                LPInitiatePaymentRequestDTO.builder()
                        .orderId(RandomStringUtils.secure().nextAlphanumeric(33))
                        .currency("UAH")
                        .amount(BigDecimal.valueOf(RandomUtils.secure().randomDouble(10, 500)))
                        .beneficiaryCode(beneficiaryRepository.save(Beneficiary.builder()
                                        .code(RandomStringUtils.secure().nextAlphanumeric(33))
                                        .IBAN("UA123456789000000000000000000")
                                        .SWIFT("SWIFT9999")
                                        .name("Test Recipient")
                                        .card("4731195301524633")
                                        .build())
                                .getCode())
                        .build());
        assertNotNull(result);

        if (sandbox) {
            assertEquals(LPResponse.Status.SUCCESS, result.getStatus());
        }
    }

    @Test
    void initiateOutcomingPayment_failsWithFalseHealth() {
        liqPayAPIService.setHealth(false);
        assertThatThrownBy(() -> liqPayAPIService.initiateOutcomingPayment(new LPInitiatePaymentRequestDTO()))
                .isInstanceOf(HealthCheckException.class);
    }
}
