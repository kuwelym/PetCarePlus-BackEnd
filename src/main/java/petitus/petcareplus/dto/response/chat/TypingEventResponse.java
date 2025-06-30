package petitus.petcareplus.dto.response.chat;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TypingEventResponse {
    private String senderId;
    private String recipientId;
    private boolean typing;      // true = typing, false = stopped
}
