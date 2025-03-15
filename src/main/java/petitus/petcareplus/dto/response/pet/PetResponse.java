package petitus.petcareplus.dto.response.pet;

import lombok.Builder;
import lombok.Data;
import petitus.petcareplus.utils.enums.Species;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class PetResponse {

    private UUID id;
    private UUID userId;
    private String name;
    private Integer age;
    private Species species;
    private String breed;
    private Boolean hasChip;
    private String vaccinated;
    private String imageUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

}
