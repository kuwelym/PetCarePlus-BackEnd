package petitus.petcareplus.service;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import petitus.petcareplus.dto.request.chat.ChatMessageRequest;
import petitus.petcareplus.dto.request.chat.ReadReceipt;
import petitus.petcareplus.dto.request.chat.TypingEvent;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WebSocketService {

    private final SimpMessagingTemplate messagingTemplate;

    public void sendMessage(ChatMessageRequest chatMessageRequest) {
        String messageDestination = "/user/" + chatMessageRequest.getRecipientId() + "/queue/messages";
        messagingTemplate.convertAndSend(messageDestination, chatMessageRequest);
        
        notifyConversationUpdate(chatMessageRequest.getSenderId());
        notifyConversationUpdate(chatMessageRequest.getRecipientId());
    }
    
    public void notifyConversationUpdate(UUID userId) {
        String destination = "/user/" + userId.toString() + "/queue/conversation-update";
        messagingTemplate.convertAndSend(destination, "refresh");
        System.out.println("Sending conversation update to: " + destination);
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