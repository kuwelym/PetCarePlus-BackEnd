package petitus.petcareplus.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;
import petitus.petcareplus.dto.response.chat.ChatMessageResponse;
import petitus.petcareplus.dto.request.chat.ChatMessageRequest;
import petitus.petcareplus.dto.request.chat.ReadReceiptRequest;
import petitus.petcareplus.dto.request.chat.TypingEvent;
import petitus.petcareplus.dto.request.chat.UserPresenceRequest;
import petitus.petcareplus.service.ChatService;
import petitus.petcareplus.service.WebSocketService;

import java.security.Principal;
import java.util.Map;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
@Slf4j
public class WebSocketController {

    private final WebSocketService webSocketService;
    private final ChatService chatService;

    @MessageMapping("/chat.sendMessage")
    public void sendMessage(
            @Payload ChatMessageRequest chatMessageRequest,
            Principal principal
    ) {
        chatMessageRequest.setSenderId(UUID.fromString(principal.getName()));
        
        ChatMessageResponse response = chatService.sendMessage(chatMessageRequest, principal);

        webSocketService.sendMessage(response);
    }

    @MessageMapping("/chat.typing")
    public void handleTyping(@Payload TypingEvent event, SimpMessageHeaderAccessor headerAccessor) {
        String userId = (String) headerAccessor.getSessionAttributes().get("userId");
        event.setSenderId(userId);
        webSocketService.notifyTyping(event);
    }

    @MessageMapping("/chat.read")
    public void handleMessageRead(@Payload ReadReceiptRequest receipt) {
        webSocketService.notifyMessageRead(receipt);
    }
    
    @MessageMapping("/chat.markAsRead")
    public void handleMarkAsRead(
            @Payload ReadReceiptRequest readReceiptRequest,
            Principal principal
    ) {
        try {
            webSocketService.handleMarkAsRead(readReceiptRequest,
                UUID.fromString(principal.getName()));
        } catch (Exception e) {
            log.error("Error processing mark as read request", e);
        }
    }
    
    @MessageMapping("/user.presence")
    public void handleUserPresence(
            @Payload UserPresenceRequest presenceRequest,
            Principal principal
    ) {
        try {
            UUID userId = UUID.fromString(principal.getName());
            webSocketService.handleUserPresence(userId, presenceRequest.isOnline());
        } catch (Exception e) {
            log.error("Error processing user presence", e);
        }
    }
    
    @MessageMapping("/heartbeat")
    public void handleHeartbeat(
            @Payload Map<String, Object> heartbeat,
            Principal principal
    ) {
        try {
            UUID userId = UUID.fromString(principal.getName());
            webSocketService.handleHeartbeat(userId);
        } catch (Exception e) {
            log.error("Error processing heartbeat", e);
        }
    }
} 
