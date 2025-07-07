package petitus.petcareplus.dto.response.chat;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserPresenceResponse {
    private String userId;
    private boolean online;
} 
