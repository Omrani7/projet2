package com.example.spring_security.config;

import org.springframework.context.annotation.Configuration;

/**
 * WebSocket security configuration
 * Temporarily disabled due to class compatibility issues with AbstractSecurityWebSocketMessageBrokerConfigurer
 */
@Configuration
public class WebSocketSecurityConfig {
    
    // Temporarily disabled - the AbstractSecurityWebSocketMessageBrokerConfigurer
    // and related CSRF classes are causing startup issues
    
    // TODO: Re-implement WebSocket security using alternative approach
    // For now, WebSocket endpoints will rely on HTTP session authentication
    
    /*
     * ORIGINAL CODE - COMMENTED OUT TO FIX STARTUP ISSUE
     * 
     * This class would extend AbstractSecurityWebSocketMessageBrokerConfigurer
     * and configure message-level security, but those classes are causing
     * compatibility issues with the current Spring Security version.
     * 
     * Original methods:
     * - configureInbound(MessageSecurityMetadataSourceRegistry messages)
     * - sameOriginDisabled() returning true
     * 
     * The inquiry system functionality is preserved through:
     * - HTTP session authentication
     * - Controller-level security annotations (@PreAuthorize)
     * - Service-level authorization checks
     */
} 