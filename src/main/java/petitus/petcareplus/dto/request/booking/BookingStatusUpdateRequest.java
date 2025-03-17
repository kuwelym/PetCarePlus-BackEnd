package petitus.petcareplus.dto.request.booking;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import petitus.petcareplus.enums.BookingStatus;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request payload for updating booking status")
public class BookingStatusUpdateRequest {

    @NotNull(message = "Status is required")
    @Schema(description = "New status for the booking", example = "ACCEPTED", requiredMode = Schema.RequiredMode.REQUIRED)
    private BookingStatus status;

    @Schema(description = "Reason for cancellation, required when status is CANCELLED", example = "Schedule conflict with another appointment")
    private String cancellationReason;
}