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

            if (StompCommand.SEND.equals(accessor.getCommand()) || StompCommand.CONNECT.equals(accessor.getCommand())) {
                // Check if the user is already authenticated with a StompPrincipal
                if (accessor.getUser() instanceof StompPrincipal) {
                    // User is already authenticated, no need to process again
                    return message;
                }
                
                // Get the authentication token
                Object simpUser = accessor.getHeader("simpUser");
                if (simpUser instanceof UsernamePasswordAuthenticationToken token) {
                    JwtUserDetails userDetails = (JwtUserDetails) token.getPrincipal();
                    accessor.setUser(new StompPrincipal(userDetails.getId().toString()));
                } else {
                    throw new IllegalArgumentException("Missing or invalid Authentication");
                }
            }

            return message;
        } catch (Exception e) {
            throw new IllegalArgumentException("Error processing message: " + e.getMessage());
        }
    }
}
