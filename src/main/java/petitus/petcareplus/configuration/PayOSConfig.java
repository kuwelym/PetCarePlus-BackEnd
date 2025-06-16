package petitus.petcareplus.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.Data;
import vn.payos.PayOS;

@Configuration
@Data
public class PayOSConfig {
    @Value("${payos.client-id}")
    private String clientId;

    @Value("${payos.api-key}")
    private String apiKey;

    @Value("${payos.checksum-key}")
    private String checkSumKey;

    @Value("${payos.return-url}")
    private String returnUrl;

    @Value("${payos.cancel-url}")
    private String cancelUrl;

    @Value("${payos.webhook-url}")
    private String webhookUrl;

    @Bean
    public PayOS payOS() {
        return new PayOS(clientId, apiKey, checkSumKey);
    }
}
