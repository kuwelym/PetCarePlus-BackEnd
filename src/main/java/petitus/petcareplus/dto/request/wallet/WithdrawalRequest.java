package petitus.petcareplus.dto.request.wallet;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class WithdrawalRequest {

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "10000", message = "Minimum withdrawal amount is 10,000 VND")
    @DecimalMax(value = "50000000", message = "Maximum withdrawal amount is 50,000,000 VND")
    private BigDecimal amount;

    @NotBlank(message = "Bank code is required")
    private String bankCode;

    @NotBlank(message = "Bank name is required")
    private String bankName;

    @NotBlank(message = "Account number is required")
    @Pattern(regexp = "\\d{6,20}", message = "Account number must be 6-20 digits")
    private String accountNumber;

    @NotBlank(message = "Account holder name is required")
    @Size(max = 100, message = "Account holder name must not exceed 100 characters")
    private String accountHolderName;

    private String note;
}