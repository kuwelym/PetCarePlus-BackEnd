package petitus.petcareplus.dto.request.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import petitus.petcareplus.dto.annotation.FieldMatch;

@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldMatch(
        first = "newPassword",
        second = "confirmPassword"
)
public class ResetPasswordRequest {
    @NotBlank(message = "Token is required")
    private String token;

    @NotBlank(message = "New password is required")
    private String newPassword;

    @NotBlank(message = "Password confirmation is required")
    private String confirmPassword;
} 