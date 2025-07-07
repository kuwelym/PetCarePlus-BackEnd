package petitus.petcareplus.dto.request.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VerifyPasswordRequest {
    @NotBlank(message = "Password is required")
    private String password;
} 