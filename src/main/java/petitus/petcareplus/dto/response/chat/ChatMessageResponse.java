package petitus.petcareplus.dto.response.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
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
} 
