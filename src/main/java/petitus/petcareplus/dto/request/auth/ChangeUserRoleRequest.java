package petitus.petcareplus.dto.request.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChangeUserRoleRequest {
    @NotBlank(message = "Role is required")
    @Pattern(regexp = "ADMIN|SERVICE_PROVIDER|USER", message = "Role must be ADMIN, SERVICE_PROVIDER or USER")
    private String role;
}