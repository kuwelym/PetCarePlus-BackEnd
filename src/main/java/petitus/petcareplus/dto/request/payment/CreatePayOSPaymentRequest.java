package petitus.petcareplus.dto.request.payment;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
public class CreatePayOSPaymentRequest {
    @NotNull(message = "Booking ID is required")
    private UUID bookingId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "1000", message = "Amount must be at least 1000 VND")
    private BigDecimal amount;

    @NotNull(message = "Description is required")
    @Size(max = 255, message = "Description must not exceed 255 characters")
    private String description;

    @Size(max = 100, message = "Buyer name must not exceed 100 characters")
    private String buyerName;

    @Size(max = 100, message = "Buyer email must not exceed 100 characters")
    private String buyerEmail;

    @Size(max = 20, message = "Buyer phone must not exceed 20 characters")
    private String buyerPhone;

    @Size(max = 255, message = "Buyer address must not exceed 255 characters")
    private String buyerAddress;

    private List<PayOSItem> items;
}