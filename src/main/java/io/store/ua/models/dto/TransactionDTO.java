package io.store.ua.models.dto;

import io.store.ua.enums.PaymentProvider;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;

import java.math.BigInteger;

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
    private Long receiverFinancialAccountId;
    private PaymentProvider paymentProvider;
}
