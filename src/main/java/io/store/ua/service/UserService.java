package io.store.ua.service;

import io.store.ua.entity.User;
import io.store.ua.enums.UserRole;
import io.store.ua.enums.UserStatus;
import io.store.ua.exceptions.ApplicationException;
import io.store.ua.exceptions.RegularAuthenticationException;
import io.store.ua.mappers.UserMapper;
import io.store.ua.models.dto.UserDTO;
import io.store.ua.models.dto.UserActionResultDTO;
import io.store.ua.repository.UserRepository;
import io.store.ua.validations.FieldValidator;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
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
@PreAuthorize("isAuthenticated()")
public class UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final EntityManager entityManager;
    private final PasswordEncoder passwordEncoder;
    private final FieldValidator fieldValidator;

    public static void assertAuthenticatedUserRoles(List<UserRole> roles) {
        getCurrentlyAuthenticatedUser().filter(user -> roles.contains(user.getRole()))
                .orElseThrow(() -> new RegularAuthenticationException(("User role has to be one of [%s]").formatted(roles)));
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
                .orElseThrow(() -> new RegularAuthenticationException("User is not authenticated"));
    }

    public List<User> findByRole(@NotNull(message = "User role can't be null") UserRole role,
                                 @Min(value = 1, message = "A size of page can't be less than one") int pageSize,
                                 @Min(value = 1, message = "A number of page can't be less than one") int pageNumber) {
        return userRepository.findRegularUsersByRole(role, Pageable.ofSize(pageSize).withPage(pageNumber - 1));
    }

    public List<User> findByStatus(@NotNull(message = "User status can't be null") UserStatus status,
                                   @Min(value = 1, message = "A size of page can't be less than one") int pageSize,
                                   @Min(value = 1, message = "A number of page can't be less than one") int pageNumber) {
        return userRepository.findRegularUsersByStatus(status, Pageable.ofSize(pageSize).withPage(pageNumber - 1));
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
        return Optional.ofNullable(userRepository.findRegularUserByEmail(email));
    }

    @PreAuthorize("permitAll()")
    public Optional<User> findByUsername(@NotBlank(message = "Username can't be blank") String username) {
        return Optional.ofNullable(userRepository.findRegularUserByUsername(username));
    }

    @PreAuthorize("hasAnyAuthority('OWNER', 'MANAGER')")
    public List<User> saveAll(List<UserDTO> regularUsers) {
        List<User> users = new ArrayList<>();

        for (UserDTO regularUser : regularUsers) {
            fieldValidator.validate(regularUser, true, UserDTO.Fields.email, UserDTO.Fields.username, UserDTO.Fields.role);
            User user = userMapper.toUser(regularUser);

            if (regularUser.getStatus() != null) {
                fieldValidator.validate(regularUser, UserDTO.Fields.status, true);
                user.setStatus(UserStatus.valueOf(regularUser.getStatus()));
            }

            if (regularUser.getTimezone() != null) {
                fieldValidator.validate(regularUser, UserDTO.Fields.timezone, true);
                user.setTimezone(regularUser.getTimezone());
            } else {
                user.setTimezone(TimeZone.getTimeZone("UTC").getDisplayName());
            }

            fieldValidator.validate(regularUser, UserDTO.Fields.password, true);
            user.setPassword(passwordEncoder.encode(regularUser.getPassword()));

            users.add(user);
        }

        return userRepository.saveAll(users);
    }

    @PreAuthorize("hasAnyAuthority('OWNER', 'MANAGER')")
    public User save(UserDTO regularUser) {
        fieldValidator.validate(regularUser, true,
                UserDTO.Fields.email,
                UserDTO.Fields.username,
                UserDTO.Fields.role,
                UserDTO.Fields.status);

        User user = userMapper.toUser(regularUser);

        if (regularUser.getTimezone() != null) {
            fieldValidator.validate(regularUser, UserDTO.Fields.timezone, true);
            user.setTimezone(regularUser.getTimezone());
        } else {
            user.setTimezone(TimeZone.getTimeZone("UTC").getDisplayName());
        }

        fieldValidator.validate(regularUser, UserDTO.Fields.password, true);
        user.setPassword(passwordEncoder.encode(regularUser.getPassword()));

        return userRepository.save(user);
    }

    @PreAuthorize("hasAnyAuthority('OWNER', 'MANAGER')")
    public List<UserActionResultDTO> updateAll(List<UserDTO> regularUsers) {
        List<UserActionResultDTO> results = new ArrayList<>();
        List<User> users = new ArrayList<>();

        for (UserDTO regularUser : regularUsers) {
            fieldValidator.validateObject(regularUser, UserDTO.Fields.username, true);
            Optional<User> userOptional = findByUsername(regularUser.getUsername());

            if (userOptional.isEmpty()) {
                results.add(UserActionResultDTO.builder()
                        .user(User.builder().username(regularUser.getUsername()).build())
                        .success(false)
                        .error(new UsernameNotFoundException(regularUser.getUsername()))
                        .build());
            } else {
                User user = userOptional.get();

                if (regularUser.getPassword() != null) {
                    fieldValidator.validateObject(regularUser, UserDTO.Fields.password, true);
                    user.setPassword(passwordEncoder.encode(regularUser.getPassword()));
                }

                if (regularUser.getTimezone() != null) {
                    fieldValidator.validateObject(regularUser, UserDTO.Fields.timezone, true);
                    user.setTimezone(regularUser.getTimezone());
                }

                if (regularUser.getStatus() != null) {
                    fieldValidator.validateObject(regularUser, UserDTO.Fields.status, true);
                    user.setStatus(UserStatus.valueOf(regularUser.getStatus()));
                }

                if (regularUser.getRole() != null) {
                    fieldValidator.validateObject(regularUser, UserDTO.Fields.role, true);
                    user.setRole(UserRole.valueOf(regularUser.getRole()));
                }

                results.add(UserActionResultDTO.builder().user(user).success(true).build());

                users.add(user);
            }
        }

        userRepository.saveAll(users);

        return results;
    }

    @PreAuthorize("hasAnyAuthority('OWNER', 'MANAGER', 'OPERATOR')")
    public User update(UserDTO regularUser) {
        fieldValidator.validateObject(regularUser, UserDTO.Fields.username, true);
        Optional<User> userOptional = findByUsername(regularUser.getUsername());

        if (userOptional.isEmpty()) {
            throw new ApplicationException("User with username %s was not found"
                    .formatted(regularUser.getUsername()), HttpStatus.NOT_FOUND);
        }

        User user = userOptional.get();

        if (regularUser.getPassword() != null) {
            fieldValidator.validate(regularUser, UserDTO.Fields.password, true);
            user.setPassword(passwordEncoder.encode(regularUser.getPassword()));
        }

        if (regularUser.getTimezone() != null) {
            fieldValidator.validate(regularUser, UserDTO.Fields.timezone, true);
            user.setTimezone(regularUser.getTimezone());
        }

        if (List.of(UserRole.OWNER, UserRole.MANAGER).contains(getCurrentlyAuthenticatedUser().map(User::getRole).orElse(null))) {
            if (regularUser.getStatus() != null) {
                fieldValidator.validate(regularUser, UserDTO.Fields.status, true);
                user.setStatus(UserStatus.valueOf(regularUser.getStatus()));
            }

            if (regularUser.getRole() != null) {
                fieldValidator.validate(regularUser, UserDTO.Fields.role, true);
                user.setRole(UserRole.valueOf(regularUser.getRole()));
            }
        }

        return userRepository.save(user);
    }
}
