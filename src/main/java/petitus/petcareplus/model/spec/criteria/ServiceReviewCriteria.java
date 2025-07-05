package petitus.petcareplus.model.spec.criteria;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ServiceReviewCriteria {
    private String query; // Search by comment or user name
    private UUID userId; // Filter by reviewer
    private UUID providerId; // Filter by provider
    private UUID serviceId; // Filter by service
    private UUID providerServiceId; // Filter by provider service
    private Integer minRating; // Filter by minimum rating
    private Integer maxRating; // Filter by maximum rating
    private Integer rating; // Filter by exact rating
    private LocalDateTime createdAtStart; // Filter from date
    private LocalDateTime createdAtEnd; // Filter to date
    private Boolean hasComment; // Filter reviews with/without comments
    private Boolean isDeleted; // Filter by deleted status (for admin)
}