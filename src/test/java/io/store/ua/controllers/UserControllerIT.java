package io.store.ua.controllers;

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
import java.util.Set;
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

    private HttpHeaders generateJsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.addAll(authenticationHeaders);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
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
    @DisplayName("GET /api/v1/users/findBy")
    class FindByTests {
        @Test
        @DisplayName("filters by username prefix, email part, roles, statuses and online flag")
        void findBy_success_allFiltersApplied() {
            String targetUsername = "alphaUser";
            String otherUsername = "betaUser";

            User target = userRepository.save(User.builder()
                    .username(targetUsername)
                    .password(passwordEncoder.encode(GENERATOR.nextAlphanumeric(12)))
                    .email("alpha.user@example.com")
                    .role(UserRole.MANAGER)
                    .status(UserStatus.ACTIVE)
                    .timezone("UTC")
                    .build());

            userRepository.save(User.builder()
                    .username(otherUsername)
                    .password(passwordEncoder.encode(GENERATOR.nextAlphanumeric(12)))
                    .email("other.user@other.com")
                    .role(UserRole.OPERATOR)
                    .status(UserStatus.INACTIVE)
                    .timezone("UTC")
                    .build());

            String url = UriComponentsBuilder.fromPath("/api/v1/users/findBy")
                    .queryParam("username", "alpha")
                    .queryParam("email", "example.com")
                    .queryParam("role", UserRole.MANAGER.name())
                    .queryParam("status", UserStatus.ACTIVE.name())
                    .queryParam("isOnline", false)
                    .queryParam("pageSize", 50)
                    .queryParam("page", 1)
                    .build(true)
                    .toUriString();

            ResponseEntity<List<User>> response = getList(url, new ParameterizedTypeReference<>() {});

            assertThat(response.getStatusCode())
                    .isEqualTo(HttpStatus.OK);

            List<User> users = response.getBody();

            assertThat(users)
                    .isNotNull()
                    .isNotEmpty();
            assertThat(users.stream().map(User::getId))
                    .contains(target.getId());
            users.forEach(user -> {
                assertThat(user.getUsername()).startsWith("alpha");
                assertThat(user.getEmail()).contains("example.com");
                assertThat(user.getRole()).isEqualTo(UserRole.MANAGER);
                assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
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
                    new HttpEntity<>(user, generateJsonHeaders()),
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
            var firstUser = UserDTO.builder()
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

            var otherUser = UserDTO.builder()
                    .username(GENERATOR.nextAlphabetic(10))
                    .email("%s@%s.%s".formatted(
                            GENERATOR.nextAlphabetic(10),
                            GENERATOR.nextAlphabetic(5),
                            GENERATOR.nextAlphabetic(3)))
                    .role(UserRole.OPERATOR.name())
                    .status(UserStatus.INACTIVE.name())
                    .timezone("UTC")
                    .password(GENERATOR.nextAlphanumeric(13))
                    .build();

            ResponseEntity<List<User>> response = restClient.exchange(
                    "/api/v1/users/all",
                    HttpMethod.POST,
                    new HttpEntity<>(List.of(firstUser, otherUser), generateJsonHeaders()),
                    new ParameterizedTypeReference<>() {});

            assertThat(response.getStatusCode())
                    .isEqualTo(HttpStatus.OK);

            List<User> users = response.getBody();

            assertThat(users)
                    .isNotNull();

            Set<String> createdUsernames = users.stream()
                    .map(User::getUsername)
                    .collect(Collectors.toSet());
            assertThat(createdUsernames)
                    .contains(firstUser.getUsername(), otherUser.getUsername());

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
                    .isEqualTo(UserRole.OPERATOR);
            assertThat(savedSecond.getStatus())
                    .isEqualTo(UserStatus.INACTIVE);
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

            UserDTO userDTO = UserDTO.builder()
                    .username(actualUser.getUsername())
                    .email("%s@%s.%s".formatted(
                            GENERATOR.nextAlphabetic(13),
                            GENERATOR.nextAlphabetic(5),
                            GENERATOR.nextAlphabetic(3)))
                    .role(UserRole.MANAGER.name())
                    .status(UserStatus.ACTIVE.name())
                    .timezone("Europe/Kyiv")
                    .oldPassword(password)
                    .password(GENERATOR.nextAlphanumeric(13))
                    .build();

            ResponseEntity<User> response = restClient.exchange(
                    "/api/v1/users",
                    HttpMethod.PUT,
                    new HttpEntity<>(userDTO, generateJsonHeaders()),
                    User.class);

            assertThat(response.getStatusCode())
                    .isEqualTo(HttpStatus.OK);

            User result = userRepository.findUserByUsername(userDTO.getUsername());

            assertThat(result)
                    .isNotNull();
            assertThat(result.getRole().name())
                    .isEqualTo(userDTO.getRole());
            assertThat(result.getStatus().name())
                    .isEqualTo(userDTO.getStatus());
            assertThat(result.getTimezone())
                    .isEqualTo(userDTO.getTimezone());
            assertThat(passwordEncoder.matches(userDTO.getPassword(), result.getPassword()))
                    .isTrue();
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/users/all")
    class UpdateAllUsersTests {
        @Test
        @DisplayName("updates multiple users and persists changes")
        void updateAll_success_persistsAndReturnsResults() {
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

            ResponseEntity<List<UserActionResultDTO>> updateResponse = restClient.exchange(
                    "/api/v1/users/all",
                    HttpMethod.PUT,
                    new HttpEntity<>(List.of(firstUpdateDto, otherUpdateDto), generateJsonHeaders()),
                    new ParameterizedTypeReference<>() {});

            assertThat(updateResponse.getStatusCode().is2xxSuccessful())
                    .isTrue();

            List<UserActionResultDTO> actionResults = updateResponse.getBody();

            assertThat(actionResults)
                    .isNotNull()
                    .hasSize(2);

            UserActionResultDTO firstResult = actionResults.get(0);
            UserActionResultDTO otherResult = actionResults.get(1);

            assertThat(firstResult.getSuccess())
                    .isTrue();
            assertThat(firstResult.getUser())
                    .isNotNull();
            assertThat(firstResult.getUser().getUsername())
                    .isEqualTo(firstUsername);

            assertThat(otherResult.getSuccess())
                    .isTrue();
            assertThat(otherResult.getUser())
                    .isNotNull();
            assertThat(otherResult.getUser().getUsername())
                    .isEqualTo(otherUsername);

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
