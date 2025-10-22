package io.store.ua.models.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.store.ua.models.data.Address;
import io.store.ua.models.data.WorkingHours;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldNameConstants
public class WarehouseDTO {
    @NotBlank(message = "Code can't be blank")
    private String code;
    @NotBlank(message = "A name can't be blank")
    private String name;
    @NotNull(message = "An address can't be null")
    private Address address;
    @NotNull(message = "Working hours can't be null")
    private WorkingHours workingHours;
    @NotEmpty(message = "A list with phones can't be empty")
    private List<
            @NotBlank(message = "A phone can't be blank")
            @Pattern(regexp = "\\+[1-9]\\d(?:[ \\-()]?\\d){6,13}",
                    message = "Phone must start with + and contain 8–15 digits")
                    String> phones;
    @NotNull(message = "Activity state can't be null")
    private Boolean isActive;
    @JsonProperty("reassign_manager")
    private Long managerId;
}
