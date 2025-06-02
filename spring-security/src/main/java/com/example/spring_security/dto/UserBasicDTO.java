package com.example.spring_security.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Basic user information DTO for use in other DTOs
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserBasicDTO {
    private Integer id;
    private String username;
    private String email;
    private String phoneNumber;
    private String role;
    
    // Additional fields for roommate/connection features
    private String institute;
    private String fieldOfStudy;
    private String educationLevel;
} 