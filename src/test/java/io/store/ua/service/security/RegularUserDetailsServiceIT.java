package io.store.ua.service.security;

import io.store.ua.AbstractIT;
import io.store.ua.entity.RegularUser;
import io.store.ua.enums.Role;
import io.store.ua.enums.Status;
import jakarta.validation.ValidationException;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

class RegularUserDetailsServiceIT extends AbstractIT {
    @Autowired
    private RegularUserDetailsService regularUserDetailsService;

    @Test
    @DisplayName("loads user by username when user exists")
    void loadUserByUsername_success() {
        var user = userRepository.save(
                RegularUser.builder()
                        .username(RandomStringUtils.secure().nextAlphanumeric(333))
                        .email("any@gmail.com")
                        .password(RandomStringUtils.secure().nextAlphanumeric(333))
                        .status(Status.ACTIVE)
                        .role(Role.OWNER)
                        .timezone("UTC")
                        .build()
        );

        UserDetails userDetails = regularUserDetailsService.loadUserByUsername(user.getUsername());

        assertNotNull(userDetails);
        assertEquals(user.getUsername(), userDetails.getUsername());
        assertTrue(userDetails.isAccountNonExpired());
        assertTrue(userDetails.isAccountNonLocked());
        assertTrue(userDetails.isCredentialsNonExpired());
        assertTrue(userDetails.isEnabled());
    }

    @Test
    @DisplayName("throws UsernameNotFoundException when user not found")
    void loadUserByUsername_fail_shouldThrowException_whenUserNotFound() {
        assertThrows(UsernameNotFoundException.class,
                () -> regularUserDetailsService.loadUserByUsername(RandomStringUtils.secure().nextAlphanumeric(333)));
    }

    @ParameterizedTest(name = "rejects blank username: \"{0}\"")
    @NullAndEmptySource
    void loadUserByUsername_fail_shouldRejectBlankUsername(String username) {
        assertThatThrownBy(() -> regularUserDetailsService.loadUserByUsername(username))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Username can't be blank");
    }
}
