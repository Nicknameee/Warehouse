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
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldNameConstants
public class ProductDTO {
    @NotBlank(message = "Code can't be blank")
    private String code;
    @NotBlank(message = "Title can't be blank")
    private String title;
    @NotBlank(message = "Description can't be blank")
    private String description;
    @NotNull(message = "Price can't be null")
    @Min(value = 1, message = "Price can't be less than 1")
    private BigInteger price;
    private Set<@NotNull(message = "Tag ID can't be null") Long> tags;
    @NotNull(message = "Weight can't be null")
    @Min(value = 1, message = "Weight can't be less than 1")
    private BigInteger weight;
    @NotNull(message = "Length can't be null")
    @Min(value = 1, message = "Length can't be less than 1")
    private BigInteger length;
    @NotNull(message = "Width can't be null")
    @Min(value = 1, message = "Width can't be less than 1")
    private BigInteger width;
    @NotNull(message = "Height can't be null")
    @Min(value = 1, message = "Height can't be less than 1")
    private BigInteger height;
}
