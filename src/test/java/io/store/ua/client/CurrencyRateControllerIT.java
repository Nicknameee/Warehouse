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
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class CurrencyRateControllerIT extends AbstractIT {
    private HttpHeaders ownerAuthenticationHeaders;

    @BeforeAll
    void setupAuthentication() {
        ownerAuthenticationHeaders = generateAuthenticationHeaders();
    }

    private void upsertRates(BigDecimal usdRate, BigDecimal eurRate, BigDecimal chfRate, BigDecimal uahRate) {
        currencyRateRepository.deleteAll();
        currencyRateRepository.saveAll(List.of(
                CurrencyRate.builder()
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

    private BigInteger expectedCents(String base, String target, BigInteger amountCents) {
        BigDecimal baseRate = currencyRateRepository.findById(base).orElseThrow().getRate();
        BigDecimal targetRate = currencyRateRepository.findById(target).orElseThrow().getRate();
        return new BigDecimal(amountCents)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                .multiply(baseRate)
                .divide(targetRate, 2, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .toBigIntegerExact();
    }

    @Nested
    @DisplayName("GET /api/v1/currencyRates/findAll")
    class FindAllTests {
        @Test
        @DisplayName("findAll_success_returnsAllRates")
        void findAll_success_returnsAllRates() {
            upsertRates(new BigDecimal("1.0000"), new BigDecimal("0.9300"), new BigDecimal("0.9800"), new BigDecimal("41.0000"));

            ResponseEntity<List<CurrencyRate>> responseEntity = restClient.exchange("/api/v1/currencyRates/findAll",
                    HttpMethod.GET,
                    new HttpEntity<>(ownerAuthenticationHeaders),
                    new ParameterizedTypeReference<>() {
                    });

            assertThat(responseEntity.getStatusCode())
                    .isEqualTo(HttpStatus.OK);
            assertThat(responseEntity.getBody())
                    .isNotNull()
                    .hasSize(4);
            assertThat(responseEntity.getBody())
                    .extracting(CurrencyRate::getCurrencyCode)
                    .containsExactlyInAnyOrder("USD", "EUR", "CHF", "UAH");
        }
    }

    @Nested
    @DisplayName("GET /api/v1/currencyRates/findBy/currency")
    class FindByCurrencyTests {
        @Test
        @DisplayName("findByCurrency_success_returnsEntity")
        void findByCurrency_success_returnsEntity() {
            upsertRates(new BigDecimal("1.0000"), new BigDecimal("0.9300"), new BigDecimal("0.9800"), new BigDecimal("41.0000"));

            String url = UriComponentsBuilder.fromPath("/api/v1/currencyRates/findBy/currency")
                    .queryParam("currency", "EUR")
                    .build(true)
                    .toUriString();

            ResponseEntity<CurrencyRate> responseEntity = restClient.exchange(url,
                    HttpMethod.GET,
                    new HttpEntity<>(ownerAuthenticationHeaders),
                    CurrencyRate.class);

            assertThat(responseEntity.getStatusCode())
                    .isEqualTo(HttpStatus.OK);
            assertThat(responseEntity.getBody())
                    .isNotNull();
            assertThat(responseEntity.getBody().getCurrencyCode())
                    .isEqualTo("EUR");
        }

        @Test
        @DisplayName("findByCurrency_fail_missing_returns4xx")
        void findByCurrency_fail_missing_returns4xx() {
            currencyRateRepository.deleteAll();

            String url = UriComponentsBuilder.fromPath("/api/v1/currencyRates/findBy/currency")
                    .queryParam("currency", "AAA")
                    .build(true)
                    .toUriString();

            ResponseEntity<String> responseEntity = restClient.exchange(url,
                    HttpMethod.GET,
                    new HttpEntity<>(ownerAuthenticationHeaders),
                    String.class);

            assertThat(responseEntity.getStatusCode().is4xxClientError())
                    .isTrue();
        }
    }

    @Nested
    @DisplayName("GET /api/v1/currencyRates/convert")
    class ConvertEndpointTests {
        @Test
        @DisplayName("convert_success_returnsExpectedCents")
        void convert_success_returnsExpectedCents() {
            upsertRates(new BigDecimal("1.0000"), new BigDecimal("0.9400"), new BigDecimal("0.9600"), new BigDecimal("40.5000"));

            BigInteger amount = new BigInteger("12345");

            String url = UriComponentsBuilder.fromPath("/api/v1/currencyRates/convert")
                    .queryParam("baseCurrency", "USD")
                    .queryParam("targetCurrency", "EUR")
                    .queryParam("amount", amount)
                    .build(true)
                    .toUriString();

            ResponseEntity<BigInteger> responseEntity = restClient.exchange(url,
                    HttpMethod.GET,
                    new HttpEntity<>(ownerAuthenticationHeaders),
                    BigInteger.class);

            BigInteger expected = expectedCents("USD", "EUR", amount);

            assertThat(responseEntity.getStatusCode())
                    .isEqualTo(HttpStatus.OK);
            assertThat(responseEntity.getBody())
                    .isNotNull();
            assertThat(responseEntity.getBody())
                    .isEqualTo(expected);
        }

        @Test
        @DisplayName("convert_success_sameCurrency_returnsSameAmount")
        void convert_success_sameCurrency_returnsSameAmount() {
            upsertRates(new BigDecimal("1.0000"), new BigDecimal("0.9300"), new BigDecimal("0.9800"), new BigDecimal("41.0000"));

            BigInteger amount = new BigInteger("777");

            String url = UriComponentsBuilder.fromPath("/api/v1/currencyRates/convert")
                    .queryParam("baseCurrency", "USD")
                    .queryParam("targetCurrency", "USD")
                    .queryParam("amount", amount)
                    .build(true)
                    .toUriString();

            ResponseEntity<BigInteger> responseEntity = restClient.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(ownerAuthenticationHeaders),
                    BigInteger.class
            );

            assertThat(responseEntity.getStatusCode())
                    .isEqualTo(HttpStatus.OK);
            assertThat(responseEntity.getBody())
                    .isNotNull();
            assertThat(responseEntity.getBody())
                    .isEqualTo(amount);
        }

        @Test
        @DisplayName("convert_fail_invalidArgs_returns4xx")
        void convert_fail_invalidArgs_returns4xx() {
            String urlBlankBase = UriComponentsBuilder.fromPath("/api/v1/currencyRates/convert")
                    .queryParam("baseCurrency", "")
                    .queryParam("targetCurrency", "USD")
                    .queryParam("amount", 1)
                    .build(true)
                    .toUriString();

            ResponseEntity<String> r1 = restClient.exchange(
                    urlBlankBase,
                    HttpMethod.GET,
                    new HttpEntity<>(ownerAuthenticationHeaders),
                    String.class
            );

            String urlBlankTarget = UriComponentsBuilder.fromPath("/api/v1/currencyRates/convert")
                    .queryParam("baseCurrency", "USD")
                    .queryParam("targetCurrency", "")
                    .queryParam("amount", 1)
                    .build(true)
                    .toUriString();

            ResponseEntity<String> r2 = restClient.exchange(
                    urlBlankTarget,
                    HttpMethod.GET,
                    new HttpEntity<>(ownerAuthenticationHeaders),
                    String.class
            );

            String urlZeroAmount = UriComponentsBuilder.fromPath("/api/v1/currencyRates/convert")
                    .queryParam("baseCurrency", "USD")
                    .queryParam("targetCurrency", "EUR")
                    .queryParam("amount", 0)
                    .build(true)
                    .toUriString();

            ResponseEntity<String> r3 = restClient.exchange(
                    urlZeroAmount,
                    HttpMethod.GET,
                    new HttpEntity<>(ownerAuthenticationHeaders),
                    String.class
            );

            assertThat(r1.getStatusCode().is4xxClientError())
                    .isTrue();
            assertThat(r2.getStatusCode().is4xxClientError())
                    .isTrue();
            assertThat(r3.getStatusCode().is4xxClientError())
                    .isTrue();
        }

        @Test
        @DisplayName("convert_fail_unknownCurrency_returns4xx")
        void convert_fail_unknownCurrency_returns4xx() {
            upsertRates(new BigDecimal("1.0000"), new BigDecimal("0.9300"), new BigDecimal("0.9800"), new BigDecimal("41.0000"));

            String urlUnknownBase = UriComponentsBuilder.fromPath("/api/v1/currencyRates/convert")
                    .queryParam("baseCurrency", "ZZZ")
                    .queryParam("targetCurrency", "USD")
                    .queryParam("amount", 100)
                    .build(true)
                    .toUriString();

            ResponseEntity<String> r1 = restClient.exchange(
                    urlUnknownBase,
                    HttpMethod.GET,
                    new HttpEntity<>(ownerAuthenticationHeaders),
                    String.class
            );

            String urlUnknownTarget = UriComponentsBuilder.fromPath("/api/v1/currencyRates/convert")
                    .queryParam("baseCurrency", "USD")
                    .queryParam("targetCurrency", "ZZZ")
                    .queryParam("amount", 100)
                    .build(true)
                    .toUriString();

            ResponseEntity<String> r2 = restClient.exchange(
                    urlUnknownTarget,
                    HttpMethod.GET,
                    new HttpEntity<>(ownerAuthenticationHeaders),
                    String.class
            );

            assertThat(r1.getStatusCode().is4xxClientError())
                    .isTrue();
            assertThat(r2.getStatusCode().is4xxClientError())
                    .isTrue();
        }
    }

    @Nested
    @DisplayName("GET /api/v1/currencyRates/convertFromCentsToCurrencyUnit")
    class ConvertToUnitEndpointTests {
        @Test
        @DisplayName("convertFromCentsToCurrencyUnit_success_matchesConvertMoveLeft")
        void convertFromCentsToCurrencyUnit_success_matchesConvertMoveLeft() {
            upsertRates(new BigDecimal("1.0000"), new BigDecimal("0.9400"), new BigDecimal("0.9600"), new BigDecimal("40.7500"));

            BigInteger amount = new BigInteger("8888");

            String url = UriComponentsBuilder.fromPath("/api/v1/currencyRates/convertFromCentsToCurrencyUnit")
                    .queryParam("baseCurrency", "USD")
                    .queryParam("targetCurrency", "EUR")
                    .queryParam("amount", amount)
                    .build(true)
                    .toUriString();

            ResponseEntity<BigDecimal> responseEntity = restClient.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(ownerAuthenticationHeaders),
                    BigDecimal.class
            );

            BigInteger converted = expectedCents("USD", "EUR", amount);
            BigDecimal expected = new BigDecimal(converted).movePointLeft(2);

            assertThat(responseEntity.getStatusCode())
                    .isEqualTo(HttpStatus.OK);
            assertThat(responseEntity.getBody())
                    .isNotNull();
            assertThat(responseEntity.getBody().compareTo(expected))
                    .isZero();
        }

        @Test
        @DisplayName("convertFromCentsToCurrencyUnit_fail_invalidArgs_returns4xx")
        void convertFromCentsToCurrencyUnit_fail_invalidArgs_returns4xx() {
            String urlBlankBase = UriComponentsBuilder.fromPath("/api/v1/currencyRates/convertFromCentsToCurrencyUnit")
                    .queryParam("baseCurrency", "")
                    .queryParam("targetCurrency", "USD")
                    .queryParam("amount", 1)
                    .build(true)
                    .toUriString();

            ResponseEntity<String> r1 = restClient.exchange(
                    urlBlankBase,
                    HttpMethod.GET,
                    new HttpEntity<>(ownerAuthenticationHeaders),
                    String.class
            );

            String urlBlankTarget = UriComponentsBuilder.fromPath("/api/v1/currencyRates/convertFromCentsToCurrencyUnit")
                    .queryParam("baseCurrency", "USD")
                    .queryParam("targetCurrency", "")
                    .queryParam("amount", 1)
                    .build(true)
                    .toUriString();

            ResponseEntity<String> r2 = restClient.exchange(
                    urlBlankTarget,
                    HttpMethod.GET,
                    new HttpEntity<>(ownerAuthenticationHeaders),
                    String.class
            );

            String urlZeroAmount = UriComponentsBuilder.fromPath("/api/v1/currencyRates/convertFromCentsToCurrencyUnit")
                    .queryParam("baseCurrency", "USD")
                    .queryParam("targetCurrency", "EUR")
                    .queryParam("amount", 0)
                    .build(true)
                    .toUriString();

            ResponseEntity<String> r3 = restClient.exchange(
                    urlZeroAmount,
                    HttpMethod.GET,
                    new HttpEntity<>(ownerAuthenticationHeaders),
                    String.class
            );

            assertThat(r1.getStatusCode().is4xxClientError())
                    .isTrue();
            assertThat(r2.getStatusCode().is4xxClientError())
                    .isTrue();
            assertThat(r3.getStatusCode().is4xxClientError())
                    .isTrue();
        }
    }
}
