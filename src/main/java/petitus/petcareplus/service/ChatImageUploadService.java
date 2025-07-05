package petitus.petcareplus.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import petitus.petcareplus.dto.response.chat.ChatMessageResponse;
import petitus.petcareplus.dto.response.chat.ImageUploadResponse;
import petitus.petcareplus.event.ImageUploadCompletedEvent;
import petitus.petcareplus.event.ImageUploadErrorEvent;
import petitus.petcareplus.model.ChatMessage;
import petitus.petcareplus.model.ChatImageMessage;
import petitus.petcareplus.repository.ChatMessageRepository;
import petitus.petcareplus.repository.ChatImageMessageRepository;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatImageUploadService {
    private final ChatImageMessageRepository chatImageMessageRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final CloudinaryService cloudinaryService;
    private final ApplicationEventPublisher eventPublisher;

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
            
            return ChatMessageResponse.from(savedMessage);
            
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
        long startTime = System.currentTimeMillis();
        
        try {
            performImageUpload(imageDataBase64, originalResponse, messageId);
        } catch (Exception e) {
            long processingTime = System.currentTimeMillis() - startTime;
            log.error("Unexpected error during async image upload for message ID: {} after {}ms",
                    messageId, processingTime, e);
            handleUploadFailure(messageId, originalResponse.getSenderId(), 
                    "Unexpected error occurred while uploading image: " + e.getMessage());
        }
    }

    /**
     * Perform the actual image upload and processing
     */
    private void performImageUpload(String imageDataBase64, ImageUploadResponse originalResponse, UUID messageId) {
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
            String errorMessage = String.format("Image too large. Maximum size is %dMB, but received %.1fMB", 
                    maxSizeMB, imageBytes.length / (1024.0 * 1024.0));
            handleUploadFailure(messageId, originalResponse.getSenderId(), errorMessage);
            return;
        }
        
        // Check for minimum viable image size
        if (imageBytes.length < 1024) { // Less than 1KB is suspicious
            handleUploadFailure(messageId, originalResponse.getSenderId(), "Image file is too small or corrupted");
            return;
        }
        
        // Upload to Cloudinary with timeout consideration
        Map<String, Object> uploadResult;
        try {
            uploadResult = cloudinaryService.uploadImage(imageBytes, "chat-images");
            
            if (uploadResult == null || !uploadResult.containsKey("secure_url")) {
                log.error("Cloudinary upload returned null or invalid result for message ID: {}", messageId);
                handleUploadFailure(messageId, originalResponse.getSenderId(), "Cloud storage upload failed");
                return;
            }
            
        } catch (IOException e) {
            handleUploadFailure(messageId, originalResponse.getSenderId(),
                    "Failed to upload image to cloud storage: " + e.getMessage());
            return;
        } catch (Exception e) {
            handleUploadFailure(messageId, originalResponse.getSenderId(),
                    "Unexpected cloud storage error: " + e.getMessage());
            return;
        }
        
        // Update the message with actual upload data
        Optional<ChatMessage> messageOpt = chatMessageRepository.findById(messageId);
        if (messageOpt.isEmpty()) {
            handleUploadFailure(messageId, originalResponse.getSenderId(), "Message not found in database");
            return;
        }
        
        if (!(messageOpt.get() instanceof ChatImageMessage chatImageMessage)) {
            handleUploadFailure(messageId, originalResponse.getSenderId(), "Invalid message type");
            return;
        }
        
        updateMessageWithUploadResults(chatImageMessage, uploadResult, originalResponse, messageId);
    }

    /**
     * Update message with upload results and notify users
     */
    private void updateMessageWithUploadResults(ChatImageMessage chatImageMessage, Map<String, Object> uploadResult, 
                                                ImageUploadResponse originalResponse, UUID messageId) {
        try {
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
            ImageUploadResponse completedResponse = ImageUploadResponse.from(updatedMessage, originalResponse);
            
            // Notify users of completion
            publishImageUploadCompletedEvent(completedResponse, messageId);
            
        } catch (Exception dbUpdateError) {
            log.error("Failed to update database for message ID: {}", messageId, dbUpdateError);
            handleUploadFailure(messageId, originalResponse.getSenderId(), "Failed to save upload results");
        }
    }

    /**
     * Publish image upload completed event with error handling
     */
    private void publishImageUploadCompletedEvent(ImageUploadResponse completedResponse, UUID messageId) {
        try {
            eventPublisher.publishEvent(new ImageUploadCompletedEvent(completedResponse));
        } catch (Exception eventError) {
            log.error("Failed to publish completion event for message ID: {}", messageId, eventError);
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