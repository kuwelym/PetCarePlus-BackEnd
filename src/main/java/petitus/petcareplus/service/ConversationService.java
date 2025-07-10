package petitus.petcareplus.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import petitus.petcareplus.dto.response.chat.ConversationResponse;
import petitus.petcareplus.model.ChatMessage;
import petitus.petcareplus.model.User;
import petitus.petcareplus.repository.ChatMessageRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConversationService {
    private final ChatMessageRepository chatMessageRepository;
    private final UserService userService;
    private final CipherService cipherService;

    /**
     * Get all conversations for a user with pagination
     */
    public List<ConversationResponse> getAllConversations(UUID currentUserId, int limit) {
        List<Object[]> conversationResults = chatMessageRepository.findAllConversationUsersWithTimes(
                currentUserId,
                limit);

        List<UUID> userIds = extractUserIds(conversationResults);
        return buildConversationResponses(currentUserId, userIds);
    }

    /**
     * Get all conversations with keyset pagination
     */
    public List<ConversationResponse> getAllConversationsWithKeyset(UUID currentUserId, 
            LocalDateTime lastMessageTime, int limit) {
        List<Object[]> conversationResults = chatMessageRepository.findAllConversationUsersWithKeyset(
                currentUserId,
                lastMessageTime,
                limit);

        List<UUID> userIds = extractUserIds(conversationResults);
        return buildConversationResponses(currentUserId, userIds);
    }

    /**
     * Get list of user IDs who have conversations with the specified user
     * Used for targeted presence notifications
     */
    public List<String> getConversationPartnerIds(UUID userId) {
        try {
            // Use optimized query that directly returns user IDs (no timestamps or extra data)
            return chatMessageRepository.findConversationPartnerIds(userId);
        } catch (Exception e) {
            log.error("Error getting conversation partner IDs for user {}: {}", userId, e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Extract user IDs from conversation query results
     */
    private List<UUID> extractUserIds(List<Object[]> results) {
        return results.stream()
                .map(result -> UUID.fromString(result[0].toString()))
                .toList();
    }

    /**
     * Build conversation responses from user IDs
     */
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
                            userId);

                    if (lastMessage == null) {
                        log.warn("No messages found between users {} and {}", currentUserId, userId);
                        return null;
                    }

                    long unreadCount = 0;
                    if (lastMessage.getRecipientId().equals(currentUserId) && Boolean.TRUE.equals(!lastMessage.getIsRead())) {
                        unreadCount = chatMessageRepository.countUnreadMessages(currentUserId);
                    }

                    // Get avatarUrl safely handling null profile
                    String avatarUrl = null;
                    if (user.getProfile() != null) {
                        avatarUrl = user.getProfile().getAvatarUrl();
                    }

                    // Format last message based on type
                    String displayMessage;
                    try {
                        // Decrypt the content before displaying
                        displayMessage = cipherService.decrypt(lastMessage.getContent());
                    } catch (Exception e) {
                        log.warn("Failed to decrypt last message content for conversation with user {}: {}", userId, e.getMessage());
                        displayMessage = "[Encrypted message]";
                    }
                    
                    return ConversationResponse.builder()
                            .userId(userId)
                            .userName(user.getFullName())
                            .userAvatarUrl(avatarUrl)
                            .lastMessage(displayMessage)
                            .lastMessageTime(lastMessage.getCreatedAt())
                            .lastMessageSenderId(lastMessage.getSenderId())
                            .hasUnreadMessages(unreadCount > 0)
                            .unreadCount(unreadCount)
                            .build();
                })
                .filter(Objects::nonNull)
                .toList();
    }
} 
