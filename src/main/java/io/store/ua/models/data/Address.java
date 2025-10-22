package io.store.ua.models.data;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Address {
    @NotBlank(message = "Building can't be blank")
    private String building;
    @NotBlank(message = "Street can't be blank")
    private String street;
    @NotBlank(message = "City can't be blank")
    private String city;
    @NotBlank(message = "State can't be blank")
    private String state;
    @NotBlank(message = "Country can't be blank")
    private String country;
    @NotBlank(message = "Postal code can't be blank")
    private String postalCode;
    private BigDecimal latitude;
    private BigDecimal longitude;
}
