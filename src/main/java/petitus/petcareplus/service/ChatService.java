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
    private final CipherService cipherService;

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

        return createDecryptedResponse(chatMessage);
    }

    private ChatMessage createAndSaveChatMessage(ChatMessageRequest request, UUID senderId) {
        // Encrypt the message content before saving
        String encryptedContent = cipherService.encrypt(request.getContent());
        
        ChatMessage chatMessage = ChatMessage.builder()
                .senderId(senderId)
                .recipientId(request.getRecipientId())
                .content(encryptedContent)
                .isRead(false)
                .build();

        return chatMessageRepository.save(chatMessage);
    }

    /**
     * Create a ChatMessageResponse with decrypted content
     */
    private ChatMessageResponse createDecryptedResponse(ChatMessage message) {
        // Decrypt the content before creating the response
        String decryptedContent;
        try {
            decryptedContent = cipherService.decrypt(message.getContent());
        } catch (Exception e) {
            log.warn("Failed to decrypt message content for message {}: {}", message.getId(), e.getMessage());
            decryptedContent = "[Decryption failed]";
        }
        
        // Handle different message types
        if (message instanceof ChatImageMessage imageMessage) {
            // For image messages, also decrypt the caption if it exists
            String decryptedCaption = null;
            if (imageMessage.getCaption() != null && !imageMessage.getCaption().trim().isEmpty()) {
                try {
                    decryptedCaption = cipherService.decrypt(imageMessage.getCaption());
                } catch (Exception e) {
                    log.warn("Failed to decrypt image caption for message {}: {}", message.getId(), e.getMessage());
                    decryptedCaption = "[Caption decryption failed]";
                }
            }
            
            // Decrypt all image URLs
            String decryptedImageUrl = null;
            String decryptedThumbnailUrl = null;
            String decryptedMediumUrl = null;
            String decryptedLargeUrl = null;
            
            try {
                if (imageMessage.getImageUrl() != null) {
                    decryptedImageUrl = cipherService.decrypt(imageMessage.getImageUrl());
                }
            } catch (Exception e) {
                log.warn("Failed to decrypt image URL for message {}: {}", message.getId(), e.getMessage());
                decryptedImageUrl = null;
            }
            
            try {
                if (imageMessage.getThumbnailUrl() != null) {
                    decryptedThumbnailUrl = cipherService.decrypt(imageMessage.getThumbnailUrl());
                }
            } catch (Exception e) {
                log.warn("Failed to decrypt thumbnail URL for message {}: {}", message.getId(), e.getMessage());
                decryptedThumbnailUrl = null;
            }
            
            try {
                if (imageMessage.getMediumUrl() != null) {
                    decryptedMediumUrl = cipherService.decrypt(imageMessage.getMediumUrl());
                }
            } catch (Exception e) {
                log.warn("Failed to decrypt medium URL for message {}: {}", message.getId(), e.getMessage());
                decryptedMediumUrl = null;
            }
            
            try {
                if (imageMessage.getLargeUrl() != null) {
                    decryptedLargeUrl = cipherService.decrypt(imageMessage.getLargeUrl());
                }
            } catch (Exception e) {
                log.warn("Failed to decrypt large URL for message {}: {}", message.getId(), e.getMessage());
                decryptedLargeUrl = null;
            }
            
            // Create a copy of the image message with decrypted content
            ChatImageMessage decryptedImageMessage = new ChatImageMessage();
            decryptedImageMessage.setSenderId(imageMessage.getSenderId());
            decryptedImageMessage.setRecipientId(imageMessage.getRecipientId());
            decryptedImageMessage.setIsRead(imageMessage.getIsRead());
            decryptedImageMessage.setUploadStatus(imageMessage.getUploadStatus());
            decryptedImageMessage.setReadAt(imageMessage.getReadAt());
            decryptedImageMessage.setCaption(decryptedCaption);
            decryptedImageMessage.setImageUrl(decryptedImageUrl);
            decryptedImageMessage.setPublicId(imageMessage.getPublicId());
            decryptedImageMessage.setImageName(imageMessage.getImageName());
            decryptedImageMessage.setMimeType(imageMessage.getMimeType());
            decryptedImageMessage.setFileSize(imageMessage.getFileSize());
            decryptedImageMessage.setWidth(imageMessage.getWidth());
            decryptedImageMessage.setHeight(imageMessage.getHeight());
            decryptedImageMessage.setThumbnailUrl(decryptedThumbnailUrl);
            decryptedImageMessage.setMediumUrl(decryptedMediumUrl);
            decryptedImageMessage.setLargeUrl(decryptedLargeUrl);
            
            // Copy the ID and timestamps from the original message
            decryptedImageMessage.setId(imageMessage.getId());
            decryptedImageMessage.setCreatedAt(imageMessage.getCreatedAt());
            decryptedImageMessage.setUpdatedAt(imageMessage.getUpdatedAt());
            
            return ChatMessageResponse.from(decryptedImageMessage);
        } else {
            // For regular text messages
            ChatMessage decryptedMessage = ChatMessage.builder()
                    .senderId(message.getSenderId())
                    .recipientId(message.getRecipientId())
                    .content(decryptedContent)
                    .isRead(message.getIsRead())
                    .uploadStatus(message.getUploadStatus())
                    .readAt(message.getReadAt())
                    .build();
            
            // Copy the ID and timestamps from the original message
            decryptedMessage.setId(message.getId());
            decryptedMessage.setCreatedAt(message.getCreatedAt());
            decryptedMessage.setUpdatedAt(message.getUpdatedAt());
            
            return ChatMessageResponse.from(decryptedMessage);
        }
    }

    public Page<ChatMessageResponse> getConversation(UUID otherUserId, Pageable pageable) {
        UUID currentUserId = userService.getCurrentUserId();
        Page<ChatMessage> a = chatMessageRepository.findConversationBetweenUsers(currentUserId, otherUserId, pageable);
                return a.map(this::createDecryptedResponse);
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
                .map(this::createDecryptedResponse)
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
