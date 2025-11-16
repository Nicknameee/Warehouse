package io.store.ua.client;

import io.store.ua.AbstractIT;
import io.store.ua.entity.cache.CurrencyRate;
import io.store.ua.enums.Currency;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class CurrencyRateControllerIT extends AbstractIT {
    private HttpHeaders ownerAuthenticationHeaders;

    @BeforeAll
    void setupAuthentication() {
        ownerAuthenticationHeaders = generateAuthenticationHeaders();
    }

    private List<CurrencyRate> upsertRates(BigDecimal usdRate, BigDecimal eurRate, BigDecimal chfRate, BigDecimal uahRate) {
        currencyRateRepository.deleteAll();

        return currencyRateRepository.saveAll(List.of(CurrencyRate.builder()
                        .currencyCode(Currency.USD.name())
                        .baseCurrencyCode(Currency.USD.name())
                        .rate(usdRate)
                        .expiryTime(TimeUnit.DAYS.toSeconds(1))
                        .build(),
                CurrencyRate.builder()
                        .currencyCode(Currency.EUR.name())
                        .baseCurrencyCode(Currency.USD.name())
                        .rate(eurRate)
                        .expiryTime(TimeUnit.DAYS.toSeconds(1))
                        .build(),
                CurrencyRate.builder()
                        .currencyCode(Currency.CHF.name())
                        .baseCurrencyCode(Currency.USD.name())
                        .rate(chfRate)
                        .expiryTime(TimeUnit.DAYS.toSeconds(1))
                        .build(),
                CurrencyRate.builder()
                        .currencyCode(Currency.UAH.name())
                        .baseCurrencyCode(Currency.USD.name())
                        .rate(uahRate)
                        .expiryTime(TimeUnit.DAYS.toSeconds(1))
                        .build()
        ));
    }

    @Nested
    @DisplayName("GET /api/v1/currencyRates/findAll")
    class FindAllTests {
        @Test
        @DisplayName("findAll_success_returnsAllRates")
        void findAll_success_returnsAllRates() {
            var currencies = upsertRates(new BigDecimal("1.0000"),
                    new BigDecimal("0.9300"),
                    new BigDecimal("0.9800"),
                    new BigDecimal("41.0000"));

            ResponseEntity<List<CurrencyRate>> responseEntity = restClient.exchange("/api/v1/currencyRates/findAll",
                    HttpMethod.GET,
                    new HttpEntity<>(ownerAuthenticationHeaders),
                    new ParameterizedTypeReference<>() {
                    });

            assertThat(responseEntity.getStatusCode())
                    .isEqualTo(HttpStatus.OK);
            assertThat(responseEntity.getBody())
                    .isNotNull()
                    .hasSize(currencies.size());
            assertThat(responseEntity.getBody())
                    .extracting(CurrencyRate::getCurrencyCode)
                    .containsExactlyInAnyOrder(Arrays.stream(Currency.values()).map(Enum::name).toArray(String[]::new));
        }
    }
}
