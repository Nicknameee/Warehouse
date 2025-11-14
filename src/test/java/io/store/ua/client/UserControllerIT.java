package io.store.ua.client;

import io.store.ua.AbstractIT;
import io.store.ua.entity.User;
import io.store.ua.enums.UserRole;
import io.store.ua.enums.UserStatus;
import io.store.ua.models.dto.UserActionResultDTO;
import io.store.ua.models.dto.UserDTO;
import org.junit.jupiter.api.*;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class UserControllerIT extends AbstractIT {
    private HttpHeaders authenticationHeaders;

    @BeforeAll
    void setupAuthentication() {
        authenticationHeaders = generateAuthenticationHeaders();
    }

    @BeforeEach
    void setUsers() {
        generateUsers();
    }

    private void generateUsers() {
        userRepository.save(User.builder()
                .username(GENERATOR.nextAlphanumeric(10))
                .password(passwordEncoder.encode(GENERATOR.nextAlphanumeric(10)))
                .email("%s@example.com".formatted(GENERATOR.nextAlphanumeric(10)))
                .role(UserRole.MANAGER)
                .status(UserStatus.ACTIVE)
                .timezone("UTC")
                .build());
        userRepository.save(User.builder()
                .username(GENERATOR.nextAlphanumeric(10))
                .password(passwordEncoder.encode(GENERATOR.nextAlphanumeric(10)))
                .email("%s@example.com".formatted(GENERATOR.nextAlphanumeric(10)))
                .role(UserRole.OPERATOR)
                .status(UserStatus.ACTIVE)
                .timezone("UTC")
                .build());
        userRepository.save(User.builder()
                .username(GENERATOR.nextAlphanumeric(10))
                .password(passwordEncoder.encode(GENERATOR.nextAlphanumeric(10)))
                .email("%s@example.com".formatted(GENERATOR.nextAlphanumeric(10)))
                .role(UserRole.MANAGER)
                .status(UserStatus.INACTIVE)
                .timezone("UTC")
                .build());
        userRepository.save(User.builder()
                .username(GENERATOR.nextAlphanumeric(10))
                .password(passwordEncoder.encode(GENERATOR.nextAlphanumeric(10)))
                .email("%s@example.com".formatted(GENERATOR.nextAlphanumeric(10)))
                .role(UserRole.OPERATOR)
                .status(UserStatus.INACTIVE)
                .timezone("UTC")
                .build());
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
        void getCurrentUser_success_returnsCurrentlyAuthenticatedUser() {
            ResponseEntity<User> response = get("/api/v1/users", User.class);

            assertThat(response.getStatusCode())
                    .isEqualTo(HttpStatus.OK);
            assertThat(response.getBody())
                    .isNotNull();
            assertThat(response.getBody().getUsername())
                    .isEqualTo(owner.getUsername());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/users/findBy/role")
    class FindByRoleTests {
        @Test
        @DisplayName("role = MANAGER returns only managers")
        void findByRole_success_returnsManagersOnly() {
            String url = UriComponentsBuilder.fromPath("/api/v1/users/findBy/role")
                    .queryParam("role", UserRole.MANAGER.name())
                    .queryParam("pageSize", 50)
                    .queryParam("page", 1)
                    .build(true).toUriString();

            ResponseEntity<List<User>> response = getList(url, new ParameterizedTypeReference<>() {
            });

            assertThat(response.getStatusCode())
                    .isEqualTo(HttpStatus.OK);

            List<User> users = response.getBody();

            assertThat(users)
                    .isNotNull()
                    .isNotEmpty();

            for (User returnedUser : users) {
                assertThat(returnedUser.getRole())
                        .isEqualTo(UserRole.MANAGER);
            }
        }
    }

    @Nested
    @DisplayName("GET /api/v1/users/findBy/status")
    class FindByStatusTests {
        @Test
        @DisplayName("status = ACTIVE returns only active users")
        void findByStatus_success_returnsActiveOnly() {
            String url = UriComponentsBuilder.fromPath("/api/v1/users/findBy/status")
                    .queryParam("status", UserStatus.ACTIVE.name())
                    .queryParam("pageSize", 200)
                    .queryParam("page", 1)
                    .build(true).toUriString();

            ResponseEntity<List<User>> response = getList(url, new ParameterizedTypeReference<>() {
            });

            assertThat(response.getStatusCode())
                    .isEqualTo(HttpStatus.OK);

            List<User> users = response.getBody();

            assertThat(users)
                    .isNotNull()
                    .isNotEmpty();

            for (User returnedUser : users) {
                assertThat(returnedUser.getStatus())
                        .isEqualTo(UserStatus.ACTIVE);
            }
        }
    }

    @Nested
    @DisplayName("GET /api/v1/users/findBy")
    class FindByTests {
        @Test
        @DisplayName("filters by username prefix + roles + statuses")
        void findBy_success_filtersByUsernameRoleStatus() {
            String url = UriComponentsBuilder.fromPath("/api/v1/users/findBy")
                    .queryParam("username", "alpha")
                    .queryParam("roles", UserRole.MANAGER.name())
                    .queryParam("statuses", UserStatus.ACTIVE.name())
                    .queryParam("pageSize", 50)
                    .queryParam("page", 1)
                    .build(true).toUriString();

            ResponseEntity<List<User>> response = getList(url, new ParameterizedTypeReference<>() {
            });

            assertThat(response.getStatusCode())
                    .isEqualTo(HttpStatus.OK);

            List<User> users = response.getBody();

            assertThat(users)
                    .isNotNull();
            users.forEach(user -> {
                assertThat(user.getRole() == UserRole.MANAGER).isTrue();
                assertThat(user.getStatus() == UserStatus.ACTIVE).isTrue();
            });
        }

        @Test
        @DisplayName("filters by email part + multiple roles + multiple statuses")
        void findBy_success_filtersByEmailMultipleRolesStatuses() {
            String url = UriComponentsBuilder.fromPath("/api/v1/users/findBy")
                    .queryParam("email", "example.com")
                    .queryParam("roles", UserRole.MANAGER.name())
                    .queryParam("roles", UserRole.OPERATOR.name())
                    .queryParam("statuses", UserStatus.ACTIVE.name())
                    .queryParam("statuses", UserStatus.INACTIVE.name())
                    .queryParam("pageSize", 500)
                    .queryParam("page", 1)
                    .build(true).toUriString();

            ResponseEntity<List<User>> response = getList(url, new ParameterizedTypeReference<>() {
            });

            assertThat(response.getStatusCode())
                    .isEqualTo(HttpStatus.OK);
            List<User> users = response.getBody();

            assertThat(users)
                    .isNotNull()
                    .isNotEmpty();
            users.forEach(user -> {
                assertThat(user.getRole() == UserRole.MANAGER || user.getRole() == UserRole.OPERATOR).isTrue();
                assertThat(user.getStatus() == UserStatus.ACTIVE || user.getStatus() == UserStatus.INACTIVE).isTrue();
            });
        }
    }

    @Nested
    @DisplayName("POST /api/v1/users")
    class SaveUserTests {
        @Test
        @DisplayName("creates a user and encodes password")
        void save_success_createsUser() {
            var user = UserDTO.builder()
                    .username(GENERATOR.nextAlphabetic(10))
                    .email("%s@%s.%s".formatted(
                            GENERATOR.nextAlphabetic(10),
                            GENERATOR.nextAlphabetic(5),
                            GENERATOR.nextAlphabetic(3)))
                    .role(UserRole.MANAGER.name())
                    .status(UserStatus.ACTIVE.name())
                    .timezone("UTC")
                    .password(GENERATOR.nextAlphanumeric(13))
                    .build();

            ResponseEntity<User> saveResponse = restClient.exchange(
                    "/api/v1/users",
                    HttpMethod.POST,
                    new HttpEntity<>(user, authenticationHeaders),
                    User.class);

            assertThat(saveResponse.getStatusCode())
                    .isEqualTo(HttpStatus.OK);

            User actualUser = saveResponse.getBody();

            assertThat(actualUser)
                    .isNotNull();
            assertThat(actualUser.getUsername())
                    .isEqualTo(user.getUsername());
            assertThat(actualUser.getRole().name())
                    .isEqualTo(user.getRole());
            assertThat(actualUser.getStatus().name())
                    .isEqualTo(user.getStatus());
            assertThat(actualUser.getTimezone())
                    .isEqualTo(user.getTimezone());

            User fetchUser = userRepository.findUserByUsername(user.getUsername());

            assertThat(fetchUser)
                    .isNotNull();

            assertThat(passwordEncoder.matches(user.getPassword(), fetchUser.getPassword()))
                    .isTrue();
        }
    }

    @Nested
    @DisplayName("POST /api/v1/users/all")
    class SaveAllUsersTests {
        @Test
        @DisplayName("creates multiple users")
        void saveAll_success_createsUsers() {
            var firstUser = new UserDTO();
            firstUser.setUsername(GENERATOR.nextAlphabetic(10));
            firstUser.setEmail("%s@%s.%s".formatted(
                    GENERATOR.nextAlphabetic(10),
                    GENERATOR.nextAlphabetic(5),
                    GENERATOR.nextAlphabetic(3)));
            firstUser.setRole(UserRole.MANAGER.name());
            firstUser.setStatus(UserStatus.ACTIVE.name());
            firstUser.setTimezone("UTC");
            firstUser.setPassword(GENERATOR.nextAlphanumeric(13));

            var otherUser = new UserDTO();
            otherUser.setUsername(GENERATOR.nextAlphabetic(10));
            otherUser.setEmail("%s@%s.%s".formatted(
                    GENERATOR.nextAlphabetic(10),
                    GENERATOR.nextAlphabetic(5),
                    GENERATOR.nextAlphabetic(3)));
            otherUser.setRole(UserRole.MANAGER.name());
            otherUser.setStatus(UserStatus.ACTIVE.name());
            otherUser.setTimezone("UTC");
            otherUser.setPassword(GENERATOR.nextAlphanumeric(13));

            ResponseEntity<List<User>> response = restClient.exchange("/api/v1/users/all",
                    HttpMethod.POST,
                    new HttpEntity<>(List.of(firstUser, otherUser), authenticationHeaders),
                    new ParameterizedTypeReference<>() {
                    });

            assertThat(response.getStatusCode())
                    .isEqualTo(HttpStatus.OK);

            List<User> users = response.getBody();

            assertThat(users)
                    .isNotNull()
                    .hasSize(3 - 1);

            var createdUsernames = users
                    .stream()
                    .map(User::getUsername)
                    .collect(Collectors.toSet());
            assertThat(createdUsernames).contains(firstUser.getUsername(), otherUser.getUsername());

            User savedFirst = userRepository.findUserByUsername(firstUser.getUsername());
            User savedSecond = userRepository.findUserByUsername(otherUser.getUsername());

            assertThat(savedFirst)
                    .isNotNull();
            assertThat(savedFirst.getRole())
                    .isEqualTo(UserRole.MANAGER);
            assertThat(savedFirst.getStatus())
                    .isEqualTo(UserStatus.ACTIVE);
            assertThat(savedFirst.getTimezone())
                    .isEqualTo("UTC");
            assertThat(passwordEncoder.matches(firstUser.getPassword(), savedFirst.getPassword()))
                    .isTrue();

            assertThat(savedSecond)
                    .isNotNull();
            assertThat(savedSecond.getRole())
                    .isEqualTo(UserRole.MANAGER);
            assertThat(savedSecond.getStatus())
                    .isEqualTo(UserStatus.ACTIVE);
            assertThat(savedSecond.getTimezone())
                    .isEqualTo("UTC");
            assertThat(passwordEncoder.matches(otherUser.getPassword(), savedSecond.getPassword()))
                    .isTrue();
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/users")
    class UpdateUserTests {
        @Test
        @DisplayName("updates role, status, timezone, and password")
        void update_success_updatesSingle() {
            String password = GENERATOR.nextAlphanumeric(13);
            var actualUser = userRepository.save(User.builder()
                    .username(GENERATOR.nextAlphabetic(10))
                    .email("%s@%s.%s".formatted(
                            GENERATOR.nextAlphanumeric(10),
                            GENERATOR.nextAlphanumeric(5),
                            GENERATOR.nextAlphanumeric(3)))
                    .role(UserRole.MANAGER)
                    .status(UserStatus.ACTIVE)
                    .timezone("UTC")
                    .password(passwordEncoder.encode(password))
                    .build());

            var user = UserDTO.builder()
                    .username(actualUser.getUsername())
                    .email("%s@%s.%s".formatted(
                            GENERATOR.nextAlphanumeric(10),
                            GENERATOR.nextAlphanumeric(5),
                            GENERATOR.nextAlphanumeric(3)))
                    .role(UserRole.MANAGER.name())
                    .status(UserStatus.ACTIVE.name())
                    .timezone("UTC")
                    .oldPassword(password)
                    .password(GENERATOR.nextAlphanumeric(13))
                    .build();

            ResponseEntity<User> response = restClient.exchange("/api/v1/users",
                    HttpMethod.PUT,
                    new HttpEntity<>(user, authenticationHeaders),
                    User.class);

            assertThat(response.getStatusCode())
                    .isEqualTo(HttpStatus.OK);

            User result = userRepository.findUserByUsername(user.getUsername());

            assertThat(result)
                    .isNotNull();
            assertThat(result.getRole().name())
                    .isEqualTo(user.getRole());
            assertThat(result.getStatus().name())
                    .isEqualTo(user.getStatus());
            assertThat(result.getTimezone())
                    .isEqualTo(user.getTimezone());
            assertThat(passwordEncoder.matches(user.getPassword(), result.getPassword()))
                    .isTrue();
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/users/all")
    class UpdateAllUsersTests {
        @Test
        @DisplayName("updates multiple users with random data and returns ordered results; persists changes")
        void updateAll_success_persistsAndReturnsOrderedResults() {
            String firstUsername = GENERATOR.nextAlphabetic(10);
            String otherUsername = GENERATOR.nextAlphabetic(10);

            userRepository.save(User.builder()
                    .username(firstUsername)
                    .email("%s@%s.%s".formatted(
                            GENERATOR.nextAlphabetic(10),
                            GENERATOR.nextAlphabetic(5),
                            GENERATOR.nextAlphabetic(3)))
                    .role(UserRole.OPERATOR)
                    .status(UserStatus.ACTIVE)
                    .timezone("UTC")
                    .password(passwordEncoder.encode(GENERATOR.nextAlphanumeric(13)))
                    .build());

            userRepository.save(User.builder()
                    .username(otherUsername)
                    .email("%s@%s.%s".formatted(
                            GENERATOR.nextAlphabetic(10),
                            GENERATOR.nextAlphabetic(5),
                            GENERATOR.nextAlphabetic(3)))
                    .role(UserRole.MANAGER)
                    .status(UserStatus.INACTIVE)
                    .timezone("UTC")
                    .password(passwordEncoder.encode(GENERATOR.nextAlphanumeric(13)))
                    .build());

            String firstNewPlainPassword = GENERATOR.nextAlphanumeric(13);
            String otherNewPlainPassword = GENERATOR.nextAlphanumeric(13);

            UserDTO firstUpdateDto = UserDTO.builder()
                    .username(firstUsername)
                    .role(UserRole.MANAGER.name())
                    .status(UserStatus.INACTIVE.name())
                    .timezone("Europe/Kyiv")
                    .password(firstNewPlainPassword)
                    .build();

            UserDTO otherUpdateDto = UserDTO.builder()
                    .username(otherUsername)
                    .role(UserRole.OPERATOR.name())
                    .status(UserStatus.ACTIVE.name())
                    .timezone("Europe/Kyiv")
                    .password(otherNewPlainPassword)
                    .build();

            ResponseEntity<List<UserActionResultDTO>> updateResponse = restClient.exchange("/api/v1/users/all",
                    HttpMethod.PUT,
                    new HttpEntity<>(List.of(firstUpdateDto, otherUpdateDto), authenticationHeaders),
                    new ParameterizedTypeReference<>() {
                    });

            assertThat(updateResponse.getStatusCode().is2xxSuccessful())
                    .isTrue();

            List<UserActionResultDTO> actionResults = updateResponse.getBody();

            assertThat(actionResults).isNotNull()
                    .hasSize(3 - 1);

            UserActionResultDTO firstResult = actionResults.getFirst();
            UserActionResultDTO otherResult = actionResults.getLast();

            assertThat(firstResult.getSuccess())
                    .isTrue();
            assertThat(firstResult.getUser())
                    .isNotNull();
            assertThat(firstResult.getUser().getUsername())
                    .isEqualTo(firstUsername);
            assertThat(firstResult.getError())
                    .isNull();

            assertThat(otherResult.getSuccess())
                    .isTrue();
            assertThat(otherResult.getUser())
                    .isNotNull();
            assertThat(otherResult.getUser().getUsername())
                    .isEqualTo(otherUsername);
            assertThat(otherResult.getError())
                    .isNull();

            User firstUserAfterUpdate = userRepository.findUserByUsername(firstUsername);
            User otherUserAfterUpdate = userRepository.findUserByUsername(otherUsername);

            assertThat(firstUserAfterUpdate.getRole())
                    .isEqualTo(UserRole.MANAGER);
            assertThat(firstUserAfterUpdate.getStatus())
                    .isEqualTo(UserStatus.INACTIVE);
            assertThat(firstUserAfterUpdate.getTimezone())
                    .isEqualTo("Europe/Kyiv");
            assertThat(passwordEncoder.matches(firstNewPlainPassword, firstUserAfterUpdate.getPassword()))
                    .isTrue();

            assertThat(otherUserAfterUpdate.getRole())
                    .isEqualTo(UserRole.OPERATOR);
            assertThat(otherUserAfterUpdate.getStatus())
                    .isEqualTo(UserStatus.ACTIVE);
            assertThat(otherUserAfterUpdate.getTimezone())
                    .isEqualTo("Europe/Kyiv");
            assertThat(passwordEncoder.matches(otherNewPlainPassword, otherUserAfterUpdate.getPassword()))
                    .isTrue();
        }
    }
}
