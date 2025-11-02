package io.store.ua.models.data;

import io.store.ua.enums.PaymentProvider;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;

/**
 * Represents information required to use for building customer checkout forms.
 * Required because no server-side initiated incoming payment can be done via available APIs.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldNameConstants
public class CheckoutFinancialInformation {
    //LIQ_PAY
    private String checkoutUrl;
    private String signature;
    private String encodedContent;
    //DATA_TRANS
    private String transactionId;
    private String reference;
    private PaymentProvider paymentProvider;
}
