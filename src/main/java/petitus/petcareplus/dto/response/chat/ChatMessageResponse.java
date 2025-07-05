package petitus.petcareplus.dto.response.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import petitus.petcareplus.model.ChatMessage;
import petitus.petcareplus.model.ChatImageMessage;
import petitus.petcareplus.model.MessageType;
import petitus.petcareplus.model.UploadStatus;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageResponse {
    private UUID id;
    private UUID senderId;
    private UUID recipientId;
    private String content;
    private LocalDateTime sentAt;
    private LocalDateTime readAt;
    private boolean isRead;
    private MessageType messageType;
    
    // Image-specific fields
    private String imageUrl;
    private String publicId;
    private String caption;
    private String imageName;
    private String mimeType;
    private Long fileSize;
    private Integer width;
    private Integer height;
    private String thumbnailUrl;
    private String mediumUrl;
    private String largeUrl;
    private UploadStatus uploadStatus;
    
    /**
     * Convert ChatMessage to ChatMessageResponse
     */
    public static ChatMessageResponse from(ChatMessage message) {
        ChatMessageResponse.ChatMessageResponseBuilder builder = ChatMessageResponse.builder()
                .id(message.getId())
                .senderId(message.getSenderId())
                .recipientId(message.getRecipientId())
                .messageType(message.getMessageType())
                .content(message.getContent())
                .sentAt(message.getCreatedAt())
                .readAt(message.getReadAt())
                .isRead(message.getIsRead())
                .uploadStatus(message.getUploadStatus()); // Include upload status for all message types

        // If it's an image message, add image-specific fields
        if (message instanceof ChatImageMessage imageMessage) {
            builder.imageUrl(imageMessage.getImageUrl())
                    .publicId(imageMessage.getPublicId())
                    .caption(imageMessage.getCaption())
                    .imageName(imageMessage.getImageName())
                    .mimeType(imageMessage.getMimeType())
                    .fileSize(imageMessage.getFileSize())
                    .width(imageMessage.getWidth())
                    .height(imageMessage.getHeight())
                    .thumbnailUrl(imageMessage.getThumbnailUrl())
                    .mediumUrl(imageMessage.getMediumUrl())
                    .largeUrl(imageMessage.getLargeUrl());
        }

        return builder.build();
    }
} 
