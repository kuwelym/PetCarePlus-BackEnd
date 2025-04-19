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
import java.util.*;

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
        User sender = userService.getUser();
        return sendMessageInternal(request, sender.getId());
    }

    @Transactional
    public ChatMessageResponse sendMessage(ChatMessageRequest request, Principal principal) {
        UUID senderId = UUID.fromString(principal.getName());
        return sendMessageInternal(request, senderId);
    }

    private ChatMessageResponse sendMessageInternal(ChatMessageRequest request, UUID senderId) {
        User sender = userService.findById(senderId);

        // Create and save chat message
        ChatMessage chatMessage = ChatMessage.builder()
                .senderId(senderId)
                .recipientId(request.getRecipientId())
                .content(request.getContent())
                .build();

        chatMessage = chatMessageRepository.save(chatMessage);

        // Create notification
        NotificationRequest notificationRequest = NotificationRequest.builder()
                .userIdReceive(request.getRecipientId())
                .type(Notifications.CHAT)
                .title("New Message")
                .message(request.getContent())
                .relatedId(chatMessage.getId())
                .build();

        notificationService.pushNotification(notificationRequest, senderId);

        // Send FCM notification
        List<String> receiverTokens = fcmTokenService.getUserTokens(request.getRecipientId());
        if (!receiverTokens.isEmpty()) {
            String title = "New message from " + sender.getFullName();
            String body = request.getContent();

            Map<String, String> data = new HashMap<>();
            data.put("type", "CHAT");
            data.put("messageId", chatMessage.getId().toString());
            data.put("senderId", senderId.toString());
            data.put("sentAt", chatMessage.getCreatedAt().toString());

            for (String token : receiverTokens) {
                firebaseMessagingService.sendNotification(token, title, body, data);
            }
        }

        return convertToResponse(chatMessage);
    }

    public Page<ChatMessageResponse> getConversation(UUID otherUserId, Pageable pageable) {
        User currentUser = userService.getUser();
        return chatMessageRepository.findConversationBetweenUsers(currentUser.getId(), otherUserId, pageable)
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
        User currentUser = userService.getUser();
        return chatMessageRepository.countUnreadMessages(currentUser.getId());
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