package com.example.spring_security.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Data Transfer Object for RoommateApplication entity
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoommateApplicationDTO {
    private Long id;
    
    // Basic announcement info to avoid circular dependency
    private Long announcementId;
    private String announcementTitle;
    
    private UserBasicDTO applicant;
    private UserBasicDTO poster;
    private String message;
    private BigDecimal compatibilityScore;
    private String status;
    private LocalDateTime appliedAt;
    private LocalDateTime respondedAt;
    private String responseMessage;
    
    // Helper fields for frontend
    private Integer compatibilityPercentage;
    private Boolean canBeWithdrawn;
    private String qualityCategory; // HIGH, MEDIUM, LOW
} 