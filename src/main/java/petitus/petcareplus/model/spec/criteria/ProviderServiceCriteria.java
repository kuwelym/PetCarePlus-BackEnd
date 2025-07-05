package petitus.petcareplus.model.spec.criteria;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProviderServiceCriteria {
    private String query; // Search by service name or provider name
    private UUID providerId; // Filter by specific provider
    private UUID serviceId; // Filter by specific service
    private BigDecimal minCustomPrice; // Filter by minimum price
    private BigDecimal maxCustomPrice; // Filter by maximum price
    // private String location; // Filter by provider location
    private LocalDateTime createdAtStart; // Filter from date
    private LocalDateTime createdAtEnd; // Filter to date
    private Boolean isDeleted; // Filter by deleted status (for admin)
}