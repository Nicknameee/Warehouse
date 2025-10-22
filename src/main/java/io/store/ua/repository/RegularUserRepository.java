package io.store.ua.repository;

import io.store.ua.entity.RegularUser;
import io.store.ua.enums.Role;
import io.store.ua.enums.Status;
import jakarta.transaction.Transactional;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Profile("users")
public interface RegularUserRepository extends JpaRepository<RegularUser, Long> {
  RegularUser findRegularUserByUsername(String username);

  RegularUser findRegularUserByEmail(String email);

  List<RegularUser> findRegularUsersByRole(Role role, Pageable pageable);

  List<RegularUser> findRegularUsersByStatus(Status status, Pageable pageable);

  @Query("UPDATE RegularUser u SET u.loginTime = CURRENT_TIMESTAMP WHERE u.id = :id")
  @Modifying
  @Transactional
  void updateLoginTime(@Param("id") Long id);

  @Query("UPDATE RegularUser u SET u.logoutTime = CURRENT_TIMESTAMP WHERE u.id = :id")
  @Modifying
  @Transactional
  void updateLogoutTime(@Param("id") Long id);
}
