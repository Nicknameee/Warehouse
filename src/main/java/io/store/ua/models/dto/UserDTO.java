package io.store.ua.models.dto;

import io.store.ua.enums.UserRole;
import io.store.ua.enums.UserStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.Length;

@Data
@FieldNameConstants
public class UserDTO {
    @Email(message = "Invalid email",
            regexp = "^(?=.{1,254}$)(?=.{1,64}@)[A-Za-z0-9._%+-]+@(?:[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?\\.)+[A-Za-z]{2,63}$")
    @NotBlank(message = "Email can't be blank")
    private String email;
    @NotBlank(message = "Username can not be blank")
    @Length(min = 8, max = 64, message = "Username length should be in range [8; 64]")
    private String username;
    @NotBlank(message = "Password can not be blank")
    @Length(min = 8, message = "Password length should be at least 8 chars")
    private String password;
    @Pattern(regexp = "^(?:[A-Za-z]+(?:/[A-Za-z0-9._+-]+)+|UTC)$",
            flags = Pattern.Flag.CASE_INSENSITIVE,
            message = "Use an IANA timezone like 'Europe/Kyiv', 'America/Los_Angeles', or 'UTC'")
    private String timezone;
    @NotNull
    @Pattern(regexp = UserRole.ROLE_PATTERN, flags = Pattern.Flag.CASE_INSENSITIVE, message = "Role must be one of: OPERATOR, MANAGER")
    private String role;
    @NotNull
    @Pattern(regexp = UserStatus.STATUS_PATTERN, flags = Pattern.Flag.CASE_INSENSITIVE, message = "Status must be one of: ACTIVE, INACTIVE")
    private String status;
}
