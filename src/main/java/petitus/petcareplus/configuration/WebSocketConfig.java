package petitus.petcareplus.configuration;

import jakarta.websocket.server.ServerContainer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;
import petitus.petcareplus.exceptions.WebSocketExceptionHandler;
import petitus.petcareplus.security.config.AuthChannelInterceptor;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
@Slf4j
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final AuthChannelInterceptor authChannelInterceptor;
    private final WebSocketExceptionHandler webSocketExceptionHandler;

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(authChannelInterceptor);
        // Increase task executor pool for handling large messages
        registration.taskExecutor().corePoolSize(8);
        registration.taskExecutor().maxPoolSize(16);
        registration.taskExecutor().queueCapacity(1000);
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOrigins("*")
                .withSockJS()
                .setStreamBytesLimit(16 * 1024 * 1024) // 16MB for SockJS
                .setHttpMessageCacheSize(2000)
                .setDisconnectDelay(5 * 1000); // 5 seconds disconnect delay
        registry.setErrorHandler(webSocketExceptionHandler);
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue");
    }

    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
        // Configure message size limits for image uploads
        registration.setMessageSizeLimit(20 * 1024 * 1024); // 20MB
        registration.setSendBufferSizeLimit(20 * 1024 * 1024); // 20MB buffer
        registration.setSendTimeLimit(30 * 1000); // 30 seconds
        registration.setTimeToFirstMessage(30 * 1000); // 30 seconds for first message
    }

    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> tomcatCustomizer() {
        return factory -> {
            factory.addConnectorCustomizers(connector ->
                    connector.setProperty("maxHttpHeaderSize", "65536") // 64KB headers
            );

            factory.addContextCustomizers(context ->
                    // Configure WebSocket container limits
                    context.addServletContainerInitializer((classes, servletContext) -> {
                        try {
                            ServerContainer serverContainer = (ServerContainer) servletContext.getAttribute(ServerContainer.class.getName());
                            if (serverContainer != null) {
                                // Set WebSocket message size limits
                                serverContainer.setDefaultMaxTextMessageBufferSize(20 * 1024 * 1024); // 20MB
                                serverContainer.setDefaultMaxBinaryMessageBufferSize(20 * 1024 * 1024); // 20MB
                                serverContainer.setDefaultMaxSessionIdleTimeout(300000); // 5 minutes
                            }
                        } catch (Exception e) {
                            log.error("Error configuring WebSocket container limits", e);
                        }
                    }, null)
            );
        };
    }

    @Bean
    public ServerEndpointExporter serverEndpointExporter() {
        return new ServerEndpointExporter();
    }
}
