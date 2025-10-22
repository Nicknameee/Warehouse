package io.store.ua.entity;

import io.store.ua.entity.immutable.ProductTagLink;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;

import java.util.List;

/**
 * Entity represents tags for product(-s)
 * Serves for additional filtering, searching or grouping
 */
@Entity
@Table(name = "tags")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldNameConstants
public class Tag {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(unique = true, nullable = false)
    private String name;
    @Column(name = "is_active", nullable = false)
    private Boolean isActive;
    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "tag_id",
            referencedColumnName = "id",
            insertable = false,
            updatable = false
    )
    private List<ProductTagLink> links;
}
