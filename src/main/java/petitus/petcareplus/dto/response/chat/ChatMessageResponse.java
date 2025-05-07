package petitus.petcareplus.dto.response.chat;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class ChatMessageResponse {
    private UUID id;
    private UUID senderId;
    private UUID recipientId;
    private String content;
    private LocalDateTime sentAt;
    private LocalDateTime readAt;
    private Boolean isRead;
} 