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

            // Step 1: Create immediate response with PENDING status for optimistic UI
            ImageUploadResponse pendingResponse = ImageUploadResponse.builder()
                    .senderId(senderId)
                    .recipientId(imageUploadRequest.getRecipientId())
                    .imageUrl("pending") // Temporary placeholder
                    .publicId("pending") // Temporary placeholder
                    .imageName(imageUploadRequest.getImageName() != null ? imageUploadRequest.getImageName() : "image")
                    .mimeType(imageUploadRequest.getMimeType() != null ? imageUploadRequest.getMimeType() : "image/jpeg")
                    .caption(imageUploadRequest.getCaption())
                    .fileSize(estimatedSizeBytes) // Estimated size
                    .width(0) // Will be updated after upload
                    .height(0) // Will be updated after upload
                    .uploadedAt(java.time.LocalDateTime.now())
                    .isRead(false)
                    .uploadStatus(petitus.petcareplus.model.UploadStatus.PENDING)
                    .build();

            // Step 2: Save pending message to database immediately
            ChatMessageResponse savedMessage = chatService.savePendingImageMessage(pendingResponse, senderId);
            pendingResponse.setId(savedMessage.getId());

            // Step 3: Send pending message to users immediately (optimistic UI)
            webSocketService.sendImageMessage(pendingResponse);
            
            log.info("Pending image message sent immediately to users. Starting background upload...");

            // Step 4: Process upload asynchronously
            chatService.processImageUploadAsync(imageDataBase64, pendingResponse, savedMessage.getId());

        } catch (Exception e) {
            log.error("Error processing image upload", e);
            webSocketService.sendImageUploadError(UUID.fromString(principal.getName()), 
                    "Unexpected error occurred while uploading image");
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
