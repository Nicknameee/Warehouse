package io.store.ua.models.api.external.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.store.ua.models.api.external.AbstractAPIResponse;
import lombok.*;
import lombok.experimental.FieldNameConstants;

@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldNameConstants
@JsonIgnoreProperties(ignoreUnknown = true)
public class DTPaymentResponse extends AbstractAPIResponse {
    private String transactionId;
    private String acquirerAuthorizationCode;
    private Card card;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldNameConstants
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class Card {
        private String masked;
    }
}
