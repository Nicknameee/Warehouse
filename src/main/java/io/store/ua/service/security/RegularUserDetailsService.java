package io.store.ua.service.security;

import io.store.ua.service.RegularUserService;
import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Service
@RequiredArgsConstructor
@Profile("users")
@Validated
public class RegularUserDetailsService implements UserDetailsService {
    private final RegularUserService regularUserService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        if (StringUtils.isBlank(username)) {
            throw new ValidationException("Username can't be blank");
        }

        return regularUserService
                .findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(username));
    }
}
