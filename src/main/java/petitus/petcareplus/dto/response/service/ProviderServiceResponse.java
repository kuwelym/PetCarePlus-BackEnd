package petitus.petcareplus.dto.response.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProviderServiceResponse {
    private UUID providerId;
    private UUID serviceId;
    private String serviceName;
    private String providerName;
    private BigDecimal basePrice;
    private BigDecimal customPrice;
    private String customDescription;
    private String iconUrl;
    private LocalDateTime createdAt;
}