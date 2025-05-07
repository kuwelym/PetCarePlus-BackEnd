package petitus.petcareplus.dto.request.fcm;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class FcmTokenRequest {
    
    @NotBlank
    private String token;
} 