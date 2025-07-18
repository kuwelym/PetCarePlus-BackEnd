package petitus.petcareplus.model.spec.criteria;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public final class ServiceProviderProfileCriteria {
    private String query;

    private String location;

    private String businessAddress;

    private Integer rating;

    private List<String> skills;

    private LocalDateTime availableAtStart;

    private LocalDateTime availableAtEnd;

    private String availableTime;

    private LocalDateTime createdAtStart;

    private LocalDateTime createdAtEnd;
} 
