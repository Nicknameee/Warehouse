package io.store.ua.entity.immutable;

import io.store.ua.enums.StockItemStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Immutable;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "stock_item_history")
@Immutable
@Data
@Builder
@FieldNameConstants
@NoArgsConstructor
@AllArgsConstructor
public class StockItemHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "stock_item_id", nullable = false, updatable = false)
    private Long stockItemId;
    @Column(name = "title", nullable = false, updatable = false)
    private String title;
    @Column(name = "current_product_price", nullable = false, updatable = false)
    private BigInteger currentProductPrice;
    @Column(name = "currency", nullable = false)
    private String currency;
    @Column(name = "logged_at", nullable = false, updatable = false)
    private LocalDateTime loggedAt;
    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;
    @Column(name = "old_group_id")
    private Long oldGroupId;
    @Column(name = "new_group_id")
    private Long newGroupId;
    @Column(name = "old_warehouse_id")
    private Long oldWarehouseId;
    @Column(name = "new_warehouse_id")
    private Long newWarehouseId;
    @Column(name = "quantity_before")
    private BigInteger quantityBefore;
    @Column(name = "quantity_after")
    private BigInteger quantityAfter;
    @Column(name = "old_expiration")
    @DateTimeFormat(pattern = "dd-MM-yyyy")
    private LocalDate oldExpiration;
    @Column(name = "new_expiration")
    @DateTimeFormat(pattern = "dd-MM-yyyy")
    private LocalDate newExpiration;
    @Column(name = "old_status")
    @Enumerated(EnumType.STRING)
    private StockItemStatus oldStatus;
    @Column(name = "new_status")
    @Enumerated(EnumType.STRING)
    private StockItemStatus newStatus;
    @Column(name = "old_section_id")
    private Long oldSectionId;
    @Column(name = "new_section_id")
    private Long newSectionId;
    @Column(name = "old_activity")
    private Boolean oldActivity;
    @Column(name = "new_activity")
    private Boolean newActivity;

    @PreUpdate
    private void notSupported() {
        throw new UnsupportedOperationException();
    }
}
