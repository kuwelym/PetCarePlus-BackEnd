package petitus.petcareplus.dto.request.chat;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReadReceipt {
    private String messageId;
    private String readerId;
    private String senderId;
    private Instant readAt;       // Timestamp when the message was read
}
