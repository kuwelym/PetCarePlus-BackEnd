package petitus.petcareplus.dto.response.chat;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ReadReceiptResponse {
    private List<UUID> messageIds;
    private String senderId;
    private String recipientId;
}
