package io.store.ua.entity;

import io.store.ua.enums.PaymentProvider;
import io.store.ua.enums.TransactionFlowType;
import io.store.ua.enums.TransactionPurpose;
import io.store.ua.enums.TransactionStatus;
import io.store.ua.models.data.ExternalReferences;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigInteger;
import java.time.LocalDateTime;

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
    @Column(name = "beneficiary_id", updatable = false)
    private Long beneficiaryId;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "external_references", columnDefinition = "json")
    private ExternalReferences externalReferences;
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    @Column(name = "paid_at")
    private LocalDateTime paidAt;
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_provider")
    private PaymentProvider paymentProvider;
}
