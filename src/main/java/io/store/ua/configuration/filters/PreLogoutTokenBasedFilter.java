package io.store.ua.configuration.filters;

import io.store.ua.exceptions.ApplicationAuthenticationException;
import io.store.ua.utility.AuthenticationService;
import io.store.ua.utility.RegularObjectMapper;
import io.store.ua.utility.UserSecurityStrategyService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.GenericFilterBean;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class PreLogoutTokenBasedFilter extends GenericFilterBean {
    private final AuthenticationService authenticationService;

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;

        if (request.getRequestURI().equals("/logout")) {
            String authorizationHeaderValue = request.getHeader(HttpHeaders.AUTHORIZATION);

            if (authorizationHeaderValue != null
                    && authorizationHeaderValue.startsWith("%s ".formatted(UserSecurityStrategyService.USER_AUTHENTICATION_TYPE))) {
                String authorizationToken = authorizationHeaderValue.substring(7);
                String username = authenticationService.getUsernameFromToken(authorizationToken);
                UserDetails userDetails = authenticationService.loadUserByUsername(username);

                if (authorizationToken.isEmpty() || !authenticationService.validateToken(authorizationToken, userDetails)) {
                    processNonAuthenticatedExceptionResponse((HttpServletResponse) servletResponse);
                }
            } else {
                processNonAuthenticatedExceptionResponse((HttpServletResponse) servletResponse);
            }
        }

        filterChain.doFilter(servletRequest, servletResponse);
    }

    private void processNonAuthenticatedExceptionResponse(HttpServletResponse servletResponse) throws IOException {
        servletResponse.setContentType("application/json");
        servletResponse.setCharacterEncoding(StandardCharsets.UTF_8.name());
        servletResponse.setStatus(HttpStatus.UNAUTHORIZED.value());
        servletResponse.getWriter().write(RegularObjectMapper
                                .writeToString(new ApplicationAuthenticationException("Unauthorized, logout failed")));
        servletResponse.getWriter().flush();
    }
}
