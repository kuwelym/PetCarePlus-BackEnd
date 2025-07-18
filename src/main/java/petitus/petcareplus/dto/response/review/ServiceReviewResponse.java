package petitus.petcareplus.dto.response.review;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceReviewResponse {
    private UUID id;
    private UUID userId;
    private String userName;
    private String userAvatar;
    private UUID providerId;
    private String providerName;
    private UUID serviceId;
    private String serviceName;
    private UUID providerServiceId;
    private UUID bookingId;
    private Integer rating;
    private String comment;
    private Integer ratingHistory;
    private String commentHistory;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}