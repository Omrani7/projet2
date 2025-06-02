package com.example.spring_security.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for poster responding to a roommate application
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoommateApplicationResponseDTO {
    
    private String status; // ACCEPTED or REJECTED
    private String responseMessage; // Optional message from poster
} 