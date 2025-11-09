package io.store.ua.service.external;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.store.ua.entity.Transaction;
import io.store.ua.enums.Currency;
import io.store.ua.enums.PaymentProvider;
import io.store.ua.enums.TransactionStatus;
import io.store.ua.exceptions.BusinessException;
import io.store.ua.exceptions.HealthCheckException;
import io.store.ua.models.api.external.request.LPInitiatePaymentRequest;
import io.store.ua.models.api.external.request.LPInitiatePaymentRequestDTO;
import io.store.ua.models.api.external.request.LPStatusPayload;
import io.store.ua.models.api.external.response.LPInitiatePaymentResponse;
import io.store.ua.models.api.external.response.LPResponse;
import io.store.ua.models.data.CheckoutFinancialInformation;
import io.store.ua.models.data.ExternalReferences;
import io.store.ua.repository.BeneficiaryRepository;
import io.store.ua.service.CurrencyRateService;
import io.store.ua.service.FinancialAPIService;
import io.store.ua.utility.CodeGenerator;
import io.store.ua.utility.HttpRequestService;
import io.store.ua.utility.RegularObjectMapper;
import io.store.ua.validations.FieldValidator;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import okhttp3.FormBody;
import okhttp3.Request;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
@RequiredArgsConstructor
@Validated
public class LiqPayAPIService implements ExternalAPIService, FinancialAPIService {
    private final HttpRequestService httpRequestService;
    private final CurrencyRateService currencyRateService;
    private final BeneficiaryRepository beneficiaryRepository;
    private final FieldValidator fieldValidator;

    @Value("${transaction.outcoming.provider}")
    private String provider;
    @Value("${transaction.outcoming.url}")
    private String apiUrl;
    @Value("${transaction.outcoming.checkoutUrl}")
    private String checkoutUrl;
    @Value("${transaction.outcoming.version}")
    private String version;
    @Value("${transaction.outcoming.publicKey}")
    private String publicKey;
    @Value("${transaction.outcoming.privateKey}")
    private String privateKey;
    @Value("${transaction.outcoming.sandbox}")
    private boolean isSandboxAPIState;

    private static String encodeToBase64(String content) {
        return Base64.getEncoder().encodeToString(content.getBytes(StandardCharsets.UTF_8));
    }

    private static String sign(String privateKey, String data) {
        try {
            MessageDigest instance = MessageDigest.getInstance("SHA-1");
            instance.update((privateKey + data + privateKey).getBytes(StandardCharsets.UTF_8));

            return Base64.getEncoder().encodeToString(instance.digest());
        } catch (Exception e) {
            throw new RuntimeException("LiqPay signature fail", e);
        }
    }

    @Override
    public PaymentProvider provider() {
        return PaymentProvider.LIQ_PAY;
    }

    @Override
    public CheckoutFinancialInformation initiateIncomingPayment(Transaction transaction, boolean settleOnInitiation) {
        if (settleOnInitiation) {
            throw new UnsupportedOperationException("Auto Settlement is not supported by LiqPay");
        }

        var reference = CodeGenerator.TransactionCodeGenerator.generate(PaymentProvider.LIQ_PAY);

        LPInitiatePaymentResponse initiatePaymentRequestDTO = initiateIncomingPaymentAPICall(LPInitiatePaymentRequestDTO.builder()
                .orderId(reference)
                .currency(transaction.getCurrency())
                .amount(transaction.getAmount())
                .beneficiaryID(transaction.getBeneficiaryId())
                .build());

        transaction.setReference(reference);
        transaction.setTransactionId(reference);
        transaction.setExternalReferences(
                ExternalReferences.builder()
                        .transactionId(reference)
                        .reference(reference)
                        .build());
        transaction.setCreatedAt(LocalDateTime.now(Clock.systemUTC()));
        transaction.setStatus(TransactionStatus.INITIATED);
        transaction.setPaymentProvider(PaymentProvider.LIQ_PAY);

        return CheckoutFinancialInformation.builder()
                .checkoutUrl(initiatePaymentRequestDTO.getCheckoutUrl())
                .signature(initiatePaymentRequestDTO.getSignature())
                .encodedContent(initiatePaymentRequestDTO.getEncodedContent())
                .transactionId(reference)
                .reference(reference)
                .paymentProvider(PaymentProvider.LIQ_PAY)
                .build();
    }

    @Override
    public Transaction initiateOutcomingPayment(Transaction transaction, boolean settleOnInitiation) {
        if (settleOnInitiation) {
            throw new UnsupportedOperationException("Auto Settlement is not supported by LiqPay");
        }

        var reference = CodeGenerator.TransactionCodeGenerator.generate(PaymentProvider.LIQ_PAY);

        LPResponse transactionResult = initiateOutcomingPaymentAPICall(LPInitiatePaymentRequestDTO
                .builder()
                .orderId(reference)
                .currency(transaction.getCurrency())
                .amount(transaction.getAmount())
                .beneficiaryID(transaction.getBeneficiaryId())
                .build());

        transaction.setReference(reference);
        transaction.setTransactionId(reference);
        transaction.setExternalReferences(
                ExternalReferences.builder()
                        .transactionId(reference)
                        .reference(reference)
                        .build());
        transaction.setCreatedAt(LocalDateTime.now(Clock.systemUTC()));
        transaction.setStatus(LPResponse.Status.convertToBasicStatus(transactionResult.getStatus()));
        transaction.setPaymentProvider(PaymentProvider.LIQ_PAY);

        return transaction;
    }

    @Override
    public Transaction settlePayment(Transaction transaction) {
        if (transaction.getStatus() != TransactionStatus.INITIATED) {
            throw new BusinessException("Transaction is already finalized");
        }

        LPResponse response = checkPaymentStatusAPICall(transaction.getReference());

        transaction.setStatus(LPResponse.Status.convertToBasicStatus(response.getStatus()));

        if (transaction.getStatus() == TransactionStatus.SETTLED) {
            transaction.setPaidAt(LocalDateTime.now(Clock.systemUTC()));
        }

        return transaction;
    }

    public LPInitiatePaymentResponse initiateIncomingPaymentAPICall(@NotNull LPInitiatePaymentRequestDTO requestDTO) {
        if (!isHealthy()) {
            throw new HealthCheckException();
        }

        try {
            String encodedRequest = encodeToBase64(RegularObjectMapper.writeToString(LPInitiatePaymentRequest.builder()
                    .version(version)
                    .publicKey(publicKey)
                    .action(Constants.ACTION_PAY)
                    .amount(currencyRateService.convertFromCentsToCurrencyUnit(requestDTO.getCurrency(),
                            Currency.UAH.name(),
                            requestDTO.getAmount()).toEngineeringString())
                    .currency(requestDTO.getCurrency())
                    .description("Incoming payment for #%s".formatted(requestDTO.getOrderId()))
                    .orderId(requestDTO.getOrderId())
                    .sandbox(String.valueOf(isSandboxAPIState ? BigInteger.ONE : BigInteger.ZERO))
                    .build()));

            return LPInitiatePaymentResponse.builder()
                    .checkoutUrl(checkoutUrl)
                    .signature(sign(privateKey, encodedRequest))
                    .encodedContent(encodedRequest)
                    .build();
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public LPResponse initiateOutcomingPaymentAPICall(@NotNull LPInitiatePaymentRequestDTO requestDTO) {
        if (!isHealthy()) {
            throw new HealthCheckException();
        }

        fieldValidator.validate(requestDTO, true,
                LPInitiatePaymentRequestDTO.Fields.orderId,
                LPInitiatePaymentRequestDTO.Fields.currency,
                LPInitiatePaymentRequestDTO.Fields.amount,
                LPInitiatePaymentRequestDTO.Fields.beneficiaryID);

        try {
            var beneficiary = beneficiaryRepository.findById(requestDTO.getBeneficiaryID())
                    .orElseThrow(() -> new RuntimeException("Beneficiary with code '%s' was not found"
                            .formatted(requestDTO.getBeneficiaryID())));


            if (isSandboxAPIState) {
                return LPResponse.builder()
                        .orderId(requestDTO.getOrderId())
                        .status(LPResponse.Status.SUCCESS)
                        .build();
            }

            String encoded = encodeToBase64(RegularObjectMapper.writeToString(LPInitiatePaymentRequest.builder()
                    .version(version)
                    .publicKey(publicKey)
                    .action(Constants.ACTION_P2P_CREDIT)
                    .amount(currencyRateService.convertFromCentsToCurrencyUnit(requestDTO.getCurrency(),
                            Currency.UAH.name(),
                            requestDTO.getAmount()).toEngineeringString())
                    .currency(requestDTO.getCurrency())
                    .description("Payout for beneficiary #" + beneficiary.getCard())
                    .orderId(requestDTO.getOrderId())
                    .receiverCard(beneficiary.getCard())
                    .sandbox(String.valueOf(isSandboxAPIState ? BigInteger.ONE : BigInteger.ZERO))
                    .build())
            );

            try (var response = httpRequestService.queryAsync(new Request.Builder()
                            .url(apiUrl)
                            .post(new FormBody.Builder()
                                    .add(Constants.CONTENT, encoded)
                                    .add(Constants.SIGNATURE, sign(privateKey, encoded))
                                    .build())
                            .build())
                    .join()) {
                return RegularObjectMapper.read(response.peekBody(Long.MAX_VALUE).string(), LPResponse.class);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to initiate outgoing LiqPay payment", e);
        }
    }

    public LPResponse checkPaymentStatusAPICall(@NotBlank String orderId) {
        if (!isHealthy()) {
            throw new HealthCheckException();
        }

        try {
            String data = encodeToBase64(RegularObjectMapper.writeToString(LPStatusPayload.builder()
                    .publicKey(publicKey)
                    .version(version)
                    .action(Constants.ACTION_STATUS)
                    .orderId(orderId)
                    .build()));

            try (var response = httpRequestService.queryAsync(new Request.Builder()
                            .url(apiUrl)
                            .post(new FormBody.Builder()
                                    .add(Constants.CONTENT, data)
                                    .add(Constants.SIGNATURE, sign(privateKey, data))
                                    .build())
                            .build())
                    .join()) {
                return RegularObjectMapper.read(response.peekBody(Long.MAX_VALUE).string(), LPResponse.class);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static class Constants {
        public static final String ACTION_PAY = "pay";
        public static final String ACTION_STATUS = "status";
        public static final String CONTENT = "data";
        public static final String SIGNATURE = "signature";
        public static final String ACTION_P2P_CREDIT = "p2pcredit";
    }
}
