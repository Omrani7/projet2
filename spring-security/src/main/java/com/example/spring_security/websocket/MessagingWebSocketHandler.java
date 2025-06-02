package com.example.spring_security.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket handler for real-time messaging notifications
 */
@Component
public class MessagingWebSocketHandler implements WebSocketHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(MessagingWebSocketHandler.class);
    
    // Store active WebSocket sessions by user ID
    private final Map<Integer, WebSocketSession> userSessions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        logger.info("WebSocket connection established: {}", session.getId());
        
        // Extract user ID from session (you might need to implement authentication)
        Integer userId = extractUserIdFromSession(session);
        if (userId != null) {
            userSessions.put(userId, session);
            logger.info("User {} connected to messaging WebSocket", userId);
        } else {
            logger.warn("Could not extract user ID from WebSocket session");
            session.close();
        }
    }
    
    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        if (message instanceof TextMessage) {
            String payload = ((TextMessage) message).getPayload();
            logger.debug("Received WebSocket message: {}", payload);
            
            try {
                Map<String, Object> messageData = objectMapper.readValue(payload, Map.class);
                String type = (String) messageData.get("type");
                
                if ("SUBSCRIBE".equals(type)) {
                    logger.info("User subscribed to messaging notifications");
                    // Send confirmation
                    sendMessage(session, Map.of(
                        "type", "SUBSCRIPTION_CONFIRMED",
                        "message", "Successfully subscribed to messaging notifications"
                    ));
                }
            } catch (Exception e) {
                logger.error("Error processing WebSocket message: {}", e.getMessage());
            }
        }
    }
    
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        logger.error("WebSocket transport error for session {}: {}", session.getId(), exception.getMessage());
        removeSession(session);
    }
    
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        logger.info("WebSocket connection closed: {} with status: {}", session.getId(), closeStatus);
        removeSession(session);
    }
    
    @Override
    public boolean supportsPartialMessages() {
        return false;
    }
    
    /**
     * Send a notification to a specific user
     */
    public void sendNotificationToUser(Integer userId, String type, String message, Object data) {
        WebSocketSession session = userSessions.get(userId);
        if (session != null && session.isOpen()) {
            try {
                Map<String, Object> notification = Map.of(
                    "type", type,
                    "message", message,
                    "data", data != null ? data : Map.of(),
                    "timestamp", System.currentTimeMillis()
                );
                
                sendMessage(session, notification);
                logger.debug("Sent {} notification to user {}", type, userId);
            } catch (Exception e) {
                logger.error("Failed to send notification to user {}: {}", userId, e.getMessage());
                // Remove invalid session
                userSessions.remove(userId);
            }
        } else {
            logger.debug("User {} not connected to messaging WebSocket", userId);
        }
    }
    
    /**
     * Send a message to a WebSocket session
     */
    private void sendMessage(WebSocketSession session, Object message) throws IOException {
        if (session.isOpen()) {
            String jsonMessage = objectMapper.writeValueAsString(message);
            session.sendMessage(new TextMessage(jsonMessage));
        }
    }
    
    /**
     * Remove session from active sessions
     */
    private void removeSession(WebSocketSession session) {
        userSessions.entrySet().removeIf(entry -> entry.getValue().equals(session));
    }
    
    /**
     * Extract user ID from WebSocket session
     * This is a simplified implementation - you might need to implement proper authentication
     */
    private Integer extractUserIdFromSession(WebSocketSession session) {
        try {
            // Try to get user ID from session attributes or query parameters
            String query = session.getUri().getQuery();
            if (query != null && query.contains("userId=")) {
                String userIdStr = query.split("userId=")[1].split("&")[0];
                return Integer.parseInt(userIdStr);
            }
            
            // Alternative: extract from session attributes if available
            Object userIdAttr = session.getAttributes().get("userId");
            if (userIdAttr != null) {
                return Integer.parseInt(userIdAttr.toString());
            }
            
            return null;
        } catch (Exception e) {
            logger.error("Error extracting user ID from session: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Get count of active connections
     */
    public int getActiveConnectionsCount() {
        return userSessions.size();
    }
    
    /**
     * Check if user is connected
     */
    public boolean isUserConnected(Integer userId) {
        WebSocketSession session = userSessions.get(userId);
        return session != null && session.isOpen();
    }
} 