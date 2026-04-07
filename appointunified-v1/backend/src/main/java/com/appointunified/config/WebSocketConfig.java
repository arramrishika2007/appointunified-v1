package com.appointunified.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

/**
 * V3: STOMP WebSocket configuration.
 *
 * Topic structure:
 *   /topic/queue/{professionalId}   — public queue board (everyone in that queue)
 *   /user/queue/{appointmentId}     — private client-specific position updates
 *
 * Upstash Redis pub/sub powers cross-instance broadcasting when
 * multiple Railway instances are running (horizontal scale).
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Value("${app.cors.allowed-origins:http://localhost:3000}")
    private String allowedOrigins;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Enable simple in-memory broker for /topic and /user destinations
        registry.enableSimpleBroker("/topic", "/user");
        // Client sends TO /app/... → handled by @MessageMapping methods
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry
            .addEndpoint("/ws")
            .setAllowedOrigins(allowedOrigins.split(","))
            .withSockJS();          // SockJS fallback for browsers without WS
    }
}
