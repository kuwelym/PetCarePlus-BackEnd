package petitus.petcareplus.dto.request.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class ResendEmailVerificationRequest {
    @NotBlank(message = "{email_required}")
    @Email(message = "{email_invalid}")
    private String email;
}
