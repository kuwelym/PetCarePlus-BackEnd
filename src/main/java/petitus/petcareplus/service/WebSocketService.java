package petitus.petcareplus.service;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import petitus.petcareplus.dto.request.chat.ChatMessageRequest;
import petitus.petcareplus.dto.request.chat.ReadReceiptRequest;
import petitus.petcareplus.dto.request.chat.TypingEvent;
import petitus.petcareplus.dto.response.chat.ConversationResponse;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WebSocketService {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatService chatService;

    public void sendMessage(ChatMessageRequest chatMessageRequest) {
        String messageDestination = "/user/" + chatMessageRequest.getRecipientId() + "/queue/messages";
        messagingTemplate.convertAndSend(messageDestination, chatMessageRequest);
        
        // Send updated conversation data directly instead of just "refresh"
        sendUpdatedConversations(chatMessageRequest.getSenderId());
        sendUpdatedConversations(chatMessageRequest.getRecipientId());
    }
    
    public void sendUpdatedConversations(UUID userId) {
        try {
            // Fetch the user's updated conversations
            List<ConversationResponse> conversations = chatService.getAllConversations(20);
            
            // Send the actual conversation data via WebSocket
            String destination = "/user/" + userId.toString() + "/queue/conversation-update";
            messagingTemplate.convertAndSend(destination, conversations);
        } catch (Exception e) {
            // Fallback to simple refresh signal if there's an error
            String destination = "/user/" + userId.toString() + "/queue/conversation-update";
            messagingTemplate.convertAndSend(destination, "refresh");
        }
    }

    public void notifyTyping(TypingEvent event) {
        String destination = "/queue/typing/" + event.getRecipientId();
        messagingTemplate.convertAndSend(destination, event);
    }

    public void notifyMessageRead(ReadReceiptRequest receipt) {
        String destination = "/queue/read-status/" + receipt.getSenderId();
        messagingTemplate.convertAndSend(destination, receipt);
    }
    
    public void handleMarkAsRead(ReadReceiptRequest readReceiptRequest, UUID readerId) {
        try {
            // Update messages as read in the database
            UUID otherUserId = UUID.fromString(readReceiptRequest.getSenderId());
            chatService.markMessageAsRead(readerId, otherUserId);

            // Send read receipt to the sender
            String readReceiptDestination = "/user/" + otherUserId + "/queue/read-receipt";
            messagingTemplate.convertAndSend(readReceiptDestination, readReceiptRequest);
            
            // Send updated conversation data to both users to reflect read status
            sendUpdatedConversations(readerId);
            sendUpdatedConversations(otherUserId);
            
        } catch (Exception e) {
            System.err.println("Error handling mark as read: " + e.getMessage());
        }
    }
} 
