package petitus.petcareplus.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

import jakarta.persistence.Embeddable;

@Data
@Embeddable
@NoArgsConstructor
@AllArgsConstructor
public class PetBookingId implements Serializable {
    private UUID bookingId;
    private UUID petId;
    private UUID serviceId;
}