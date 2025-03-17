package petitus.petcareplus.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@Data
@Embbeddable
@NoArgsConstructor
@AllArgsConstructor
public class ServiceBookingId implements Serializable {
    private UUID bookingId;
    private UUID serviceId;
}