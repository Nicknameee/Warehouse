package io.store.ua.client;

import io.store.ua.AbstractIT;
import io.store.ua.models.dto.LoginDTO;
import io.store.ua.models.dto.LoginResponseDTO;
import io.store.ua.models.dto.LogoutResponseDTO;
import io.store.ua.utility.UserSecurityStrategyService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;

import java.math.BigInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AuthenticationControllerIT extends AbstractIT {
    @Nested
    @DisplayName("POST /login")
    class LoginTests {
        @Test
        void login_success() {
            ResponseEntity<LoginResponseDTO> response = restClient.postForEntity("/login",
                    new LoginDTO(OWNER, OWNER),
                    LoginResponseDTO.class);

            assertThat(response.getStatusCode())
                    .isEqualTo(HttpStatus.OK);
            assertNotNull(response.getBody());

            var content = response.getBody();

            assertThat(content)
                    .isNotNull();
            assertThat(content.getToken())
                    .isNotBlank();
            assertThat(content.getAuthenticationType())
                    .isEqualTo(UserSecurityStrategyService.USER_AUTHENTICATION_TYPE);
            assertThat(content.getExpirationDateMs())
                    .isNotNull();
            assertThat(content.getExpirationDateMs())
                    .isInstanceOf(BigInteger.class);
            assertThat(content.getExpirationDateMs().longValue())
                    .isGreaterThan(System.currentTimeMillis());
        }

        @Test
        void login_invalid() {
            ResponseEntity<String> response = restClient.postForEntity("/login",
                    new LoginDTO(OWNER, "wrong-pass"),
                    String.class);

            assertThat(response.getStatusCode())
                    .isEqualTo(HttpStatus.UNAUTHORIZED);
        }
    }

    @Nested
    @DisplayName("POST /logout")
    class LogoutTests {
        @Test
        void logout_success() {
            ResponseEntity<LoginResponseDTO> loginResponse = restClient.postForEntity("/login",
                    new LoginDTO(OWNER, OWNER),
                    LoginResponseDTO.class);

            assertThat(loginResponse.getStatusCode())
                    .isEqualTo(HttpStatus.OK);
            assertNotNull(loginResponse.getBody());

            var content = loginResponse.getBody();

            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.AUTHORIZATION, "%s %s"
                    .formatted(UserSecurityStrategyService.USER_AUTHENTICATION_TYPE, content.getToken()));

            ResponseEntity<LogoutResponseDTO> logoutResponse = restClient.exchange("/logout",
                    HttpMethod.POST,
                    new HttpEntity<>(null, headers),
                    LogoutResponseDTO.class);

            assertThat(logoutResponse.getStatusCode())
                    .isEqualTo(HttpStatus.OK);
            assertNotNull(logoutResponse.getBody());
            assertThat(logoutResponse.getBody().isSuccess())
                    .isTrue();
        }

        @Test
        void logout_fails() {
            ResponseEntity<Void> logoutResponse = restClient.exchange("/logout",
                    HttpMethod.POST,
                    new HttpEntity<>(null, null),
                    Void.class);

            assertThat(logoutResponse.getStatusCode())
                    .isEqualTo(HttpStatus.UNAUTHORIZED);
        }
    }
}
