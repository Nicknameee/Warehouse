package io.store.ua.service.external;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.store.ua.exceptions.HealthCheckException;
import io.store.ua.models.api.external.request.LPCheckoutPayload;
import io.store.ua.models.api.external.request.LPCheckoutRequest;
import io.store.ua.models.api.external.request.LPStatusPayload;
import io.store.ua.models.api.external.response.LPResponse;
import io.store.ua.utility.HttpRequestService;
import io.store.ua.utility.RegularObjectMapper;
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
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            md.update((privateKey + data + privateKey).getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(md.digest());
        } catch (Exception e) {
            throw new RuntimeException("Failed to sign LiqPay payload", e);
        }
    }

    private static String htmlEscape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    public String initiatePayment(@NotNull LPCheckoutRequest LPCheckoutRequest) {
        if (!isHealthy()) {
            throw new HealthCheckException();
        }

        try {
            LPCheckoutPayload payload = LPCheckoutPayload.builder()
                    .version(version)
                    .publicKey(publicKey)
                    .action(Constants.ACTION_PAY)
                    .amount(LPCheckoutRequest.getAmount())
                    .currency(LPCheckoutRequest.getCurrency())
                    .description(LPCheckoutRequest.getDescription())
                    .orderId(LPCheckoutRequest.getOrderId())
                    .sandbox(String.valueOf(sandbox ? BigInteger.ONE : BigInteger.ZERO))
                    .build();

            String json = RegularObjectMapper.writeToString(payload);
            String data = base64(json);
            String signature = sign(privateKey, data);

            return """
                    <form method="POST" action="%s">
                      <input type="hidden" name="data" value="%s"/>
                      <input type="hidden" name="signature" value="%s"/>
                      <button type="submit">Pay</button>
                    </form>
                    """.formatted(checkoutUrl, htmlEscape(data), htmlEscape(signature));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public LPResponse checkPaymentStatus(@NotBlank String orderId) {
        if (!isHealthy()) {
            throw new HealthCheckException();
        }

        try {
            LPStatusPayload payload = LPStatusPayload.builder()
                    .publicKey(publicKey)
                    .version(version)
                    .action(Constants.ACTION_STATUS)
                    .orderId(orderId)
                    .build();

            String json = RegularObjectMapper.writeToString(payload);
            String data = base64(json);
            String signature = sign(privateKey, data);

            Request request = new Request.Builder()
                    .url(apiUrl)
                    .post(new FormBody.Builder()
                            .add(Constants.CONTENT, data)
                            .add(Constants.SIGNATURE, signature)
                            .build())
                    .build();

            try (var response = httpRequestService.fetchAsync(request).join()) {
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
    }
}
