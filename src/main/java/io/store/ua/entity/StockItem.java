package io.store.ua.entity;

import io.store.ua.enums.StockItemStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigInteger;
import java.time.LocalDate;

/**
 * Entity represents storage information for specific product at specific warehouse
 */
@Entity
@Table(name = "stock_items")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldNameConstants
public class StockItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "batch_version", nullable = false)
    private Long batchVersion;
    @Column(name = "code", nullable = false, updatable = false, unique = true)
    private String code;
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "product_id", nullable = false, insertable = false, updatable = false)
    private Product product;
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "group_id", nullable = false, insertable = false, updatable = false)
    private StockItemGroup stockItemGroup;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "storage_section_id", insertable = false, updatable = false)
    private StorageSection storageSection;
    @Column(name = "product_id", nullable = false)
    private Long productId;
    @Column(name = "group_id", nullable = false)
    private Long stockItemGroupId;
    @Column(name = "warehouse_id", nullable = false)
    private Long warehouseId;
    @Column(name = "expiry_date")
    @DateTimeFormat(pattern = "dd-MM-yyyy")
    private LocalDate expiryDate;
    @Column(name = "available_quantity", nullable = false)
    private BigInteger availableQuantity;
    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private StockItemStatus status;
    @Column(name = "is_active", nullable = false)
    private Boolean isActive;
    @Column(name = "storage_section_id")
    private Long storageSectionId;
}
