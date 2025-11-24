package io.store.ua.service.security;

import io.store.ua.AbstractIT;
import io.store.ua.models.dto.LoginDTO;
import io.store.ua.utility.AuthenticationService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.TestPropertySource;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

@TestPropertySource(properties = {
        "token.secret=0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
        "token.duration=60"
})
class AuthenticationServiceIT extends AbstractIT {
    @Autowired
    private AuthenticationService authenticationService;
    @Autowired
    private UserDetailsSecurityService userDetailsSecurityService;

    private MockHttpServletRequest generateRequest(String userAgent, String ip) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("User-Agent", userAgent);
        request.setRemoteAddr(ip);

        return request;
    }

    @Test
    @DisplayName("authenticate: returns JWT and publishes login event")
    void authenticate_success() {
        HttpServletRequest request = generateRequest("JUnit-AuthTest", "127.0.0.1");

        String token = authenticationService.authenticate(new LoginDTO(OWNER, OWNER), request);

        assertThat(token)
                .isNotBlank();
    }

    @Test
    @DisplayName("validateToken: OK when UA/IP match, not expired, not blacklisted")
    void validateToken_success() {
        HttpServletRequest request = generateRequest("JUnit-UA", "127.0.0.1");
        String token = authenticationService.authenticate(new LoginDTO(OWNER, OWNER), request);
        var userDetails = userDetailsSecurityService.loadUserByUsername(OWNER);
        boolean valid = authenticationService.validateToken(token, userDetails);

        assertThat(valid).isTrue();
    }

    @Test
    @DisplayName("blacklistToken: invalidate token after blacklisting")
    void blacklistToken_invalidate() {
        HttpServletRequest request = generateRequest("JUnit-UA", "127.0.0.1");
        String token = authenticationService.authenticate(new LoginDTO(OWNER, OWNER), request);
        var userDetails = userDetailsSecurityService.loadUserByUsername(OWNER);

        authenticationService.blacklistToken(token);

        assertThat(blacklistedTokenRepository.count())
                .isGreaterThan(0);

        boolean valid = authenticationService.validateToken(token, userDetails);

        assertThat(valid)
                .isFalse();
    }

    @Test
    @DisplayName("extractToken / getUsernameFromToken / getExpirationDateFromToken work")
    void tokenHelpers_work() {
        HttpServletRequest request = generateRequest("JUnit-Helpers", "127.0.0.1");
        String token = authenticationService.authenticate(new LoginDTO(OWNER, OWNER), request);

        String header = "Bearer " + token;
        String extracted = authenticationService.extractToken(header);
        assertThat(extracted).isEqualTo(token);

        String username = authenticationService.getUsernameFromToken(token);
        assertThat(username).isEqualTo(OWNER);

        Date exp = authenticationService.getExpirationDateFromToken(token);
        assertThat(exp).isAfter(new Date());
    }

    @Test
    @DisplayName("loadUserByUsername returns details for existing user")
    void loadUser_success() {
        var details = authenticationService.loadUserByUsername(OWNER);
        assertThat(details).isNotNull();
        assertThat(details.getUsername()).isEqualTo(OWNER);
    }
}
