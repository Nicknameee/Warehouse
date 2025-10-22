package io.store.ua.models.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

@Data
@Builder
@FieldNameConstants
public class StockItemGroupDTO {
    @NotBlank(message = "Stock item group code can't be blank")
    private String code;
    @NotBlank(message = "Stock item group name can't be blank")
    private String name;
    private Boolean isActive;
}
