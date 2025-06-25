package petitus.petcareplus.model.spec.criteria;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import petitus.petcareplus.utils.Constants;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public final class UserCriteria {
    private List<Constants.RoleEnum> roles;

    private LocalDateTime createdAtStart;

    private LocalDateTime createdAtEnd;

    private Boolean isEmailActivated;

    private String query;

    private Boolean isBlocked;
}