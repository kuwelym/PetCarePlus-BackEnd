package petitus.petcareplus.configuration;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@Configuration
@Data
public class WalletConfig {
    @Value("${wallet.withdrawal.fee-rate}")
    private BigDecimal feeRate;

    @Value("${wallet.withdrawal.min-fee}")
    private BigDecimal minFee;

    @Value("${wallet.withdrawal.max-fee}")
    private BigDecimal maxFee;
}
