package petitus.petcareplus.dto.response.booking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingPetServiceResponse {
    private UUID petId;
    private String petName;
    private String petImageUrl;
    private UUID serviceId;
    private String serviceName;
    private BigDecimal price;
}