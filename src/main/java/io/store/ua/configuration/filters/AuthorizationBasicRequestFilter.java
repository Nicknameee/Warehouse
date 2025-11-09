package io.store.ua.configuration.filters;

import io.store.ua.exceptions.RegularAuthenticationException;
import io.store.ua.utility.RegularObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
@RequiredArgsConstructor
public class AuthorizationBasicRequestFilter extends OncePerRequestFilter {
    private final InMemoryUserDetailsManager basicUserDetailsService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {
        String authorizationHeaderValue = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authorizationHeaderValue != null && authorizationHeaderValue.startsWith("Basic ")) {
            String authorizationToken = authorizationHeaderValue.substring(6);
            try {
                String[] credentials = extractAndDecodeHeader(authorizationToken);

                if (!credentials[0].isEmpty() && SecurityContextHolder.getContext().getAuthentication() == null) {
                    UserDetails userDetails = basicUserDetailsService.loadUserByUsername(credentials[0]);

                    if (userDetails.getPassword().equals(credentials[1])) {
                        UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken =
                                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                        usernamePasswordAuthenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);
                    } else {
                        response.setContentType("application/json");
                        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                        response.setStatus(HttpStatus.UNAUTHORIZED.value());
                        response.getWriter().write(RegularObjectMapper.writeToString(new RegularAuthenticationException("Invalid credentials")));
                        response.getWriter().flush();
                    }
                }
            } catch (IllegalArgumentException e) {
                logger.error("Unable to fetch HTTP Basic Token");
            } catch (Exception e) {
                logger.error(e.getMessage());
            }
        }

        chain.doFilter(request, response);
    }

    private String[] extractAndDecodeHeader(String token) {
        String decodedString = new String(Base64.getDecoder().decode(token));

        int indexOfSplit = decodedString.indexOf(":");

        return new String[]{
                decodedString.substring(0, indexOfSplit), decodedString.substring(indexOfSplit + 1)
        };
    }
}
