package io.store.ua.controllers;

import io.store.ua.AbstractIT;
import io.store.ua.enums.*;
import io.store.ua.utility.UserSecurityStrategyService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ResourcesControllerIT extends AbstractIT {
    @Value("${spring.security.user.name}")
    private String basicUsername;

    @Value("${spring.security.user.password}")
    private String basicPassword;

    private static List<String> enumNames(Enum<?>[] values) {
        return Arrays.stream(values).map(Enum::name).toList();
    }

    private HttpHeaders basicAuthHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, "Basic %s"
                .formatted(Base64.getEncoder()
                        .encodeToString(("%s:%s".formatted(basicUsername, basicPassword))
                                .getBytes(StandardCharsets.UTF_8))));

        return headers;
    }

    private ResponseEntity<List<String>> getAsStringList(String url) {
        return restClient.exchange(url,
                HttpMethod.GET,
                new HttpEntity<>(basicAuthHeaders()),
                new ParameterizedTypeReference<>() {
                });
    }

    private <T> ResponseEntity<T> get(String url, Class<T> type) {
        return restClient.exchange(url,
                HttpMethod.GET,
                new HttpEntity<>(basicAuthHeaders()),
                type);
    }

    @Nested
    @DisplayName("/vars exact values (basic auth)")
    class VarsExactValuesTests {
        @Test
        void userRoles() {
            ResponseEntity<List<String>> response = getAsStringList("/vars/userRoles");

            assertThat(response.getStatusCode())
                    .isEqualTo(HttpStatus.OK);
            assertThat(response.getBody())
                    .containsExactlyElementsOf(enumNames(UserRole.values()));
        }

        @Test
        void userStatuses() {
            ResponseEntity<List<String>> response = getAsStringList("/vars/userStatuses");

            assertThat(response.getStatusCode())
                    .isEqualTo(HttpStatus.OK);
            assertThat(response.getBody())
                    .containsExactlyElementsOf(enumNames(UserStatus.values()));
        }

        @Test
        void securityType() {
            ResponseEntity<String> response = get("/vars/securityType", String.class);

            assertThat(response.getStatusCode())
                    .isEqualTo(HttpStatus.OK);
            assertThat(response.getBody())
                    .isEqualTo(UserSecurityStrategyService.USER_AUTHENTICATION_TYPE);
        }

        @Test
        void cardTypes() {
            ResponseEntity<List<String>> response = getAsStringList("/vars/cardTypes");

            assertThat(response.getStatusCode())
                    .isEqualTo(HttpStatus.OK);
            assertThat(response.getBody())
                    .containsExactlyElementsOf(enumNames(CardType.values()));
        }

        @Test
        void currencies() {
            ResponseEntity<List<String>> response = getAsStringList("/vars/currencies");

            assertThat(response.getStatusCode())
                    .isEqualTo(HttpStatus.OK);
            assertThat(response.getBody())
                    .containsExactlyElementsOf(enumNames(Currency.values()));
        }

        @Test
        void paymentProviders() {
            ResponseEntity<List<String>> response = getAsStringList("/vars/paymentProviders");

            assertThat(response.getStatusCode())
                    .isEqualTo(HttpStatus.OK);
            assertThat(response.getBody())
                    .containsExactlyElementsOf(enumNames(PaymentProvider.values()));
        }

        @Test
        void shipmentDirections() {
            ResponseEntity<List<String>> response = getAsStringList("/vars/shipmentDirections");

            assertThat(response.getStatusCode())
                    .isEqualTo(HttpStatus.OK);
            assertThat(response.getBody())
                    .containsExactlyElementsOf(enumNames(ShipmentDirection.values()));
        }

        @Test
        void shipmentStatuses() {
            ResponseEntity<List<String>> response = getAsStringList("/vars/shipmentStatuses");

            assertThat(response.getStatusCode())
                    .isEqualTo(HttpStatus.OK);
            assertThat(response.getBody())
                    .containsExactlyElementsOf(enumNames(ShipmentStatus.values()));
        }

        @Test
        void stockItemStatuses() {
            ResponseEntity<List<String>> response = getAsStringList("/vars/stockItemStatuses");

            assertThat(response.getStatusCode())
                    .isEqualTo(HttpStatus.OK);
            assertThat(response.getBody())
                    .containsExactlyElementsOf(enumNames(StockItemStatus.values()));
        }

        @Test
        void transactionFlowTypes() {
            ResponseEntity<List<String>> response = getAsStringList("/vars/transactionFlowTypes");

            assertThat(response.getStatusCode())
                    .isEqualTo(HttpStatus.OK);
            assertThat(response.getBody())
                    .containsExactlyElementsOf(enumNames(TransactionFlowType.values()));
        }

        @Test
        void transactionPurposes() {
            ResponseEntity<List<String>> response = getAsStringList("/vars/transactionPurposes");

            assertThat(response.getStatusCode())
                    .isEqualTo(HttpStatus.OK);
            assertThat(response.getBody())
                    .containsExactlyElementsOf(enumNames(TransactionPurpose.values()));
        }

        @Test
        void transactionStatuses() {
            ResponseEntity<List<String>> response = getAsStringList("/vars/transactionStatuses");

            assertThat(response.getStatusCode())
                    .isEqualTo(HttpStatus.OK);
            assertThat(response.getBody())
                    .containsExactlyElementsOf(enumNames(TransactionStatus.values()));
        }
    }
}
