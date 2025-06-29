package petitus.petcareplus.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "chat_messages", indexes = {
    @Index(columnList = "sender_id, recipient_id", name = "idx_chat_messages_sender_recipient"),
    @Index(columnList = "recipient_id, is_read", name = "idx_chat_messages_recipient_read"),
    @Index(columnList = "created_at", name = "idx_chat_messages_created_at"),
    @Index(columnList = "message_type", name = "idx_chat_messages_message_type")
})
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "message_type", discriminatorType = DiscriminatorType.STRING)
@DiscriminatorValue("TEXT")
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

    // Content field for text messages
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    private LocalDateTime readAt;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isRead = false;

    // Upload status for tracking message processing (mainly for images, but required for all messages)
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private UploadStatus uploadStatus = UploadStatus.COMPLETED; // Text messages are immediately completed

    // Virtual method to get message type (can be overridden by subclasses)
    public MessageType getMessageType() {
        return MessageType.TEXT;
    }

    // Virtual method to get display content for conversations (can be overridden)
    public String getDisplayContent() {
        return content; // Text message content
    }
} 