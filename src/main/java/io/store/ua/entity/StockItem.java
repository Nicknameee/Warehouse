package io.store.ua.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;

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
    @ManyToOne(optional = false)
    @JoinColumn(name = "product_id", nullable = false, insertable = false, updatable = false)
    private Product product;
    @ManyToOne(optional = false)
    @JoinColumn(name = "group_id", nullable = false, insertable = false, updatable = false)
    private StockItemGroup stockItemGroup;
    @Column(name = "product_id", nullable = false)
    private Long productId;
    @Column(name = "group_id", nullable = false)
    private Long stockItemGroupId;
    @Column(name = "warehouse_id", nullable = false)
    private Long warehouseId;
    @Column(name = "expiry_date")
    private LocalDate expiryDate;
    @Column(name = "available_quantity", nullable = false)
    private BigInteger availableQuantity;
    @Column(name = "reserved_quantity", nullable = false)
    private BigInteger reservedQuantity;
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Status status;
    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    /**
     * Represents item state in the system
     */
    public enum Status {
        /**
         * All items shipped out
         */
        OUT_OF_STOCK,
        /**
         * Items available for reserve or shipment
         */
        AVAILABLE,
        /**
         * All items on site are reserved
         */
        RESERVED,
        /**
         * Item is off
         */
        OUT_OF_SERVICE
    }
}
