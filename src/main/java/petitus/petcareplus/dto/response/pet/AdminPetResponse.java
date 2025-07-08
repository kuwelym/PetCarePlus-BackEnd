package petitus.petcareplus.dto.response.pet;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Builder;
import lombok.Data;
import petitus.petcareplus.utils.enums.Species;

@Data
@Builder
public class AdminPetResponse {
    private UUID id;
    private UUID userId;
    private String name;
    private Integer age;
    private String dayOfBirth;
    private Species species;
    private String breed;
    private String gender;
    private String size;
    private String description;
    private String imageUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
}
