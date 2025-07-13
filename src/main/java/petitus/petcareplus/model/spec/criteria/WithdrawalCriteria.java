package petitus.petcareplus.model.spec.criteria;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import petitus.petcareplus.utils.enums.WithdrawalStatus;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WithdrawalCriteria {
    private WithdrawalStatus status;
    private BigDecimal amountFrom;
    private BigDecimal amountTo;
    private String bankName;
    private Boolean isDeleted; // Filter by deleted status (for admin)
}
