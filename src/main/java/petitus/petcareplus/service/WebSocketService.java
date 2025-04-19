package petitus.petcareplus.service;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import petitus.petcareplus.dto.request.chat.ChatMessageRequest;
import petitus.petcareplus.dto.request.chat.ReadReceipt;
import petitus.petcareplus.dto.request.chat.TypingEvent;

@Service
@RequiredArgsConstructor
public class WebSocketService {

    private final SimpMessagingTemplate messagingTemplate;

    public void sendMessage(ChatMessageRequest chatMessageRequest) {
        String destination = "/queue/messages/" + chatMessageRequest.getRecipientId();
        messagingTemplate.convertAndSend(destination, chatMessageRequest);
    }

    public void notifyTyping(TypingEvent event) {
        String destination = "/queue/typing/" + event.getRecipientId();
        messagingTemplate.convertAndSend(destination, event);
    }

    public void notifyMessageRead(ReadReceipt receipt) {
        String destination = "/queue/read-status/" + receipt.getSenderId();
        messagingTemplate.convertAndSend(destination, receipt);
    }
} 