package com.example.spring_security.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for responding to a connection request (accept or reject)
 * Used when a student wants to respond to a received connection request
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConnectionRequestResponseDTO {
    
    /**
     * Response action: ACCEPTED or REJECTED
     */
    @NotBlank(message = "Status is required")
    @Pattern(regexp = "^(ACCEPTED|REJECTED)$", message = "Status must be either ACCEPTED or REJECTED")
    private String status;
    
    /**
     * Optional response message
     */
    @Size(max = 500, message = "Response message cannot exceed 500 characters")
    private String responseMessage;
} 