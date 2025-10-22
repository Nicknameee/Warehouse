package io.store.ua.service.external;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.store.ua.exceptions.HealthCheckException;
import io.store.ua.models.api.data.DataTransTransaction;
import io.store.ua.models.api.external.request.DTPaymentInitiationRequest;
import io.store.ua.models.api.external.response.DTPaymentResponse;
import io.store.ua.service.CurrencyRateService;
import io.store.ua.utility.CodeGenerator;
import io.store.ua.utility.HttpRequestService;
import io.store.ua.utility.RegularObjectMapper;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldNameConstants;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

@Service
@Profile("external")
@RequiredArgsConstructor
@FieldNameConstants
@Validated
public class DataTransAPIService implements ExternalAPIService {
    private final HttpRequestService httpRequestService;
    private final CurrencyRateService currencyRateService;
    @Value("${transaction.provider:DataTrans}")
    private String provider;
    @Value("${transaction.merchantId}")
    private String merchantId;
    @Value("${transaction.merchantPassword}")
    private String merchantPassword;
    @Value("${transaction.url:https://api.sandbox.datatrans.com/v1}")
    private String url;
    @Value("${transaction.healthCheckUrl:https://api.sandbox.datatrans.com/upp/check}")
    private String healthCheckUrl;
    @Value("${transaction.reference.length}")
    private int referenceLength;

    @Retryable(
            maxAttempts = 10,
            backoff = @Backoff(delay = 10_000, multiplier = 1.0),
            retryFor = Exception.class
    )
    @Scheduled(fixedRate = 1, timeUnit = TimeUnit.MINUTES, initialDelay = 0)
    public void healthCheck() {
        httpRequestService.fetchAsync(
                        new Request.Builder()
                                .addHeader(HttpHeaders.AUTHORIZATION, getAuthorizationHeader(Constants.TokenType.BASIC))
                                .url(healthCheckUrl)
                                .get()
                                .build()
                ).orTimeout(10, TimeUnit.SECONDS)
                .thenApply(ignore -> {
                    IS_HEALTHY.set(true);
                    return null;
                })
                .exceptionally(ignore -> {
                    IS_HEALTHY.set(false);
                    return null;
                });
    }

    private String getAuthorizationHeader(String type) {
        return ("%s %s").formatted(type, Base64.getEncoder().encodeToString(("%s:%s")
                .formatted(merchantId, merchantPassword)
                .getBytes()));
    }

    /**
     *
     * Securely send all the necessary parameters to the transaction initialization API.
     * The result of this API call is an HTTP 201 status code with a transactionId in the response body and the Location header set
     * This call is required to proceed with our Redirect and Lightbox integration
     *
     * @param currency   is an initial currency of transaction
     * @param amount     amount of transaction in specified currency
     * @param autoSettle whether to automatically settle the transaction after an authorization or not
     * @return {@link DataTransTransaction} which contains transactionId and generated reference
     */
    public DataTransTransaction initialisePayment(@NotBlank(message = "Currency can't be blank") String currency,
                                                  @NotNull(message = "Amount can't be null")
                                                  @Min(value = 1, message = "Amount can't be less than 1")
                                                  BigDecimal amount,
                                                  boolean autoSettle) {
        try {
            if (!isHealthy()) {
                throw new HealthCheckException();
            }

            String reference = CodeGenerator.generate(referenceLength);

            var request = DTPaymentInitiationRequest.builder()
                    .currency(Constants.Currency.USD)
                    .amount(String.valueOf(currencyRateService.convert(currency, Constants.Currency.USD, amount).intValue()))
                    .transactionReference(reference)
                    .redirect(new DTPaymentInitiationRequest.Redirect())
                    .theme(new DTPaymentInitiationRequest.Theme())
                    .autoSettle(autoSettle)
                    .build();

            Response response = httpRequestService.fetchAsync(new Request.Builder()
                            .addHeader(HttpHeaders.AUTHORIZATION, getAuthorizationHeader(Constants.TokenType.BASIC))
                            .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                            .addHeader(Constants.Headers.IDEMPOTENCY_KEY, "initialisePayment_%s".formatted(reference))
                            .url(("%s%s").formatted(url, Constants.Transactions.INITIATE_TRANSACTION.getValue()))
                            .method(HttpMethod.POST.name(), RequestBody.create(RegularObjectMapper.writeToBytes(request))).build())
                    .orTimeout(15, TimeUnit.SECONDS)
                    .join();

            try (response) {
                String body = response.peekBody(Long.MAX_VALUE).string();
                var content = RegularObjectMapper.read(body, DTPaymentResponse.class);

                return DataTransTransaction.builder()
                        .transactionId(content.getTransactionId())
                        .reference(reference)
                        .build();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public DataTransTransaction authorizePayment(@NotBlank(message = "Currency can't be blank") String currency,
                                                 @NotNull(message = "Amount can't be null")
                                                 @Min(value = 1, message = "Amount can't be less than 1")
                                                 BigDecimal amount) {
        try {
            if (!isHealthy()) {
                throw new HealthCheckException();
            }

            String reference = CodeGenerator.generate(referenceLength);

            var request = DTPaymentInitiationRequest.builder()
                    .currency(Constants.Currency.USD)
                    .amount(String.valueOf(currencyRateService.convert(currency, Constants.Currency.USD, amount).intValue()))
                    .transactionReference(reference)
                    .card(new DTPaymentInitiationRequest.Card())
                    .build();

            Response response = httpRequestService.fetchAsync(new Request.Builder()
                            .addHeader(HttpHeaders.AUTHORIZATION, getAuthorizationHeader(Constants.TokenType.BASIC))
                            .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                            .addHeader(Constants.Headers.IDEMPOTENCY_KEY, "authorizePayment_%s".formatted(reference))
                            .url(("%s%s").formatted(url, Constants.Transactions.AUTHORIZE_TRANSACTION.getValue()))
                            .method(HttpMethod.POST.name(), RequestBody.create(RegularObjectMapper.writeToBytes(request))).build())
                    .orTimeout(15, TimeUnit.SECONDS)
                    .join();

            try (response) {
                String body = response.peekBody(Long.MAX_VALUE).string();
                var content = RegularObjectMapper.read(body, DTPaymentResponse.class);

                return DataTransTransaction.builder()
                        .transactionId(content.getTransactionId())
                        .reference(reference)
                        .acquirerAuthorizationCode(content.getAcquirerAuthorizationCode())
                        .build();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public DataTransTransaction settlePayment(@NotBlank(message = "Currency can't be blank") String currency,
                                              @NotNull(message = "Amount can't be null")
                                              @Min(value = 1, message = "Amount can't be less than 1")
                                              BigDecimal amount,
                                              @NotBlank(message = "Transaction ID can't be blank") String transactionId,
                                              @NotBlank(message = "Reference can't be blank") String reference) {
        try {
            if (!isHealthy()) {
                throw new HealthCheckException();
            }

            var request = DTPaymentInitiationRequest.builder()
                    .currency(Constants.Currency.USD)
                    .amount(String.valueOf(currencyRateService.convert(currency, Constants.Currency.USD, amount).intValue()))
                    .transactionReference(reference)
                    .build();

            httpRequestService.fetchAsync(new Request.Builder()
                            .addHeader(HttpHeaders.AUTHORIZATION, getAuthorizationHeader(Constants.TokenType.BASIC))
                            .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                            .addHeader(Constants.Headers.IDEMPOTENCY_KEY, "settlePayment_%s".formatted(reference))
                            .url(("%s%s").formatted(url, Constants.Transactions.SETTLE_TRANSACTION.formatPath(transactionId)))
                            .method(HttpMethod.POST.name(), RequestBody.create(RegularObjectMapper.writeToBytes(request))).build())
                    .orTimeout(15, TimeUnit.SECONDS)
                    .join().close();


            return DataTransTransaction.builder()
                    .transactionId(transactionId)
                    .reference(reference)
                    .build();
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Constants {
        @RequiredArgsConstructor
        public enum Transactions {
            INITIATE_TRANSACTION("/transactions"),
            AUTHORIZE_TRANSACTION("/transactions/authorize"),
            VALIDATE_TRANSACTION("/validate"),
            SETTLE_TRANSACTION("/transactions/:transactionId/settle"),
            CANCEL_TRANSACTION("/{{transactionId}}/cancel"),
            REFUND_TRANSACTION("/{{transactionId}}/credit"),
            STATUS_TRANSACTION("/{{transactionId}}");

            @Getter
            private final String value;

            public String formatPath(String transactionId) {
                return value.replaceAll(":transactionId", transactionId);
            }
        }

        public static class Currency {
            public static final String EUR = "EUR";
            public static final String USD = "USD";
            public static final String CHF = "CHF";
        }

        public static class TokenType {
            public static final String BEARER = "Bearer";
            public static final String BASIC = "Basic";
        }

        static class Headers {
            public static final String IDEMPOTENCY_KEY = "Idempotency-Key";
        }
    }
}
