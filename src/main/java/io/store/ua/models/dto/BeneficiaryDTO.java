package io.store.ua.models.dto;

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
public class BeneficiaryDTO {
    @NotNull(message = "Beneficiary ID can't be null")
    private Long id;
    @NotBlank(message = "Beneficiary IBAN can't be blank")
    private String IBAN;
    @NotNull(message = "Beneficiary SWIFT can't be null")
    private String SWIFT;
    @NotBlank(message = "Beneficiary name can't be blank")
    private String name;
    @NotBlank(message = "Beneficiary card can't be blank")
    private String card;
    private Boolean isActive;
}
