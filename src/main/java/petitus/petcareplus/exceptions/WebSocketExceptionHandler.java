package petitus.petcareplus.exceptions;

import lombok.RequiredArgsConstructor;
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
public class WebSocketExceptionHandler extends StompSubProtocolErrorHandler {

    @Override
    @NonNull
    public Message<byte[]> handleClientMessageProcessingError(@NonNull Message<byte[]> clientMessage,@NonNull Throwable ex) {
        String errorMessage;
        if (ex instanceof IllegalArgumentException) {
            errorMessage = ex.getMessage();
        } else if (ex instanceof NullPointerException) {
            errorMessage = "Authentication required";
        } else {
            errorMessage = "Internal server error";
        }

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