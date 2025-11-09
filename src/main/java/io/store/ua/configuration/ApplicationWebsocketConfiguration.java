package io.store.ua.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class ApplicationWebsocketConfiguration implements WebSocketMessageBrokerConfigurer {
    public static final String WEBSOCKET_TOPIC = "/topic";
    public static final String WEBSOCKET_NOTIFICATIONS = "/notifications";
    public static final String WEBSOCKET_ENDPOINT = "/socket";

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker(WEBSOCKET_TOPIC);
        config.setApplicationDestinationPrefixes(WEBSOCKET_NOTIFICATIONS);
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint(WEBSOCKET_ENDPOINT).setAllowedOriginPatterns("*").withSockJS();
    }
}
