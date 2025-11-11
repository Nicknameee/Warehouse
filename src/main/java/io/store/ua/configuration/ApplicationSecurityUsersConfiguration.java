package io.store.ua.configuration;

import io.store.ua.configuration.filters.AuthorizationTokenRequestFilter;
import io.store.ua.configuration.filters.PreLogoutTokenBasedFilter;
import io.store.ua.configuration.handlers.AuthenticationLogoutSecurityHandler;
import io.store.ua.service.security.UserDetailsSecurityService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.security.web.authentication.rememberme.AbstractRememberMeServices;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true)
@RequiredArgsConstructor
public class ApplicationSecurityUsersConfiguration {
    private final AuthenticationEntryPoint authenticationFailureEntryPoint;
    private final AuthenticationLogoutSecurityHandler authenticationLogoutSecurityHandler;
    private final AuthorizationTokenRequestFilter authorizationTokenRequestFilter;
    private final PreLogoutTokenBasedFilter preLogoutTokenBasedFilter;
    private final UserDetailsSecurityService userDetailsSecurityService;
    private final AuthenticationManager authenticationManager;

    @Bean
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity httpSecurity) throws Exception {
        return httpSecurity
                .securityMatcher("/api/**", "/logout")
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(authorizationTokenRequestFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(preLogoutTokenBasedFilter, LogoutFilter.class)
                .authorizeHttpRequests(authentication -> authentication.requestMatchers("/api/**", "/logout").authenticated().anyRequest().denyAll())
                .logout(logout -> logout.logoutRequestMatcher(request ->
                                request.getRequestURI().equals("/logout") && request.getMethod().equals(HttpMethod.POST.name()))
                        .logoutSuccessHandler(authenticationLogoutSecurityHandler)
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID", AbstractRememberMeServices.SPRING_SECURITY_REMEMBER_ME_COOKIE_KEY))
                .exceptionHandling(exceptionHandlingConfigurer -> exceptionHandlingConfigurer.authenticationEntryPoint(authenticationFailureEntryPoint))
                .userDetailsService(userDetailsSecurityService)
                .authenticationManager(authenticationManager)
                .build();
    }
}
