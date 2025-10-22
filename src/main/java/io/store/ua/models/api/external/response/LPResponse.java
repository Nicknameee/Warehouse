package io.store.ua.models.api.external.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import io.store.ua.models.api.external.AbstractAPIResponse;
import lombok.*;
import lombok.experimental.FieldNameConstants;

import java.util.Arrays;


@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldNameConstants
public class LPResponse extends AbstractAPIResponse {
    private Status status;
    @JsonProperty("order_id")
    private String orderId;

    @JsonSetter("status")
    public void setStatus(String status) {
        this.status = Arrays.stream(Status.values()).filter(any -> any.name().equalsIgnoreCase(status)).findAny().orElse(Status.UNKNOWN);
    }

    public enum Status {
        SUCCESS,
        FAILURE,
        ERROR,
        REVERSED,
        SANDBOX,
        CHARGEBACK,
        UNKNOWN
    }

}