package io.store.ua.models.api.external.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;

/**
 * Contains the result of a LiqPay payment initialization.
 * Used to return all required data to the client for building and submitting
 * a payment form to the LiqPay checkout endpoint.
 * This response is created after the backend signs and encodes the
 * original payment request with the merchant's private key.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldNameConstants
public class LPInitiatePaymentResponse {
    /**
     * Required parameter
     * LiqPay checkout endpoint to which the client submits the form
     * Example: https://www.liqpay.ua/api/3/checkout
     */
    private String checkoutUrl;

    /**
     * Required parameter
     * Base64-encoded SHA-1 signature generated with the private key
     * Formula: base64(sha1(private_key + encodedContent + private_key))
     */
    private String signature;

    /**
     * Required parameter
     * Base64-encoded JSON payload describing payment details
     * Used as the "data" field value in the checkout form
     */
    private String encodedContent;
}
