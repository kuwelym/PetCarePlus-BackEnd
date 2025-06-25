package petitus.petcareplus.dto.response.wallet;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletResponse {
    private UUID id;
    private BigDecimal balance;
    private BigDecimal pendingBalance;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
