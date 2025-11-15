package io.store.ua.service;

import io.store.ua.entity.User;
import io.store.ua.enums.UserRole;
import io.store.ua.enums.UserStatus;
import io.store.ua.exceptions.ApplicationException;
import io.store.ua.exceptions.AuthenticationException;
import io.store.ua.exceptions.BusinessException;
import io.store.ua.mappers.UserMapper;
import io.store.ua.models.dto.UserActionResultDTO;
import io.store.ua.models.dto.UserDTO;
import io.store.ua.repository.UserRepository;
import io.store.ua.validations.FieldValidator;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.validation.ValidationException;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.TimeZone;

@Service
@Slf4j
@Validated
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final EntityManager entityManager;
    private final PasswordEncoder passwordEncoder;
    private final FieldValidator fieldValidator;

    public static void assertAuthenticatedUserRoles(List<UserRole> roles) {
        getCurrentlyAuthenticatedUser().filter(user -> roles.contains(user.getRole()))
                .orElseThrow(() -> new AuthenticationException(("User role has to be one of [%s]").formatted(roles)));
    }

    public static Optional<User> getCurrentlyAuthenticatedUser() {
        SecurityContext securityContext = SecurityContextHolder.getContext();
        if (securityContext != null) {
            Authentication authentication = securityContext.getAuthentication();

            if (authentication != null && authentication.isAuthenticated()) {
                if (authentication.getPrincipal() instanceof User) {
                    return Optional.ofNullable((User) securityContext.getAuthentication().getPrincipal());
                } else {
                    log.warn("Authentication principal is not of type RegularUser.class");
                }
            }
        }

        return Optional.empty();
    }

    public static Long getCurrentlyAuthenticatedUserID() {
        return getCurrentlyAuthenticatedUser().map(User::getId)
                .orElseThrow(() -> new AuthenticationException("User is not authenticated"));
    }

    public List<User> findByRole(@NotNull(message = "User role can't be null") UserRole role,
                                 @Min(value = 1, message = "A size of page can't be less than one") int pageSize,
                                 @Min(value = 1, message = "A number of page can't be less than one") int pageNumber) {
        return userRepository.findUsersByRole(role, Pageable.ofSize(pageSize).withPage(pageNumber - 1));
    }

    public List<User> findByStatus(@NotNull(message = "User status can't be null") UserStatus status,
                                   @Min(value = 1, message = "A size of page can't be less than one") int pageSize,
                                   @Min(value = 1, message = "A number of page can't be less than one") int pageNumber) {
        return userRepository.findUsersByStatus(status, Pageable.ofSize(pageSize).withPage(pageNumber - 1));
    }

    public List<User> findBy(String usernamePrefix,
                             String emailPart,
                             List<UserRole> roles,
                             List<UserStatus> statuses,
                             Boolean isOnline,
                             @Min(value = 1, message = "A size of page can't be less than one") int pageSize,
                             @Min(value = 1, message = "A number of page can't be less than one") int pageNumber) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<User> criteriaQuery = criteriaBuilder.createQuery(User.class);
        Root<User> root = criteriaQuery.from(User.class);

        List<Predicate> predicates = new ArrayList<>();

        if (usernamePrefix != null && !usernamePrefix.isEmpty()) {
            predicates.add(
                    criteriaBuilder.like(root.get(User.Fields.username), usernamePrefix + "%"));
        }

        if (emailPart != null && !emailPart.isEmpty()) {
            predicates.add(
                    criteriaBuilder.like(root.get(User.Fields.email), "%" + emailPart + "%"));
        }

        if (roles != null && !roles.isEmpty()) {
            predicates.add(root.get(User.Fields.role).in(roles));
        }

        if (statuses != null && !statuses.isEmpty()) {
            predicates.add(root.get(User.Fields.status).in(statuses));
        }

        if (isOnline != null) {
            if (isOnline) {
                Predicate loginNotNull = criteriaBuilder.isNotNull(root.get(User.Fields.loginTime));
                Predicate logoutNull = criteriaBuilder.isNull(root.get(User.Fields.logoutTime));
                Predicate logoutBeforeLogin = criteriaBuilder.lessThan(root.get(User.Fields.logoutTime), root.get(User.Fields.loginTime));

                predicates.add(criteriaBuilder.and(loginNotNull, criteriaBuilder.or(logoutNull, logoutBeforeLogin)));
            } else {
                Predicate loginNull = criteriaBuilder.isNull(root.get(User.Fields.loginTime));
                Predicate logoutNotNull =
                        criteriaBuilder.isNotNull(root.get(User.Fields.logoutTime));
                Predicate logoutAfterOrEqualLogin =
                        criteriaBuilder.greaterThanOrEqualTo(
                                root.get(User.Fields.logoutTime), root.get(User.Fields.loginTime));

                predicates.add(criteriaBuilder.or(loginNull, criteriaBuilder.and(logoutNotNull, logoutAfterOrEqualLogin)));
            }
        }

        criteriaQuery.where(predicates.toArray(new Predicate[0]));

        return entityManager
                .createQuery(criteriaQuery)
                .setFirstResult(pageSize * (pageNumber - 1))
                .setMaxResults(pageSize)
                .getResultList();
    }

    public Optional<User> findById(@Min(value = 1, message = "UserID can't be lower than 1") long id) {
        return userRepository.findById(id);
    }

    public Optional<User> findByEmail(@NotBlank(message = "Email can't be blank") String email) {
        return Optional.ofNullable(userRepository.findUserByEmail(email));
    }

    public Optional<User> findByUsername(@NotBlank(message = "Username can't be blank") String username) {
        return Optional.ofNullable(userRepository.findUserByUsername(username));
    }

    public List<User> saveAll(List<UserDTO> userDTOs) {
        List<User> users = new ArrayList<>();

        var currentUser = getCurrentlyAuthenticatedUser()
                .orElseThrow(() -> new AuthenticationException("User is not authenticated"));

        for (UserDTO userDTO : userDTOs) {
            fieldValidator.validate(userDTO, true,
                    UserDTO.Fields.email,
                    UserDTO.Fields.username,
                    UserDTO.Fields.role);

            User user = userMapper.toUser(userDTO);

            if (currentUser.getRole() == UserRole.MANAGER && user.getRole() != UserRole.OPERATOR) {
                throw new BusinessException("Only operator can be created by manager");
            }

            if (userRepository.existsByEmail(user.getEmail()) && !userDTO.getEmail().equals(user.getEmail())) {
                throw new ValidationException("Email %s is already taken".formatted(user.getEmail()));
            }

            if (userDTO.getStatus() != null) {
                fieldValidator.validate(userDTO, UserDTO.Fields.status, true);
                user.setStatus(UserStatus.valueOf(userDTO.getStatus()));
            }

            if (userDTO.getTimezone() != null) {
                fieldValidator.validate(userDTO, UserDTO.Fields.timezone, true);
                user.setTimezone(userDTO.getTimezone());
            } else {
                user.setTimezone(TimeZone.getTimeZone("UTC").getDisplayName());
            }

            fieldValidator.validate(userDTO, UserDTO.Fields.password, true);
            user.setPassword(passwordEncoder.encode(userDTO.getPassword()));

            users.add(user);
        }

        return userRepository.saveAll(users);
    }

    public User save(UserDTO userDTO) {
        fieldValidator.validate(userDTO, true,
                UserDTO.Fields.email,
                UserDTO.Fields.username,
                UserDTO.Fields.role,
                UserDTO.Fields.status);

        User user = userMapper.toUser(userDTO);

        var currentUser = getCurrentlyAuthenticatedUser()
                .orElseThrow(() -> new AuthenticationException("User is not authenticated"));

        if (currentUser.getRole() == UserRole.MANAGER && user.getRole() != UserRole.OPERATOR) {
            throw new BusinessException("Only operator can be created by manager");
        }

        if (userRepository.existsByEmail(user.getEmail()) && !userDTO.getEmail().equals(user.getEmail())) {
            throw new ValidationException("Email %s is already taken".formatted(user.getEmail()));
        }

        if (userDTO.getTimezone() != null) {
            fieldValidator.validate(userDTO, UserDTO.Fields.timezone, true);
            user.setTimezone(userDTO.getTimezone());
        } else {
            user.setTimezone(TimeZone.getTimeZone("UTC").getDisplayName());
        }

        fieldValidator.validate(userDTO, UserDTO.Fields.password, true);
        user.setPassword(passwordEncoder.encode(userDTO.getPassword()));

        return userRepository.save(user);
    }

    public List<UserActionResultDTO> updateAll(List<UserDTO> userDTOs) {
        List<UserActionResultDTO> results = new ArrayList<>();
        List<User> users = new ArrayList<>();
        var currentUser = getCurrentlyAuthenticatedUser()
                .orElseThrow(() -> new AuthenticationException("User is not authenticated"));

        for (UserDTO userDTO : userDTOs) {
            fieldValidator.validateObject(userDTO, UserDTO.Fields.username, true);
            Optional<User> userOptional = findByUsername(userDTO.getUsername());

            if (userOptional.isEmpty()) {
                results.add(UserActionResultDTO.builder()
                        .user(User.builder().username(userDTO.getUsername()).build())
                        .success(false)
                        .error(new UsernameNotFoundException(userDTO.getUsername()))
                        .build());
            } else {
                User user = userOptional.get();

                if (userDTO.getPassword() != null) {
                    fieldValidator.validateObject(userDTO, UserDTO.Fields.password, true);
                    user.setPassword(passwordEncoder.encode(userDTO.getPassword()));
                }

                if (userDTO.getEmail() != null) {
                    fieldValidator.validateObject(userDTO, UserDTO.Fields.email, true);
                    if (userRepository.existsByEmail(userDTO.getEmail()) && !userDTO.getEmail().equals(user.getEmail())) {
                        results.add(UserActionResultDTO.builder()
                                .user(user)
                                .success(false)
                                .error(new ValidationException("Email %s is already taken".formatted(userDTO.getEmail())))
                                .build());

                        continue;
                    }

                    user.setEmail(userDTO.getEmail());
                }

                if (userDTO.getTimezone() != null) {
                    fieldValidator.validateObject(userDTO, UserDTO.Fields.timezone, true);
                    user.setTimezone(userDTO.getTimezone());
                }

                if (userDTO.getStatus() != null) {
                    fieldValidator.validateObject(userDTO, UserDTO.Fields.status, true);
                    user.setStatus(UserStatus.valueOf(userDTO.getStatus()));
                }

                if (userDTO.getRole() != null) {
                    if (currentUser.getRole() != UserRole.OWNER) {
                        results.add(UserActionResultDTO.builder()
                                .user(user)
                                .success(false)
                                .error(new BusinessException("Only owner can change user roles"))
                                .build());

                        continue;
                    }

                    fieldValidator.validateObject(userDTO, UserDTO.Fields.role, true);
                    user.setRole(UserRole.valueOf(userDTO.getRole()));
                }

                results.add(UserActionResultDTO.builder()
                        .user(user)
                        .success(true)
                        .build());

                users.add(user);
            }
        }

        userRepository.saveAll(users);

        return results;
    }

    public User update(UserDTO userDTO) {
        fieldValidator.validateObject(userDTO, UserDTO.Fields.username, true);
        Optional<User> userOptional = findByUsername(userDTO.getUsername());

        if (userOptional.isEmpty()) {
            throw new ApplicationException("User with username %s was not found"
                    .formatted(userDTO.getUsername()), HttpStatus.NOT_FOUND);
        }

        User user = userOptional.get();

        var currentUser = getCurrentlyAuthenticatedUser()
                .orElseThrow(() -> new AuthenticationException("User is not authenticated"));

        if (userDTO.getPassword() != null) {
            fieldValidator.validate(userDTO, true, UserDTO.Fields.password, UserDTO.Fields.oldPassword);

            if (userDTO.getPassword().equals(userDTO.getOldPassword())) {
                throw new ValidationException("New password can't be the same as old password");
            }

            if (!passwordEncoder.matches(userDTO.getOldPassword(), user.getPassword())) {
                throw new ValidationException("Old password is incorrect");
            }

            user.setPassword(passwordEncoder.encode(userDTO.getPassword()));
        }

        if (userDTO.getEmail() != null) {
            fieldValidator.validate(userDTO, UserDTO.Fields.email, true);

            if (userRepository.existsByEmail(userDTO.getEmail()) && !userDTO.getEmail().equals(user.getEmail())) {
                throw new ValidationException("Email %s is already taken".formatted(userDTO.getEmail()));
            }

            user.setEmail(userDTO.getEmail());
        }

        if (userDTO.getTimezone() != null) {
            fieldValidator.validate(userDTO, UserDTO.Fields.timezone, true);
            user.setTimezone(userDTO.getTimezone());
        }

        if (userDTO.getStatus() != null) {
            fieldValidator.validate(userDTO, UserDTO.Fields.status, true);
            user.setStatus(UserStatus.valueOf(userDTO.getStatus()));
        }

        if (userDTO.getRole() != null) {
            if (currentUser.getRole() != UserRole.OWNER) {
                throw new ValidationException("Only owner can change user roles");
            }

            fieldValidator.validate(userDTO, UserDTO.Fields.role, true);
            user.setRole(UserRole.valueOf(userDTO.getRole()));
        }

        return userRepository.save(user);
    }
}
