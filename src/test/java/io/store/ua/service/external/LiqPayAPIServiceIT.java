package io.store.ua.service.external;

import io.store.ua.AbstractIT;
import io.store.ua.exceptions.HealthCheckException;
import io.store.ua.models.api.external.request.LPCheckoutRequest;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("external")
@TestPropertySource(properties = {
        "transaction.outcoming.provider=LiqPay",
        "transaction.outcoming.publicKey=${LIQPAY_PUBLIC_KEY}",
        "transaction.outcoming.privateKey=${LIQPAY_PRIVATE_KEY}",
        "transaction.outcoming.url=https://www.liqpay.ua/api/request"
})
@Disabled("IT for real LiqPay API invocation for testing service setup flow")
class LiqPayAPIServiceIT extends AbstractIT {
    @Autowired
    private LiqPayAPIService liqPayAPIService;

    @Test
    void initiatePayment_success() {
        liqPayAPIService.setHealth(true);

        Assertions.assertNotNull(liqPayAPIService.initiatePayment(
                LPCheckoutRequest.builder()
                        .orderId(String.valueOf(System.currentTimeMillis()))
                        .description(RandomStringUtils.secure().nextAlphanumeric(300))
                        .currency("UAH")
                        .amount(String.valueOf(RandomUtils.secure().randomInt(1, 1_000)))
                        .build()
        ));
    }

    @Test
    void initiatePayment_failsWithFalseHealth() {
        liqPayAPIService.setHealth(false);
        assertThatThrownBy(() -> liqPayAPIService.initiatePayment(new LPCheckoutRequest()))
                .isInstanceOf(HealthCheckException.class);
    }

    @Test
    void checkPaymentStatus_success() {
        liqPayAPIService.setHealth(true);

        Assertions.assertNotNull(liqPayAPIService.checkPaymentStatus("TEST-1761163416429"));
    }

    @Test
    void checkPaymentStatus_failsWithFalseHealth() {
        liqPayAPIService.setHealth(false);
        assertThatThrownBy(() -> liqPayAPIService.checkPaymentStatus(RandomStringUtils.secure().nextAlphanumeric(100)))
                .isInstanceOf(HealthCheckException.class);
    }
}
