package io.store.ua.configuration.filters;

import io.store.ua.exceptions.ApplicationException;
import io.store.ua.utility.RegularObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
@Order(1)
@Profile("!test")
public class RateLimitingFilter extends OncePerRequestFilter {
    private static final long INTERVAL_MS = 5_000L;
    private static final int MAX_REQUESTS = 10;
    private final ConcurrentMap<String, RequestCounter> buckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        long requestTime = System.currentTimeMillis();

        RequestCounter requestCounter = buckets.compute("%s:%s".formatted(getClientIp(request), request.getRequestURI()),
                (ignore, counter) -> {
                    if (counter == null || (requestTime - counter.initialRequestTiming) > INTERVAL_MS) {
                        return new RequestCounter(1, requestTime);
                    } else {
                        return new RequestCounter(counter.count + 1, counter.initialRequestTiming);
                    }
                });

        if (requestCounter.count > MAX_REQUESTS) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write(RegularObjectMapper.writeToString(new ApplicationException("Too many requests", HttpStatus.TOO_MANY_REQUESTS)));

            return;
        }

        filterChain.doFilter(request, response);
    }

    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");

        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }

        return request.getRemoteAddr();
    }

    private record RequestCounter(int count, long initialRequestTiming) {
    }
}
