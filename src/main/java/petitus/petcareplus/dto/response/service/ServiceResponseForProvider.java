package petitus.petcareplus.dto.response.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceResponseForProvider {
    private UUID id;
    private String name;
    private String description;
    private String iconUrl;
    private BigDecimal basePrice;

    private boolean serviceAvailable;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}