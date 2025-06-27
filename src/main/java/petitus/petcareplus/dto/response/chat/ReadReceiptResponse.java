package petitus.petcareplus.dto.response.chat;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ReadReceiptResponse {
    private String messageId;
    private String senderId;
    private String recipientId;
    private String readAt; // ISO 8601 format
}
