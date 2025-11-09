package io.store.ua.models.data;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;

import java.math.BigInteger;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldNameConstants
public class FinancialStatistic {
    private String currency;
    private BigInteger totalDebit = BigInteger.ZERO;
    private BigInteger totalCredit = BigInteger.ZERO;
}