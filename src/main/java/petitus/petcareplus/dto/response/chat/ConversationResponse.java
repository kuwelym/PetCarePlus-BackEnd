package petitus.petcareplus.dto.response.chat;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
public class ConversationResponse {
    private UUID userId;
    private String userName;
    private String lastMessage;
    private String userAvatarUrl;
    private LocalDateTime lastMessageTime;
    private UUID lastMessageSenderId;
    private Boolean hasUnreadMessages;
    private Long unreadCount;
}
