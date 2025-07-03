package petitus.petcareplus.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.RedisTemplate;
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

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketService {

    private static final String USER_DESTINATION_PREFIX = "/user/";
    private static final String REDIS_ONLINE_USERS_KEY = "chat:online_users";
    private static final int ONLINE_USER_TTL_SECONDS = 300; // 5 minutes TTL

    private final SimpMessagingTemplate messagingTemplate;
    @Lazy
    private final ChatService chatService;
    private final RedisTemplate<String, String> redisTemplate;

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
            String userIdStr = userId.toString();
            String redisKey = REDIS_ONLINE_USERS_KEY + ":" + userIdStr;

            if (isOnline) {
                redisTemplate.opsForValue().set(redisKey, "online", Duration.ofSeconds(ONLINE_USER_TTL_SECONDS));
                sendConversationPartnersStatus(userId);
            } else {
                redisTemplate.delete(redisKey);
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
                if (isUserOnline(partnerId)) {
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
     * Get current online users count from Redis
     */
    public int getOnlineUsersCount() {
        try {
            Set<String> keys = redisTemplate.keys(REDIS_ONLINE_USERS_KEY + ":*");
            return keys.size();
        } catch (Exception e) {
            log.error("Error getting online users count from Redis: {}", e.getMessage(), e);
            return 0;
        }
    }

    /**
     * Check if a user is currently online in Redis
     */
    public boolean isUserOnline(String userId) {
        try {
            String redisKey = REDIS_ONLINE_USERS_KEY + ":" + userId;
            return Boolean.TRUE.equals(redisTemplate.hasKey(redisKey));
        } catch (Exception e) {
            log.error("Error checking user online status in Redis: {}", e.getMessage(), e);
            return false;
        }
    }

    public void handleHeartbeat(UUID userId) {
        try {
            // Refresh TTL in Redis to keep user online
            String redisKey = REDIS_ONLINE_USERS_KEY + ":" + userId.toString();

            if (Boolean.TRUE.equals(redisTemplate.hasKey(redisKey))) {
                // Refresh the TTL without triggering presence broadcast
                redisTemplate.expire(redisKey, Duration.ofSeconds(ONLINE_USER_TTL_SECONDS));
                log.debug("Refreshed TTL for user: {}", userId);
            } else {
                handleUserPresence(userId, true);
            }
        } catch (Exception e) {
            log.error("Error handling heartbeat for user {}: {}", userId, e.getMessage(), e);
            handleUserPresence(userId, true);
        }
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
            log.info("WebSocket session disconnected for user: {}", userId);
            handleUserPresence(UUID.fromString(userId), false);
        }
    }
} 
