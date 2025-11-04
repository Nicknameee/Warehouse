package io.store.ua.service.external;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.store.ua.entity.Transaction;
import io.store.ua.enums.PaymentProvider;
import io.store.ua.enums.TransactionStatus;
import io.store.ua.exceptions.BusinessException;
import io.store.ua.exceptions.HealthCheckException;
import io.store.ua.models.api.data.DataTransTransaction;
import io.store.ua.models.api.external.request.DTPaymentInitiationRequest;
import io.store.ua.models.api.external.response.DTPaymentResponse;
import io.store.ua.models.data.CheckoutFinancialInformation;
import io.store.ua.models.data.ExternalReferences;
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

import java.math.BigInteger;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

@Service
@Profile("external")
@RequiredArgsConstructor
@FieldNameConstants
@Validated
public class DataTransAPIService implements ExternalAPIService, FinancialAPIService {
    private final HttpRequestService httpRequestService;
    private final CurrencyRateService currencyRateService;
    @Value("${transaction.incoming.provider:DataTrans}")
    private String provider;
    @Value("${transaction.incoming.merchantId}")
    private String merchantId;
    @Value("${transaction.incoming.merchantPassword}")
    private String merchantPassword;
    @Value("${transaction.incoming.url:https://api.sandbox.datatrans.com/v1}")
    private String url;
    @Value("${transaction.incoming.healthCheckUrl:https://api.sandbox.datatrans.com/upp/check}")
    private String healthCheckUrl;
    @Value("${transaction.incoming.reference.length}")
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

    @Override
    public PaymentProvider provider() {
        return PaymentProvider.DATA_TRANS;
    }

    @Override
    public CheckoutFinancialInformation initiateIncomingPayment(Transaction transaction, boolean settleOnInitiation) {
        DataTransTransaction dataTransTransaction = initiateIncomingPaymentAPICall(transaction.getCurrency(), transaction.getAmount(), settleOnInitiation);

        transaction.setReference(dataTransTransaction.getReference());
        transaction.setTransactionId(dataTransTransaction.getTransactionId());
        transaction.setExternalReferences(ExternalReferences.builder()
                .reference(dataTransTransaction.getReference())
                .transactionId(dataTransTransaction.getTransactionId())
                .authenticationCode(dataTransTransaction.getAcquirerAuthorizationCode())
                .build());
        transaction.setCreatedAt(LocalDateTime.now(Clock.systemUTC()));

        if (settleOnInitiation) {
            transaction.setStatus(TransactionStatus.SETTLED);
            transaction.setPaidAt(LocalDateTime.now(Clock.systemUTC()));
        } else {
            transaction.setStatus(TransactionStatus.INITIATED);
        }

        transaction.setPaymentProvider(PaymentProvider.DATA_TRANS);

        return CheckoutFinancialInformation.builder()
                .paymentProvider(PaymentProvider.DATA_TRANS)
                .transactionId(dataTransTransaction.getTransactionId())
                .reference(dataTransTransaction.getReference())
                .build();
    }

    @Override
    public Transaction initiateOutcomingPayment(Transaction transaction, boolean settleOnInitiation) {
        DataTransTransaction dataTransTransaction = initiateOutcomingPaymentAPICall(transaction.getCurrency(), transaction.getAmount());

        transaction.setReference(dataTransTransaction.getReference());
        transaction.setTransactionId(dataTransTransaction.getTransactionId());
        transaction.setExternalReferences(ExternalReferences.builder()
                .reference(dataTransTransaction.getReference())
                .transactionId(dataTransTransaction.getTransactionId())
                .authenticationCode(dataTransTransaction.getAcquirerAuthorizationCode())
                .build());
        transaction.setCreatedAt(LocalDateTime.now(Clock.systemUTC()));

        if (settleOnInitiation) {
            transaction.setStatus(TransactionStatus.SETTLED);
            transaction.setPaidAt(LocalDateTime.now(Clock.systemUTC()));
        } else {
            transaction.setStatus(TransactionStatus.INITIATED);
        }

        transaction.setPaymentProvider(PaymentProvider.DATA_TRANS);

        return transaction;
    }

    @Override
    public Transaction settlePayment(Transaction transaction) {
        if (transaction.getStatus() != TransactionStatus.INITIATED) {
            throw new BusinessException("Transaction is already finalized");
        }

        settlePaymentAPICall(transaction.getCurrency(),
                transaction.getAmount(),
                transaction.getExternalReferences().getTransactionId(),
                transaction.getExternalReferences().getReference());

        transaction.setPaidAt(LocalDateTime.now(Clock.systemUTC()));
        transaction.setStatus(TransactionStatus.SETTLED);

        return transaction;
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
    public DataTransTransaction initiateIncomingPaymentAPICall(@NotBlank(message = "Currency can't be blank") String currency,
                                                               @NotNull(message = "Amount can't be null")
                                                               @Min(value = 1, message = "Amount can't be less than 1")
                                                               BigInteger amount,
                                                               boolean autoSettle) {
        try {
            if (!isHealthy()) {
                throw new HealthCheckException();
            }

            String reference = CodeGenerator.TransactionCodeGenerator.generate(PaymentProvider.DATA_TRANS);

            if (reference.length() > referenceLength) {
                throw new BusinessException("Transaction reference exceeds allowed length for '%s' provider".formatted(provider));
            }

            var request = DTPaymentInitiationRequest.builder()
                    .currency(Constants.Currency.USD)
                    .amount(currencyRateService.convert(currency, Constants.Currency.USD, amount).toString())
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

    public DataTransTransaction initiateOutcomingPaymentAPICall(@NotBlank(message = "Currency can't be blank") String currency,
                                                                @NotNull(message = "Amount can't be null")
                                                                @Min(value = 1, message = "Amount can't be less than 1")
                                                                BigInteger amount) {
        try {
            if (!isHealthy()) {
                throw new HealthCheckException();
            }

            String reference = CodeGenerator.generate(referenceLength);

            var request = DTPaymentInitiationRequest.builder()
                    .currency(Constants.Currency.USD)
                    .amount(currencyRateService.convert(currency, Constants.Currency.USD, amount).toString())
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

    public DataTransTransaction settlePaymentAPICall(@NotBlank(message = "Currency can't be blank") String currency,
                                                     @NotNull(message = "Amount can't be null")
                                                     @Min(value = 1, message = "Amount can't be less than 1")
                                                     BigInteger amount,
                                                     @NotBlank(message = "Transaction ID can't be blank") String transactionId,
                                                     @NotBlank(message = "Reference can't be blank") String reference) {
        try {
            if (!isHealthy()) {
                throw new HealthCheckException();
            }

            var request = DTPaymentInitiationRequest.builder()
                    .currency(Constants.Currency.USD)
                    .amount(currencyRateService.convert(currency, Constants.Currency.USD, amount).toString())
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
