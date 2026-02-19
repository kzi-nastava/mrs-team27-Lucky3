package com.team27.lucky3.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // WebSocket endpoint that clients will connect to
        registry.addEndpoint("/ws")
                .setAllowedOrigins("http://localhost:4200", "http://127.0.0.1:4200")
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Prefix for messages FROM client TO server (e.g., /app/some-endpoint)
        registry.setApplicationDestinationPrefixes("/app");

        // Enable both /topic (broadcast) and /user (per-user queues) prefixes.
        // Per-user notifications are sent to: /user/{userId}/queue/notifications
        registry.enableSimpleBroker("/topic", "/user");
    }
}
