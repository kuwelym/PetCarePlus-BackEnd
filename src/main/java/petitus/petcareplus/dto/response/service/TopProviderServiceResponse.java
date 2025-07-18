package petitus.petcareplus.dto.response.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopProviderServiceResponse {
    private UUID id;
    private UUID providerId;
    private String providerName;
    private String providerAvatarUrl;
    private UUID serviceId;
    private String serviceName;
    private String serviceIconUrl;
    private BigDecimal customPrice;
    private BigDecimal basePrice;
    private String customDescription;
    private Long totalBookings;
}