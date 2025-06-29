package petitus.petcareplus.configuration;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;
import petitus.petcareplus.exceptions.WebSocketExceptionHandler;
import petitus.petcareplus.security.config.AuthChannelInterceptor;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final AuthChannelInterceptor authChannelInterceptor;
    private final WebSocketExceptionHandler webSocketExceptionHandler;

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(authChannelInterceptor);
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOrigins("*")
                .withSockJS();
        registry.setErrorHandler(webSocketExceptionHandler);
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue");
    }
    
    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
        // Configure message size limits for image uploads
        registration.setMessageSizeLimit(8 * 1024 * 1024); // 8MB
        registration.setSendBufferSizeLimit(8 * 1024 * 1024); // 8MB
        registration.setSendTimeLimit(20 * 1000); // 20 seconds
    }
}
