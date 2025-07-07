package petitus.petcareplus.dto.response.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import petitus.petcareplus.model.ChatImageMessage;
import petitus.petcareplus.model.UploadStatus;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageUploadResponse {
    
    private UUID id;
    private UUID senderId;
    private UUID recipientId;
    private String imageUrl;
    private String publicId; // Cloudinary public ID for future operations
    private String imageName;
    private String mimeType;
    private String caption;
    private Long fileSize;
    private Integer width;
    private Integer height;
    private LocalDateTime uploadedAt;
    private boolean isRead;
    
    // Thumbnail URLs for different sizes
    private String thumbnailUrl;
    private String mediumUrl;
    private String largeUrl;
    
    private UploadStatus uploadStatus;
    
    /**
     * Convert ChatImageMessage to ImageUploadResponse
     */
    public static ImageUploadResponse from(ChatImageMessage message, ImageUploadResponse originalResponse) {
        return ImageUploadResponse.builder()
                .id(message.getId())
                .senderId(message.getSenderId())
                .recipientId(message.getRecipientId())
                .caption(message.getCaption())
                .imageUrl(message.getImageUrl())
                .publicId(message.getPublicId())
                .imageName(message.getImageName())
                .mimeType(message.getMimeType())
                .fileSize(message.getFileSize())
                .width(message.getWidth())
                .height(message.getHeight())
                .thumbnailUrl(message.getThumbnailUrl())
                .mediumUrl(message.getMediumUrl())
                .largeUrl(message.getLargeUrl())
                .uploadedAt(originalResponse.getUploadedAt())
                .isRead(message.getIsRead())
                .uploadStatus(message.getUploadStatus())
                .build();
    }
}
