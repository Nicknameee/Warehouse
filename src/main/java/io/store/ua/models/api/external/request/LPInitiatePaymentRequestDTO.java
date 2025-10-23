package io.store.ua.models.api.external.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;

import java.math.BigDecimal;

/**
 * Used to send all required parameters for initializing a LiqPay payment.
 * The response will contain a base64-encoded data payload and signature
 * that can be used to create a payment form or redirect the customer
 * to the LiqPay checkout page.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldNameConstants
public class LPInitiatePaymentRequestDTO {
    /**
     * Required parameter
     * Unique identifier of the transaction in your system
     * Must be unique per payment attempt
     * Example: INV-2025-001
     */
    @NotBlank(message = "Order ID can't be blank")
    private String orderId;
    /**
     * Required parameter
     * 3-letter ISO-4217 currency code
     * Example: UAH, USD, EUR
     */
    @NotBlank(message = "Currency can't be blank")
    @Pattern(regexp = "UAH|USD|EUR")
    private String currency;
    /**
     * Required parameter
     * Payment amount as a decimal
     * Example: 100.00
     */
    @NotNull(message = "Amount can't be null")
    @DecimalMin(value = "0.01", message = "Amount must be >= 0.01")
    private BigDecimal amount;
    /**
     * Optional parameter
     * For CREDIT only
     */
    @NotBlank(message = "Beneficiary code can't be blank")
    private String beneficiaryCode;
}
