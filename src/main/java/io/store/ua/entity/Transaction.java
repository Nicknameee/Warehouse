package io.store.ua.entity;

import io.store.ua.enums.PaymentProvider;
import io.store.ua.enums.TransactionFlowType;
import io.store.ua.enums.TransactionPurpose;
import io.store.ua.enums.TransactionStatus;
import io.store.ua.models.data.ExternalReferences;
import io.store.ua.models.data.PaymentCredentials;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.math.BigInteger;
import java.time.OffsetDateTime;
import java.util.Map;

@Entity
@Table(name = "transactions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldNameConstants
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "transaction_id", unique = true, nullable = false, updatable = false)
    private String transactionId;
    @Column(unique = true, nullable = false, updatable = false)
    private String reference;
    @Enumerated(EnumType.STRING)
    @Column(name = "flow_type", nullable = false)
    private TransactionFlowType flowType;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionPurpose purpose;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionStatus status;
    @Column(nullable = false)
    private BigInteger amount;
    @Column(nullable = false)
    private String currency;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    private PaymentCredentials payer;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    private PaymentCredentials receiver;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    private Map<String, String> metadata;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    private Map<String, String> fee;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "external_references", columnDefinition = "json")
    private ExternalReferences externalReferences;
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
    @Column(name = "paid_at")
    private OffsetDateTime paidAt;
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_provider")
    private PaymentProvider paymentProvider;
}
