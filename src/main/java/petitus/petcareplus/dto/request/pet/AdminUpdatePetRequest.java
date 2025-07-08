package petitus.petcareplus.dto.request.pet;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import petitus.petcareplus.utils.enums.Species;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUpdatePetRequest {
    private String name;
    private Integer age;
    private Species species;
    private String breed;
    private String dayOfBirth;
    private String gender;
    private String size;
    private String description;
    private String imageUrl;
}
