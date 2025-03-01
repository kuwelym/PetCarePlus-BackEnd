package petitus.petcareplus.dto.request.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import petitus.petcareplus.dto.annotation.FieldMatch;
import petitus.petcareplus.dto.annotation.Password;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@FieldMatch(first = "password", second = "confirmPassword")
public class RegisterRequest {

    @NotBlank(message = "{email_required}")
    @Email(message = "{email_invalid}")
    private String email;

    @NotBlank(message = "{password_required}")
    @Password
    private String password;

    @NotBlank(message = "{confirmPassword_required}")
    private String confirmPassword;

    @NotBlank(message = "{name_required}")
    private String name;

    @NotBlank(message = "{lastName_required}")
    private String lastName;
}