package io.store.ua.entity.immutable;

import io.store.ua.entity.Shipment;
import io.store.ua.entity.Transaction;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "stock_item_logs")
@Immutable
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldNameConstants
public class StockItemLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "event_id", unique = true, nullable = false, updatable = false)
    private String eventId;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "initiator_id", insertable = false, updatable = false)
    private Employee employee;
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id", insertable = false, updatable = false)
    private Transaction transaction;
    @Column(nullable = false, updatable = false)
    @Enumerated(EnumType.STRING)
    private ItemEventType type;
    @Column(name = "action_initiator_scope")
    @Enumerated(EnumType.STRING)
    private ActionScope actionInitiatorScope;
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipment_id", insertable = false, updatable = false)
    private Shipment shipment;
    @Column(name = "quantity_of_items", nullable = false, updatable = false)
    private BigInteger quantityOfItems;
    @Column(nullable = false, updatable = false)
    private BigDecimal availableQuantityBeforeEvent;
    @Column(nullable = false, updatable = false)
    private BigDecimal availableQuantityAfterEvent;
    @Column(nullable = false, updatable = false)
    private BigDecimal reservedQuantityBefore;
    @Column(nullable = false, updatable = false)
    private BigDecimal reservedQuantityAfter;
    @Column(name = "initiator_id", updatable = false)
    private Long initiatorId;
    @Column(name = "transaction_id", updatable = false)
    private Long transactionId;
    @Column(nullable = false, updatable = false)
    private Long stockItemId;
    @Column(name = "warehouse_id", nullable = false, updatable = false)
    private Long warehouseId;
    @Column(name = "shipment_id", updatable = false)
    private Long shipmentId;
    @Column(updatable = false)
    private String description;
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime occurredAt;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", updatable = false)
    private Map<String, Object> metadata;

    @PreUpdate
    @PreRemove
    private void notSupported() {
        throw new UnsupportedOperationException();
    }

    /**
     * Represents scope of the item flow
     */
    public enum ActionScope {
        /**
         * Item flow without payment(between warehouses)
         */
        INTERNAL,
        /**
         * Item flow with payment(getting new items from vendors or selling to merchants)
         */
        EXTERNAL
    }

    public enum ItemEventType {
        INBOUND,
        OUTBOUND,
        RESERVE,
        RELEASE
    }
}
