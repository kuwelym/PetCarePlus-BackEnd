package petitus.petcareplus.dto.response.booking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingResponse {
    private UUID id;
    private UUID userId;
    private String userName;
    private String userAvatar;
    private UUID providerServiceId;
    private String serviceName;
    private UUID providerId;
    private String providerName;
    private String providerAvatar;
    private String status;
    private BigDecimal totalPrice;
    private String paymentStatus;
    private LocalDateTime bookingTime;
    private LocalDateTime scheduledStartTime;
    private LocalDateTime scheduledEndTime;
    private LocalDateTime actualEndTime;
    private String cancellationReason;
    private String note;
    private LocalDateTime createdAt;
    private List<BookingPetServiceResponse> petServices;
}