package com.example.spring_security.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for ConnectionRequest entity
 * Used for API responses when returning connection request data
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConnectionRequestDTO {
    
    private Long id;
    
    /**
     * Basic information about the sender
     */
    private UserBasicDTO sender;
    
    /**
     * Basic information about the receiver
     */
    private UserBasicDTO receiver;
    
    /**
     * Message from sender to receiver
     */
    private String message;
    
    /**
     * Status of the connection request
     */
    private String status;
    
    /**
     * When the request was created
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
    
    /**
     * When the request was responded to (if applicable)
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime respondedAt;
    
    /**
     * Response message when accepting/rejecting (if applicable)
     */
    private String responseMessage;
    
    /**
     * Whether the request can be withdrawn (only pending requests)
     */
    private boolean canBeWithdrawn;
    
    /**
     * Whether the current user is the sender of this request
     */
    private boolean isSender;
    
    /**
     * Whether the current user is the receiver of this request
     */
    private boolean isReceiver;
    
    /**
     * The other user in the connection (from current user's perspective)
     */
    private UserBasicDTO otherUser;
    
    /**
     * Additional computed fields for frontend convenience
     */
    private String statusDisplayText;
    private String timeAgo;
    private boolean isPending;
    private boolean isAccepted;
    private boolean isRejected;
} 