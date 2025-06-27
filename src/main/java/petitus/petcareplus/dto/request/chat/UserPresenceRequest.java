package petitus.petcareplus.dto.request.chat;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserPresenceRequest {
    private String userId;
    private boolean isOnline;
} 