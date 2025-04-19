package petitus.petcareplus.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "chat_messages", indexes = {
    @Index(columnList = "sender_id, recipient_id", name = "idx_chat_messages_sender_recipient"),
    @Index(columnList = "recipient_id, is_read", name = "idx_chat_messages_recipient_read"),
    @Index(columnList = "created_at", name = "idx_chat_messages_created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessage extends AbstractBaseEntity {

    @Column(nullable = false)
    private UUID senderId;

    @Column(nullable = false)
    private UUID recipientId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    private LocalDateTime readAt;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isRead = false;
} 