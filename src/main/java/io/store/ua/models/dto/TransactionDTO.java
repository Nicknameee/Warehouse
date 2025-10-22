package io.store.ua.models.dto;

import io.store.ua.enums.PaymentProvider;
import io.store.ua.models.data.PaymentCredentials;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;

import java.math.BigInteger;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldNameConstants
public class TransactionDTO {
    @NotBlank(message = "Transaction ID can't be blank")
    private String transactionId;
    @NotBlank(message = "Reference can't be blank")
    private String reference;
    @NotNull(message = "Flow type can't be null")
    private String flowType;
    @NotNull(message = "Purpose can't be null")
    private String purpose;
    @NotNull(message = "Status can't be null")
    private String status;
    @NotNull(message = "Amount can't be null")
    @Min(value = 1, message = "Amount can't be less than 1")
    private BigInteger amount;
    @NotNull(message = "Currency can't be null")
    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be a 3-letter uppercase code")
    private String currency;
    private PaymentCredentials payer;
    private PaymentCredentials receiver;
    @Size(max = 50, message = "Meta can contain at most 100 entries")
    private Map<@NotBlank String, @NotBlank String> metadata;
    @Size(max = 10, message = "Fee can contain at most 10 entries")
    private Map<@NotBlank String, @NotBlank String> fee;
    private PaymentProvider paymentProvider;
}
