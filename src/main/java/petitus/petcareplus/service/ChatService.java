package petitus.petcareplus.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import petitus.petcareplus.dto.request.chat.ChatMessageRequest;
import petitus.petcareplus.dto.response.chat.ChatMessageResponse;
import petitus.petcareplus.dto.response.chat.ConversationResponse;
import petitus.petcareplus.dto.response.chat.ImageUploadResponse;
import petitus.petcareplus.model.ChatImageMessage;
import petitus.petcareplus.model.ChatMessage;
import petitus.petcareplus.model.User;
import petitus.petcareplus.repository.ChatMessageRepository;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {
    private final ChatMessageRepository chatMessageRepository;
    private final UserService userService;
    private final ChatNotificationService chatNotificationService;
    private final ChatImageUploadService chatImageUploadService;
    private final ConversationService conversationService;

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

        chatNotificationService.createNotification(chatMessage, senderId);

        chatNotificationService.sendFcmNotification(chatMessage, sender);

        return ChatMessageResponse.from(chatMessage);
    }

    private ChatMessage createAndSaveChatMessage(ChatMessageRequest request, UUID senderId) {
        ChatMessage chatMessage = ChatMessage.builder()
                .senderId(senderId)
                .recipientId(request.getRecipientId())
                .content(request.getContent())
                .isRead(false)
                .build();

        return chatMessageRepository.save(chatMessage);
    }

    public Page<ChatMessageResponse> getConversation(UUID otherUserId, Pageable pageable) {
        UUID currentUserId = userService.getCurrentUserId();
        return chatMessageRepository.findConversationBetweenUsers(currentUserId, otherUserId, pageable)
                .map(ChatMessageResponse::from);
    }

    public List<ChatMessageResponse> getConversationWithKeyset(UUID otherUserId, LocalDateTime lastMessageTime, int limit) {
        UUID currentUserId = userService.getCurrentUserId();
        
        // Create a custom pageable for keyset pagination
        PageRequest pageRequest = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt"));
        
        Page<ChatMessage> messagesPage;
        if (lastMessageTime != null) {
            // Get messages older than lastMessageTime
            messagesPage = chatMessageRepository.findConversationBetweenUsersOlderThan(
                currentUserId, otherUserId, lastMessageTime, pageRequest
            );
        } else {
            // Get the most recent messages
            messagesPage = chatMessageRepository.findConversationBetweenUsers(
                currentUserId, otherUserId, pageRequest
            );
        }
        
        return messagesPage.getContent().stream()
                .map(ChatMessageResponse::from)
                .toList();
    }

    @Transactional
    public List<UUID> markMessageAsRead(UUID currentUserId, UUID otherUserId) {
        return chatMessageRepository.updateChatMessagesAsRead(
                otherUserId,
                currentUserId);
    }

    public long getUnreadMessageCount() {
        UUID currentUserId = userService.getCurrentUserId();
        return chatMessageRepository.countUnreadMessages(currentUserId);
    }

    /**
     * Get list of user IDs who have conversations with the specified user
     * Used for targeted presence notifications
     */
    public List<String> getConversationPartnerIds(UUID userId) {
        return conversationService.getConversationPartnerIds(userId);
    }

    public List<ConversationResponse> getAllConversations(int limit) {
        UUID currentUserId = userService.getCurrentUserId();
        return conversationService.getAllConversations(currentUserId, limit);
    }

    public List<ConversationResponse> getAllConversationsWithKeyset(
            LocalDateTime lastMessageTime,
            int limit) {
        UUID currentUserId = userService.getCurrentUserId();
        return conversationService.getAllConversationsWithKeyset(currentUserId, lastMessageTime, limit);
    }

    // Delegate image upload methods to ChatImageUploadService
    @Transactional
    public ChatMessageResponse savePendingImageMessage(ImageUploadResponse imageUploadResponse, UUID senderId) {
        return chatImageUploadService.savePendingImageMessage(imageUploadResponse, senderId);
    }

    public void processImageUploadAsync(String imageDataBase64, ImageUploadResponse originalResponse, UUID messageId) {
        chatImageUploadService.processImageUploadAsync(imageDataBase64, originalResponse, messageId);
    }
}
