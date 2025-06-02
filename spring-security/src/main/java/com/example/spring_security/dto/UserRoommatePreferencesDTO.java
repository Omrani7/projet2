package com.example.spring_security.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * DTO for user roommate preferences
 * Used for API requests and responses
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRoommatePreferencesDTO {
    
    /**
     * User ID (read-only in responses)
     */
    private Integer userId;
    
    /**
     * Lifestyle preference tags
     * Examples: QUIET, SOCIAL, STUDIOUS, PARTY, NIGHT_OWL, EARLY_BIRD
     */
    private Set<String> lifestyleTags;
    
    /**
     * Cleanliness level (1-5 scale)
     * 1 = Very relaxed, 5 = Very strict
     */
    @Min(value = 1, message = "Cleanliness level must be between 1 and 5")
    @Max(value = 5, message = "Cleanliness level must be between 1 and 5")
    private Integer cleanlinessLevel;
    
    /**
     * Social level (1-5 scale)
     * 1 = Very introverted, 5 = Very extroverted
     */
    @Min(value = 1, message = "Social level must be between 1 and 5")
    @Max(value = 5, message = "Social level must be between 1 and 5")
    private Integer socialLevel;
    
    /**
     * Study habits preferences
     * Examples: QUIET_STUDY, GROUP_STUDY, LIBRARY_PREFERRED, HOME_STUDY
     */
    private Set<String> studyHabits;
    
    /**
     * Minimum acceptable rent budget
     */
    @DecimalMin(value = "0.0", message = "Budget minimum must be positive")
    @Digits(integer = 8, fraction = 2, message = "Budget must have at most 8 integer digits and 2 decimal places")
    private BigDecimal budgetMin;
    
    /**
     * Maximum rent budget
     */
    @DecimalMin(value = "0.0", message = "Budget maximum must be positive")
    @Digits(integer = 8, fraction = 2, message = "Budget must have at most 8 integer digits and 2 decimal places")
    private BigDecimal budgetMax;
    
    // Location preferences removed as requested by user
    
    /**
     * Additional preferences or notes
     */
    @Size(max = 1000, message = "Additional preferences cannot exceed 1000 characters")
    private String additionalPreferences;
    
    /**
     * Timestamp when preferences were last updated (read-only)
     */
    private LocalDateTime updatedAt;
    
    // Helper fields for frontend
    
    // hasPreferredLocation removed with location preferences
    
    /**
     * Whether preferences are complete enough for ML matching
     */
    private Boolean isComplete;
    
    // Validation methods
    
    /**
     * Custom validation to ensure budget min <= budget max
     */
    @AssertTrue(message = "Budget minimum must be less than or equal to budget maximum")
    public boolean isBudgetRangeValid() {
        if (budgetMin == null || budgetMax == null) {
            return true; // Skip validation if either is null
        }
        return budgetMin.compareTo(budgetMax) <= 0;
    }
    
    // Location validation removed with location preferences
} 