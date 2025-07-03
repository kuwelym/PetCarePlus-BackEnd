package petitus.petcareplus.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import petitus.petcareplus.dto.request.chat.ChatMessageRequest;
import petitus.petcareplus.dto.request.notification.NotificationRequest;
import petitus.petcareplus.dto.response.chat.ChatMessageResponse;
import petitus.petcareplus.dto.response.chat.ImageUploadResponse;
import petitus.petcareplus.dto.response.chat.ConversationResponse;
import petitus.petcareplus.event.ImageUploadCompletedEvent;
import petitus.petcareplus.event.ImageUploadErrorEvent;
import petitus.petcareplus.model.ChatMessage;
import petitus.petcareplus.model.ChatImageMessage;
import petitus.petcareplus.model.User;
import petitus.petcareplus.repository.ChatMessageRepository;
import petitus.petcareplus.repository.ChatImageMessageRepository;
import petitus.petcareplus.utils.enums.Notifications;

import java.io.IOException;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {
    private final ChatMessageRepository chatMessageRepository;
    private final ChatImageMessageRepository chatImageMessageRepository;
    private final UserService userService;
    private final NotificationService notificationService;
    private final FcmTokenService fcmTokenService;
    private final FirebaseMessagingService firebaseMessagingService;
    private final CloudinaryService cloudinaryService;
    private final ApplicationEventPublisher eventPublisher;

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
                .isRead(false)
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
                    sender.getId().toString());

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
                .map(this::convertToResponse)
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
        try {
            // Use optimized query that directly returns user IDs (no timestamps or extra data)
            return chatMessageRepository.findConversationPartnerIds(userId);
        } catch (Exception e) {
            log.error("Error getting conversation partner IDs for user {}: {}", userId, e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    public List<ConversationResponse> getAllConversations(int limit) {
        UUID currentUserId = userService.getCurrentUserId();
        List<Object[]> conversationResults = chatMessageRepository.findAllConversationUsersWithTimes(
                currentUserId,
                limit);

        List<UUID> userIds = extractUserIds(conversationResults);
        return buildConversationResponses(currentUserId, userIds);
    }

    public List<ConversationResponse> getAllConversationsWithKeyset(
            LocalDateTime lastMessageTime,
            int limit) {
        UUID currentUserId = userService.getCurrentUserId();
        List<Object[]> conversationResults = chatMessageRepository.findAllConversationUsersWithKeyset(
                currentUserId,
                lastMessageTime,
                limit);

        List<UUID> userIds = extractUserIds(conversationResults);
        return buildConversationResponses(currentUserId, userIds);
    }

    private List<UUID> extractUserIds(List<Object[]> results) {
        return results.stream()
                .map(result -> UUID.fromString(result[0].toString()))
                .toList();
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
                    String displayMessage = lastMessage.getDisplayContent();
                    
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

    private ChatMessageResponse convertToResponse(ChatMessage message) {
        ChatMessageResponse.ChatMessageResponseBuilder builder = ChatMessageResponse.builder()
                .id(message.getId())
                .senderId(message.getSenderId())
                .recipientId(message.getRecipientId())
                .messageType(message.getMessageType())
                .content(message.getContent())
                .sentAt(message.getCreatedAt())
                .readAt(message.getReadAt())
                .isRead(message.getIsRead())
                .uploadStatus(message.getUploadStatus()); // Include upload status for all message types

        // If it's an image message, add image-specific fields
        if (message instanceof ChatImageMessage imageMessage) {
            builder.imageUrl(imageMessage.getImageUrl())
                    .publicId(imageMessage.getPublicId())
                    .caption(imageMessage.getCaption())
                    .imageName(imageMessage.getImageName())
                    .mimeType(imageMessage.getMimeType())
                    .fileSize(imageMessage.getFileSize())
                    .width(imageMessage.getWidth())
                    .height(imageMessage.getHeight())
                    .thumbnailUrl(imageMessage.getThumbnailUrl())
                    .mediumUrl(imageMessage.getMediumUrl())
                    .largeUrl(imageMessage.getLargeUrl());
        }

        return builder.build();
    }

    /**
     * Save pending image message (optimistic UI approach)
     */
    @Transactional
    public ChatMessageResponse savePendingImageMessage(ImageUploadResponse imageUploadResponse, UUID senderId) {
        try {
            // Create ChatImageMessage with pending status
            ChatImageMessage chatImageMessage = new ChatImageMessage();
            chatImageMessage.setSenderId(senderId);
            chatImageMessage.setRecipientId(imageUploadResponse.getRecipientId());
            chatImageMessage.setCreatedAt(LocalDateTime.now());
            chatImageMessage.setIsRead(false);
            chatImageMessage.setCaption(imageUploadResponse.getCaption());
            
            // Set content for compatibility
            String content = (imageUploadResponse.getCaption() != null && !imageUploadResponse.getCaption().trim().isEmpty()) 
                    ? imageUploadResponse.getCaption() 
                    : "Image";
            chatImageMessage.setContent(content);
            
            // Set image fields with temporary data
            chatImageMessage.setImageUrl(imageUploadResponse.getImageUrl());
            chatImageMessage.setPublicId(imageUploadResponse.getPublicId());
            chatImageMessage.setImageName(imageUploadResponse.getImageName());
            chatImageMessage.setMimeType(imageUploadResponse.getMimeType());
            chatImageMessage.setFileSize(imageUploadResponse.getFileSize());
            chatImageMessage.setWidth(imageUploadResponse.getWidth());
            chatImageMessage.setHeight(imageUploadResponse.getHeight());
            chatImageMessage.setUploadStatus(petitus.petcareplus.model.UploadStatus.PENDING);
            
            // Save to database
            ChatImageMessage savedMessage = chatImageMessageRepository.save(chatImageMessage);
            
            log.info("Pending image message saved with ID: {}", savedMessage.getId());
            
            return convertToResponse(savedMessage);
            
        } catch (Exception e) {
            log.error("Error saving pending image message", e);
            throw new RuntimeException("Failed to save pending image message", e);
        }
    }

    /**
     * Process image upload asynchronously
     */
    @Async
    @Transactional
    public void processImageUploadAsync(String imageDataBase64, ImageUploadResponse originalResponse, UUID messageId) {
        try {
            log.info("Starting async image upload process for message ID: {}", messageId);
            
            // Decode base64 image data
            byte[] imageBytes;
            try {
                imageBytes = Base64.getDecoder().decode(imageDataBase64);
            } catch (IllegalArgumentException e) {
                handleUploadFailure(messageId, originalResponse.getSenderId(), "Invalid image data format");
                return;
            }
            
            // Double-check actual decoded size
            long maxSizeMB = 5; // 5MB limit
            long maxSizeBytes = maxSizeMB * 1024 * 1024;
            if (imageBytes.length > maxSizeBytes) {
                handleUploadFailure(messageId, originalResponse.getSenderId(), 
                    String.format("Image too large. Maximum size is %dMB, but received %.1fMB", 
                        maxSizeMB, imageBytes.length / (1024.0 * 1024.0)));
                return;
            }
            
            // Upload to Cloudinary
            Map<String, Object> uploadResult = cloudinaryService.uploadImage(imageBytes, "chat-images");
            
            // Update the message with actual upload data
            Optional<ChatMessage> messageOpt = chatMessageRepository.findById(messageId);
            if (messageOpt.isPresent() && messageOpt.get() instanceof ChatImageMessage chatImageMessage) {
                
                // Update with actual Cloudinary data
                chatImageMessage.setImageUrl((String) uploadResult.get("secure_url"));
                chatImageMessage.setPublicId((String) uploadResult.get("public_id"));
                chatImageMessage.setFileSize(((Number) uploadResult.get("bytes")).longValue());
                chatImageMessage.setWidth((Integer) uploadResult.get("width"));
                chatImageMessage.setHeight((Integer) uploadResult.get("height"));
                chatImageMessage.setUploadStatus(petitus.petcareplus.model.UploadStatus.COMPLETED);
                
                // Generate different sized URLs
                String publicId = (String) uploadResult.get("public_id");
                chatImageMessage.setThumbnailUrl(cloudinaryService.generateOptimizedUrl(publicId, 150, 150));
                chatImageMessage.setMediumUrl(cloudinaryService.generateOptimizedUrl(publicId, 400, 400));
                chatImageMessage.setLargeUrl(cloudinaryService.generateOptimizedUrl(publicId, 800, 800));
                
                // Save updated message
                ChatImageMessage updatedMessage = chatImageMessageRepository.save(chatImageMessage);
                
                // Create updated response
                ImageUploadResponse completedResponse = ImageUploadResponse.builder()
                        .id(updatedMessage.getId())
                        .senderId(updatedMessage.getSenderId())
                        .recipientId(updatedMessage.getRecipientId())
                        .caption(updatedMessage.getCaption())
                        .imageUrl(updatedMessage.getImageUrl())
                        .publicId(updatedMessage.getPublicId())
                        .imageName(updatedMessage.getImageName())
                        .mimeType(updatedMessage.getMimeType())
                        .fileSize(updatedMessage.getFileSize())
                        .width(updatedMessage.getWidth())
                        .height(updatedMessage.getHeight())
                        .thumbnailUrl(updatedMessage.getThumbnailUrl())
                        .mediumUrl(updatedMessage.getMediumUrl())
                        .largeUrl(updatedMessage.getLargeUrl())
                        .uploadedAt(originalResponse.getUploadedAt())
                        .isRead(updatedMessage.getIsRead())
                        .uploadStatus(updatedMessage.getUploadStatus())
                        .build();
                
                // Notify users of completion
                eventPublisher.publishEvent(new ImageUploadCompletedEvent(completedResponse));
                
                log.info("Image upload completed successfully for message ID: {}, URL: {}, Size: {}KB", 
                        messageId, updatedMessage.getImageUrl(), imageBytes.length / 1024);
                
            } else {
                log.error("Message not found or not an image message: {}", messageId);
                handleUploadFailure(messageId, originalResponse.getSenderId(), "Message not found");
            }
            
        } catch (IOException e) {
            log.error("Error uploading image to Cloudinary for message ID: {}", messageId, e);
            handleUploadFailure(messageId, originalResponse.getSenderId(), 
                    "Failed to upload image to cloud storage: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error during async image upload for message ID: {}", messageId, e);
            handleUploadFailure(messageId, originalResponse.getSenderId(), 
                    "Unexpected error occurred while uploading image");
        }
    }

    /**
     * Handle upload failure by updating message status and notifying users
     */
    private void handleUploadFailure(UUID messageId, UUID senderId, String errorMessage) {
        try {
            // Update message status to FAILED
            Optional<ChatMessage> messageOpt = chatMessageRepository.findById(messageId);
            if (messageOpt.isPresent() && messageOpt.get() instanceof ChatImageMessage chatImageMessage) {
                chatImageMessage.setUploadStatus(petitus.petcareplus.model.UploadStatus.FAILED);
                chatImageMessageRepository.save(chatImageMessage);
            }
            
            // Notify user of failure
            eventPublisher.publishEvent(new ImageUploadErrorEvent(senderId, errorMessage));
            
            log.warn("Image upload failed for message ID: {}, reason: {}", messageId, errorMessage);
            
        } catch (Exception e) {
            log.error("Error handling upload failure for message ID: {}", messageId, e);
        }
    }
}
