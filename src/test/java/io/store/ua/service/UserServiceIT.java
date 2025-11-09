package io.store.ua.service;

import io.store.ua.AbstractIT;
import io.store.ua.entity.User;
import io.store.ua.enums.UserRole;
import io.store.ua.enums.UserStatus;
import io.store.ua.models.dto.UserActionResultDTO;
import io.store.ua.models.dto.UserDTO;
import jakarta.validation.ValidationException;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserServiceIT extends AbstractIT {
    @Autowired
    private UserService userService;

    private User generateUser(UserDTO user) {
        User regularUser = new User();
        regularUser.setUsername(user.getUsername());
        regularUser.setEmail(user.getEmail());
        regularUser.setRole(UserRole.valueOf(user.getRole()));
        regularUser.setStatus(UserStatus.valueOf(user.getStatus()));
        regularUser.setPassword(passwordEncoder.encode(user.getPassword()));

        if (!StringUtils.isBlank(user.getTimezone())) {
            regularUser.setTimezone(user.getTimezone());
        } else {
            regularUser.setTimezone("UTC");
        }

        return regularUser;
    }

    private UserDTO generateUserDTO() {
        UserDTO userDTO = new UserDTO();
        userDTO.setEmail(("%s%s").formatted(RandomStringUtils.secure().nextAlphanumeric(8), "@example.aaa"));
        userDTO.setUsername(RandomStringUtils.secure().nextAlphanumeric(8));
        userDTO.setPassword(RandomStringUtils.secure().nextAlphanumeric(13));
        userDTO.setTimezone("UTC");
        userDTO.setRole(UserRole.MANAGER.name());
        userDTO.setStatus(UserStatus.ACTIVE.name());

        return userDTO;
    }

    @Nested
    @DisplayName("save(user: RegularUserDTO)")
    class SaveTests {
        @Test
        @DisplayName("save_success: save a new user")
        void save_success() {
            UserDTO userDTO = generateUserDTO();

            var userCount = userRepository.count();

            User savedUser = userService.save(userDTO);

            assertThat(savedUser.getId()).isNotNull();
            assertThat(savedUser.getUsername()).isEqualTo(userDTO.getUsername());
            assertThat(savedUser.getEmail()).isEqualTo(userDTO.getEmail());
            assertThat(savedUser.getRole()).isEqualTo(UserRole.valueOf(userDTO.getRole()));
            assertThat(savedUser.getStatus()).isEqualTo(UserStatus.valueOf(userDTO.getStatus()));
            assertThat(savedUser.getPassword()).isNotEqualTo(userDTO.getPassword());

            if (!StringUtils.isBlank(userDTO.getTimezone())) {
                assertThat(savedUser.getTimezone()).isEqualTo(userDTO.getTimezone());
            } else {
                assertThat(savedUser.getTimezone()).isEqualTo("UTC");
            }

            assertThat(userRepository.count()).isEqualTo(userCount + 1);
        }

        @ParameterizedTest(name = "save_fail_whenUsernameInvalid: username=''{0}''")
        @NullAndEmptySource
        @ValueSource(strings = {" ", "\t", "\n", "short", "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"})
        void save_fail_whenUsernameInvalid(String invalidUsername) {
            UserDTO userDTO = generateUserDTO();
            userDTO.setUsername(invalidUsername);

            assertThatThrownBy(() -> userService.save(userDTO))
                    .isInstanceOf(ValidationException.class);
        }

        @ParameterizedTest(name = "save_fail_whenEmailInvalid: email=''{0}''")
        @NullAndEmptySource
        @ValueSource(strings = {"bad", "x@y", "x@.com"})
        void save_fail_whenEmailInvalid(String invalidEmail) {
            UserDTO userDTO = generateUserDTO();
            userDTO.setEmail(invalidEmail);

            assertThatThrownBy(() -> userService.save(userDTO))
                    .isInstanceOf(ValidationException.class);
        }

        @ParameterizedTest(name = "save_fail_whenPasswordInvalid: password=''{0}''")
        @NullAndEmptySource
        @ValueSource(strings = {"short"})
        void save_fail_whenPasswordInvalid(String invalidPassword) {
            UserDTO userDTO = generateUserDTO();
            userDTO.setPassword(invalidPassword);

            assertThatThrownBy(() -> userService.save(userDTO))
                    .isInstanceOf(ValidationException.class);
        }

        @ParameterizedTest(name = "save_fail_whenRoleInvalid: role=''{0}''")
        @NullAndEmptySource
        @ValueSource(strings = {"UNKNOWN", "managerFFFFF"})
        void save_fail_whenRoleInvalid(String invalidRole) {
            UserDTO userDTO = generateUserDTO();
            userDTO.setRole(invalidRole);

            assertThatThrownBy(() -> userService.save(userDTO))
                    .isInstanceOf(ValidationException.class);
        }

        @ParameterizedTest(name = "save_fail_whenStatusInvalid: status=''{0}''")
        @NullAndEmptySource
        @ValueSource(strings = {"SLEEPING"})
        void save_fail_whenStatusInvalid(String invalidStatus) {
            UserDTO userDTO = generateUserDTO();
            userDTO.setStatus(invalidStatus);

            assertThatThrownBy(() -> userService.save(userDTO))
                    .isInstanceOf(ValidationException.class);
        }
    }

    @Nested
    @DisplayName("saveAll(users: List<RegularUserDTO>)")
    class SaveAllTests {
        @Test
        @DisplayName("saveAll_success: saves multiple users")
        void saveAll_success() {
            List<UserDTO> userDTOS = Stream.generate(UserServiceIT.this::generateUserDTO)
                    .limit(10)
                    .toList();

            var userCount = userRepository.count();

            List<User> savedUsers = userService.saveAll(userDTOS);

            assertThat(savedUsers).hasSize(10);
            assertThat(userRepository.count()).isEqualTo(userCount + savedUsers.size());
            assertThat(savedUsers)
                    .extracting(User::getUsername)
                    .containsExactlyInAnyOrderElementsOf(
                            userDTOS.stream().map(UserDTO::getUsername).toList());
        }

        @Test
        @DisplayName("saveAll_fail_whenAnyInvalid: fails fast on invalid DTO")
        void saveAll_fail_whenAnyInvalid() {
            UserDTO validUserDTO = generateUserDTO();
            UserDTO invalidUserDTO = generateUserDTO();
            invalidUserDTO.setEmail("not-an-email");

            var userCount = userRepository.count();

            assertThatThrownBy(() -> userService.saveAll(List.of(validUserDTO, invalidUserDTO)))
                    .isInstanceOf(ValidationException.class);
            assertThat(userRepository.count()).isEqualTo(userCount);
        }
    }

    @Nested
    @DisplayName("update(user: RegularUserDTO)")
    class UpdateTests {
        @Test
        @DisplayName("update_success: patches only provided fields")
        @Transactional
        void update_success() {
            User existingUser = userRepository.save(generateUser(generateUserDTO()));

            UserDTO patchUser = new UserDTO();
            patchUser.setUsername(existingUser.getUsername());
            patchUser.setPassword(RandomStringUtils.secure().nextAlphanumeric(12));
            patchUser.setTimezone("America/Los_Angeles");
            patchUser.setStatus(UserStatus.INACTIVE.name());
            patchUser.setRole(UserRole.MANAGER.name());

            User updatedUser = userService.update(patchUser);

            assertThat(updatedUser.getId()).isEqualTo(existingUser.getId());
            assertThat(updatedUser.getTimezone()).isEqualTo("America/Los_Angeles");
            assertThat(updatedUser.getStatus()).isEqualTo(UserStatus.INACTIVE);
            assertThat(updatedUser.getRole()).isEqualTo(UserRole.MANAGER);
            assertTrue(passwordEncoder.matches(patchUser.getPassword(), updatedUser.getPassword()));
        }

        @Test
        @DisplayName("update_fail_whenMissingUsername")
        void update_fail_whenMissingUsername() {
            UserDTO patchDto = new UserDTO();
            patchDto.setPassword(RandomStringUtils.secure().nextAlphanumeric(10));

            assertThatThrownBy(() -> userService.update(patchDto))
                    .isInstanceOf(ValidationException.class);
        }
    }

    @Nested
    @DisplayName("updateAll(users: List<RegularUserDTO>)")
    class UpdateAllTests {
        @Test
        @DisplayName("updateAll_mixedResults: updates existing, reports missing")
        @Transactional
        void updateAll_mixedResults() {
            User firstUser = userRepository.save(generateUser(generateUserDTO()));

            UserDTO existingUserPatch = new UserDTO();
            existingUserPatch.setUsername(firstUser.getUsername());
            existingUserPatch.setStatus(UserStatus.INACTIVE.name());

            UserDTO missingUserPatch = new UserDTO();
            String missingUsername = "missing_" + RandomStringUtils.secure().nextAlphanumeric(6);
            missingUserPatch.setUsername(missingUsername);
            missingUserPatch.setRole(UserRole.MANAGER.name());

            List<UserActionResultDTO> actionResults =
                    userService.updateAll(List.of(existingUserPatch, missingUserPatch));

            assertThat(actionResults).hasSize(2);
            long successCount = actionResults.stream().filter(UserActionResultDTO::getSuccess).count();
            assertThat(successCount).isEqualTo(1);

            User reloadedUser =
                    userRepository.findUserByUsername(firstUser.getUsername());
            assertThat(reloadedUser.getStatus()).isEqualTo(UserStatus.INACTIVE);
        }
    }

    @Nested
    @DisplayName("findBy(...) criteria")
    class DynamicFindTests {
        @Test
        @DisplayName("findBy_success: filters by username prefix, email part, roles, statuses, isOnline=false")
        @Transactional
        void findBy_success() {
            IntStream.range(0, 6)
                    .forEach(
                            i -> {
                                UserDTO userDTO = generateUserDTO();
                                String username = "user_" + RandomStringUtils.secure().nextAlphanumeric(6);
                                String email = "mail" + i + "@example.com";
                                userDTO.setUsername(username);
                                userDTO.setEmail(email);
                                userDTO.setRole(i % 2 == 0 ? UserRole.MANAGER.name() : UserRole.OPERATOR.name());
                                userDTO.setStatus(i % 3 == 0 ? UserStatus.INACTIVE.name() : UserStatus.ACTIVE.name());
                                userRepository.save(generateUser(userDTO));
                            });

            List<User> result =
                    userService.findBy(
                            "user",
                            "@example.com",
                            List.of(UserRole.MANAGER, UserRole.OPERATOR),
                            List.of(UserStatus.ACTIVE, UserStatus.INACTIVE),
                            false,
                            10,
                            1);

            assertThat(result)
                    .allSatisfy(
                            user -> {
                                assertThat(user.getUsername()).startsWith("user");
                                assertThat(user.getEmail()).contains("@example.com");
                                assertThat(Set.of(UserRole.MANAGER, UserRole.OPERATOR)).contains(user.getRole());
                                assertThat(Set.of(UserStatus.ACTIVE, UserStatus.INACTIVE)).contains(user.getStatus());
                            });
        }
    }

    @Nested
    @DisplayName("findByRole(role: Role)")
    class FindByUserRoleTests {
        @Test
        @DisplayName("findByRole_success: returns page")
        @Transactional
        void findByRole_success() {
            IntStream.range(0, 5).forEach(ignore -> {
                UserDTO dto = generateUserDTO();
                dto.setRole(UserRole.MANAGER.name());
                userRepository.save(generateUser(dto));
            });

            List<User> page = userService.findByRole(UserRole.MANAGER, 2, 1);

            assertThat(page).hasSize(2);
            assertThat(page).extracting(User::getRole).containsOnly(UserRole.MANAGER);
        }

        @ParameterizedTest(name = "findByRole_fail_whenPageSizeInvalid: {0}")
        @ValueSource(ints = {0, -1})
        void findByRole_fail_whenPageSizeInvalid(int invalidPageSize) {
            assertThatThrownBy(() -> userService.findByRole(UserRole.MANAGER, invalidPageSize, 1))
                    .isInstanceOf(ValidationException.class);
        }
    }

    @Nested
    @DisplayName("findByStatus(status: Status)")
    class FindByTransactionUserStatusTests {
        @Test
        @DisplayName("findByStatus_success: returns page")
        @Transactional
        void findByStatus_success() {
            IntStream.range(0, 5).forEach(ignore -> {
                UserDTO dto = generateUserDTO();
                dto.setStatus(UserStatus.ACTIVE.name());
                userRepository.save(generateUser(dto));
            });

            List<User> page = userService.findByStatus(UserStatus.ACTIVE, 2, 1);

            assertThat(page).hasSize(2);
            assertThat(page).extracting(User::getStatus).containsOnly(UserStatus.ACTIVE);
        }

        @ParameterizedTest(name = "findByStatus_fail_whenPageInvalid: {0}")
        @ValueSource(ints = {0, -3})
        void findByStatus_fail_whenPageInvalid(int invalidPageNumber) {
            assertThatThrownBy(() -> userService.findByStatus(UserStatus.ACTIVE, 10, invalidPageNumber))
                    .isInstanceOf(ValidationException.class);
        }
    }

    @Nested
    @DisplayName("findByEmail(email: String)")
    class FindByEmailTests {
        @Test
        void findByEmail_success() {
            User savedUser = userRepository.save(generateUser(generateUserDTO()));
            assertThat(userService.findByEmail(savedUser.getEmail())).isPresent();
        }
    }

    @Nested
    @DisplayName("findByUsername(username: String)")
    class FindByUsernameTests {
        @Test
        void findByUsername_success() {
            User savedUser = userRepository.save(generateUser(generateUserDTO()));
            assertThat(userService.findByUsername(savedUser.getUsername())).isPresent();
        }
    }

    @Nested
    @DisplayName("findById(id: long)")
    class FindByIdTests {
        @Test
        void findById_success() {
            User savedUser = userRepository.save(generateUser(generateUserDTO()));
            assertThat(userService.findById(savedUser.getId())).isPresent();
        }
    }
}
