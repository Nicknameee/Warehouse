package io.store.ua.entity;

import io.store.ua.enums.ShipmentDirection;
import io.store.ua.enums.ShipmentStatus;
import io.store.ua.models.data.Address;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

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
    @Column(name = "code", unique = true, nullable = false, updatable = false)
    private String code;
    @Column(name = "warehouse_id_sender")
    private Long warehouseIdSender;
    @Column(name = "warehouse_id_recipient")
    private Long warehouseIdRecipient;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    private Address address;
    @Column(name = "stock_item_id")
    private Long stockItemId;
    @Column(name = "stock_item_quantity")
    private Long stockItemQuantity;
    @Column(name = "initiator_id")
    private Long initiatorId;
    @Column
    @Enumerated(EnumType.STRING)
    private ShipmentStatus status;
    @Column(name = "created_at")
    @CreationTimestamp
    private LocalDateTime createdAt;
    @Column(name = "shipment_direction", nullable = false)
    @Enumerated(EnumType.STRING)
    private ShipmentDirection shipmentDirection;
}
