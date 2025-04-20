package petitus.petcareplus.security.config;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.lang.NonNull;
import petitus.petcareplus.security.jwt.JwtUserDetails;

@Component
@RequiredArgsConstructor
public class AuthChannelInterceptor implements ChannelInterceptor {

    @Override
    @NonNull
    public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        try {
            StompHeaderAccessor accessor =
                    MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

            if (StompCommand.SEND.equals(accessor.getCommand())) {
                UsernamePasswordAuthenticationToken token = (UsernamePasswordAuthenticationToken) accessor.getHeader("simpUser");
                JwtUserDetails userDetails = (JwtUserDetails) token.getPrincipal();
                accessor.setUser(new StompPrincipal(userDetails.getId().toString()));
            } else {
                throw new IllegalArgumentException("Missing or invalid Authorization header");
            }

            return message;
        } catch (Exception e) {
            throw new IllegalArgumentException("Error processing message: " + e.getMessage());
        }
    }
}
