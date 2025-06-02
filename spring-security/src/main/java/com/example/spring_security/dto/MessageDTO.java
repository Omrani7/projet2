package com.example.spring_security.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for Message entity responses
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageDTO {
    
    private Long id;
    
    /**
     * ID of the conversation this message belongs to
     */
    private Long conversationId;
    
    /**
     * Basic information about the sender
     */
    private UserBasicDTO sender;
    
    /**
     * Content of the message
     */
    private String content;
    
    /**
     * Type of message (TEXT, IMAGE, ANNOUNCEMENT_REFERENCE, SYSTEM)
     */
    private String messageType;
    
    /**
     * When the message was sent
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;
    
    /**
     * Whether the message has been read
     */
    private boolean isRead;
    
    /**
     * Human-readable time ago string
     */
    private String timeAgo;
    
    /**
     * Optional metadata for the message
     */
    private String metadata;
} 