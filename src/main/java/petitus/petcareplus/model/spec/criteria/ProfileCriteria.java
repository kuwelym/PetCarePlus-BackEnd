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
public final class ProfileCriteria {
    private String query;

    private Boolean isServiceProvider;

    private String location;

    private Integer rating;

    private List<String> skills;

    private LocalDateTime availableAtStart;

    private LocalDateTime availableAtEnd;

    private String availableTime;

    private LocalDateTime createdAtStart;

    private LocalDateTime createdAtEnd;
}
