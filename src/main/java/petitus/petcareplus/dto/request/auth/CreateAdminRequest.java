package petitus.petcareplus.dto.request.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import petitus.petcareplus.dto.annotation.FieldMatch;
import petitus.petcareplus.dto.annotation.Password;

@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldMatch(first = "password", second = "passwordConfirm", message = "{password_mismatch}")
public class CreateAdminRequest {
    @NotBlank(message = "Email is required")
    @Email(message = "{email_invalid}")
    private String email;

    @NotBlank(message = "Password is required")
    @Password(message = "{invalid_password}")
    private String password;

    @NotBlank(message = "Password confirmation is required")
    private String passwordConfirm;

    @NotBlank(message = "Name is required")
    @Size(max = 50, message = "Name must not exceed 50 characters")
    private String name;

    @NotBlank(message = "Last name is required")
    @Size(max = 50, message = "Last name must not exceed 50 characters")
    private String lastName;
}