package com.example.spring_security.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for Conversation entity responses
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConversationDTO {
    
    private Long id;
    
    /**
     * The other participant in the conversation (for 1-on-1 conversations)
     */
    private UserBasicDTO otherParticipant;
    
    /**
     * The most recent message in the conversation
     */
    private MessageDTO lastMessage;
    
    /**
     * Number of unread messages for the current user
     */
    private long unreadCount;
    
    /**
     * When the conversation was last updated
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;
    
    /**
     * When the conversation was created
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
    
    /**
     * Whether this is a 1-on-1 conversation
     */
    private boolean isOneOnOne;
    
    /**
     * Display name for the conversation
     */
    public String getDisplayName() {
        if (otherParticipant != null) {
            return otherParticipant.getUsername();
        }
        return "Group Chat";
    }
    
    /**
     * Preview text for the conversation (last message content)
     */
    public String getPreviewText() {
        if (lastMessage != null) {
            return lastMessage.getContent();
        }
        return "No messages yet";
    }
} 