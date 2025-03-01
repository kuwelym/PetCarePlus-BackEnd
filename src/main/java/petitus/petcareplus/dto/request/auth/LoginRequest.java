package petitus.petcareplus.dto.request.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import petitus.petcareplus.dto.annotation.Password;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    @NotBlank(message = "{email_required}")
    @Email(message = "{email_invalid}")
    private String email;

    @NotBlank(message = "{password_required}")
    @Password
    private String password;
}