package petitus.petcareplus.dto.request.pet;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import petitus.petcareplus.utils.enums.Species;

@Data
public class CreatePetRequest {
    @NotBlank
    private String name;

    @NotNull
    private Integer age;

    @NotNull
    private Species species;
    private String breed;
    private Boolean hasChip;
    private String vaccinated;
    private String imageUrl;
}
