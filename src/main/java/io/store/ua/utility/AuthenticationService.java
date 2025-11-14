package io.store.ua.utility;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.store.ua.entity.cache.BlacklistedToken;
import io.store.ua.events.LoginEvent;
import io.store.ua.events.publishers.GenericEventPublisher;
import io.store.ua.exceptions.AuthenticationException;
import io.store.ua.models.dto.LoginDTO;
import io.store.ua.repository.cache.BlacklistedTokenRepository;
import io.store.ua.service.security.UserDetailsSecurityService;
import io.store.ua.validations.FieldValidator;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
@PropertySource("classpath:token.properties")
@RequiredArgsConstructor
@Validated
public class AuthenticationService {
    private final UserDetailsSecurityService userDetailsService;
    private final GenericEventPublisher<LoginEvent> loginEventPublisher;
    private final BlacklistedTokenRepository blacklistedTokenRepository;
    private final AuthenticationManager authenticationManager;
    private final FieldValidator fieldValidator;

    /**
     * JWT token validity period in seconds
     */
    @Value("${token.duration:86400}")
    private int tokenValidityDuration;

    /**
     * Secret for signing key
     */
    @Value("${token.secret}")
    private String tokenSecret;

    /**
     * Effectively final signing key
     */
    private Key key;

    @PostConstruct
    private void setKey() {
        key = new SecretKeySpec(tokenSecret.getBytes(StandardCharsets.UTF_8), SignatureAlgorithm.HS512.getJcaName());
    }

    public UserDetails loadUserByUsername(@NotBlank(message = "Username can't be blank") String username) throws UsernameNotFoundException {
        return userDetailsService.loadUserByUsername(username);
    }

    public void blacklistToken(@NotBlank(message = "Token can't be blank") String token) {
        BlacklistedToken blacklistedToken = new BlacklistedToken();
        blacklistedToken
                .setTokenId(getClaimFromToken(token, Claims::getId));
        blacklistedToken
                .setExpiryTime(Math.max(0, Duration.ofMillis(getExpirationDateFromToken(token).getTime() - System.currentTimeMillis()).toSeconds()));

        blacklistedTokenRepository.save(blacklistedToken);
    }

    public String extractToken(@NotBlank(message = "Header can't be blank") String authorizationHeader) {
        return authorizationHeader.replace("%s ".formatted(UserSecurityStrategyService.USER_AUTHENTICATION_TYPE), "");
    }

    public String getUsernameFromToken(@NotBlank(message = "Token can't be blank") String token) {
        return getClaimFromToken(token, Claims::getSubject);
    }

    public String authenticate(@NotNull(message = "Login information can't be null") LoginDTO loginDTO,
                               HttpServletRequest request) {
        fieldValidator.validate(loginDTO, true, LoginDTO.Fields.login, LoginDTO.Fields.password);
        String username = loginDTO.getLogin();
        String password = loginDTO.getPassword();

        Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, password));

        if (authentication.isAuthenticated()) {
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            String token = generateToken(userDetails, request);
            loginEventPublisher.publishEvent(new LoginEvent((UserDetails) authentication.getPrincipal()));

            return token;
        } else {
            throw new AuthenticationException("Could not authenticate given credentials");
        }
    }

    public Date getExpirationDateFromToken(@NotBlank(message = "Token can't be blank") String token) {
        return getClaimFromToken(token, Claims::getExpiration);
    }

    public boolean validateToken(@NotBlank(message = "Token can't be blank") String token,
                                 @NotNull(message = "User details can't be null") UserDetails userDetails, HttpServletRequest request) {
        return getClaimFromToken(token, Claims::getSubject).equals(userDetails.getUsername())
                && checkTokenExpiration(token)
                && checkCustomClaims(token, request)
                && !checkTokenBlacklist(token);
    }

    private String generateToken(UserDetails userDetails, HttpServletRequest request) {
        Map<String, String> claims = new HashMap<>();
        claims.put(UserSecurityStrategyService.CustomClaims.USER_AGENT, request.getHeader(HttpHeaders.USER_AGENT));
        claims.put(UserSecurityStrategyService.CustomClaims.IP, request.getRemoteAddr());

        return Jwts.builder()
                .setClaims(claims)
                .setId(RandomStringUtils.secure().nextAlphanumeric(16))
                .setSubject(userDetails.getUsername())
                .setIssuedAt(Date.from(LocalDateTime.now(Clock.systemUTC()).toInstant(ZoneOffset.UTC)))
                .setExpiration(Date.from(LocalDateTime.now(Clock.systemUTC())
                        .plusSeconds(tokenValidityDuration)
                        .toInstant(ZoneOffset.UTC)))
                .signWith(key)
                .compact();
    }

    private <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
        Claims claims = getAllClaimsFromToken(token);

        return claimsResolver.apply(claims);
    }

    private Claims getAllClaimsFromToken(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
    }

    private boolean checkCustomClaims(String token, HttpServletRequest request) {
        Claims claims = getAllClaimsFromToken(token);

        return claims
                .get(UserSecurityStrategyService.CustomClaims.USER_AGENT)
                .equals(request.getHeader(HttpHeaders.USER_AGENT))
                && claims.get(UserSecurityStrategyService.CustomClaims.IP).equals(request.getRemoteAddr());
    }

    private boolean checkTokenExpiration(String token) {
        Date now = Date.from(LocalDateTime.now(Clock.systemUTC()).toInstant(ZoneOffset.UTC));
        Date expiration = getExpirationDateFromToken(token);

        return expiration.after(now);
    }

    private boolean checkTokenBlacklist(String token) {
        return blacklistedTokenRepository.findById(getClaimFromToken(token, Claims::getId)).isPresent();
    }
}
