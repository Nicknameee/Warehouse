package io.store.ua.configuration.filters;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApplicationCrossOriginFilter extends OncePerRequestFilter {
    @Override
    public void doFilterInternal(@NonNull HttpServletRequest request,
                                 @NonNull HttpServletResponse response,
                                 @NonNull FilterChain chain) throws IOException, ServletException {
        response.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS,
                "%s, %s, %s, %s, %s"
                        .formatted(
                                HttpMethod.GET.name(),
                                HttpMethod.POST.name(),
                                HttpMethod.PUT.name(),
                                HttpMethod.DELETE.name(),
                                HttpMethod.OPTIONS.name()));
        response.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS,
                "%s, %s".formatted(HttpHeaders.AUTHORIZATION, HttpHeaders.CONTENT_TYPE));
        response.setHeader(HttpHeaders.ACCESS_CONTROL_MAX_AGE, String.valueOf(Duration.ofHours(1L).toSeconds()));
        response.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*");


        if (HttpMethod.OPTIONS.name().equalsIgnoreCase(request.getMethod())) {
            response.setStatus(HttpServletResponse.SC_OK);
        } else {
            chain.doFilter(request, response);
        }
    }
}
