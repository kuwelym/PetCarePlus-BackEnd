package petitus.petcareplus.dto.request.auth;

import jakarta.validation.constraints.Email;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import petitus.petcareplus.dto.annotation.FieldMatch;

@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
@FieldMatch(first = "password", second = "passwordConfirm", message = "{password_mismatch}")
public class UpdateUserRequest {
    @Email(message = "{email_invalid}")
    private String email;

    private String name;

    private String lastName;

    private String role;

    @Builder.Default
    private Boolean isEmailVerified = false;

    @Builder.Default
    private Boolean isBlocked = false;
}
