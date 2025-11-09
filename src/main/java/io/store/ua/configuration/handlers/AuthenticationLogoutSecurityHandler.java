package io.store.ua.configuration.handlers;

import io.store.ua.events.LogoutEvent;
import io.store.ua.events.publishers.GenericEventPublisher;
import io.store.ua.utility.AuthenticationService;
import io.store.ua.utility.RegularObjectMapper;
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
import java.util.HashMap;
import java.util.Map;

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

            if (authorizationHeaderValue != null && authorizationHeaderValue.startsWith("Bearer ")) {
                String authorizationToken = authorizationHeaderValue.substring(7);
                String username = authenticationService.getUsernameFromToken(authorizationToken);
                UserDetails userDetails = authenticationService.loadUserByUsername(username);

                if (!authorizationToken.isEmpty() && authenticationService.validateToken(authorizationToken, userDetails, request)) {
                    authenticationService.blacklistToken(authorizationToken);

                    logoutEventEventPublisher.publishEvent(new LogoutEvent(userDetails));

                    Map<String, Object> responseBodyMap = new HashMap<>();
                    responseBodyMap.put("logout", true);
                    response.setContentType("application/json");

                    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                    response.setStatus(HttpStatus.OK.value());
                    response.getWriter().write(RegularObjectMapper.writeToString(responseBodyMap));
                    response.getWriter().flush();
                }
            }
        }
    }
}
