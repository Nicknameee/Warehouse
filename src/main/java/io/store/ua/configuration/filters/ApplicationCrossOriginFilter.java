package io.store.ua.configuration.filters;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApplicationCrossOriginFilter implements Filter {
    @Override
    public void doFilter(@NonNull ServletRequest request,
                         @NonNull ServletResponse response,
                         @NonNull FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String requestURI = httpRequest.getRequestURI();
        String originHeader = httpRequest.getHeader(HttpHeaders.ORIGIN);

        httpResponse.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS,
                "%s, %s, %s, %s, %s"
                        .formatted(
                                HttpMethod.GET.name(),
                                HttpMethod.POST.name(),
                                HttpMethod.PUT.name(),
                                HttpMethod.DELETE.name(),
                                HttpMethod.OPTIONS.name()));
        httpResponse.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS,
                "%s, %s".formatted(HttpHeaders.AUTHORIZATION, HttpHeaders.CONTENT_TYPE));
        httpResponse.setHeader(HttpHeaders.ACCESS_CONTROL_MAX_AGE, String.valueOf(Duration.ofHours(1L).toSeconds()));

        if (requestURI.startsWith("/socket") && originHeader != null) {
            httpResponse.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, originHeader);
            httpResponse.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
        } else {
            httpResponse.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
        }

        if (HttpMethod.OPTIONS.name().equalsIgnoreCase(httpRequest.getMethod())) {
            httpResponse.setStatus(HttpServletResponse.SC_OK);
        } else {
            chain.doFilter(request, response);
        }
    }
}
