package petitus.petcareplus.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import petitus.petcareplus.dto.request.notification.NotificationRequest;
import petitus.petcareplus.model.ChatMessage;
import petitus.petcareplus.model.User;
import petitus.petcareplus.utils.enums.Notifications;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatNotificationService {
    private final NotificationService notificationService;
    private final FcmTokenService fcmTokenService;
    private final FirebaseMessagingService firebaseMessagingService;
    private final ActiveChatService activeChatService;

    /**
     * Create and send internal notification for new chat message
     */
    public void createNotification(ChatMessage chatMessage, UUID senderId) {
        NotificationRequest notificationRequest = NotificationRequest.builder()
                .userIdReceive(chatMessage.getRecipientId())
                .type(Notifications.CHAT)
                .title("New Message")
                .message(chatMessage.getContent())
                .relatedId(chatMessage.getId())
                .build();

        notificationService.pushNotification(notificationRequest, senderId);
    }

    /**
     * Send FCM push notification for new chat message
     */
    public void sendFcmNotification(ChatMessage chatMessage, User sender) {
        // Check if recipient is currently in active chat with sender
        boolean isRecipientInActiveChat = activeChatService.isUserInActiveChatWith(
                chatMessage.getRecipientId(), 
                chatMessage.getSenderId()
        );
        
        if (isRecipientInActiveChat) {
            log.debug("Skipping FCM notification - recipient {} is in active chat with sender {}", 
                    chatMessage.getRecipientId(), chatMessage.getSenderId());
            return;
        }
        
        List<String> receiverTokens = fcmTokenService.getUserTokens(chatMessage.getRecipientId());
        if (!receiverTokens.isEmpty()) {
            String title = "New message from " + sender.getFullName();
            String body = chatMessage.getContent();

            Map<String, String> data = createFcmNotificationData(
                    chatMessage.getId().toString(),
                    sender.getId().toString());

            for (String token : receiverTokens) {
                firebaseMessagingService.sendNotification(token, title, body, data);
            }
            
            log.debug("Sent FCM notification to {} tokens for message from {} to {}", 
                    receiverTokens.size(), chatMessage.getSenderId(), chatMessage.getRecipientId());
        }
    }

    /**
     * Create FCM notification data payload
     */
    private Map<String, String> createFcmNotificationData(String messageId, String senderId) {
        Map<String, String> data = new HashMap<>();
        data.put("type", Notifications.CHAT.name());
        data.put("messageId", messageId);
        data.put("senderId", senderId);
        data.put("sentAt", LocalDateTime.now().toString());
        return data;
    }
} 