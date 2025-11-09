package io.store.ua.configuration;

import io.store.ua.configuration.provider.UserAuthenticationProvider;
import io.store.ua.service.security.UserDetailsSecurityService;
import lombok.Data;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.session.HttpSessionEventPublisher;

import java.util.List;

@Configuration
public class ApplicationCentralSecurityConfiguration {
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    @Bean
    public SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            UserDetailsSecurityService userDetailsSecurityService,
            PasswordEncoder passwordEncoder,
            InMemoryUserDetailsManager basicUserDetailsService) {
        return new ProviderManager(List.of(
                        new UserAuthenticationProvider(userDetailsSecurityService, passwordEncoder),
                        new DaoAuthenticationProvider(basicUserDetailsService)));
    }

    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }

    @Bean
    public InMemoryUserDetailsManager basicUserDetailsService(@Qualifier("defaultUser") SpringSecurityUser defaultUser) {
        return new InMemoryUserDetailsManager(
                User.withUsername(defaultUser.getName())
                        .password(defaultUser.getPassword())
                        .roles(defaultUser.getRoles())
                        .build());
    }

    @Bean(name = "defaultUser")
    @ConfigurationProperties(prefix = "spring.security.user")
    public SpringSecurityUser defaultUser() {
        return new SpringSecurityUser();
    }

    @Data
    public static class SpringSecurityUser {
        private String name;
        private String password;
        private String[] roles;
    }
}
