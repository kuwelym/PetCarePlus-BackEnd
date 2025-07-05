package petitus.petcareplus.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import petitus.petcareplus.dto.request.chat.ReadReceiptRequest;
import petitus.petcareplus.dto.request.chat.TypingEvent;
import petitus.petcareplus.dto.response.chat.ChatMessageResponse;
import petitus.petcareplus.dto.response.chat.ReadReceiptResponse;
import petitus.petcareplus.dto.response.chat.UserPresenceResponse;
import petitus.petcareplus.event.ImageUploadCompletedEvent;
import petitus.petcareplus.event.ImageUploadErrorEvent;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketService {

    private static final String USER_DESTINATION_PREFIX = "/user/";

    private final SimpMessagingTemplate messagingTemplate;
    @Lazy
    private final ChatService chatService;
    private final ActiveChatService activeChatService;
    private final OnlineUserService onlineUserService;

    public void sendMessage(ChatMessageResponse chatMessageResponse) {
        String messageDestination = USER_DESTINATION_PREFIX + chatMessageResponse.getRecipientId() + "/queue/messages";
        messagingTemplate.convertAndSend(messageDestination, chatMessageResponse);

        // Send the specific message to both users instead of full conversation updates
        sendMessageUpdate(chatMessageResponse.getSenderId(), chatMessageResponse);
        sendMessageUpdate(chatMessageResponse.getRecipientId(), chatMessageResponse);

        // Auto-mark as read if recipient is in active chat with sender
        autoMarkAsReadIfInActiveChat(chatMessageResponse);
    }

    /**
     * Automatically mark messages as read if the recipient is currently in active chat with the sender
     */
    private void autoMarkAsReadIfInActiveChat(ChatMessageResponse chatMessageResponse) {
        autoMarkAsReadIfInActiveChat(chatMessageResponse.getSenderId(), chatMessageResponse.getRecipientId());
    }

    /**
     * Core method to automatically mark messages as read if the recipient is currently in active chat with the sender
     */
    private void autoMarkAsReadIfInActiveChat(UUID senderId, UUID recipientId) {
        try {
            // Check if recipient is in active chat with sender
            if (activeChatService.isUserInActiveChatWith(recipientId, senderId)) {
                // Mark messages as read
                List<UUID> messageIds = chatService.markMessageAsRead(recipientId, senderId);

                if (!messageIds.isEmpty()) {
                    // Create and send read receipt to both users
                    ReadReceiptResponse readReceiptResponse = new ReadReceiptResponse(
                            messageIds,
                            recipientId.toString(),
                            senderId.toString()
                    );

                    sendReadReceipt(senderId, readReceiptResponse);
                    sendReadReceipt(recipientId, readReceiptResponse);
                }
            }
        } catch (Exception e) {
            log.error("Error auto-marking messages as read: {}", e.getMessage(), e);
        }
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

    public void notifyTyping(TypingEvent event) {
        String destination = "/queue/typing/" + event.getRecipientId();
        messagingTemplate.convertAndSend(destination, event);
    }

    public void handleMarkAsRead(ReadReceiptRequest readReceiptRequest, UUID readerId) {
        try {
            UUID otherUserId = UUID.fromString(readReceiptRequest.getSenderId());
            List<UUID> messageIds = chatService.markMessageAsRead(readerId, otherUserId);

            ReadReceiptResponse readReceiptResponse = new ReadReceiptResponse(
                    messageIds,
                    readerId.toString(),
                    otherUserId.toString()
            );
            sendReadReceipt(otherUserId, readReceiptResponse);
            sendReadReceipt(readerId, readReceiptResponse);

        } catch (Exception e) {
            log.error("Error handling mark as read: {}", e.getMessage(), e);
        }
    }

    public void sendReadReceipt(UUID userId, ReadReceiptResponse readReceiptResponse) {
        String readReceiptDestination = USER_DESTINATION_PREFIX + userId + "/queue/read-receipt";
        messagingTemplate.convertAndSend(readReceiptDestination, readReceiptResponse);
    }

    public void handleUserPresence(UUID userId, boolean isOnline) {
        try {
            // Delegate online status management to OnlineUserService
            onlineUserService.handleUserPresence(userId, isOnline);

            if (isOnline) {
                sendConversationPartnersStatus(userId);
            }

            notifyConversationPartners(userId, isOnline);

        } catch (Exception e) {
            log.error("Error handling user presence: {}", e.getMessage(), e);
        }
    }

    /**
     * Send online status of conversation partners to a newly connected user
     */
    public void sendConversationPartnersStatus(UUID userId) {
        try {
            List<String> conversationPartnerIds = chatService.getConversationPartnerIds(userId);

            for (String partnerId : conversationPartnerIds) {
                if (onlineUserService.isUserOnline(partnerId)) {
                    UserPresenceResponse presenceUpdate = new UserPresenceResponse(partnerId, true);
                    String destination = USER_DESTINATION_PREFIX + userId + "/queue/initial-online-users";
                    messagingTemplate.convertAndSend(destination, presenceUpdate);
                }
            }

        } catch (Exception e) {
            log.error("Error sending conversation partners status: {}", e.getMessage(), e);
        }
    }

    /**
     * Notify only users who have conversations with the specified user about their presence change
     */
    public void notifyConversationPartners(UUID userId, boolean isOnline) {
        try {
            List<String> conversationPartnerIds = chatService.getConversationPartnerIds(userId);

            UserPresenceResponse presenceUpdate = new UserPresenceResponse(userId.toString(), isOnline);
            for (String partnerId : conversationPartnerIds) {
                String destination = USER_DESTINATION_PREFIX + partnerId + "/queue/user-status";

                messagingTemplate.convertAndSend(destination, presenceUpdate);
            }

        } catch (Exception e) {
            log.error("Error notifying conversation partners: {}", e.getMessage(), e);
        }
    }

    /**
     * Handle active chat tracking - when user enters or leaves a specific chat conversation
     */
    public void handleActiveChat(UUID userId, UUID otherUserId, boolean isActive) {
        activeChatService.handleActiveChat(userId, otherUserId, isActive);
    }

    public void handleHeartbeat(UUID userId) {
        onlineUserService.handleHeartbeat(userId);
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

            autoMarkAsReadIfInActiveChatForImage(imageUploadResponse);

        } catch (Exception e) {
            log.error("Error sending image message: {}", e.getMessage(), e);
        }
    }

    /**
     * Automatically mark messages as read if the recipient is currently in active chat with the sender (for image messages)
     */
    private void autoMarkAsReadIfInActiveChatForImage(petitus.petcareplus.dto.response.chat.ImageUploadResponse imageUploadResponse) {
        autoMarkAsReadIfInActiveChat(imageUploadResponse.getSenderId(), imageUploadResponse.getRecipientId());
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

        } catch (Exception e) {
            log.error("Error sending image deletion notification: {}", e.getMessage(), e);
        }
    }

    /**
     * Send image upload completion notification to users
     */
    public void sendImageUploadCompleted(petitus.petcareplus.dto.response.chat.ImageUploadResponse imageUploadResponse) {
        try {
            // Send completed message to recipient
            String recipientDestination = USER_DESTINATION_PREFIX + imageUploadResponse.getRecipientId() + "/queue/image-upload-completed";
            messagingTemplate.convertAndSend(recipientDestination, imageUploadResponse);

            // Send completion confirmation to sender
            String senderDestination = USER_DESTINATION_PREFIX + imageUploadResponse.getSenderId() + "/queue/image-upload-completed";
            messagingTemplate.convertAndSend(senderDestination, imageUploadResponse);

            // Auto-mark as read if recipient is in active chat with sender
            autoMarkAsReadIfInActiveChatForImage(imageUploadResponse);

        } catch (Exception e) {
            log.error("Error sending image upload completion notification: {}", e.getMessage(), e);
        }
    }

    @EventListener
    public void handleImageUploadCompleted(ImageUploadCompletedEvent event) {
        sendImageUploadCompleted(event.getImageUploadResponse());
    }

    @EventListener
    public void handleImageUploadError(ImageUploadErrorEvent event) {
        sendImageUploadError(event.getUserId(), event.getErrorMessage());
    }

    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        if (headerAccessor.getUser() != null) {
            String userId = headerAccessor.getUser().getName();
            UUID userUUID = UUID.fromString(userId);

            handleUserPresence(userUUID, false);
            cleanupUserActiveChats(userUUID);
        }
    }

    /**
     * Clean up all active chats for a user (called on disconnect)
     */
    private void cleanupUserActiveChats(UUID userId) {
        activeChatService.cleanupUserActiveChats(userId);
    }
} 
