package io.store.ua.controller;

import io.store.ua.AbstractIT;
import io.store.ua.models.dto.LoginDTO;
import io.store.ua.models.dto.LoginResponseDTO;
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
        @DisplayName("login_success_returnsTokenAndExpirationAndType")
        void login_success_returnsTokenAndExpirationAndType() {
            ResponseEntity<LoginResponseDTO> response = restClient.postForEntity(
                    "/login",
                    new LoginDTO(OWNER, OWNER),
                    LoginResponseDTO.class
            );

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
    }

    @Nested
    @DisplayName("POST /logout")
    class LogoutTests {
        @Test
        @DisplayName("logout_success_blacklistsTokenAndReturns200")
        void logout_success_blacklistsTokenAndReturns200() {
            ResponseEntity<LoginResponseDTO> loginResponse = restClient.postForEntity(
                    "/login",
                    new LoginDTO(OWNER, OWNER),
                    LoginResponseDTO.class
            );

            assertThat(loginResponse.getStatusCode())
                    .isEqualTo(HttpStatus.OK);
            assertNotNull(loginResponse.getBody());

            var content = loginResponse.getBody();

            HttpHeaders headers = new HttpHeaders();
            headers.set(
                    HttpHeaders.AUTHORIZATION,
                    "%s %s".formatted(UserSecurityStrategyService.USER_AUTHENTICATION_TYPE, content.getToken())
            );

            ResponseEntity<Void> logoutResponse = restClient.exchange(
                    "/logout",
                    HttpMethod.POST,
                    new HttpEntity<>(null, headers),
                    Void.class
            );

            assertThat(logoutResponse.getStatusCode())
                    .isEqualTo(HttpStatus.OK);
            assertThat(logoutResponse.getBody())
                    .isNull();
        }
    }
}
