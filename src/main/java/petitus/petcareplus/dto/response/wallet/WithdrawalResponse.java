package petitus.petcareplus.dto.response.wallet;

import lombok.*;
import petitus.petcareplus.utils.enums.WithdrawalStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WithdrawalResponse {
    private UUID id;
    private BigDecimal amount;
    private BigDecimal fee;
    private BigDecimal netAmount;
    private WithdrawalStatus status;
    private String bankName;
    private String accountNumber;
    private String accountHolderName;
    private LocalDateTime createdAt;
    private LocalDateTime processedAt;
    private String adminNote;
    private String rejectionReason;
    private String transactionRef;
}