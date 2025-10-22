package io.store.ua.service;

import io.store.ua.entity.RegularUser;
import io.store.ua.enums.Role;
import io.store.ua.enums.Status;
import io.store.ua.exceptions.ApplicationException;
import io.store.ua.exceptions.RegularAuthenticationException;
import io.store.ua.mappers.RegularUserMapper;
import io.store.ua.models.dto.RegularUserDTO;
import io.store.ua.models.dto.UserActionResultDTO;
import io.store.ua.repository.RegularUserRepository;
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
import org.springframework.context.annotation.Profile;
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
@Profile("users")
@Validated
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class RegularUserService {
    private final RegularUserRepository regularUserRepository;
    private final RegularUserMapper regularUserMapper;
    private final EntityManager entityManager;
    private final PasswordEncoder passwordEncoder;
    private final FieldValidator fieldValidator;

    public static void assertAuthenticatedUserRoles(List<Role> roles) {
        getCurrentlyAuthenticatedUser().filter(user -> roles.contains(user.getRole()))
                .orElseThrow(() -> new RegularAuthenticationException(("User role has to be one of [%s]").formatted(roles)));
    }

    public static Optional<RegularUser> getCurrentlyAuthenticatedUser() {
        SecurityContext securityContext = SecurityContextHolder.getContext();
        if (securityContext != null) {
            Authentication authentication = securityContext.getAuthentication();

            if (authentication != null && authentication.isAuthenticated()) {
                if (authentication.getPrincipal() instanceof RegularUser) {
                    return Optional.ofNullable((RegularUser) securityContext.getAuthentication().getPrincipal());
                } else {
                    log.warn("Authentication principal is not of type RegularUser.class");
                }
            }
        }

        return Optional.empty();
    }

    public static Long getCurrentlyAuthenticatedUserID() {
        return getCurrentlyAuthenticatedUser().map(RegularUser::getId).orElseThrow(() -> new RegularAuthenticationException("User is not authenticated"));
    }

    public List<RegularUser> findByRole(@NotNull(message = "User role can't be null") Role role,
                                        @Min(value = 1, message = "A size of page can't be less than one") int pageSize,
                                        @Min(value = 1, message = "A number of page can't be less than one") int pageNumber) {
        return regularUserRepository.findRegularUsersByRole(
                role, Pageable.ofSize(pageSize).withPage(pageNumber - 1));
    }

    public List<RegularUser> findByStatus(@NotNull(message = "User status can't be null") Status status,
                                          @Min(value = 1, message = "A size of page can't be less than one") int pageSize,
                                          @Min(value = 1, message = "A number of page can't be less than one") int pageNumber) {
        return regularUserRepository.findRegularUsersByStatus(
                status, Pageable.ofSize(pageSize).withPage(pageNumber - 1));
    }

    public List<RegularUser> findBy(String usernamePrefix,
                                    String emailPart,
                                    List<Role> roles,
                                    List<Status> statuses,
                                    Boolean isOnline,
                                    @Min(value = 1, message = "A size of page can't be less than one") int pageSize,
                                    @Min(value = 1, message = "A number of page can't be less than one") int pageNumber) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<RegularUser> criteriaQuery = criteriaBuilder.createQuery(RegularUser.class);
        Root<RegularUser> root = criteriaQuery.from(RegularUser.class);

        List<Predicate> predicates = new ArrayList<>();

        if (usernamePrefix != null && !usernamePrefix.isEmpty()) {
            predicates.add(
                    criteriaBuilder.like(root.get(RegularUser.Fields.username), usernamePrefix + "%"));
        }

        if (emailPart != null && !emailPart.isEmpty()) {
            predicates.add(
                    criteriaBuilder.like(root.get(RegularUser.Fields.email), "%" + emailPart + "%"));
        }

        if (roles != null && !roles.isEmpty()) {
            predicates.add(root.get(RegularUser.Fields.role).in(roles));
        }

        if (statuses != null && !statuses.isEmpty()) {
            predicates.add(root.get(RegularUser.Fields.status).in(statuses));
        }

        if (isOnline != null) {
            if (isOnline) {
                Predicate loginNotNull = criteriaBuilder.isNotNull(root.get(RegularUser.Fields.loginTime));
                Predicate logoutNull = criteriaBuilder.isNull(root.get(RegularUser.Fields.logoutTime));
                Predicate logoutBeforeLogin =
                        criteriaBuilder.lessThan(
                                root.get(RegularUser.Fields.logoutTime), root.get(RegularUser.Fields.loginTime));

                predicates.add(
                        criteriaBuilder.and(loginNotNull, criteriaBuilder.or(logoutNull, logoutBeforeLogin)));
            } else {
                Predicate loginNull = criteriaBuilder.isNull(root.get(RegularUser.Fields.loginTime));
                Predicate logoutNotNull =
                        criteriaBuilder.isNotNull(root.get(RegularUser.Fields.logoutTime));
                Predicate logoutAfterOrEqualLogin =
                        criteriaBuilder.greaterThanOrEqualTo(
                                root.get(RegularUser.Fields.logoutTime), root.get(RegularUser.Fields.loginTime));

                predicates.add(
                        criteriaBuilder.or(
                                loginNull, criteriaBuilder.and(logoutNotNull, logoutAfterOrEqualLogin)));
            }
        }

        criteriaQuery.where(predicates.toArray(new Predicate[0]));

        return entityManager
                .createQuery(criteriaQuery)
                .setFirstResult(pageSize * (pageNumber - 1))
                .setMaxResults(pageSize)
                .getResultList();
    }

    public Optional<RegularUser> findById(@Min(value = 1, message = "UserID can't be lower than 1") long id) {
        return regularUserRepository.findById(id);
    }

    public Optional<RegularUser> findByEmail(@NotBlank(message = "Email can't be blank") String email) {
        return Optional.ofNullable(regularUserRepository.findRegularUserByEmail(email));
    }

    @PreAuthorize("permitAll()")
    public Optional<RegularUser> findByUsername(@NotBlank(message = "Username can't be blank") String username) {
        return Optional.ofNullable(regularUserRepository.findRegularUserByUsername(username));
    }

    @PreAuthorize("hasAnyAuthority('OWNER', 'MANAGER')")
    public List<RegularUser> saveAll(List<RegularUserDTO> regularUsers) {
        List<RegularUser> users = new ArrayList<>();

        for (RegularUserDTO regularUser : regularUsers) {
            fieldValidator.validate(regularUser, true, RegularUserDTO.Fields.email, RegularUserDTO.Fields.username, RegularUserDTO.Fields.role);
            RegularUser user = regularUserMapper.toRegularUser(regularUser);

            if (regularUser.getStatus() != null) {
                fieldValidator.validate(regularUser, RegularUserDTO.Fields.status, true);
                user.setStatus(Status.valueOf(regularUser.getStatus()));
            }

            if (regularUser.getTimezone() != null) {
                fieldValidator.validate(regularUser, RegularUserDTO.Fields.timezone, true);
                user.setTimezone(regularUser.getTimezone());
            } else {
                user.setTimezone(TimeZone.getTimeZone("UTC").getDisplayName());
            }

            fieldValidator.validate(regularUser, RegularUserDTO.Fields.password, true);
            user.setPassword(passwordEncoder.encode(regularUser.getPassword()));

            users.add(user);
        }

        return regularUserRepository.saveAll(users);
    }

    @PreAuthorize("hasAnyAuthority('OWNER', 'MANAGER')")
    public RegularUser save(RegularUserDTO regularUser) {
        fieldValidator.validate(regularUser, true, RegularUserDTO.Fields.email, RegularUserDTO.Fields.username, RegularUserDTO.Fields.role, RegularUserDTO.Fields.status);
        RegularUser user = regularUserMapper.toRegularUser(regularUser);

        if (regularUser.getTimezone() != null) {
            fieldValidator.validate(regularUser, RegularUserDTO.Fields.timezone, true);
            user.setTimezone(regularUser.getTimezone());
        } else {
            user.setTimezone(TimeZone.getTimeZone("UTC").getDisplayName());
        }

        fieldValidator.validate(regularUser, RegularUserDTO.Fields.password, true);
        user.setPassword(passwordEncoder.encode(regularUser.getPassword()));

        return regularUserRepository.save(user);
    }

    @PreAuthorize("hasAnyAuthority('OWNER', 'MANAGER')")
    public List<UserActionResultDTO> updateAll(List<RegularUserDTO> regularUsers) {
        List<UserActionResultDTO> results = new ArrayList<>();
        List<RegularUser> users = new ArrayList<>();

        for (RegularUserDTO regularUser : regularUsers) {
            fieldValidator.validateObject(regularUser, RegularUserDTO.Fields.username, true);
            Optional<RegularUser> userOptional = findByUsername(regularUser.getUsername());

            if (userOptional.isEmpty()) {
                results.add(
                        UserActionResultDTO.builder()
                                .regularUser(RegularUser.builder().username(regularUser.getUsername()).build())
                                .success(false)
                                .error(new UsernameNotFoundException(regularUser.getUsername()))
                                .build());
            } else {
                RegularUser user = userOptional.get();

                if (regularUser.getPassword() != null) {
                    fieldValidator.validateObject(regularUser, RegularUserDTO.Fields.password, true);
                    user.setPassword(passwordEncoder.encode(regularUser.getPassword()));
                }

                if (regularUser.getTimezone() != null) {
                    fieldValidator.validateObject(regularUser, RegularUserDTO.Fields.timezone, true);
                    user.setTimezone(regularUser.getTimezone());
                }

                if (regularUser.getStatus() != null) {
                    fieldValidator.validateObject(regularUser, RegularUserDTO.Fields.status, true);
                    user.setStatus(Status.valueOf(regularUser.getStatus()));
                }

                if (regularUser.getRole() != null) {
                    fieldValidator.validateObject(regularUser, RegularUserDTO.Fields.role, true);
                    user.setRole(Role.valueOf(regularUser.getRole()));
                }

                results.add(UserActionResultDTO.builder().regularUser(user).success(true).build());

                users.add(user);
            }
        }

        regularUserRepository.saveAll(users);

        return results;
    }

    @PreAuthorize("hasAnyAuthority('OWNER', 'MANAGER', 'OPERATOR')")
    public RegularUser update(RegularUserDTO regularUser) {
        fieldValidator.validateObject(regularUser, RegularUserDTO.Fields.username, true);
        Optional<RegularUser> userOptional = findByUsername(regularUser.getUsername());

        if (userOptional.isEmpty()) {
            throw new ApplicationException("User with username %s was not found".formatted(regularUser.getUsername()),
                    HttpStatus.NOT_FOUND);
        }

        RegularUser user = userOptional.get();

        if (regularUser.getPassword() != null) {
            fieldValidator.validate(regularUser, RegularUserDTO.Fields.password, true);
            user.setPassword(passwordEncoder.encode(regularUser.getPassword()));
        }

        if (regularUser.getTimezone() != null) {
            fieldValidator.validate(regularUser, RegularUserDTO.Fields.timezone, true);
            user.setTimezone(regularUser.getTimezone());
        }

        if (List.of(Role.OWNER, Role.MANAGER).contains(getCurrentlyAuthenticatedUser().map(RegularUser::getRole).orElse(null))) {
            if (regularUser.getStatus() != null) {
                fieldValidator.validate(regularUser, RegularUserDTO.Fields.status, true);
                user.setStatus(Status.valueOf(regularUser.getStatus()));
            }

            if (regularUser.getRole() != null) {
                fieldValidator.validate(regularUser, RegularUserDTO.Fields.role, true);
                user.setRole(Role.valueOf(regularUser.getRole()));
            }
        }

        return regularUserRepository.save(user);
    }
}
