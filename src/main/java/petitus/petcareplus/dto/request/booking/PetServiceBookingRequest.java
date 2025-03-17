package petitus.petcareplus.dto.request.booking;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PetServiceBookingRequest {

    @NotNull(message = "Pet ID is required")
    private UUID petId;

    @NotNull(message = "Service ID is required")
    private UUID serviceId;
}