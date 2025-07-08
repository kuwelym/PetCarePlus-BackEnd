package petitus.petcareplus.model.spec.criteria;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PetCriteria {
    private String query;
    private String species;
    private String breed;
    private String gender;
    private Integer minAge;
    private Integer maxAge;
    private UUID ownerId;
}