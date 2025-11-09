package io.store.ua.entity;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.store.ua.enums.UserRole;
import io.store.ua.enums.UserStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;

@Data
@Entity
@Table(name = "users")
@FieldNameConstants
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User implements UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "username", nullable = false, unique = true)
    private String username;
    @Column(name = "password", nullable = false)
    private String password;
    @Column(name = "email", nullable = false, unique = true)
    private String email;
    @Column(name = "login_time")
    private ZonedDateTime loginTime;
    @Column(name = "logout_time")
    private ZonedDateTime logoutTime;
    @Column(name = "role", nullable = false)
    @Enumerated(EnumType.STRING)
    private UserRole role;
    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private UserStatus status;
    @Column(name = "timezone", nullable = false)
    private String timezone;

    @JsonIgnore
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role.name()));
    }

    @JsonIgnore
    @Override
    public String getPassword() {
        return password;
    }

    @JsonIgnore
    @Override
    public String getUsername() {
        return username;
    }

    @JsonIgnore
    @Override
    public boolean isAccountNonExpired() {
        return status == UserStatus.ACTIVE;
    }

    @JsonIgnore
    @Override
    public boolean isAccountNonLocked() {
        return status == UserStatus.ACTIVE;
    }

    @JsonIgnore
    @Override
    public boolean isCredentialsNonExpired() {
        return status == UserStatus.ACTIVE;
    }

    @JsonIgnore
    @Override
    public boolean isEnabled() {
        return status == UserStatus.ACTIVE;
    }

    @JsonGetter("loginTime")
    public String getLoginTime() {
        return loginTime != null ? loginTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME) : null;
    }

    @JsonGetter("logoutTime")
    public String getLogoutTime() {
        return logoutTime != null ? logoutTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME) : null;
    }
}
