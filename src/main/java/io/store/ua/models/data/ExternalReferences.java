package io.store.ua.models.data;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExternalReferences {
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