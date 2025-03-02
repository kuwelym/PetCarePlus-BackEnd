package petitus.petcareplus.dto.request.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProviderServicePatchRequest {
    @DecimalMin(value = "0.0", inclusive = false, message = "Custom price must be greater than 0")
    private BigDecimal customPrice;
    
    private String customDescription;
}