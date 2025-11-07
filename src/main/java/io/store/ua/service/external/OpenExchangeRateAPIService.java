package io.store.ua.service.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.store.ua.entity.cache.CurrencyRate;
import io.store.ua.exceptions.ExternalException;
import io.store.ua.exceptions.HealthCheckException;
import io.store.ua.utility.HttpRequestService;
import io.store.ua.utility.RegularObjectMapper;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldNameConstants;
import okhttp3.Request;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@Profile("external")
@RequiredArgsConstructor
@FieldNameConstants
@Validated
public class OpenExchangeRateAPIService implements ExternalAPIService {
    private final HttpRequestService httpRequestService;
    @Value("${exchange.url}")
    private String url;
    @Value("${exchange.appId}")
    private String appId;

    public List<CurrencyRate> refreshCurrencyRates() {
        if (!isHealthy()) {
            throw new HealthCheckException();
        }

        Request request = new Request.Builder()
                .url(url)
                .header(HttpHeaders.AUTHORIZATION, "Authorization: Token %s".formatted(appId))
                .get()
                .build();

        var response = httpRequestService.queryAsync(request)
                .orTimeout(15, TimeUnit.SECONDS)
                .join();

        try (response) {
            var exchangeRates = RegularObjectMapper.read(response.peekBody(Long.MAX_VALUE).string(), OpenExchangeResponse.class);

            if (ObjectUtils.anyNull(exchangeRates, exchangeRates.getBaseCurrency(), exchangeRates.getRates())
                    || ObjectUtils.notEqual(exchangeRates.getBaseCurrency(), "USD")
                    || exchangeRates.getRates().isEmpty()) {
                throw new ExternalException("Invalid response from OpenExchangeRate API");
            }

            return exchangeRates.getRates()
                    .entrySet()
                    .stream()
                    .filter(entry -> entry.getValue() != null && entry.getValue().compareTo(BigDecimal.ZERO) > 0)
                    .map(rate -> CurrencyRate.builder()
                            .currencyCode(rate.getKey())
                            .baseCurrencyCode(exchangeRates.getBaseCurrency())
                            .rate(rate.getValue())
                            .expiryTime(TimeUnit.DAYS.toSeconds(1))
                            .build())
                    .toList();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @RequiredArgsConstructor
    enum QueryParam {
        /**
         * Your unique App ID
         */
        APP_ID("app_id"),
        /**
         * Change base currency (3-letter code, default: USD)
         */
        BASE_CURRENCY("base"),
        /**
         * Set to false for reduce response size (removes whitespace)
         */
        REMOVE_WHITESPACES("prettyprint"),
        /**
         * Extend returned values with alternative, black market and digital currency rates
         */
        SHOW_ALTERNATIVE("show_alternative");

        @Getter
        private final String value;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class OpenExchangeResponse {
        @JsonProperty("base")
        private String baseCurrency;
        @JsonProperty("rates")
        private Map<String, BigDecimal> rates;
    }
}