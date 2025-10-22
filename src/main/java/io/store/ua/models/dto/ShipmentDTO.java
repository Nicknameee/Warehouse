package io.store.ua.models.dto;

import io.store.ua.models.data.Address;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class ShipmentDTO {
    @NotNull(message = "Shipment id can't be null")
    private Long id;
    @NotBlank(message = "Warehouse sender code can't be blank")
    private String senderCode;
    @NotBlank(message = "Warehouse recipient code can't be blank")
    private String recipientCode;
    @NotNull(message = "An address can't be null")
    private Address address;
    @NotNull(message = "Stock item id can't be null")
    private Long stockItemId;
    @NotNull(message = "Stock item amount can't be null")
    private Long stockItemAmount;
    @NotBlank(message = "Status can't be blank")
    private String status;
}
