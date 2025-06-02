package com.example.spring_security.config;

import com.example.spring_security.websocket.MessagingWebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

/**
 * WebSocket configuration for real-time notifications using STOMP protocol
 * and simple WebSocket for messaging
 */
@Configuration
@EnableWebSocketMessageBroker
@EnableWebSocket
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer, WebSocketConfigurer {

    @Autowired
    private MessagingWebSocketHandler messagingWebSocketHandler;

    /**
     * Configure message broker for routing messages (STOMP)
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Enable a simple in-memory message broker to carry messages back to clients
        // Destinations with prefix "/topic" and "/queue" will be handled by the broker
        config.enableSimpleBroker("/topic", "/queue");
        
        // Messages with destination prefix "/app" will be routed to @MessageMapping methods
        config.setApplicationDestinationPrefixes("/app");
        
        // Prefix for user-specific destinations
        config.setUserDestinationPrefix("/user");
    }

    /**
     * Register STOMP endpoints that clients will use to connect
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Register the "/ws" endpoint for WebSocket connections
        // Enable SockJS fallback for browsers that don't support WebSocket
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("http://localhost:4200", "http://localhost:*")
                .withSockJS();
                
        // Also register without SockJS for native WebSocket clients
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("http://localhost:4200", "http://localhost:*");
    }

    /**
     * Register simple WebSocket handlers for messaging
     */
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(messagingWebSocketHandler, "/ws/messaging")
                .setAllowedOriginPatterns("http://localhost:4200", "http://localhost:*");
    }
} 