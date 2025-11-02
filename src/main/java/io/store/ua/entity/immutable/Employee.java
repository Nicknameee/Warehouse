package io.store.ua.entity.immutable;

import io.store.ua.enums.Role;
import io.store.ua.enums.Status;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

@Entity
@Table(name = "users")
@Immutable
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Employee {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, updatable = false)
    private String username;
    @Column(nullable = false, unique = true, updatable = false)
    private String email;
    @Column(nullable = false, updatable = false)
    @Enumerated(EnumType.STRING)
    private Role role;
    @Column(nullable = false, updatable = false)
    @Enumerated(EnumType.STRING)
    private Status status;
    @Column(nullable = false, updatable = false)
    private String timezone;

    @PrePersist
    @PreUpdate
    @PreRemove
    private void notSupported() {
        throw new UnsupportedOperationException();
    }
}
