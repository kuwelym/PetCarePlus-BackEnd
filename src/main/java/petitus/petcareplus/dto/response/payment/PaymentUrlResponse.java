package petitus.petcareplus.dto.response.payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentUrlResponse {
    private String paymentUrl;
    private String qrCode;
    private String orderCode;
    private String message;
    private String status;
    private String currency;
    private int amount;
}