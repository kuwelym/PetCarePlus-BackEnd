package petitus.petcareplus.service;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import petitus.petcareplus.dto.request.chat.ReadReceiptRequest;
import petitus.petcareplus.dto.request.chat.TypingEvent;
import petitus.petcareplus.dto.request.chat.UserPresenceRequest;
import petitus.petcareplus.dto.response.chat.ChatMessageResponse;
import petitus.petcareplus.dto.response.chat.ConversationResponse;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class WebSocketService {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatService chatService;
    
    // Track online users
    private final ConcurrentHashMap<String, Boolean> onlineUsers = new ConcurrentHashMap<>();

    public void sendMessage(ChatMessageResponse chatMessageResponse) {
        String messageDestination = "/user/" + chatMessageResponse.getRecipientId() + "/queue/messages";
        messagingTemplate.convertAndSend(messageDestination, chatMessageResponse);
        
        // Send the specific message to both users instead of full conversation updates
        sendMessageUpdate(chatMessageResponse.getSenderId(), chatMessageResponse);
        sendMessageUpdate(chatMessageResponse.getRecipientId(), chatMessageResponse);
    }
    
    public void sendMessageUpdate(UUID userId, ChatMessageResponse message) {
        try {
            // Send the specific message data via WebSocket
            String destination = "/user/" + userId.toString() + "/queue/message-update";
            messagingTemplate.convertAndSend(destination, message);
        } catch (Exception e) {
            System.err.println("Error sending message update: " + e.getMessage());
        }
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
            sendReadReceipt(otherUserId, readReceiptRequest);
            sendReadReceipt(readerId, readReceiptRequest);

            // Send updated conversation data to both users to reflect read status
            sendUpdatedConversations(readerId);
            sendUpdatedConversations(otherUserId);
            
        } catch (Exception e) {
            System.err.println("Error handling mark as read: " + e.getMessage());
        }
    }

    public void sendReadReceipt(UUID userId, ReadReceiptRequest readReceiptRequest) {
        String readReceiptDestination = "/user/" + userId + "/queue/read-receipt";
        messagingTemplate.convertAndSend(readReceiptDestination, readReceiptRequest);
    }
    
    public void handleUserPresence(UUID userId, boolean isOnline) {
        try {
            String userIdStr = userId.toString();
            
            // Update user online status
            onlineUsers.put(userIdStr, isOnline);
            
            // Create presence update message
            UserPresenceRequest presenceUpdate = new UserPresenceRequest(userIdStr, isOnline);
            
            // Broadcast to all users via topic
            messagingTemplate.convertAndSend("/topic/user-status", presenceUpdate);
            
            System.out.println("User " + userIdStr + " is now " + (isOnline ? "online" : "offline"));
            
        } catch (Exception e) {
            System.err.println("Error handling user presence: " + e.getMessage());
        }
    }
    
    public void handleHeartbeat(UUID userId) {
        // Keep user online and update their presence
        handleUserPresence(userId, true);
    }
    
    public boolean isUserOnline(String userId) {
        return onlineUsers.getOrDefault(userId, false);
    }
    
    public void removeUser(UUID userId) {
        handleUserPresence(userId, false);
        onlineUsers.remove(userId.toString());
    }
} 
