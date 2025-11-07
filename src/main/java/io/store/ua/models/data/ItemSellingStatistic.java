package io.store.ua.models.data;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigInteger;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemSellingStatistic {
    private Long stockItemId;
    private LocalDate startDate;
    private BigInteger soldQuantity;
    private BigInteger totalRevenueAmount;
}
