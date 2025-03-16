package petitus.petcareplus.dto.request.pet;

import lombok.Data;
import petitus.petcareplus.utils.enums.Species;

@Data
public class UpdatePetRequest {
    private String name;
    private Integer age;
    private Species species;
    private String breed;
    private Boolean hasChip;
    private String vaccinated;
    private String imageUrl;
}
