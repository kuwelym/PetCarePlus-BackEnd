package petitus.petcareplus.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;
import petitus.petcareplus.dto.request.chat.ChatMessageRequest;
import petitus.petcareplus.dto.request.chat.ReadReceipt;
import petitus.petcareplus.dto.request.chat.TypingEvent;
import petitus.petcareplus.service.ChatService;
import petitus.petcareplus.service.WebSocketService;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class WebSocketController {

    private final WebSocketService webSocketService;
    private final ChatService chatService;  // Add this

    @MessageMapping("/chat.sendMessage")
    public void sendMessage(
            @Payload ChatMessageRequest chatMessageRequest,
            Principal principal
    ) {
        chatService.sendMessage(chatMessageRequest, principal);

        webSocketService.sendMessage(chatMessageRequest);
    }

    @MessageMapping("/chat.typing")
    public void handleTyping(@Payload TypingEvent event, SimpMessageHeaderAccessor headerAccessor) {
        String userId = (String) headerAccessor.getSessionAttributes().get("userId");
        event.setSenderId(userId);
        webSocketService.notifyTyping(event);
    }

    @MessageMapping("/chat.read")
    public void handleMessageRead(@Payload ReadReceipt receipt) {
        webSocketService.notifyMessageRead(receipt);
    }
} 