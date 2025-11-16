package io.store.ua.controllers;

import io.store.ua.AbstractIT;
import io.store.ua.entity.cache.CurrencyRate;
import io.store.ua.enums.Currency;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class CurrencyRateControllerIT extends AbstractIT {
    private HttpHeaders authenticationHeaders;

    @BeforeAll
    void setup() {
        authenticationHeaders = generateAuthenticationHeaders();
    }

    @Test
    @DisplayName("findAll_success_returnsAllRates")
    void findAll_success_returnsAllRates() {
        currencyRateRepository.deleteAll();
        currencyRateRepository.saveAll(List.of(
                CurrencyRate.builder()
                        .currencyCode(Currency.USD.name())
                        .baseCurrencyCode(Currency.USD.name())
                        .rate(new BigDecimal("1.0000"))
                        .expiryTime(TimeUnit.DAYS.toSeconds(1))
                        .build(),
                CurrencyRate.builder()
                        .currencyCode(Currency.EUR.name())
                        .baseCurrencyCode(Currency.USD.name())
                        .rate(new BigDecimal("0.9300"))
                        .expiryTime(TimeUnit.DAYS.toSeconds(1))
                        .build()
        ));

        ResponseEntity<List<CurrencyRate>> response = restClient.exchange(
                "/api/v1/currencyRates/findAll",
                HttpMethod.GET,
                new HttpEntity<>(authenticationHeaders),
                new ParameterizedTypeReference<>() {
                }
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull().hasSize(2);
        assertThat(response.getBody())
                .extracting(CurrencyRate::getCurrencyCode)
                .containsExactlyInAnyOrder(Currency.USD.name(), Currency.EUR.name());
    }
}
