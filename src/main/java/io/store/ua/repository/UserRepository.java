package io.store.ua.repository;

import io.store.ua.entity.User;
import io.store.ua.enums.UserRole;
import io.store.ua.enums.UserStatus;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {
    User findUserByUsername(String username);

    User findUserByEmail(String email);

    List<User> findUsersByRole(UserRole role, Pageable pageable);

    List<User> findUsersByStatus(UserStatus status, Pageable pageable);

    @Query("UPDATE User u SET u.loginTime = CURRENT_TIMESTAMP WHERE u.id = :id")
    @Modifying
    @Transactional
    void updateLoginTime(@Param("id") Long id);

    @Query("UPDATE User u SET u.logoutTime = CURRENT_TIMESTAMP WHERE u.id = :id")
    @Modifying
    @Transactional
    void updateLogoutTime(@Param("id") Long id);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
}
