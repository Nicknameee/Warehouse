package io.store.ua.service.external;

import io.store.ua.configuration.ApplicationExecutorConfiguration;
import io.store.ua.entity.cache.CurrencyRate;
import io.store.ua.utility.HttpRequestService;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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
        "exchange.url=${EXCHANGE_URL:https://openexchangerates.org/api/latest.json}",
        "exchange.appId=${EXCHANGE_APP_ID}"
})
@Disabled("IT for real OpenExchangeRates API invocation for testing service setup flow")
class OpenExchangeRateAPIServiceIT {
    @Autowired
    private OpenExchangeRateAPIService service;

    @Test
    void refreshCurrencyRates_success() {
        List<CurrencyRate> rates = service.refreshCurrencyRates();

        assertFalse(rates.isEmpty());

        assertTrue(rates.stream().allMatch(r ->
                r.getCurrencyCode() != null &&
                        !r.getCurrencyCode().isBlank() &&
                        r.getBaseCurrencyCode() != null &&
                        !r.getBaseCurrencyCode().isBlank() &&
                        r.getRate() != null &&
                        r.getRate().signum() > 0
        ));
    }
}
