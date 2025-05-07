package petitus.petcareplus.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import petitus.petcareplus.dto.request.chat.ChatMessageRequest;
import petitus.petcareplus.dto.request.fcm.FcmTokenRequest;
import petitus.petcareplus.dto.response.chat.ChatMessageResponse;
import petitus.petcareplus.security.jwt.JwtTokenProvider;
import petitus.petcareplus.service.ChatService;
import petitus.petcareplus.service.FcmTokenService;

import java.util.UUID;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Tag(name = "Chat", description = "Chat API")
public class ChatController {

    private final ChatService chatService;
    private final FcmTokenService fcmTokenService;

    @PostMapping("/messages")
    @Operation(summary = "Send a chat message", description = "Send a chat message to another user", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ChatMessageResponse> sendMessage(
            @RequestBody @Valid ChatMessageRequest request) {
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(chatService.sendMessage(request));
    }

    @GetMapping("/conversations/{userId}")
    @Operation(summary = "Get conversation with a user", description = "Get the conversation history with a specific user", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Page<ChatMessageResponse>> getConversation(
            @Parameter(description = "User ID to get conversation with") @PathVariable UUID userId,
            Pageable pageable) {

        return ResponseEntity.ok(chatService.getConversation(userId, pageable));
    }

    @PutMapping("/messages/{messageId}/read")
    @Operation(summary = "Mark message as read", description = "Mark a specific message as read", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Void> markMessageAsRead(
            @Parameter(description = "Message ID to mark as read") @PathVariable UUID messageId) {
        
        chatService.markMessageAsRead(messageId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/unread/count")
    @Operation(summary = "Get unread message count", description = "Get the count of unread messages", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Long> getUnreadMessageCount() {
        
        return ResponseEntity.ok(chatService.getUnreadMessageCount());
    }

    @PostMapping("/fcm/token")
    @Operation(summary = "Register FCM token", description = "Register a Firebase Cloud Messaging token for push notifications", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Void> registerFcmToken(
            @RequestBody @Valid FcmTokenRequest request) {
        
        fcmTokenService.saveToken(request.getToken());
        return ResponseEntity.ok().build();
    }
} 