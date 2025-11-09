package io.store.ua.models.data;

import io.store.ua.entity.Beneficiary;
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
public class BeneficiaryFinancialFlowStatistic {
    private Beneficiary beneficiary;
    private List<FinancialStatistic> financialStatistic;
}
