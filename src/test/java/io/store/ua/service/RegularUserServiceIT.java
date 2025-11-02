package io.store.ua.service;

import io.store.ua.AbstractIT;
import io.store.ua.entity.RegularUser;
import io.store.ua.enums.Role;
import io.store.ua.enums.Status;
import io.store.ua.models.dto.RegularUserDTO;
import io.store.ua.models.dto.UserActionResultDTO;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegularUserServiceIT extends AbstractIT {
    @Autowired
    private RegularUserService userService;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private static List<RegularUserDTO> buildRegularUserDTOs(int count) {
        return Stream.generate(() -> {
                    RegularUserDTO regularUserDTO = new RegularUserDTO();
                    regularUserDTO.setEmail(("%s@%s").formatted(RandomStringUtils.secure().nextAlphanumeric(8), "example.com"));
                    regularUserDTO.setUsername(RandomStringUtils.secure().nextAlphanumeric(8));
                    regularUserDTO.setPassword(RandomStringUtils.secure().nextAlphanumeric(13));
                    regularUserDTO.setTimezone("UTC");
                    regularUserDTO.setRole(Role.MANAGER.name());
                    regularUserDTO.setStatus(Status.ACTIVE.name());

                    return regularUserDTO;
                })
                .limit(count)
                .toList();
    }

    private RegularUser buildRegularUser(RegularUserDTO user) {
        RegularUser regularUser = new RegularUser();
        regularUser.setUsername(user.getUsername());
        regularUser.setEmail(user.getEmail());
        regularUser.setRole(Role.valueOf(user.getRole()));
        regularUser.setStatus(Status.valueOf(user.getStatus()));
        regularUser.setPassword(passwordEncoder.encode(user.getPassword()));

        if (!StringUtils.isBlank(user.getTimezone())) {
            regularUser.setTimezone(user.getTimezone());
        } else {
            regularUser.setTimezone("UTC");
        }

        return regularUser;
    }

    private RegularUserDTO buildRegularUserDTO() {
        RegularUserDTO regularUserDTO = new RegularUserDTO();
        regularUserDTO.setEmail(("%s%s").formatted(RandomStringUtils.secure().nextAlphanumeric(8), "@example.aaa"));
        regularUserDTO.setUsername(RandomStringUtils.secure().nextAlphanumeric(8));
        regularUserDTO.setPassword(RandomStringUtils.secure().nextAlphanumeric(13));
        regularUserDTO.setTimezone("UTC");
        regularUserDTO.setRole(Role.MANAGER.name());
        regularUserDTO.setStatus(Status.ACTIVE.name());

        return regularUserDTO;
    }

    @Nested
    @DisplayName("save(user: RegularUserDTO)")
    class SaveTests {
        @Test
        @DisplayName("save_success: save a new user")
        void save_success() {
            RegularUserDTO regularUserDTO = buildRegularUserDTO();

            var userCount = userRepository.count();

            RegularUser savedUser = userService.save(regularUserDTO);

            assertThat(savedUser.getId()).isNotNull();
            assertThat(savedUser.getUsername()).isEqualTo(regularUserDTO.getUsername());
            assertThat(savedUser.getEmail()).isEqualTo(regularUserDTO.getEmail());
            assertThat(savedUser.getRole()).isEqualTo(Role.valueOf(regularUserDTO.getRole()));
            assertThat(savedUser.getStatus()).isEqualTo(Status.valueOf(regularUserDTO.getStatus()));
            assertThat(savedUser.getPassword()).isNotEqualTo(regularUserDTO.getPassword());

            if (!StringUtils.isBlank(regularUserDTO.getTimezone())) {
                assertThat(savedUser.getTimezone()).isEqualTo(regularUserDTO.getTimezone());
            } else {
                assertThat(savedUser.getTimezone()).isEqualTo("UTC");
            }

            assertThat(userRepository.count()).isEqualTo(userCount + 1);
        }

        @ParameterizedTest(name = "save_fail_whenUsernameInvalid: username=''{0}''")
        @NullAndEmptySource
        @ValueSource(strings = {" ", "\t", "\n", "short", "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"})
        void save_fail_whenUsernameInvalid(String invalidUsername) {
            RegularUserDTO regularUserDTO = buildRegularUserDTO();
            regularUserDTO.setUsername(invalidUsername);

            assertThatThrownBy(() -> userService.save(regularUserDTO))
                    .isInstanceOf(ValidationException.class);
        }

        @ParameterizedTest(name = "save_fail_whenEmailInvalid: email=''{0}''")
        @NullAndEmptySource
        @ValueSource(strings = {"bad", "x@y", "x@.com"})
        void save_fail_whenEmailInvalid(String invalidEmail) {
            RegularUserDTO regularUserDTO = buildRegularUserDTO();
            regularUserDTO.setEmail(invalidEmail);

            assertThatThrownBy(() -> userService.save(regularUserDTO))
                    .isInstanceOf(ValidationException.class);
        }

        @ParameterizedTest(name = "save_fail_whenPasswordInvalid: password=''{0}''")
        @NullAndEmptySource
        @ValueSource(strings = {"short"})
        void save_fail_whenPasswordInvalid(String invalidPassword) {
            RegularUserDTO regularUserDTO = buildRegularUserDTO();
            regularUserDTO.setPassword(invalidPassword);

            assertThatThrownBy(() -> userService.save(regularUserDTO))
                    .isInstanceOf(ValidationException.class);
        }

        @ParameterizedTest(name = "save_fail_whenRoleInvalid: role=''{0}''")
        @NullAndEmptySource
        @ValueSource(strings = {"UNKNOWN", "managerFFFFF"})
        void save_fail_whenRoleInvalid(String invalidRole) {
            RegularUserDTO regularUserDTO = buildRegularUserDTO();
            regularUserDTO.setRole(invalidRole);

            assertThatThrownBy(() -> userService.save(regularUserDTO))
                    .isInstanceOf(ValidationException.class);
        }

        @ParameterizedTest(name = "save_fail_whenStatusInvalid: status=''{0}''")
        @NullAndEmptySource
        @ValueSource(strings = {"SLEEPING"})
        void save_fail_whenStatusInvalid(String invalidStatus) {
            RegularUserDTO regularUserDTO = buildRegularUserDTO();
            regularUserDTO.setStatus(invalidStatus);

            assertThatThrownBy(() -> userService.save(regularUserDTO))
                    .isInstanceOf(ValidationException.class);
        }
    }

    @Nested
    @DisplayName("saveAll(users: List<RegularUserDTO>)")
    class SaveAllTests {
        @Test
        @DisplayName("saveAll_success: saves multiple users")
        void saveAll_success() {
            List<RegularUserDTO> regularUserDTOS = buildRegularUserDTOs(10);

            var userCount = userRepository.count();

            List<RegularUser> savedUsers = userService.saveAll(regularUserDTOS);

            assertThat(savedUsers).hasSize(10);
            assertThat(userRepository.count()).isEqualTo(userCount + savedUsers.size());
            assertThat(savedUsers)
                    .extracting(RegularUser::getUsername)
                    .containsExactlyInAnyOrderElementsOf(
                            regularUserDTOS.stream().map(RegularUserDTO::getUsername).toList());
        }

        @Test
        @DisplayName("saveAll_fail_whenAnyInvalid: fails fast on invalid DTO")
        void saveAll_fail_whenAnyInvalid() {
            RegularUserDTO validRegularUserDTO = buildRegularUserDTO();
            RegularUserDTO invalidRegularUserDTO = buildRegularUserDTO();
            invalidRegularUserDTO.setEmail("not-an-email");

            var userCount = userRepository.count();

            assertThatThrownBy(() -> userService.saveAll(List.of(validRegularUserDTO, invalidRegularUserDTO)))
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
            RegularUser existingUser = userRepository.save(buildRegularUser(buildRegularUserDTO()));

            RegularUserDTO patchUser = new RegularUserDTO();
            patchUser.setUsername(existingUser.getUsername());
            patchUser.setPassword(RandomStringUtils.secure().nextAlphanumeric(12));
            patchUser.setTimezone("America/Los_Angeles");
            patchUser.setStatus(Status.INACTIVE.name());
            patchUser.setRole(Role.MANAGER.name());

            RegularUser updatedUser = userService.update(patchUser);

            assertThat(updatedUser.getId()).isEqualTo(existingUser.getId());
            assertThat(updatedUser.getTimezone()).isEqualTo("America/Los_Angeles");
            assertThat(updatedUser.getStatus()).isEqualTo(Status.INACTIVE);
            assertThat(updatedUser.getRole()).isEqualTo(Role.MANAGER);
            assertTrue(passwordEncoder.matches(patchUser.getPassword(), updatedUser.getPassword()));
        }

        @Test
        @DisplayName("update_fail_whenMissingUsername")
        void update_fail_whenMissingUsername() {
            RegularUserDTO patchDto = new RegularUserDTO();
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
            RegularUser firstUser = userRepository.save(buildRegularUser(buildRegularUserDTO()));

            RegularUserDTO existingUserPatch = new RegularUserDTO();
            existingUserPatch.setUsername(firstUser.getUsername());
            existingUserPatch.setStatus(Status.INACTIVE.name());

            RegularUserDTO missingUserPatch = new RegularUserDTO();
            String missingUsername = "missing_" + RandomStringUtils.secure().nextAlphanumeric(6);
            missingUserPatch.setUsername(missingUsername);
            missingUserPatch.setRole(Role.MANAGER.name());

            List<UserActionResultDTO> actionResults =
                    userService.updateAll(List.of(existingUserPatch, missingUserPatch));

            assertThat(actionResults).hasSize(2);
            long successCount = actionResults.stream().filter(UserActionResultDTO::getSuccess).count();
            assertThat(successCount).isEqualTo(1);

            RegularUser reloadedUser =
                    userRepository.findRegularUserByUsername(firstUser.getUsername());
            assertThat(reloadedUser.getStatus()).isEqualTo(Status.INACTIVE);
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
                                RegularUserDTO regularUserDTO = buildRegularUserDTO();
                                String username = "user_" + RandomStringUtils.secure().nextAlphanumeric(6);
                                String email = "mail" + i + "@example.com";
                                regularUserDTO.setUsername(username);
                                regularUserDTO.setEmail(email);
                                regularUserDTO.setRole(i % 2 == 0 ? Role.MANAGER.name() : Role.OPERATOR.name());
                                regularUserDTO.setStatus(i % 3 == 0 ? Status.INACTIVE.name() : Status.ACTIVE.name());
                                userRepository.save(buildRegularUser(regularUserDTO));
                            });

            List<RegularUser> result =
                    userService.findBy(
                            "user",
                            "@example.com",
                            List.of(Role.MANAGER, Role.OPERATOR),
                            List.of(Status.ACTIVE, Status.INACTIVE),
                            false,
                            10,
                            1);

            assertThat(result)
                    .allSatisfy(
                            user -> {
                                assertThat(user.getUsername()).startsWith("user");
                                assertThat(user.getEmail()).contains("@example.com");
                                assertThat(Set.of(Role.MANAGER, Role.OPERATOR)).contains(user.getRole());
                                assertThat(Set.of(Status.ACTIVE, Status.INACTIVE)).contains(user.getStatus());
                            });
        }
    }

    @Nested
    @DisplayName("findByRole(role: Role)")
    class FindByRoleTests {
        @Test
        @DisplayName("findByRole_success: returns page")
        @Transactional
        void findByRole_success() {
            IntStream.range(0, 5).forEach(ignore -> {
                RegularUserDTO dto = buildRegularUserDTO();
                dto.setRole(Role.MANAGER.name());
                userRepository.save(buildRegularUser(dto));
            });

            List<RegularUser> page = userService.findByRole(Role.MANAGER, 2, 1);

            assertThat(page).hasSize(2);
            assertThat(page).extracting(RegularUser::getRole).containsOnly(Role.MANAGER);
        }

        @ParameterizedTest(name = "findByRole_fail_whenPageSizeInvalid: {0}")
        @ValueSource(ints = {0, -1})
        void findByRole_fail_whenPageSizeInvalid(int invalidPageSize) {
            assertThatThrownBy(() -> userService.findByRole(Role.MANAGER, invalidPageSize, 1))
                    .isInstanceOf(ValidationException.class);
        }
    }

    @Nested
    @DisplayName("findByStatus(status: Status)")
    class FindByTransactionStatusTests {
        @Test
        @DisplayName("findByStatus_success: returns page")
        @Transactional
        void findByStatus_success() {
            IntStream.range(0, 5).forEach(ignore -> {
                RegularUserDTO dto = buildRegularUserDTO();
                dto.setStatus(Status.ACTIVE.name());
                userRepository.save(buildRegularUser(dto));
            });

            List<RegularUser> page = userService.findByStatus(Status.ACTIVE, 2, 1);

            assertThat(page).hasSize(2);
            assertThat(page).extracting(RegularUser::getStatus).containsOnly(Status.ACTIVE);
        }

        @ParameterizedTest(name = "findByStatus_fail_whenPageInvalid: {0}")
        @ValueSource(ints = {0, -3})
        void findByStatus_fail_whenPageInvalid(int invalidPageNumber) {
            assertThatThrownBy(() -> userService.findByStatus(Status.ACTIVE, 10, invalidPageNumber))
                    .isInstanceOf(ValidationException.class);
        }
    }

    @Nested
    @DisplayName("findByEmail(email: String)")
    class FindByEmailTests {
        @Test
        void findByEmail_success() {
            RegularUser savedUser = userRepository.save(buildRegularUser(buildRegularUserDTO()));
            assertThat(userService.findByEmail(savedUser.getEmail())).isPresent();
        }
    }

    @Nested
    @DisplayName("findByUsername(username: String)")
    class FindByUsernameTests {
        @Test
        void findByUsername_success() {
            RegularUser savedUser = userRepository.save(buildRegularUser(buildRegularUserDTO()));
            assertThat(userService.findByUsername(savedUser.getUsername())).isPresent();
        }
    }

    @Nested
    @DisplayName("findById(id: long)")
    class FindByIdTests {
        @Test
        void findById_success() {
            RegularUser savedUser = userRepository.save(buildRegularUser(buildRegularUserDTO()));
            assertThat(userService.findById(savedUser.getId())).isPresent();
        }
    }
}
