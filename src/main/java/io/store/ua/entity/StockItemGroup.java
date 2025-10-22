package io.store.ua.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;

/**
 * Entity represent stock item category The category structure is not hierarchical, but 1-level
 */
@Entity
@Table(name = "stock_item_groups")
@Data
@FieldNameConstants
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockItemGroup {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, updatable = false)
    private String code;
    @Column(nullable = false)
    private String name;
    @Column(name = "is_active", nullable = false)
    private Boolean isActive;
}
