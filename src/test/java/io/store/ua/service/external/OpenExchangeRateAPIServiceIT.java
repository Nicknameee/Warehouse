package io.store.ua.service.external;

import io.store.ua.configuration.ApplicationExecutorConfiguration;
import io.store.ua.entity.User;
import io.store.ua.entity.cache.CurrencyRate;
import io.store.ua.enums.UserRole;
import io.store.ua.enums.UserStatus;
import io.store.ua.utility.HttpRequestService;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = {
        ApplicationExecutorConfiguration.class,
        OpenExchangeRateAPIService.class,
        HttpRequestService.class
})
@ActiveProfiles("external")
@TestPropertySource(properties = {
        "exchange.appId=${EXCHANGE_APP_ID}"
})
@Disabled("IT for real OpenExchangeRates API invocation for testing service setup flow")
class OpenExchangeRateAPIServiceIT {
    @Autowired
    private OpenExchangeRateAPIService service;

    @BeforeEach
    void setUp() {
        var user = User.builder()
                .username(RandomStringUtils.secure().nextAlphanumeric(333))
                .role(UserRole.OWNER)
                .status(UserStatus.ACTIVE)
                .build();

        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void refreshCurrencyRates_success() {
        List<CurrencyRate> rates = service.refreshCurrencyRates();

        assertFalse(rates.isEmpty());

        assertTrue(rates.stream().allMatch(currencyRate -> currencyRate.getCurrencyCode() != null
                && !currencyRate.getCurrencyCode().isBlank()
                && currencyRate.getBaseCurrencyCode() != null
                && !currencyRate.getBaseCurrencyCode().isBlank()
                && currencyRate.getRate() != null
                && currencyRate.getRate().signum() > 0
        ));
    }
}
