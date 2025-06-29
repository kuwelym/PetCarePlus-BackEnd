package petitus.petcareplus.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;
import petitus.petcareplus.dto.response.chat.ChatMessageResponse;
import petitus.petcareplus.dto.request.chat.ChatMessageRequest;
import petitus.petcareplus.dto.request.chat.ReadReceiptRequest;
import petitus.petcareplus.dto.request.chat.TypingEvent;
import petitus.petcareplus.dto.request.chat.UserPresenceRequest;
import petitus.petcareplus.dto.request.chat.ImageUploadRequest;
import petitus.petcareplus.dto.response.chat.ImageUploadResponse;
import petitus.petcareplus.service.ChatService;
import petitus.petcareplus.service.WebSocketService;
import petitus.petcareplus.service.CloudinaryService;

import java.io.IOException;
import java.security.Principal;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
@Slf4j
public class WebSocketController {

    private final WebSocketService webSocketService;
    private final ChatService chatService;
    private final CloudinaryService cloudinaryService;

    @MessageMapping("/chat.sendMessage")
    public void sendMessage(
            @Payload ChatMessageRequest chatMessageRequest,
            Principal principal
    ) {
        chatMessageRequest.setSenderId(UUID.fromString(principal.getName()));
        
        ChatMessageResponse response = chatService.sendMessage(chatMessageRequest, principal);

        webSocketService.sendMessage(response);
    }

    @MessageMapping("/chat.typing")
    public void handleTyping(@Payload TypingEvent event, SimpMessageHeaderAccessor headerAccessor) {
        String userId = (String) headerAccessor.getSessionAttributes().get("userId");
        event.setSenderId(userId);
        webSocketService.notifyTyping(event);
    }

    @MessageMapping("/chat.read")
    public void handleMessageRead(@Payload ReadReceiptRequest receipt) {
        webSocketService.notifyMessageRead(receipt);
    }
    
    @MessageMapping("/chat.markAsRead")
    public void handleMarkAsRead(
            @Payload ReadReceiptRequest readReceiptRequest,
            Principal principal
    ) {
        try {
            webSocketService.handleMarkAsRead(readReceiptRequest,
                UUID.fromString(principal.getName()));
        } catch (Exception e) {
            log.error("Error processing mark as read request", e);
        }
    }
    
    @MessageMapping("/user.presence")
    public void handleUserPresence(
            @Payload UserPresenceRequest presenceRequest,
            Principal principal
    ) {
        try {
            UUID userId = UUID.fromString(principal.getName());
            webSocketService.handleUserPresence(userId, presenceRequest.isOnline());
        } catch (Exception e) {
            log.error("Error processing user presence", e);
        }
    }
    
    @MessageMapping("/heartbeat")
    public void handleHeartbeat(
            @Payload Map<String, Object> heartbeat,
            Principal principal
    ) {
        try {
            UUID userId = UUID.fromString(principal.getName());
            webSocketService.handleHeartbeat(userId);
        } catch (Exception e) {
            log.error("Error processing heartbeat", e);
        }
    }

    /**
     * Handle image upload through WebSocket
     */
    @MessageMapping("/chat.uploadImage")
    public void uploadImage(
            @Payload ImageUploadRequest imageUploadRequest,
            Principal principal
    ) {
        try {
            UUID senderId = UUID.fromString(principal.getName());
            imageUploadRequest.setSenderId(senderId);

            log.info("Processing image upload from user {} to user {}",
                    senderId, imageUploadRequest.getRecipientId());

            // Validate image data exists
            if (imageUploadRequest.getImageData() == null || imageUploadRequest.getImageData().trim().isEmpty()) {
                webSocketService.sendImageUploadError(senderId, "Image data is required");
                return;
            }

            // Check Base64 size (approximate check before decoding)
            String imageDataBase64 = imageUploadRequest.getImageData();
            long estimatedSizeBytes = (long) (imageDataBase64.length() * 0.75); // Base64 to binary conversion
            long maxSizeMB = 5; // 5MB limit
            long maxSizeBytes = maxSizeMB * 1024 * 1024;
            
            if (estimatedSizeBytes > maxSizeBytes) {
                webSocketService.sendImageUploadError(senderId, 
                    String.format("Image too large. Maximum size is %dMB, but received approximately %.1fMB", 
                        maxSizeMB, estimatedSizeBytes / (1024.0 * 1024.0)));
                return;
            }

            // Decode base64 image data
            byte[] imageBytes = decodeBase64ImageData(imageDataBase64, senderId);
            if (imageBytes == null) {
                return; // Error already handled in the method
            }
            
            // Double-check actual decoded size
            if (imageBytes.length > maxSizeBytes) {
                webSocketService.sendImageUploadError(senderId, 
                    String.format("Image too large. Maximum size is %dMB, but received %.1fMB", 
                        maxSizeMB, imageBytes.length / (1024.0 * 1024.0)));
                return;
            }
            
            // Upload to Cloudinary
            Map<String, Object> uploadResult = cloudinaryService.uploadImage(imageBytes, "chat-images");
            
            // Create response with all image information
            ImageUploadResponse response = ImageUploadResponse.builder()
                    .senderId(senderId)
                    .recipientId(imageUploadRequest.getRecipientId())
                    .imageUrl((String) uploadResult.get("secure_url"))
                    .publicId((String) uploadResult.get("public_id"))
                    .imageName(imageUploadRequest.getImageName() != null ? imageUploadRequest.getImageName() : "image")
                    .mimeType(imageUploadRequest.getMimeType() != null ? imageUploadRequest.getMimeType() : "image/jpeg")
                    .caption(imageUploadRequest.getCaption())
                    .fileSize(((Number) uploadResult.get("bytes")).longValue())
                    .width((Integer) uploadResult.get("width"))
                    .height((Integer) uploadResult.get("height"))
                    .uploadedAt(java.time.LocalDateTime.now())
                    .isRead(false)
                    .build();
            
            // Generate different sized URLs
            String publicId = (String) uploadResult.get("public_id");
            response.setThumbnailUrl(cloudinaryService.generateOptimizedUrl(publicId, 150, 150));
            response.setMediumUrl(cloudinaryService.generateOptimizedUrl(publicId, 400, 400));
            response.setLargeUrl(cloudinaryService.generateOptimizedUrl(publicId, 800, 800));
            
            // Save image message to database
            ChatMessageResponse savedMessage = chatService.saveImageMessage(response, senderId);
            
            // Update response with database ID
            response.setId(savedMessage.getId());
            
            // Send image message to recipient
            webSocketService.sendImageMessage(response);
            
            log.info("Image uploaded successfully from {} to {}, URL: {}, Size: {}KB", 
                    senderId, imageUploadRequest.getRecipientId(), response.getImageUrl(), 
                    imageBytes.length / 1024);

        } catch (IOException e) {
            log.error("Error uploading image to Cloudinary", e);
            webSocketService.sendImageUploadError(UUID.fromString(principal.getName()), 
                    "Failed to upload image to cloud storage: " + e.getMessage());
        } catch (Exception e) {
            log.error("Error processing image upload", e);
            webSocketService.sendImageUploadError(UUID.fromString(principal.getName()), 
                    "Unexpected error occurred while uploading image");
        }
    }
    
    /**
     * Decode Base64 image data with error handling
     * 
     * @param imageDataBase64 The Base64 encoded image data
     * @param senderId The sender's UUID for error reporting
     * @return Decoded image bytes, or null if decoding failed
     */
    private byte[] decodeBase64ImageData(String imageDataBase64, UUID senderId) {
        try {
            return Base64.getDecoder().decode(imageDataBase64);
        } catch (IllegalArgumentException e) {
            webSocketService.sendImageUploadError(senderId, "Invalid image data format");
            return null;
        }
    }
    
    /**
     * Handle image deletion through WebSocket
     */
    @MessageMapping("/chat.deleteImage")
    public void deleteImage(
            @Payload Map<String, String> request,
            Principal principal
    ) {
        try {
            UUID userId = UUID.fromString(principal.getName());
            String publicId = request.get("publicId");
            String messageId = request.get("messageId");
            
            log.info("Processing image deletion request from user {} for publicId: {}", userId, publicId);
            
            // Delete from Cloudinary
            Map<String, Object> deleteResult = cloudinaryService.deleteImage(publicId);
            
            if ("ok".equals(deleteResult.get("result"))) {
                // Notify about successful deletion
                webSocketService.notifyImageDeleted(messageId, userId);
                log.info("Image deleted successfully: {}", publicId);
            } else {
                log.warn("Image deletion failed for publicId: {}", publicId);
            }
            
        } catch (IOException e) {
            log.error("Error deleting image from Cloudinary", e);
        } catch (Exception e) {
            log.error("Error processing image deletion", e);
        }
    }
} 
