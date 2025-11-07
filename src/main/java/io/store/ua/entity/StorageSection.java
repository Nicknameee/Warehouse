package io.store.ua.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;

@Entity
@Table(name = "storage_sections")
@Data
@FieldNameConstants
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StorageSection {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "warehouse_id", nullable = false)
    private Long warehouseId;
    @Column(nullable = false)
    private String code;
}
