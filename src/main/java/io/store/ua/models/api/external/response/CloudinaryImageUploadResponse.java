package io.store.ua.models.api.external.response;

import io.store.ua.models.api.external.AbstractAPIResponse;
import lombok.*;
import lombok.experimental.FieldNameConstants;

@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldNameConstants
public class CloudinaryImageUploadResponse extends AbstractAPIResponse {
    private String publicId;
    /**
     * HTTPS image view
     */
    private String secureUrl;
    /**
     * HTTP image view
     */
    private String url;
}
