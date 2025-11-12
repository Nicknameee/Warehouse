package io.store.ua.client;

import io.store.ua.AbstractIT;
import io.store.ua.entity.cache.CurrencyRate;
import io.store.ua.enums.Currency;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.web.util.UriComponentsBuilder;
import org.testcontainers.shaded.org.apache.commons.lang3.RandomUtils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
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

    private void upsertRates(BigDecimal usdRate, BigDecimal eurRate, BigDecimal chfRate, BigDecimal uahRate) {
        currencyRateRepository.deleteAll();
        currencyRateRepository.saveAll(List.of(CurrencyRate.builder()
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
            upsertRates(new BigDecimal("1.0000"),
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
                    .hasSize(4);
            assertThat(responseEntity.getBody())
                    .extracting(CurrencyRate::getCurrencyCode)
                    .containsExactlyInAnyOrder(Arrays.stream(Currency.values()).map(Enum::name).toArray(String[]::new));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/currencyRates/findBy/currency")
    class FindByCurrencyTests {
        @Test
        @DisplayName("findByCurrency_success_returnsEntity")
        void findByCurrency_success_returnsEntity() {
            upsertRates(new BigDecimal("1.0000"),
                    new BigDecimal("0.9300"),
                    new BigDecimal("0.9800"),
                    new BigDecimal("41.0000"));

            String url = UriComponentsBuilder.fromPath("/api/v1/currencyRates/findBy/currency")
                    .queryParam("currency", Currency.EUR.name())
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
                    .isEqualTo(Currency.EUR.name());
        }

        @Test
        @DisplayName("findByCurrency_fail_missing_returns4xx")
        void findByCurrency_fail_missing_returns4xx() {
            currencyRateRepository.deleteAll();

            String url = UriComponentsBuilder.fromPath("/api/v1/currencyRates/findBy/currency")
                    .queryParam("currency", GENERATOR.nextAlphanumeric(3))
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
            upsertRates(new BigDecimal("1.0000"),
                    new BigDecimal("0.9400"),
                    new BigDecimal("0.9600"),
                    new BigDecimal("40.5000"));

            BigInteger amount = new BigInteger(String.valueOf(RandomUtils.nextInt(1_000, 1_000_000)));

            String url = UriComponentsBuilder.fromPath("/api/v1/currencyRates/convert")
                    .queryParam("baseCurrency", Currency.USD.name())
                    .queryParam("targetCurrency", Currency.EUR.name())
                    .queryParam("amount", amount)
                    .build(true)
                    .toUriString();

            ResponseEntity<BigInteger> responseEntity = restClient.exchange(url,
                    HttpMethod.GET,
                    new HttpEntity<>(ownerAuthenticationHeaders),
                    BigInteger.class);

            BigInteger calculatedAmount = expectedCents(Currency.USD.name(), Currency.EUR.name(), amount);

            assertThat(responseEntity.getStatusCode())
                    .isEqualTo(HttpStatus.OK);
            assertThat(responseEntity.getBody())
                    .isNotNull();
            assertThat(responseEntity.getBody())
                    .isEqualTo(calculatedAmount);
        }

        @Test
        @DisplayName("convert_success_sameCurrency_returnsSameAmount")
        void convert_success_sameCurrency_returnsSameAmount() {
            upsertRates(new BigDecimal("1.0000"),
                    new BigDecimal("0.9300"),
                    new BigDecimal("0.9800"),
                    new BigDecimal("41.0000"));

            BigInteger amount = new BigInteger(String.valueOf(RandomUtils.nextInt(1_000, 1_000_000)));

            String url = UriComponentsBuilder.fromPath("/api/v1/currencyRates/convert")
                    .queryParam("baseCurrency", Currency.USD.name())
                    .queryParam("targetCurrency", Currency.USD.name())
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
            String url = UriComponentsBuilder.fromPath("/api/v1/currencyRates/convert")
                    .queryParam("baseCurrency", StringUtils.EMPTY)
                    .queryParam("targetCurrency", Currency.USD.name())
                    .queryParam("amount", 1)
                    .build(true)
                    .toUriString();

            ResponseEntity<String> response = restClient.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(ownerAuthenticationHeaders),
                    String.class
            );

            String otherURL = UriComponentsBuilder.fromPath("/api/v1/currencyRates/convert")
                    .queryParam("baseCurrency", Currency.USD.name())
                    .queryParam("targetCurrency", StringUtils.EMPTY)
                    .queryParam("amount", 1)
                    .build(true)
                    .toUriString();

            ResponseEntity<String> otherResponse = restClient.exchange(
                    otherURL,
                    HttpMethod.GET,
                    new HttpEntity<>(ownerAuthenticationHeaders),
                    String.class
            );

            String anotherURL = UriComponentsBuilder.fromPath("/api/v1/currencyRates/convert")
                    .queryParam("baseCurrency", Currency.USD.name())
                    .queryParam("targetCurrency", Currency.EUR.name())
                    .queryParam("amount", 0)
                    .build(true)
                    .toUriString();

            ResponseEntity<String> anotherResponse = restClient.exchange(
                    anotherURL,
                    HttpMethod.GET,
                    new HttpEntity<>(ownerAuthenticationHeaders),
                    String.class
            );

            assertThat(response.getStatusCode().is4xxClientError())
                    .isTrue();
            assertThat(otherResponse.getStatusCode().is4xxClientError())
                    .isTrue();
            assertThat(anotherResponse.getStatusCode().is4xxClientError())
                    .isTrue();
        }

        @Test
        @DisplayName("convert_fail_unknownCurrency_returns4xx")
        void convert_fail_unknownCurrency_returns4xx() {
            upsertRates(new BigDecimal("1.0000"),
                    new BigDecimal("0.9300"),
                    new BigDecimal("0.9800"),
                    new BigDecimal("41.0000"));

            String url = UriComponentsBuilder.fromPath("/api/v1/currencyRates/convert")
                    .queryParam("baseCurrency", GENERATOR.nextAlphanumeric(3))
                    .queryParam("targetCurrency", Currency.USD.name())
                    .queryParam("amount", 100)
                    .build(true)
                    .toUriString();

            ResponseEntity<String> response = restClient.exchange(url,
                    HttpMethod.GET,
                    new HttpEntity<>(ownerAuthenticationHeaders),
                    String.class);

            String otherURL = UriComponentsBuilder.fromPath("/api/v1/currencyRates/convert")
                    .queryParam("baseCurrency", Currency.USD.name())
                    .queryParam("targetCurrency", GENERATOR.nextAlphanumeric(3))
                    .queryParam("amount", 100)
                    .build(true)
                    .toUriString();

            ResponseEntity<String> otherResponse = restClient.exchange(otherURL,
                    HttpMethod.GET,
                    new HttpEntity<>(ownerAuthenticationHeaders),
                    String.class);

            assertThat(response.getStatusCode().is4xxClientError())
                    .isTrue();
            assertThat(otherResponse.getStatusCode().is4xxClientError())
                    .isTrue();
        }
    }

    @Nested
    @DisplayName("GET /api/v1/currencyRates/convertFromCentsToCurrencyUnit")
    class ConvertToUnitEndpointTests {
        @Test
        @DisplayName("convertFromCentsToCurrencyUnit_success_matchesConvertMoveLeft")
        void convertFromCentsToCurrencyUnit_success_matchesConvertMoveLeft() {
            upsertRates(new BigDecimal("1.0000"),
                    new BigDecimal("0.9400"),
                    new BigDecimal("0.9600"),
                    new BigDecimal("40.7500"));

            BigInteger amount = new BigInteger(String.valueOf(RandomUtils.nextInt(1_000, 1_000_000)));

            String url = UriComponentsBuilder.fromPath("/api/v1/currencyRates/convertFromCentsToCurrencyUnit")
                    .queryParam("baseCurrency", Currency.USD.name())
                    .queryParam("targetCurrency", Currency.EUR.name())
                    .queryParam("amount", amount)
                    .build(true)
                    .toUriString();

            ResponseEntity<BigDecimal> response = restClient.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(ownerAuthenticationHeaders),
                    BigDecimal.class
            );

            BigDecimal expectedAmount = new BigDecimal(expectedCents(Currency.USD.name(), Currency.EUR.name(), amount)).movePointLeft(2);

            assertThat(response.getStatusCode())
                    .isEqualTo(HttpStatus.OK);
            assertThat(response.getBody())
                    .isNotNull();
            assertThat(response.getBody().compareTo(expectedAmount))
                    .isZero();
        }

        @Test
        @DisplayName("convertFromCentsToCurrencyUnit_fail_invalidArgs_returns4xx")
        void convertFromCentsToCurrencyUnit_fail_invalidArgs_returns4xx() {
            String url = UriComponentsBuilder.fromPath("/api/v1/currencyRates/convertFromCentsToCurrencyUnit")
                    .queryParam("baseCurrency", StringUtils.EMPTY)
                    .queryParam("targetCurrency", Currency.USD.name())
                    .queryParam("amount", 1)
                    .build(true)
                    .toUriString();

            ResponseEntity<String> response = restClient.exchange(url,
                    HttpMethod.GET,
                    new HttpEntity<>(ownerAuthenticationHeaders),
                    String.class);

            String otherURL = UriComponentsBuilder.fromPath("/api/v1/currencyRates/convertFromCentsToCurrencyUnit")
                    .queryParam("baseCurrency", Currency.USD.name())
                    .queryParam("targetCurrency", StringUtils.EMPTY)
                    .queryParam("amount", 1)
                    .build(true)
                    .toUriString();

            ResponseEntity<String> otherResponse = restClient.exchange(otherURL,
                    HttpMethod.GET,
                    new HttpEntity<>(ownerAuthenticationHeaders),
                    String.class);

            String anotherURL = UriComponentsBuilder.fromPath("/api/v1/currencyRates/convertFromCentsToCurrencyUnit")
                    .queryParam("baseCurrency", Currency.USD.name())
                    .queryParam("targetCurrency", Currency.EUR.name())
                    .queryParam("amount", 0)
                    .build(true)
                    .toUriString();

            ResponseEntity<String> anotherResponse = restClient.exchange(anotherURL,
                    HttpMethod.GET,
                    new HttpEntity<>(ownerAuthenticationHeaders),
                    String.class);

            assertThat(response.getStatusCode().is4xxClientError())
                    .isTrue();
            assertThat(otherResponse.getStatusCode().is4xxClientError())
                    .isTrue();
            assertThat(anotherResponse.getStatusCode().is4xxClientError())
                    .isTrue();
        }
    }
}
