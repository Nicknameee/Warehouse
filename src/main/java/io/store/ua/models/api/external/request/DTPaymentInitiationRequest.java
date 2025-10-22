package io.store.ua.models.api.external.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.Length;

/**
 * Securely send all the necessary parameters to the transaction initialization API!
 * The result of this API call is an HTTP 201 status code with a transactionId in the response body and the Location header set
 * This call is required to proceed with our Redirect and Lightbox integration
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldNameConstants
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DTPaymentInitiationRequest {
    /**
     * Required parameter
     * 3 letter ISO-4217 character code
     */
    @NotBlank(message = "Currency can't be blank")
    @Pattern(regexp = "CHF|USD|EUR")
    private String currency;
    /**
     * Required parameter
     * Length [ 1; 40 ] characters
     * The merchant's reference number
     * It should be unique for each transaction
     */
    @NotBlank(message = "Transaction reference can't be blank")
    @Length(min = 1, max = 40, message = "Transaction reference length should be in range [1; 40]")
    @JsonProperty("refno")
    private String transactionReference;
    /**
     * Optional parameter
     * Whether to automatically settle the transaction after an authorization or not!
     * If present with the init request, the settings defined in the dashboard
     * ('Authorization / Settlement' or 'Direct Debit') will be used
     * Those settings will only be used for web transactions and not for server-to-server API calls
     */
    private Boolean autoSettle;
    /**
     * Required parameter
     * The amount of the transaction in the currency’s smallest unit,
     * For example, use 1000 for CHF 10.00
     * Can be omitted for use cases where only a registration should take place (if the payment method supports registrations)
     */
    @NotNull(message = "Amount can't be null")
    @Min(value = 1, message = "Amount can't be less than 1")
    private String amount;
    /**
     * Optional parameter
     * The redirect object is used to customize the browser behavior when using the payment page
     */
    private Redirect redirect;
    /**
     * Optional parameter
     * The theme (including configuration options) to be used when rendering the payment page
     */
    private Theme theme;
    /**
     * Required parameter
     */
    private Card card;

    /**
     * "card": {
     * "alias": "AAABcH0Bq92s3kgAESIAAbGj5NIsAHWC",
     * "expiryMonth": "06",
     * "expiryYear": "28"
     * }
     */
    @Data
    public static class Card {
        private final String alias = "AAABcH0Bq92s3kgAESIAAbGj5NIsAHWC";
        private final String expiryMonth = "06";
        private String expiryYear = "28";
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Redirect {
        private final String successUrl = "https://pay.sandbox.datatrans.com/upp/merchant/successPage.jsp";
        private final String cancelUrl = "https://pay.sandbox.datatrans.com/upp/merchant/cancelPage.jsp";
        private final String errorUrl = "https://pay.sandbox.datatrans.com/upp/merchant/errorPage.jsp";
        /**
         * If the payment is started within an iframe or when using the Lightbox Mode, use value _top
         * This ensures a proper browser flow for payment methods who need a redirect
         */
        private String startTarget;
        @Pattern(regexp = "GET|POST")
        private String method;
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Theme {
        private final Configuration configuration = new Configuration();

        @Data
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public static class Configuration {
            private String brandColor = "#FFFFFF";
            private String textColor = "white";
            private String logoType = "circle";
            private String logoBorderColor = "#A1A1A1";
            private String brandButton = "#A1A1A1";
            private String payButtonTextColor = "#FFFFFF";
            private String logoSrc = "{svg}";
            @Pattern(regexp = "list|grid")
            private String initialView = "list";
            /**
             * If set to false and no logo is used (see logoSrc), the payment page header will be empty
             */
            private boolean brandTitle;
        }
    }
}
