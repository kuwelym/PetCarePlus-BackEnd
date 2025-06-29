package petitus.petcareplus.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import petitus.petcareplus.dto.request.chat.ReadReceiptRequest;
import petitus.petcareplus.dto.request.chat.TypingEvent;
import petitus.petcareplus.dto.request.chat.UserPresenceRequest;
import petitus.petcareplus.dto.response.chat.ChatMessageResponse;
import petitus.petcareplus.dto.response.chat.ConversationResponse;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketService {

    private static final String USER_DESTINATION_PREFIX = "/user/";
    
    private final SimpMessagingTemplate messagingTemplate;
    private final ChatService chatService;
    
    // Track online users

    public void sendMessage(ChatMessageResponse chatMessageResponse) {
        String messageDestination = USER_DESTINATION_PREFIX + chatMessageResponse.getRecipientId() + "/queue/messages";
        messagingTemplate.convertAndSend(messageDestination, chatMessageResponse);
        
        // Send the specific message to both users instead of full conversation updates
        sendMessageUpdate(chatMessageResponse.getSenderId(), chatMessageResponse);
        sendMessageUpdate(chatMessageResponse.getRecipientId(), chatMessageResponse);
    }
    
    public void sendMessageUpdate(UUID userId, ChatMessageResponse message) {
        try {
            // Send the specific message data via WebSocket
            String destination = USER_DESTINATION_PREFIX + userId.toString() + "/queue/message-update";
            messagingTemplate.convertAndSend(destination, message);
        } catch (Exception e) {
            log.error("Error sending message update: {}", e.getMessage(), e);
        }
    }
    
    public void sendUpdatedConversations(UUID userId) {
        try {
            // Fetch the user's updated conversations
            List<ConversationResponse> conversations = chatService.getAllConversations(20);
            
            // Send the actual conversation data via WebSocket
            String destination = USER_DESTINATION_PREFIX + userId.toString() + "/queue/conversation-update";
            messagingTemplate.convertAndSend(destination, conversations);
        } catch (Exception e) {
            // Fallback to simple refresh signal if there's an error
            String destination = USER_DESTINATION_PREFIX + userId.toString() + "/queue/conversation-update";
            messagingTemplate.convertAndSend(destination, "refresh");
        }
    }

    public void notifyTyping(TypingEvent event) {
        String destination = "/queue/typing/" + event.getRecipientId();
        messagingTemplate.convertAndSend(destination, event);
    }

    public void notifyMessageRead(ReadReceiptRequest receipt) {
        String destination = "/queue/read-status/" + receipt.getSenderId();
        messagingTemplate.convertAndSend(destination, receipt);
    }
    
    public void handleMarkAsRead(ReadReceiptRequest readReceiptRequest, UUID readerId) {
        try {
            // Update messages as read in the database
            UUID otherUserId = UUID.fromString(readReceiptRequest.getSenderId());
            chatService.markMessageAsRead(readerId, otherUserId);

            // Send read receipt to the sender
            sendReadReceipt(otherUserId, readReceiptRequest);
            sendReadReceipt(readerId, readReceiptRequest);

            // Send updated conversation data to both users to reflect read status
            sendUpdatedConversations(readerId);
            sendUpdatedConversations(otherUserId);
            
        } catch (Exception e) {
            log.error("Error handling mark as read: {}", e.getMessage(), e);
        }
    }

    public void sendReadReceipt(UUID userId, ReadReceiptRequest readReceiptRequest) {
        String readReceiptDestination = USER_DESTINATION_PREFIX + userId + "/queue/read-receipt";
        messagingTemplate.convertAndSend(readReceiptDestination, readReceiptRequest);
    }
    
    public void handleUserPresence(UUID userId, boolean isOnline) {
        try {
            String userIdStr = userId.toString();
            

            // Create presence update message
            UserPresenceRequest presenceUpdate = new UserPresenceRequest(userIdStr, isOnline);
            
            // Broadcast to all users via topic
            messagingTemplate.convertAndSend("/topic/user-status", presenceUpdate);
            
            log.info("User {} is now {}", userIdStr, isOnline ? "online" : "offline");
            
        } catch (Exception e) {
            log.error("Error handling user presence: {}", e.getMessage(), e);
        }
    }
    
    public void handleHeartbeat(UUID userId) {
        // Keep user online and update their presence
        handleUserPresence(userId, true);
    }

    /**
     * Send image message to recipient
     */
    public void sendImageMessage(petitus.petcareplus.dto.response.chat.ImageUploadResponse imageUploadResponse) {
        try {
            // Send to recipient
            String recipientDestination = USER_DESTINATION_PREFIX + imageUploadResponse.getRecipientId() + "/queue/image-messages";
            messagingTemplate.convertAndSend(recipientDestination, imageUploadResponse);
            
            // Send confirmation to sender
            String senderDestination = USER_DESTINATION_PREFIX + imageUploadResponse.getSenderId() + "/queue/image-message-confirm";
            messagingTemplate.convertAndSend(senderDestination, imageUploadResponse);
            
            log.info("Image message sent from {} to {}", imageUploadResponse.getSenderId(), imageUploadResponse.getRecipientId());
                             
        } catch (Exception e) {
            log.error("Error sending image message: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Send image upload error to user
     */
    public void sendImageUploadError(UUID userId, String errorMessage) {
        try {
            Map<String, Object> errorResponse = Map.of(
                "error", "Image upload failed",
                "message", errorMessage,
                "timestamp", System.currentTimeMillis()
            );
            
            String destination = USER_DESTINATION_PREFIX + userId + "/queue/image-upload-error";
            messagingTemplate.convertAndSend(destination, errorResponse);
            
            log.info("Image upload error sent to user: {}", userId);
            
        } catch (Exception e) {
            log.error("Error sending image upload error: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Notify about image deletion
     */
    public void notifyImageDeleted(String messageId, UUID userId) {
        try {
            Map<String, Object> deleteNotification = Map.of(
                "messageId", messageId,
                "deletedBy", userId.toString(),
                "deletedAt", System.currentTimeMillis()
            );
            
            String destination = USER_DESTINATION_PREFIX + userId + "/queue/image-deleted";
            messagingTemplate.convertAndSend(destination, deleteNotification);
            
            log.info("Image deletion notification sent for message: {}", messageId);
            
        } catch (Exception e) {
            log.error("Error sending image deletion notification: {}", e.getMessage(), e);
        }
    }
} 
