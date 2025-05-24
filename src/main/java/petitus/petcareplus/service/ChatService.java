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
import petitus.petcareplus.dto.response.chat.ConversationResponse;
import petitus.petcareplus.model.ChatMessage;
import petitus.petcareplus.model.User;
import petitus.petcareplus.repository.ChatMessageRepository;
import petitus.petcareplus.utils.enums.Notifications;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

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
    public void sendMessage(ChatMessageRequest request, Principal principal) {
        UUID senderId = UUID.fromString(principal.getName());
        sendMessageInternal(request, senderId);
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
    public void markMessageAsRead(UUID otherUserId) {
        UUID currentUserId = userService.getCurrentUserId();

        chatMessageRepository.updateChatMessagesAsRead(
                otherUserId,
                currentUserId
        );
    }

    public long getUnreadMessageCount() {
        UUID currentUserId = userService.getCurrentUserId();
        return chatMessageRepository.countUnreadMessages(currentUserId);
    }

    public List<ConversationResponse> getAllConversations(int limit) {
        UUID currentUserId = userService.getCurrentUserId();
        List<Object[]> conversationResults = chatMessageRepository.findAllConversationUsersWithTimes(
                currentUserId,
                limit
        );

        List<UUID> userIds = extractUserIds(conversationResults);
        return buildConversationResponses(currentUserId, userIds);
    }

    public List<ConversationResponse> getAllConversationsWithKeyset(
            LocalDateTime lastMessageTime,
            int limit
    ) {
        UUID currentUserId = userService.getCurrentUserId();
        List<Object[]> conversationResults = chatMessageRepository.findAllConversationUsersWithKeyset(
                currentUserId,
                lastMessageTime,
                limit
        );

        List<UUID> userIds = extractUserIds(conversationResults);
        return buildConversationResponses(currentUserId, userIds);
    }

    private List<UUID> extractUserIds(List<Object[]> results) {
        return results.stream()
                .map(result -> UUID.fromString(result[0].toString()))
                .collect(Collectors.toList());
    }

    private List<ConversationResponse> buildConversationResponses(UUID currentUserId, List<UUID> userIds) {
        if (userIds.isEmpty()) {
            return new ArrayList<>();
        }

        Map<UUID, User> usersMap = userService.findAllByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, user -> user));

        return userIds.stream()
                .map(userId -> {
                    User user = usersMap.get(userId);
                    if (user == null) {
                        log.warn("User with ID {} not found", userId);
                        return null;
                    }

                    ChatMessage lastMessage = chatMessageRepository.findLatestMessageBetweenUsers(
                            currentUserId,
                            userId
                    );

                    if (lastMessage == null) {
                        log.warn("No messages found between users {} and {}", currentUserId, userId);
                        return null;
                    }

                    long unreadCount = 0;
                    if (lastMessage.getRecipientId().equals(currentUserId) && !lastMessage.getIsRead()) {
                        unreadCount = chatMessageRepository.countUnreadMessages(currentUserId);
                    }

                    // Get avatarUrl safely handling null profile
                    String avatarUrl = null;
                    if (user.getProfile() != null) {
                        avatarUrl = user.getProfile().getAvatarUrl();
                    }

                    return ConversationResponse.builder()
                            .userId(userId)
                            .userName(user.getFullName())
                            .userAvatarUrl(avatarUrl)
                            .lastMessage(lastMessage.getContent())
                            .lastMessageTime(lastMessage.getCreatedAt())
                            .hasUnreadMessages(unreadCount > 0)
                            .unreadCount(unreadCount)
                            .build();
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
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