package petitus.petcareplus.model.spec.criteria;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ServiceCriteria {
    private String query; // Search by name or description
    private BigDecimal minPrice; // Filter by minimum price
    private BigDecimal maxPrice; // Filter by maximum price
}