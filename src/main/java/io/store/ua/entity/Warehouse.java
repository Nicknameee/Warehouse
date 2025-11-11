package io.store.ua.entity;

import io.store.ua.entity.immutable.Employee;
import io.store.ua.models.data.Address;
import io.store.ua.models.data.WorkingHours;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;

/**
 * Entity represents warehouse (can be more than 1 in the network)
 */
@Entity
@Table(name = "warehouses")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldNameConstants
public class Warehouse {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "code", unique = true, nullable = false)
    private String code;
    @Column(name = "name", unique = true, nullable = false)
    private String name;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "address", columnDefinition = "json")
    private Address address;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "working_hours", columnDefinition = "json")
    private WorkingHours workingHours;
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "phones", columnDefinition = "varchar[]")
    private List<String> phones;
    @ManyToOne(optional = false)
    @JoinColumn(name = "manager_id", insertable = false, updatable = false)
    private Employee manager;
    @Column(name = "manager_id", nullable = false)
    private Long managerId;
    @Column(name = "is_active", nullable = false)
    private Boolean isActive;
}
