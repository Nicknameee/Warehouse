package io.store.ua.configuration.handlers;

import io.store.ua.events.LogoutEvent;
import io.store.ua.events.publishers.GenericEventPublisher;
import io.store.ua.models.dto.LogoutResponseDTO;
import io.store.ua.utility.AuthenticationService;
import io.store.ua.utility.RegularObjectMapper;
import io.store.ua.utility.UserSecurityStrategyService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuthenticationLogoutSecurityHandler implements LogoutSuccessHandler {
    private final AuthenticationService authenticationService;
    private final GenericEventPublisher<LogoutEvent> logoutEventEventPublisher;

    @Override
    public void onLogoutSuccess(@NonNull HttpServletRequest request,
                                @NonNull HttpServletResponse response,
                                Authentication authentication) throws IOException {
        if (request.getRequestURI().equals("/logout")) {
            String authorizationHeaderValue = request.getHeader(HttpHeaders.AUTHORIZATION);

            if (authorizationHeaderValue != null
                    && authorizationHeaderValue.startsWith("%s ".formatted(UserSecurityStrategyService.USER_AUTHENTICATION_TYPE))) {
                String authorizationToken = authorizationHeaderValue.substring(7);
                String username = authenticationService.getUsernameFromToken(authorizationToken);
                UserDetails userDetails = authenticationService.loadUserByUsername(username);

                if (!authorizationToken.isEmpty() && authenticationService.validateToken(authorizationToken, userDetails, request)) {
                    authenticationService.blacklistToken(authorizationToken);

                    logoutEventEventPublisher.publishEvent(new LogoutEvent(userDetails));

                    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                    response.setContentType("application/json");
                    response.setStatus(HttpStatus.OK.value());
                    response.getWriter()
                            .write(RegularObjectMapper.writeToString(LogoutResponseDTO.builder()
                                    .isSuccess(true)
                                    .build()));
                    response.getWriter().flush();
                }
            }
        }
    }
}
