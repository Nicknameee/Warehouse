package io.store.ua.models.dto;

import jakarta.validation.constraints.NotBlank;
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
public class LoginDTO {
    @NotBlank(message = "Login can't be blank")
    private String login;
    @NotBlank(message = "Password can't be blank")
    private String password;
}
