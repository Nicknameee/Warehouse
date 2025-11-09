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
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldNameConstants
public class StockItemHistoryDTO {
    @NotNull(message = "Stock item history ID can't be null")
    @Min(value = 1, message = "Stock item history ID can't be less than 1")
    private Long stockItemId;
    @NotNull(message = "Stock item group ID can't be null")
    @Min(value = 1, message = "Stock item group ID can't be less than 1")
    private Long oldStockItemGroupId;
    @NotNull(message = "Stock item group ID can't be null")
    @Min(value = 1, message = "Stock item group ID can't be less than 1")
    private Long newStockItemGroupId;
    @NotNull(message = "Warehouse ID can't be null")
    @Min(value = 1, message = "Warehouse ID can't be less than 1")
    private Long oldWarehouseId;
    @NotNull(message = "Warehouse ID can't be null")
    @Min(value = 1, message = "Warehouse ID can't be less than 1")
    private Long newWarehouseId;
    @NotNull(message = "Quantity can't be null")
    @Min(value = 0, message = "Quantity can't be less than 0")
    private BigInteger quantityBefore;
    @NotNull(message = "Quantity can't be null")
    @Min(value = 0, message = "Quantity can't be less than 0")
    private BigInteger quantityAfter;
    private LocalDate oldExpiration;
    private LocalDate newExpiration;
    @NotBlank(message = "Status can't be blank")
    private String oldStatus;
    @NotBlank(message = "Status can't be blank")
    private String newStatus;
    @Min(value = 1, message = "Section ID can't be less than 1")
    private Long oldSectionId;
    @Min(value = 1, message = "Section ID can't be less than 1")
    private Long newSectionId;
    @NotNull(message = "Is active can't be null")
    private Boolean oldActivity;
    @NotNull(message = "Is active can't be null")
    private Boolean newActivity;
    private LocalDateTime loggedAt;
    private boolean changeExpiration;
    private boolean changeSection;
}
