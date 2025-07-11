package petitus.petcareplus.dto.request.auth;

import jakarta.validation.constraints.Email;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
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
