package petitus.petcareplus.dto.request.service;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
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
public class ProviderServiceRequest {
    @NotNull(message = "Service ID is required")
    private UUID serviceId;

    @DecimalMin(value = "0.0", inclusive = false, message = "Custom price must be greater than 0")
    private BigDecimal customPrice;

    private String customDescription;
}