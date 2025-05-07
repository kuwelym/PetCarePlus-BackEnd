package petitus.petcareplus.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import petitus.petcareplus.dto.request.chat.ChatMessageRequest;
import petitus.petcareplus.dto.request.notification.NotificationRequest;
import petitus.petcareplus.dto.response.chat.ChatMessageResponse;
import petitus.petcareplus.exceptions.ResourceNotFoundException;
import petitus.petcareplus.model.ChatMessage;
import petitus.petcareplus.model.User;
import petitus.petcareplus.repository.ChatMessageRepository;
import petitus.petcareplus.utils.enums.Notifications;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {
    private final ChatMessageRepository chatMessageRepository;
    private final UserService userService;
    private final NotificationService notificationService;
    private final FcmTokenService fcmTokenService;
    private final FirebaseMessagingService firebaseMessagingService;

    @Transactional
    public ChatMessageResponse sendMessage(ChatMessageRequest request) {
        UUID senderId = userService.getCurrentUserId();
        return sendMessageInternal(request, senderId);
    }

    @Transactional
    public ChatMessageResponse sendMessage(ChatMessageRequest request, Principal principal) {
        UUID senderId = UUID.fromString(principal.getName());
        return sendMessageInternal(request, senderId);
    }

    private ChatMessageResponse sendMessageInternal(ChatMessageRequest request, UUID senderId) {
        User sender = userService.findById(senderId);

        ChatMessage chatMessage = createAndSaveChatMessage(request, senderId);

        createNotification(chatMessage, senderId);

        sendFcmNotification(chatMessage, sender);

        return convertToResponse(chatMessage);
    }

    private ChatMessage createAndSaveChatMessage(ChatMessageRequest request, UUID senderId) {
        ChatMessage chatMessage = ChatMessage.builder()
                .senderId(senderId)
                .recipientId(request.getRecipientId())
                .content(request.getContent())
                .build();

        return chatMessageRepository.save(chatMessage);
    }

    private void createNotification(ChatMessage chatMessage, UUID senderId) {
        NotificationRequest notificationRequest = NotificationRequest.builder()
                .userIdReceive(chatMessage.getRecipientId())
                .type(Notifications.CHAT)
                .title("New Message")
                .message(chatMessage.getContent())
                .relatedId(chatMessage.getId())
                .build();

        notificationService.pushNotification(notificationRequest, senderId);
    }

    private void sendFcmNotification(ChatMessage chatMessage, User sender) {
        List<String> receiverTokens = fcmTokenService.getUserTokens(chatMessage.getRecipientId());
        if (!receiverTokens.isEmpty()) {
            String title = "New message from " + sender.getFullName();
            String body = chatMessage.getContent();

            Map<String, String> data = createFcmNotificationData(
                    chatMessage.getId().toString(),
                    sender.getId().toString()
            );

            for (String token : receiverTokens) {
                firebaseMessagingService.sendNotification(token, title, body, data);
            }
        }
    }

    private Map<String, String> createFcmNotificationData(String messageId, String senderId) {
        Map<String, String> data = new HashMap<>();
        data.put("type", Notifications.CHAT.name());
        data.put("messageId", messageId);
        data.put("senderId", senderId);
        data.put("sentAt", LocalDateTime.now().toString());
        return data;
    }

    public Page<ChatMessageResponse> getConversation(UUID otherUserId, Pageable pageable) {
        UUID currentUserId = userService.getCurrentUserId();
        return chatMessageRepository.findConversationBetweenUsers(currentUserId, otherUserId, pageable)
                .map(this::convertToResponse);
    }

    @Transactional
    public void markMessageAsRead(UUID messageId) {
        ChatMessage message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found"));

        if (!message.getIsRead()) {
            message.setIsRead(true);
            message.setReadAt(LocalDateTime.now());
            chatMessageRepository.save(message);
        }
    }

    public long getUnreadMessageCount() {
        UUID currentUserId = userService.getCurrentUserId();
        return chatMessageRepository.countUnreadMessages(currentUserId);
    }

    private ChatMessageResponse convertToResponse(ChatMessage message) {
        return ChatMessageResponse.builder()
                .id(message.getId())
                .senderId(message.getSenderId())
                .recipientId(message.getRecipientId())
                .content(message.getContent())
                .sentAt(message.getCreatedAt())
                .readAt(message.getReadAt())
                .isRead(message.getIsRead())
                .build();
    }
} 