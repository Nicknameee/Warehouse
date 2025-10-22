package io.store.ua.models.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigInteger;
import java.time.LocalDate;

@Data
@FieldNameConstants
public class StockItemDTO {
    @NotNull(message = "Stock item ID can't be null")
    private Long id;
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
    @NotNull(message = "Reserved quantity can't be null")
    @Min(value = 0, message = "Reserved quantity must be >= 0")
    private BigInteger reservedQuantity;
    @Pattern(regexp = "OUT_OF_STOCK|AVAILABLE|RESERVED|OUT_OF_SERVICE", message = "Invalid status value")
    private String status;
}
