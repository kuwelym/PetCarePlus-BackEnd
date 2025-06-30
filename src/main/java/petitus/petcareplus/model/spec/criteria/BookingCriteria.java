package petitus.petcareplus.model.spec.criteria;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import petitus.petcareplus.utils.enums.BookingStatus;
import petitus.petcareplus.utils.enums.PaymentStatus;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BookingCriteria {
    private String query; // Search by comment or user name
    private BookingStatus status; // Filter by booking status
    private PaymentStatus paymentStatus; // Filter by payment status
    private UUID userId; // Filter by user ID
    private UUID providerId; // Filter by provider ID
    private Boolean isDeleted; // Filter by deleted status (for admin)
}
