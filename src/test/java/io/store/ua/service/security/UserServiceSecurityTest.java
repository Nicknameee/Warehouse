package io.store.ua.service.security;

import io.store.ua.entity.User;
import io.store.ua.enums.UserRole;
import io.store.ua.enums.UserStatus;
import io.store.ua.exceptions.RegularAuthenticationException;
import io.store.ua.service.UserService;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

class UserServiceSecurityTest {
    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("getCurrentlyAuthenticatedUser()")
    class GetCurrentlyAuthenticatedUserTests {
        @Test
        @DisplayName("getCurrentlyAuthenticatedUser_success: returns user when principal is RegularUser and authentication is authenticated")
        void getCurrentlyAuthenticatedUser_success() {
            var user = User.builder()
                    .username(RandomStringUtils.secure().nextAlphanumeric(333))
                    .role(UserRole.MANAGER)
                    .status(UserStatus.ACTIVE)
                    .build();
            SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
            securityContext.setAuthentication(new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
            SecurityContextHolder.setContext(securityContext);

            Optional<User> result = UserService.getCurrentlyAuthenticatedUser();

            assertThat(result).isPresent();
            assertThat(result.get().getUsername()).isEqualTo(user.getUsername());
            assertThat(result.get().getRole()).isEqualTo(user.getRole());
            assertThat(result.get().getStatus()).isEqualTo(user.getStatus());
        }

        @Test
        @DisplayName("getCurrentlyAuthenticatedUser_fail_returnsEmpty_whenUnauthenticated: returns empty when authentication is unauthenticated")
        void getCurrentlyAuthenticatedUser_fail_returnsEmpty_whenUnauthenticated() {
            SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
            securityContext.setAuthentication(new UsernamePasswordAuthenticationToken(null, null, null));
            SecurityContextHolder.setContext(securityContext);

            Optional<User> result = UserService.getCurrentlyAuthenticatedUser();

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("getCurrentlyAuthenticatedUser_fail_returnsEmpty_whenPrincipalIsNotRegularUser: returns empty when principal is not RegularUser")
        void getCurrentlyAuthenticatedUser_fail_returnsEmpty_whenPrincipalIsNotRegularUser() {
            Authentication authentication =
                    new UsernamePasswordAuthenticationToken(RandomStringUtils.secure().nextAlphanumeric(3), null, List.of());
            SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
            securityContext.setAuthentication(authentication);
            SecurityContextHolder.setContext(securityContext);

            Optional<User> result = UserService.getCurrentlyAuthenticatedUser();

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("getCurrentlyAuthenticatedUser_fail_returnsEmpty_whenNoAuthentication: returns empty when SecurityContext has no authentication")
        void getCurrentlyAuthenticatedUser_fail_returnsEmpty_whenNoAuthentication() {
            SecurityContextHolder.setContext(SecurityContextHolder.createEmptyContext());

            Optional<User> result = UserService.getCurrentlyAuthenticatedUser();

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("assertAuthenticatedUserRoles(roles: List<Role>)")
    class AssertAuthenticatedUserRolesTests {
        @Test
        @DisplayName("assertAuthenticatedUserRoles: does not throw when user's role is in the allowed list")
        void assertAuthenticatedUserRoles_success() {
            var user = User.builder()
                    .username(RandomStringUtils.secure().nextAlphanumeric(333))
                    .role(UserRole.MANAGER)
                    .status(UserStatus.ACTIVE)
                    .build();
            SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
            securityContext.setAuthentication(new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
            SecurityContextHolder.setContext(securityContext);

            assertThatCode(() -> UserService.assertAuthenticatedUserRoles(List.of(UserRole.OPERATOR, UserRole.MANAGER)))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("assertAuthenticatedUserRoles: throws BusinessException when user's role is not in the allowed list")
        void assertAuthenticatedUserRoles_fail_whenRoleNotAllowed() {
            var user = User.builder()
                    .username(RandomStringUtils.secure().nextAlphanumeric(333))
                    .role(UserRole.OPERATOR)
                    .status(UserStatus.ACTIVE)
                    .build();
            SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
            securityContext.setAuthentication(new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));

            SecurityContextHolder.setContext(securityContext);

            assertThatThrownBy(() ->
                    UserService.assertAuthenticatedUserRoles(List.of(UserRole.MANAGER, UserRole.OWNER))
            ).isInstanceOf(RegularAuthenticationException.class)
                    .hasMessageContaining("User role has to be one of");
        }

        @Test
        @DisplayName("assertAuthenticatedUserRoles: throws BusinessException when there is no authenticated RegularUser")
        void assertAuthenticatedUserRoles_fail_whenNoAuthenticatedUser() {
            SecurityContextHolder.setContext(SecurityContextHolder.createEmptyContext());

            assertThatThrownBy(() ->
                    UserService.assertAuthenticatedUserRoles(List.of(UserRole.MANAGER))
            ).isInstanceOf(RegularAuthenticationException.class)
                    .hasMessageContaining("User role has to be one of");
        }
    }
}
