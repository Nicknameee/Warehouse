package io.store.ua.models.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigInteger;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldNameConstants
public class StockItemDTO {
    @NotNull(message = "Stock item ID can't be null")
    private Long stockItemId;
    @NotNull(message = "Product ID can't be null")
    private Long productId;
    @NotNull(message = "Stock item group ID can't be null")
    private Long stockItemGroupId;
    @NotNull(message = "Warehouse ID can't be null")
    private Long warehouseId;
    @NotNull(message = "Expiry date can't be null")
    @DateTimeFormat(pattern = "dd-MM-yyyy")
    private LocalDate expiryDate;
    @NotNull(message = "Available quantity can't be null")
    @Min(value = 0, message = "Available quantity must be >= 0")
    private BigInteger availableQuantity;
    @NotNull(message = "Is active can't be null")
    private Boolean isActive;
    @NotNull(message = "Storage section ID can't be null")
    @Min(value = 1, message = "Storage section ID must be >= 1")
    private Long storageSectionId;
    private boolean nullifyExpiration;
    private boolean nullifySection;
    private boolean switchOff;
}
