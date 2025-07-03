package petitus.petcareplus.dto.response.booking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import petitus.petcareplus.dto.response.service.ProviderServiceResponse;
import petitus.petcareplus.dto.response.user.UserResponse;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminBookingResponse {
    private UUID id;
    private UserResponse user;
    private ProviderServiceResponse providerService;
    private String status;
    private BigDecimal totalPrice;
    private String paymentStatus;
    private LocalDateTime bookingTime;
    private LocalDateTime scheduledStartTime;
    private LocalDateTime scheduledEndTime;
    private LocalDateTime actualEndTime;
    private String cancellationReason;
    private String note;
    private List<BookingPetServiceResponse> petList;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
}