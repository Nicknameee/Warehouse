package io.store.ua.models.api.external.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
public record LPStatusPayload(String version,
                              String action,
                              @JsonProperty("order_id") String orderId,
                              @JsonProperty("public_key") String publicKey) {
}