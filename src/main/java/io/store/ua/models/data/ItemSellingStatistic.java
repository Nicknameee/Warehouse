package io.store.ua.models.data;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigInteger;
import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemSellingStatistic {
    private Long stockItemId;
    private List<Statistic> statistics;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Statistic {
        private LocalDate startDate;
        private BigInteger soldQuantity;
        private BigInteger totalRevenueAmount;
        private String currency;
    }
}
