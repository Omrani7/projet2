package com.example.spring_security.dto;

import com.example.spring_security.model.Message;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for creating a new message in a conversation
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageCreateDTO {
    
    /**
     * ID of the conversation to send the message to
     */
    @NotNull(message = "Conversation ID is required")
    private Long conversationId;
    
    /**
     * Content of the message
     */
    @NotBlank(message = "Message content cannot be blank")
    @Size(max = 2000, message = "Message content cannot exceed 2000 characters")
    private String content;
    
    /**
     * Type of message (defaults to TEXT if not specified)
     */
    private Message.MessageType messageType;
    
    /**
     * Optional metadata for the message (e.g., image URLs, file paths)
     */
    @Size(max = 1000, message = "Metadata cannot exceed 1000 characters")
    private String metadata;
} 