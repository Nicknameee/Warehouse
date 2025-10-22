package io.store.ua.models.api.data;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldNameConstants
public class DataTransTransaction {
    private String transactionId;
    private String reference;
    private String acquirerAuthorizationCode;
}
