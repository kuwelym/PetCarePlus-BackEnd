package petitus.petcareplus.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import petitus.petcareplus.dto.response.chat.ChatMessageResponse;
import petitus.petcareplus.model.ChatImageMessage;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatMessageDecryptionUtil {
    
    private final CipherService cipherService;

    /**
     * Create a ChatMessageResponse with decrypted content for image messages
     */
    public ChatMessageResponse createDecryptedImageMessageResponse(ChatImageMessage imageMessage) {
        // Decrypt the caption if it exists
        String decryptedCaption = null;
        if (imageMessage.getCaption() != null && !imageMessage.getCaption().trim().isEmpty()) {
            try {
                decryptedCaption = cipherService.decrypt(imageMessage.getCaption());
            } catch (Exception e) {
                log.warn("Failed to decrypt image caption for message {}: {}", imageMessage.getId(), e.getMessage());
                decryptedCaption = "[Caption decryption failed]";
            }
        }
        
        // Decrypt all image URLs and publicId
        String decryptedImageUrl = decryptUrl(imageMessage.getImageUrl(), "image URL", imageMessage.getId());
        String decryptedThumbnailUrl = decryptUrl(imageMessage.getThumbnailUrl(), "thumbnail URL", imageMessage.getId());
        String decryptedMediumUrl = decryptUrl(imageMessage.getMediumUrl(), "medium URL", imageMessage.getId());
        String decryptedLargeUrl = decryptUrl(imageMessage.getLargeUrl(), "large URL", imageMessage.getId());
        String decryptedPublicId = decryptUrl(imageMessage.getPublicId(), "public ID", imageMessage.getId());
        
        // Create a copy of the image message with decrypted content
        ChatImageMessage decryptedImageMessage = new ChatImageMessage();
        decryptedImageMessage.setSenderId(imageMessage.getSenderId());
        decryptedImageMessage.setRecipientId(imageMessage.getRecipientId());
        decryptedImageMessage.setIsRead(imageMessage.getIsRead());
        decryptedImageMessage.setUploadStatus(imageMessage.getUploadStatus());
        decryptedImageMessage.setReadAt(imageMessage.getReadAt());
        decryptedImageMessage.setCaption(decryptedCaption);
        decryptedImageMessage.setImageUrl(decryptedImageUrl);
        decryptedImageMessage.setPublicId(decryptedPublicId);
        decryptedImageMessage.setImageName(imageMessage.getImageName());
        decryptedImageMessage.setMimeType(imageMessage.getMimeType());
        decryptedImageMessage.setFileSize(imageMessage.getFileSize());
        decryptedImageMessage.setWidth(imageMessage.getWidth());
        decryptedImageMessage.setHeight(imageMessage.getHeight());
        decryptedImageMessage.setThumbnailUrl(decryptedThumbnailUrl);
        decryptedImageMessage.setMediumUrl(decryptedMediumUrl);
        decryptedImageMessage.setLargeUrl(decryptedLargeUrl);
        
        // Copy the ID and timestamps from the original message
        decryptedImageMessage.setId(imageMessage.getId());
        decryptedImageMessage.setCreatedAt(imageMessage.getCreatedAt());
        decryptedImageMessage.setUpdatedAt(imageMessage.getUpdatedAt());
        
        return ChatMessageResponse.from(decryptedImageMessage);
    }

    /**
     * Helper method to decrypt a single URL with error handling
     */
    private String decryptUrl(String encryptedUrl, String urlType, UUID messageId) {
        if (encryptedUrl == null) {
            return null;
        }
        
        try {
            return cipherService.decrypt(encryptedUrl);
        } catch (Exception e) {
            log.warn("Failed to decrypt {} for message {}: {}", urlType, messageId, e.getMessage());
            return null;
        }
    }
} 