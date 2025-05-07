package petitus.petcareplus.dto.request.chat;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TypingEvent {
    private String senderId;
    private String recipientId;
    private boolean typing;      // true = typing, false = stopped
}
