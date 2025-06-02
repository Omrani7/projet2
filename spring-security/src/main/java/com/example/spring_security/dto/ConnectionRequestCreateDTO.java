package com.example.spring_security.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for creating a new connection request
 * Used when a student wants to send a connection request to another student
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConnectionRequestCreateDTO {
    
    /**
     * ID of the user to send the connection request to
     */
    @NotNull(message = "Receiver ID is required")
    private Integer receiverId;
    
    /**
     * Optional message to include with the connection request
     */
    @Size(max = 500, message = "Message cannot exceed 500 characters")
    private String message;
} 