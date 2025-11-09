package io.store.ua.client;

import io.store.ua.AbstractIT;
import io.store.ua.entity.User;
import io.store.ua.enums.UserRole;
import io.store.ua.enums.UserStatus;
import io.store.ua.models.dto.UserActionResultDTO;
import io.store.ua.models.dto.UserDTO;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.*;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class UserControllerIT extends AbstractIT {
    private HttpHeaders authenticationHeaders;

    @BeforeAll
    void setupAuthorization() {
        authenticationHeaders = generateAuthenticationHeaders();
    }

    @BeforeEach
    void prepareAuthorizationAndSeedData() {
        seedUsersForQueries();
    }

    private void seedUsersForQueries() {
        userRepository.save(
                User.builder()
                        .username("alpha_mgr")
                        .password(passwordEncoder.encode("x"))
                        .email("alpha_mgr@example.com")
                        .role(UserRole.MANAGER)
                        .status(UserStatus.ACTIVE)
                        .timezone("UTC")
                        .build()
        );
        userRepository.save(
                User.builder()
                        .username("beta_user")
                        .password(passwordEncoder.encode("x"))
                        .email("beta_user@example.com")
                        .role(UserRole.OPERATOR)
                        .status(UserStatus.ACTIVE)
                        .timezone("UTC")
                        .build()
        );
        userRepository.save(
                User.builder()
                        .username("gamma_mgr_blocked")
                        .password(passwordEncoder.encode("x"))
                        .email("gamma_mgr_blocked@example.com")
                        .role(UserRole.MANAGER)
                        .status(UserStatus.INACTIVE)
                        .timezone("UTC")
                        .build()
        );
        userRepository.save(
                User.builder()
                        .username("alpha_user_blocked")
                        .password(passwordEncoder.encode("x"))
                        .email("alpha_user_blocked@example.com")
                        .role(UserRole.OPERATOR)
                        .status(UserStatus.INACTIVE)
                        .timezone("UTC")
                        .build()
        );
    }

    private <T> ResponseEntity<T> get(String uri, Class<T> responseType) {
        HttpEntity<Void> authenticatedHttpEntity = new HttpEntity<>(authenticationHeaders);
        return restClient.exchange(uri, HttpMethod.GET, authenticatedHttpEntity, responseType);
    }

    private <T> ResponseEntity<T> getList(String uri, ParameterizedTypeReference<T> responseType) {
        HttpEntity<Void> authenticatedHttpEntity = new HttpEntity<>(authenticationHeaders);
        return restClient.exchange(uri, HttpMethod.GET, authenticatedHttpEntity, responseType);
    }

    @Nested
    @DisplayName("GET /api/v1/users")
    class GetCurrentUserTests {
        @Test
        @DisplayName("returns currently authenticated User")
        void returnsCurrentlyAuthenticatedUser() {
            ResponseEntity<User> getUserResponse = get("/api/v1/users", User.class);

            assertThat(getUserResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

            User returnedUser = getUserResponse.getBody();

            assertThat(returnedUser).isNotNull();
            assertThat(returnedUser.getUsername()).isEqualTo(owner.getUsername());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/users/findBy/role (returns List<User>)")
    class FindByRoleTests {
        @Test
        @DisplayName("role = MANAGER returns only managers")
        void returnsManagersOnly() {
            String uri = "/api/v1/users/findBy/role?role=%s&pageSize=%d&page=%d"
                    .formatted(UserRole.MANAGER.name(), 50, 1);

            ResponseEntity<List<User>> response = getList(uri, new ParameterizedTypeReference<>() {
            });

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

            List<User> users = response.getBody();

            assertThat(users).isNotNull().isNotEmpty();

            for (User returnedUser : users) {
                assertThat(returnedUser.getRole()).isEqualTo(UserRole.MANAGER);
            }

            Set<String> usernames = users.stream().map(User::getUsername).collect(Collectors.toSet());

            assertThat(usernames).contains("alpha_mgr", "gamma_mgr_blocked");
        }
    }

    @Nested
    @DisplayName("GET /api/v1/users/findBy/status (returns List<User>)")
    class FindByStatusTests {
        @Test
        @DisplayName("status = ACTIVE returns only active users")
        void returnsActiveOnly() {
            String uri = "/api/v1/users/findBy/status?status=%s&pageSize=%d&page=%d"
                    .formatted(UserStatus.ACTIVE.name(), 200, 1);

            ResponseEntity<List<User>> response = getList(uri, new ParameterizedTypeReference<>() {
            });

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

            List<User> users = response.getBody();

            assertThat(users).isNotNull().isNotEmpty();

            for (User returnedUser : users) {
                assertThat(returnedUser.getStatus()).isEqualTo(UserStatus.ACTIVE);
            }

            Set<String> usernames = users.stream().map(User::getUsername).collect(Collectors.toSet());

            assertThat(usernames).contains("alpha_mgr", "beta_user");
            assertThat(usernames).doesNotContain("gamma_mgr_blocked", "alpha_user_blocked");
        }
    }

    @Nested
    @DisplayName("GET /api/v1/users/findBy (returns List<User>)")
    class FindByTests {
        @Test
        @DisplayName("filters by username prefix + roles + statuses")
        void filtersByUsernameRoleStatus() {
            String uri = "/api/v1/users/findBy?username=%s&roles=%s&statuses=%s&pageSize=%d&page=%d"
                    .formatted("alpha", UserRole.MANAGER.name(), UserStatus.ACTIVE.name(), 50, 1);

            ResponseEntity<List<User>> response = getList(uri, new ParameterizedTypeReference<>() {
            });

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

            List<User> users = response.getBody();

            assertThat(users).isNotNull();

            Set<String> usernames = users.stream().map(User::getUsername).collect(Collectors.toSet());

            assertThat(usernames).containsExactlyInAnyOrder("alpha_mgr");
        }

        @Test
        @DisplayName("filters by email part + multiple roles + multiple statuses")
        void filtersByEmailMultipleRolesStatuses() {
            String uri = "/api/v1/users/findBy?email=%s&roles=%s&roles=%s&statuses=%s&statuses=%s&pageSize=%d&page=%d"
                    .formatted("example.com",
                            UserRole.MANAGER.name(), UserRole.OPERATOR.name(),
                            UserStatus.ACTIVE.name(), UserStatus.INACTIVE.name(),
                            500, 1
                    );

            ResponseEntity<List<User>> response = getList(uri, new ParameterizedTypeReference<>() {
            });

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

            List<User> users = response.getBody();

            assertThat(users).isNotNull().isNotEmpty();

            Set<String> usernames = users.stream().map(User::getUsername).collect(Collectors.toSet());
            assertThat(usernames).contains("alpha_mgr", "beta_user", "gamma_mgr_blocked", "alpha_user_blocked");
        }
    }

    @Nested
    @DisplayName("POST /api/v1/users (save single)")
    class SaveUserTests {
        @Test
        @DisplayName("creates a user and encodes password")
        void save_createsUser() {
            var user = UserDTO.builder()
                    .username(RandomStringUtils.secure().nextAlphabetic(10))
                    .email("%s@%s.%s".formatted(
                            RandomStringUtils.secure().nextAlphabetic(10),
                            RandomStringUtils.secure().nextAlphabetic(5),
                            RandomStringUtils.secure().nextAlphabetic(3)))
                    .role(UserRole.MANAGER.name())
                    .status(UserStatus.ACTIVE.name())
                    .timezone("UTC")
                    .password(RandomStringUtils.secure().nextAlphanumeric(13))
                    .build();

            ResponseEntity<User> saveResponse = restClient.exchange("/api/v1/users",
                    HttpMethod.POST,
                    new HttpEntity<>(user, authenticationHeaders),
                    User.class);

            assertThat(saveResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

            User actualUser = saveResponse.getBody();

            assertThat(actualUser).isNotNull();
            assertThat(actualUser.getUsername()).isEqualTo(user.getUsername());
            assertThat(actualUser.getRole().name()).isEqualTo(user.getRole());
            assertThat(actualUser.getStatus().name()).isEqualTo(user.getStatus());
            assertThat(actualUser.getTimezone()).isEqualTo(user.getTimezone());

            User fetchUser = userRepository.findUserByUsername(user.getUsername());

            assertThat(fetchUser).isNotNull();
            assertThat(passwordEncoder.matches(user.getPassword(), fetchUser.getPassword())).isTrue();
        }
    }

    @Nested
    @DisplayName("POST /api/v1/users/all (save multiple)")
    class SaveAllUsersTests {
        @Test
        @DisplayName("creates multiple users")
        void saveAll_createsUsers() {
            var firstUser = new UserDTO();
            firstUser.setUsername(RandomStringUtils.secure().nextAlphabetic(10));
            firstUser.setEmail("%s@%s.%s".formatted(RandomStringUtils.secure().nextAlphabetic(10),
                    RandomStringUtils.secure().nextAlphabetic(5),
                    RandomStringUtils.secure().nextAlphabetic(3)));
            firstUser.setRole(UserRole.MANAGER.name());
            firstUser.setStatus(UserStatus.ACTIVE.name());
            firstUser.setTimezone("UTC");
            firstUser.setPassword(RandomStringUtils.secure().nextAlphanumeric(13));

            var otherUser = new UserDTO();
            otherUser.setUsername(RandomStringUtils.secure().nextAlphabetic(10));
            otherUser.setEmail("%s@%s.%s".formatted(RandomStringUtils.secure().nextAlphabetic(10),
                    RandomStringUtils.secure().nextAlphabetic(5),
                    RandomStringUtils.secure().nextAlphabetic(3)));
            otherUser.setRole(UserRole.MANAGER.name());
            otherUser.setStatus(UserStatus.ACTIVE.name());
            otherUser.setTimezone("UTC");
            otherUser.setPassword(RandomStringUtils.secure().nextAlphanumeric(13));

            ResponseEntity<List<User>> response = restClient.exchange("/api/v1/users/all",
                    HttpMethod.POST,
                    new HttpEntity<>(List.of(firstUser, otherUser), authenticationHeaders),
                    new ParameterizedTypeReference<>() {
                    }
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

            List<User> created = response.getBody();
            assertThat(created).isNotNull().hasSize(2);

            var createdUsernames = created.stream().map(User::getUsername).collect(Collectors.toSet());
            assertThat(createdUsernames).contains(firstUser.getUsername(), otherUser.getUsername());

            User savedFirst = userRepository.findUserByUsername(firstUser.getUsername());
            User savedSecond = userRepository.findUserByUsername(otherUser.getUsername());

            assertThat(savedFirst).isNotNull();
            assertThat(savedFirst.getRole()).isEqualTo(UserRole.MANAGER);
            assertThat(savedFirst.getStatus()).isEqualTo(UserStatus.ACTIVE);
            assertThat(savedFirst.getTimezone()).isEqualTo("UTC");
            assertThat(passwordEncoder.matches(firstUser.getPassword(), savedFirst.getPassword())).isTrue();

            assertThat(savedSecond).isNotNull();
            assertThat(savedSecond.getRole()).isEqualTo(UserRole.MANAGER);
            assertThat(savedSecond.getStatus()).isEqualTo(UserStatus.ACTIVE);
            assertThat(savedSecond.getTimezone()).isEqualTo("UTC");
            assertThat(passwordEncoder.matches(otherUser.getPassword(), savedSecond.getPassword())).isTrue();
        }

    }

    @Nested
    @DisplayName("PUT /api/v1/users (update single)")
    class UpdateUserTests {
        @Test
        @DisplayName("updates role, status, timezone, and password")
        void update_updatesSingle() {
            var actualUser = userRepository.save(User.builder()
                    .username(RandomStringUtils.secure().nextAlphabetic(10))
                    .email("%s@%s.%s".formatted(
                            RandomStringUtils.secure().nextAlphanumeric(10),
                            RandomStringUtils.secure().nextAlphanumeric(5),
                            RandomStringUtils.secure().nextAlphanumeric(3)))
                    .role(UserRole.MANAGER)
                    .status(UserStatus.ACTIVE)
                    .timezone("UTC")
                    .password(RandomStringUtils.secure().nextAlphanumeric(13))
                    .build());

            var user = UserDTO.builder()
                    .username(actualUser.getUsername())
                    .email("%s@%s.%s".formatted(
                            RandomStringUtils.secure().nextAlphanumeric(10),
                            RandomStringUtils.secure().nextAlphanumeric(5),
                            RandomStringUtils.secure().nextAlphanumeric(3)))
                    .role(UserRole.MANAGER.name())
                    .status(UserStatus.ACTIVE.name())
                    .timezone("UTC")
                    .password(RandomStringUtils.secure().nextAlphanumeric(13))
                    .build();

            ResponseEntity<User> response = restClient.exchange("/api/v1/users", HttpMethod.PUT, new HttpEntity<>(user, authenticationHeaders), User.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

            User result = userRepository.findUserByUsername(user.getUsername());

            assertThat(result).isNotNull();
            assertThat(result.getRole().name()).isEqualTo(user.getRole());
            assertThat(result.getStatus().name()).isEqualTo(user.getStatus());
            assertThat(result.getTimezone()).isEqualTo(user.getTimezone());
            assertThat(passwordEncoder.matches(user.getPassword(), result.getPassword())).isTrue();
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/users/all (update multiple)")
    class UpdateAllUsersTests {
        @Test
        @DisplayName("updates multiple users with random data and returns ordered results; persists changes")
        void updateMultipleUsers_persistsAndReturnsOrderedResults() {
            String firstUsername = RandomStringUtils.secure().nextAlphabetic(10);
            String secondUsername = RandomStringUtils.secure().nextAlphabetic(10);

            userRepository.save(User.builder()
                    .username(firstUsername)
                    .email("%s@%s.%s".formatted(
                            RandomStringUtils.secure().nextAlphabetic(10),
                            RandomStringUtils.secure().nextAlphabetic(5),
                            RandomStringUtils.secure().nextAlphabetic(3)))
                    .role(UserRole.OPERATOR)
                    .status(UserStatus.ACTIVE)
                    .timezone("UTC")
                    .password(passwordEncoder.encode(RandomStringUtils.secure().nextAlphanumeric(13)))
                    .build());

            userRepository.save(User.builder()
                    .username(secondUsername)
                    .email("%s@%s.%s".formatted(
                            RandomStringUtils.secure().nextAlphabetic(10),
                            RandomStringUtils.secure().nextAlphabetic(5),
                            RandomStringUtils.secure().nextAlphabetic(3)))
                    .role(UserRole.MANAGER)
                    .status(UserStatus.INACTIVE)
                    .timezone("UTC")
                    .password(passwordEncoder.encode(RandomStringUtils.secure().nextAlphanumeric(13)))
                    .build());

            String firstNewPlainPassword = RandomStringUtils.secure().nextAlphanumeric(13);
            String secondNewPlainPassword = RandomStringUtils.secure().nextAlphanumeric(13);

            UserDTO firstUpdateDto = UserDTO.builder()
                    .username(firstUsername)
                    .role(UserRole.MANAGER.name())
                    .status(UserStatus.INACTIVE.name())
                    .timezone("Europe/Kyiv")
                    .password(firstNewPlainPassword)
                    .build();

            UserDTO secondUpdateDto = UserDTO.builder()
                    .username(secondUsername)
                    .role(UserRole.OPERATOR.name())
                    .status(UserStatus.ACTIVE.name())
                    .timezone("Europe/Kyiv")
                    .password(secondNewPlainPassword)
                    .build();

            ResponseEntity<List<UserActionResultDTO>> updateResponse = restClient.exchange(
                    "/api/v1/users/all",
                    HttpMethod.PUT,
                    new HttpEntity<>(List.of(firstUpdateDto, secondUpdateDto), authenticationHeaders),
                    new ParameterizedTypeReference<>() {
                    }
            );

            assertThat(updateResponse.getStatusCode().is2xxSuccessful()).isTrue();

            List<UserActionResultDTO> actionResults = updateResponse.getBody();
            assertThat(actionResults).isNotNull().hasSize(2);

            UserActionResultDTO firstResult = actionResults.get(0);
            UserActionResultDTO secondResult = actionResults.get(1);

            assertThat(firstResult.getSuccess()).isTrue();
            assertThat(firstResult.getUser()).isNotNull();
            assertThat(firstResult.getUser().getUsername()).isEqualTo(firstUsername);
            assertThat(firstResult.getError()).isNull();

            assertThat(secondResult.getSuccess()).isTrue();
            assertThat(secondResult.getUser()).isNotNull();
            assertThat(secondResult.getUser().getUsername()).isEqualTo(secondUsername);
            assertThat(secondResult.getError()).isNull();

            User firstUserAfterUpdate = userRepository.findUserByUsername(firstUsername);
            User secondUserAfterUpdate = userRepository.findUserByUsername(secondUsername);

            assertThat(firstUserAfterUpdate.getRole()).isEqualTo(UserRole.MANAGER);
            assertThat(firstUserAfterUpdate.getStatus()).isEqualTo(UserStatus.INACTIVE);
            assertThat(firstUserAfterUpdate.getTimezone()).isEqualTo("Europe/Kyiv");
            assertThat(passwordEncoder.matches(firstNewPlainPassword, firstUserAfterUpdate.getPassword())).isTrue();

            assertThat(secondUserAfterUpdate.getRole()).isEqualTo(UserRole.OPERATOR);
            assertThat(secondUserAfterUpdate.getStatus()).isEqualTo(UserStatus.ACTIVE);
            assertThat(secondUserAfterUpdate.getTimezone()).isEqualTo("Europe/Kyiv");
            assertThat(passwordEncoder.matches(secondNewPlainPassword, secondUserAfterUpdate.getPassword())).isTrue();
        }

    }
}

