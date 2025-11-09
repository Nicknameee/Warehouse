package io.store.ua.configuration.filters;

import io.jsonwebtoken.ExpiredJwtException;
import io.store.ua.events.LoginEvent;
import io.store.ua.events.publishers.GenericEventPublisher;
import io.store.ua.utility.AuthenticationService;
import io.store.ua.utility.UserSecurityStrategyService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class AuthorizationTokenRequestFilter extends OncePerRequestFilter {
    private final AuthenticationService authenticationService;
    private final GenericEventPublisher<LoginEvent> loginEventPublisher;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {
        String authorizationHeaderValue = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authorizationHeaderValue != null
                && authorizationHeaderValue.startsWith("%s ".formatted(UserSecurityStrategyService.USER_AUTHENTICATION_TYPE))) {
            String authorizationToken = authorizationHeaderValue.substring(UserSecurityStrategyService.USER_AUTHENTICATION_TYPE.length() + 1);
            try {
                String username = authenticationService.getUsernameFromToken(authorizationToken);

                if (!username.isEmpty() && SecurityContextHolder.getContext().getAuthentication() == null) {
                    UserDetails userDetails = authenticationService.loadUserByUsername(username);

                    if (authenticationService.validateToken(authorizationToken, userDetails, request)) {
                        UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken =
                                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                        usernamePasswordAuthenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);

                        loginEventPublisher.publishEvent(new LoginEvent(userDetails));
                    }
                }
            } catch (IllegalArgumentException e) {
                logger.error("Unable to fetch JWT Token");
            } catch (ExpiredJwtException e) {
                logger.error("JWT Token is expired");
            } catch (Exception e) {
                logger.error(e.getMessage());
            }
        }

        chain.doFilter(request, response);
    }
}
