package io.store.ua.models.api.external.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Defines all fields required to initialize a LiqPay checkout transaction.
 * This payload is serialized to JSON, Base64-encoded and signed before
 * being sent to the LiqPay API endpoint.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LPInitiatePaymentRequest {
    /**
     * Required parameter
     * API version
     * Default: 3
     */
    private String version;
    /**
     * Required parameter
     * Merchant public key from the LiqPay dashboard
     */
    @JsonProperty("public_key")
    private String publicKey;
    /**
     * Required parameter
     * Defines the operation type
     * Example: pay, hold
     */
    private String action;
    /**
     * Required parameter
     * Transaction amount as a decimal string
     * Example: 100.00
     */
    private String amount;
    /**
     * Required parameter
     * 3-letter ISO-4217 currency code
     * Example: UAH, USD, EUR
     */
    private String currency;
    /**
     * Required parameter
     * Short text shown to the payer during checkout
     * Example: Payment for order #001
     */
    private String description;
    /**
     * Required parameter
     * Unique transaction identifier in your system
     * Example: INV-2025-001
     */
    @JsonProperty("order_id")
    private String orderId;
    /**
     * Optional parameter
     * Enables test mode if set to "1"
     * Example: 1
     */
    private String sandbox;
    /**
     * Optional parameter
     * Used for outgoing payments
     * Example: 4444444444444444
     */
    @JsonProperty("receiver_card")
    private String receiverCard;
}
