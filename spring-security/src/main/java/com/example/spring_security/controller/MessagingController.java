package com.example.spring_security.controller;

import com.example.spring_security.dto.ConversationDTO;
import com.example.spring_security.dto.MessageCreateDTO;
import com.example.spring_security.dto.MessageDTO;
import com.example.spring_security.model.UserPrincipal;
import com.example.spring_security.service.MessagingService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * REST Controller for messaging functionality between connected students
 */
@RestController
@RequestMapping("/api/v1/messages")
@PreAuthorize("hasRole('STUDENT')")
public class MessagingController {
    
    private static final Logger logger = LoggerFactory.getLogger(MessagingController.class);
    
    private final MessagingService messagingService;
    
    @Autowired
    public MessagingController(MessagingService messagingService) {
        this.messagingService = messagingService;
    }
    
    /**
     * Get all conversations for the current user
     * GET /api/v1/messages/conversations
     */
    @GetMapping("/conversations")
    public ResponseEntity<Page<ConversationDTO>> getUserConversations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        
        logger.info("Getting conversations for user {}", currentUser.getId());
        
        Pageable pageable = PageRequest.of(page, size);
        Page<ConversationDTO> conversations = messagingService.getUserConversations(currentUser, pageable);
        
        return ResponseEntity.ok(conversations);
    }
    
    /**
     * Get or create a conversation with another user
     * POST /api/v1/messages/conversations/{otherUserId}
     */
    @PostMapping("/conversations/{otherUserId}")
    public ResponseEntity<ConversationDTO> getOrCreateConversation(
            @PathVariable Integer otherUserId,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        
        logger.info("Getting/creating conversation between user {} and user {}", 
                   currentUser.getId(), otherUserId);
        
        ConversationDTO conversation = messagingService.getOrCreateConversation(otherUserId, currentUser);
        
        return ResponseEntity.ok(conversation);
    }
    
    /**
     * Send a message in a conversation
     * POST /api/v1/messages
     */
    @PostMapping
    public ResponseEntity<MessageDTO> sendMessage(
            @Valid @RequestBody MessageCreateDTO messageCreateDTO,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        
        logger.info("User {} sending message to conversation {}", 
                   currentUser.getId(), messageCreateDTO.getConversationId());
        
        MessageDTO message = messagingService.sendMessage(messageCreateDTO, currentUser);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(message);
    }
    
    /**
     * Get messages for a specific conversation
     * GET /api/v1/messages/conversations/{conversationId}/messages
     */
    @GetMapping("/conversations/{conversationId}/messages")
    public ResponseEntity<Page<MessageDTO>> getConversationMessages(
            @PathVariable Long conversationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        
        logger.debug("Getting messages for conversation {} by user {}", 
                    conversationId, currentUser.getId());
        
        Pageable pageable = PageRequest.of(page, size);
        Page<MessageDTO> messages = messagingService.getConversationMessages(
                conversationId, currentUser, pageable);
        
        return ResponseEntity.ok(messages);
    }
    
    /**
     * Mark messages as read in a conversation
     * PUT /api/v1/messages/conversations/{conversationId}/read
     */
    @PutMapping("/conversations/{conversationId}/read")
    public ResponseEntity<Map<String, String>> markMessagesAsRead(
            @PathVariable Long conversationId,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        
        logger.debug("Marking messages as read in conversation {} for user {}", 
                    conversationId, currentUser.getId());
        
        messagingService.markMessagesAsRead(conversationId, currentUser);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Messages marked as read successfully");
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get unread message count for the current user
     * GET /api/v1/messages/unread-count
     */
    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadMessageCount(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        
        logger.debug("Getting unread message count for user {}", currentUser.getId());
        
        long unreadCount = messagingService.getUnreadMessageCount(currentUser);
        
        Map<String, Long> response = new HashMap<>();
        response.put("unreadCount", unreadCount);
        
        return ResponseEntity.ok(response);
    }
} 