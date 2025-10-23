package io.store.ua.service.external;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.store.ua.exceptions.HealthCheckException;
import io.store.ua.models.api.external.request.LPInitiatePaymentRequest;
import io.store.ua.models.api.external.request.LPInitiatePaymentRequestDTO;
import io.store.ua.models.api.external.request.LPStatusPayload;
import io.store.ua.models.api.external.response.LPInitiatePaymentResponse;
import io.store.ua.models.api.external.response.LPResponse;
import io.store.ua.repository.BeneficiaryRepository;
import io.store.ua.utility.HttpRequestService;
import io.store.ua.utility.RegularObjectMapper;
import io.store.ua.validations.FieldValidator;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import okhttp3.FormBody;
import okhttp3.Request;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

@Service
@Profile("external")
@RequiredArgsConstructor
@Validated
public class LiqPayAPIService implements ExternalAPIService {
    private final HttpRequestService httpRequestService;
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
    private boolean sandbox;

    private static String base64(String content) {
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

    public LPInitiatePaymentResponse initiateIncomingPayment(@NotNull LPInitiatePaymentRequestDTO requestDTO) {
        if (!isHealthy()) {
            throw new HealthCheckException();
        }

        try {
            String encodedRequest = base64(RegularObjectMapper.writeToString(LPInitiatePaymentRequest.builder()
                    .version(version)
                    .publicKey(publicKey)
                    .action(Constants.ACTION_PAY)
                    .amount(requestDTO.getAmount().toEngineeringString())
                    .currency(requestDTO.getCurrency())
                    .description("Incoming payment for #%s".formatted(requestDTO.getOrderId()))
                    .orderId(requestDTO.getOrderId())
                    .sandbox(String.valueOf(sandbox ? BigInteger.ONE : BigInteger.ZERO))
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

    public LPResponse initiateOutcomingPayment(@NotNull LPInitiatePaymentRequestDTO request) {
        if (!isHealthy()) {
            throw new HealthCheckException();
        }

        fieldValidator.validate(request, true,
                LPInitiatePaymentRequestDTO.Fields.orderId,
                LPInitiatePaymentRequestDTO.Fields.currency,
                LPInitiatePaymentRequestDTO.Fields.amount,
                LPInitiatePaymentRequestDTO.Fields.beneficiaryCode);

        try {
            var beneficiary = beneficiaryRepository.findByCode(request.getBeneficiaryCode())
                    .orElseThrow(() -> new RuntimeException("Beneficiary with code '%s' was not found".formatted(request.getBeneficiaryCode())));

            String encoded = base64(RegularObjectMapper.writeToString(
                    LPInitiatePaymentRequest.builder()
                            .version(version)
                            .publicKey(publicKey)
                            .action(Constants.ACTION_P2P_CREDIT)
                            .amount(request.getAmount().toEngineeringString())
                            .currency(request.getCurrency())
                            .description("Payout for beneficiary #" + request.getBeneficiaryCode())
                            .orderId(request.getOrderId())
                            .receiverCard(beneficiary.getCard())
                            .sandbox(String.valueOf(sandbox ? java.math.BigInteger.ONE : java.math.BigInteger.ZERO))
                            .build()
            ));

            try (var response = httpRequestService.fetchAsync(new Request.Builder()
                    .url(apiUrl)
                    .post(new FormBody.Builder()
                            .add(Constants.CONTENT, encoded)
                            .add(Constants.SIGNATURE, sign(privateKey, encoded))
                            .build())
                    .build()).join()) {
                var body = response.peekBody(Long.MAX_VALUE).string();

                var result = RegularObjectMapper.read(body, LPResponse.class);

                if (sandbox) {
                    result.setStatus(LPResponse.Status.SUCCESS.name());
                }

                return result;
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to initiate outgoing LiqPay payment", e);
        }
    }

    public LPResponse checkPaymentStatus(@NotBlank String orderId) {
        if (!isHealthy()) {
            throw new HealthCheckException();
        }

        try {
            String data = base64(RegularObjectMapper.writeToString(LPStatusPayload.builder()
                    .publicKey(publicKey)
                    .version(version)
                    .action(Constants.ACTION_STATUS)
                    .orderId(orderId)
                    .build()));

            try (var response = httpRequestService.fetchAsync(new Request.Builder()
                    .url(apiUrl)
                    .post(new FormBody.Builder()
                            .add(Constants.CONTENT, data)
                            .add(Constants.SIGNATURE, sign(privateKey, data))
                            .build())
                    .build()).join()) {
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
