package petitus.petcareplus.dto.request.pet;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import petitus.petcareplus.utils.enums.Species;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminCreatePetRequest {
    private UUID ownerId;

    @NotBlank
    private String name;

    @NotNull
    private Integer age;

    private String dayOfBirth;

    @NotNull
    private Species species;
    private String breed;
    private String gender;
    private String size;
    private String imageUrl;

    private String description;
}
