package io.store.ua.entity;

import io.store.ua.models.data.Address;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "shipments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldNameConstants
public class Shipment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "warehouse_id_sender", nullable = false)
    private Long warehouseIdSender;
    @Column(name = "warehouse_id_recipient")
    private Long warehouseIdRecipient;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    private Address address;
    @Column(name = "stock_item_id")
    private Long stockItemId;
    @Column
    private Long stockItemAmount;
    @Enumerated(EnumType.STRING)
    @Column
    private ShipmentStatus status;
    @Column(name = "initiator_id")
    private Long initiatorId;

    public enum ShipmentStatus {
        INITIATED,
        SENT,
        DELIVERED,
        ROLLBACK
    }
}
