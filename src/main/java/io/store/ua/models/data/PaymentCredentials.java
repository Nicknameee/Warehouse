package io.store.ua.models.data;

import io.store.ua.enums.CardType;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentCredentials {
    @NotBlank(message = "IBAN number can't be blank")
    private String IBAN;
    @NotBlank(message = "SWIFT number can't be blank")
    private String SWIFT;
    @NotBlank(message = "Currency can't be blank")
    private String currency;
    @NotNull(message = "Card type can't be blank")
    private CardType cardType;
    @NotBlank(message = "Card number can't be blank")
    private String cardPan;
    @NotBlank(message = "Card CVV can't be blank")
    private String cardCvv;
    @NotNull(message = "Card expiration month can't be null")
    @Min(value = 1, message = "Card expiration month must be >= 1")
    @Max(value = 12, message = "Card expiration month must be <= 12")
    private Integer cardExpMonth;
    @NotNull(message = "Card expiration year can't be null")
    @Min(value = 2025, message = "Card expiration year must be not earlier than current year")
    private Integer cardExpYear;
    @NotBlank(message = "Card holder can't be blank")
    private String cardHolder;
    @NotBlank(message = "Phone number can't be blank")
    private String phone;
    @Email(message = "Invalid email",
            regexp = "^(?=.{1,254}$)(?=.{1,64}@)[A-Za-z0-9._%+-]+@(?:[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?\\.)+[A-Za-z]{2,63}$")
    @NotBlank(message = "Email can't be blank")
    private String email;
}