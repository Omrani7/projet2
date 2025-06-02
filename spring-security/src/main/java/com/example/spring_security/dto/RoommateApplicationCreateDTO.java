package com.example.spring_security.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for creating a new roommate application
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoommateApplicationCreateDTO {
    
    private Long announcementId;
    private String message;
} 