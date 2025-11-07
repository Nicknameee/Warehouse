package io.store.ua.models.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;

import java.math.BigInteger;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldNameConstants
public class StockItemHistoryDTO {
    @NotNull(message = "Stock item history ID can't be null")
    @Min(value = 1, message = "Stock item history ID can't be less than 1")
    private Long stockItemId;
    @Min(value = 1, message = "Warehouse ID can't be less than 1")
    private Long oldWarehouseId;
    @Min(value = 1, message = "Warehouse ID can't be less than 1")
    private Long newWarehouseId;
    @Min(value = 1, message = "Quantity can't be less than 1")
    private BigInteger quantityBefore;
    @Min(value = 1, message = "Quantity can't be less than 1")
    private BigInteger quantityAfter;
    @NotNull(message = "Expiration can't be null")
    private LocalDate oldExpiration;
    @NotNull(message = "Expiration can't be null")
    private LocalDate newExpiration;
    @NotBlank(message = "Status can't be blank")
    private String oldStatus;
    @NotBlank(message = "Status can't be blank")
    private String newStatus;
    private Boolean oldActivity;
    private Boolean newActivity;
}
