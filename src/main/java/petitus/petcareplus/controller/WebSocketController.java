package petitus.petcareplus.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;
import petitus.petcareplus.dto.request.chat.*;
import petitus.petcareplus.dto.response.chat.ChatMessageResponse;
import petitus.petcareplus.dto.response.chat.ImageUploadResponse;
import petitus.petcareplus.service.ChatService;
import petitus.petcareplus.service.CloudinaryService;
import petitus.petcareplus.service.WebSocketService;

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

    @MessageMapping("/chat.activeStatus")
    public void handleActiveChat(
            @Payload ActiveChatRequest activeChatRequest,
            Principal principal
    ) {
        try {
            UUID userId = UUID.fromString(principal.getName());
            webSocketService.handleActiveChat(
                    userId,
                    activeChatRequest.getOtherUserId(),
                    activeChatRequest.isActive()
            );

        } catch (Exception e) {
            log.error("Error processing active chat status", e);
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
        UUID senderId = null;
        try {
            senderId = UUID.fromString(principal.getName());
            imageUploadRequest.setSenderId(senderId);

            // Basic validation
            if (imageUploadRequest.getImageData() == null || imageUploadRequest.getImageData().trim().isEmpty()) {
                webSocketService.sendImageUploadError(senderId, "Image data is required");
                return;
            }

            String imageDataBase64 = imageUploadRequest.getImageData().trim();
            
            // Validate Base64 format
            if (!imageDataBase64.matches("^[A-Za-z0-9+/]*={0,2}$")) {
                webSocketService.sendImageUploadError(senderId, "Invalid image data format");
                return;
            }

            // Check minimum size
            if (imageDataBase64.length() < 100) {
                webSocketService.sendImageUploadError(senderId, "Image data is too small or corrupted");
                return;
            }

            // Check size limit (5MB)
            long estimatedSizeBytes = (long) (imageDataBase64.length() * 0.75);
            long maxSizeBytes = 5 * 1024 * 1024; // 5MB

            if (estimatedSizeBytes > maxSizeBytes) {
                String errorMessage = String.format("Image too large. Maximum size is 5MB, but received approximately %.1fMB",
                        estimatedSizeBytes / (1024.0 * 1024.0));
                webSocketService.sendImageUploadError(senderId, errorMessage);
                return;
            }

            // Validate recipient
            if (imageUploadRequest.getRecipientId() == null) {
                webSocketService.sendImageUploadError(senderId, "Recipient ID is required");
                return;
            }

            // Create immediate response with PENDING status
            ImageUploadResponse pendingResponse = ImageUploadResponse.builder()
                    .senderId(senderId)
                    .recipientId(imageUploadRequest.getRecipientId())
                    .imageUrl("pending")
                    .publicId("pending")
                    .imageName(imageUploadRequest.getImageName() != null ? imageUploadRequest.getImageName() : "image")
                    .mimeType(imageUploadRequest.getMimeType() != null ? imageUploadRequest.getMimeType() : "image/jpeg")
                    .caption(imageUploadRequest.getCaption())
                    .fileSize(estimatedSizeBytes)
                    .width(0)
                    .height(0)
                    .uploadedAt(java.time.LocalDateTime.now())
                    .isRead(false)
                    .uploadStatus(petitus.petcareplus.model.UploadStatus.PENDING)
                    .build();

            // Save pending message to database
            ChatMessageResponse savedMessage;
            try {
                savedMessage = chatService.savePendingImageMessage(pendingResponse, senderId);
                pendingResponse.setId(savedMessage.getId());
            } catch (Exception dbError) {
                log.error("Failed to save pending image message to database", dbError);
                webSocketService.sendImageUploadError(senderId, "Failed to save image message");
                return;
            }

            // Send pending message to users immediately
            try {
                webSocketService.sendImageMessage(pendingResponse);
            } catch (Exception wsError) {
                log.error("Failed to send pending image message via WebSocket", wsError);
            }

            // Process upload asynchronously
            try {
                chatService.processImageUploadAsync(imageDataBase64, pendingResponse, savedMessage.getId());
            } catch (Exception asyncError) {
                log.error("Failed to start async image processing", asyncError);
                webSocketService.sendImageUploadError(senderId, "Failed to process image upload");
            }

        } catch (IllegalArgumentException e) {
            log.error("Invalid UUID format for principal: {}", principal.getName(), e);
            if (senderId != null) {
                webSocketService.sendImageUploadError(senderId, "Invalid user authentication");
            }
        } catch (Exception e) {
            log.error("Unexpected error during image upload processing", e);
            if (senderId != null) {
                webSocketService.sendImageUploadError(senderId, 
                        "Unexpected error occurred while uploading image: " + e.getMessage());
            }
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

            // Delete from Cloudinary
            Map<String, Object> deleteResult = cloudinaryService.deleteImage(publicId);

            if ("ok".equals(deleteResult.get("result"))) {
                webSocketService.notifyImageDeleted(messageId, userId);
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
