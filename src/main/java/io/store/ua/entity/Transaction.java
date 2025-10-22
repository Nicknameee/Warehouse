package io.store.ua.entity;

import io.store.ua.enums.PaymentProvider;
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

import java.math.BigDecimal;
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
    private FlowType flowType;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Purpose purpose;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;
    @Column(nullable = false)
    private BigDecimal amount;
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

    public enum FlowType {
        DEBIT,
        CREDIT
    }

    public enum Purpose {
        STOCK_INBOUND_COST,
        STOCK_OUTBOUND_REVENUE,
        SALARY,
        UTILITIES,
        RENT,
        TAX,
        SHIPPING,
        FEE,
        REFUND,
        OTHER
    }

    public enum Status {
        PLANNED,
        POSTED,
        FAILED,
        CANCELLED
    }

    @Data
    public static class PaymentCredentials {
        private String IBAN;
        private String SWIFT;
        private String currency;
        private CardType cardType;
        private String cardPan;
        private Integer cardExpMonth;
        private Integer cardExpYear;
        private String cardHolder;
        private String phone;
        private String email;

        enum CardType {
            VISA,
            MASTERCARD
        }
    }

    @Data
    public static class ExternalReferences {
        private String transactionId;
        private String status;
        private String merchantId;
        private String merchantName;
        private String authenticationCode;
        private String country;
        private String currency;
        private String type;
        private String pan;
        private String reference;
    }
}
