package petitus.petcareplus.exceptions;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.StompSubProtocolErrorHandler;
import petitus.petcareplus.dto.response.ErrorResponse;

import java.nio.charset.StandardCharsets;
import org.springframework.lang.NonNull;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketExceptionHandler extends StompSubProtocolErrorHandler {

    @Override
    @NonNull
    public Message<byte[]> handleClientMessageProcessingError(@NonNull Message<byte[]> clientMessage,@NonNull Throwable ex) {
        String errorMessage;
        String clientMessageInfo = "unknown";
        
        try {
            // Try to extract message information for logging
            StompHeaderAccessor accessor = StompHeaderAccessor.wrap(clientMessage);
            if (accessor != null) {
                clientMessageInfo = String.format("destination=%s, command=%s, user=%s", 
                    accessor.getDestination(), 
                    accessor.getCommand(),
                    accessor.getUser() != null ? accessor.getUser().getName() : "null");
            }
        } catch (Exception e) {
            log.debug("Could not extract message info for logging: {}", e.getMessage());
        }

        if (ex instanceof IllegalArgumentException) {
            errorMessage = ex.getMessage();
            log.error("❌ WebSocket IllegalArgumentException for message [{}]: {}", clientMessageInfo, errorMessage, ex);
        } else if (ex instanceof NullPointerException) {
            errorMessage = "Authentication required";
            log.error("❌ WebSocket NullPointerException for message [{}]: Authentication required", clientMessageInfo, ex);
        } else {
            errorMessage = "Internal server error";
            log.error("❌ WebSocket Internal Error for message [{}]: {}", clientMessageInfo, ex.getMessage(), ex);
        }

        log.warn("🔄 Sending error response to client: {}", errorMessage);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .message(errorMessage)
                .build();

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.ERROR);
        accessor.setLeaveMutable(true);

        return MessageBuilder
                .withPayload(errorResponse.toString().getBytes(StandardCharsets.UTF_8))
                .setHeaders(accessor)
                .build();
    }
} 