package petitus.petcareplus.dto.request.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import petitus.petcareplus.dto.annotation.DifferentPassword;
import petitus.petcareplus.dto.annotation.FieldMatch;

@Data
@NoArgsConstructor
@AllArgsConstructor
@DifferentPassword(
        currentPassword = "currentPassword",
        newPassword = "newPassword"
)
@FieldMatch(
        first = "newPassword",
        second = "confirmPassword"
)
public class ChangePasswordRequest {
    @NotBlank(message = "Current password is required")
    private String currentPassword;

    @NotBlank(message = "New password is required")
    private String newPassword;

    @NotBlank(message = "Confirm password is required")
    private String confirmPassword;
} 