package com.example.spring_security.dto;

import com.example.spring_security.model.RoommateAnnouncement;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for roommate announcements with ML compatibility scores
 * Used by RecommendationService for personalized recommendations
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnnouncementWithScore {
    
    /**
     * The roommate announcement
     */
    private RoommateAnnouncement announcement;
    
    /**
     * ML-calculated compatibility score (0.00 to 1.00)
     */
    private BigDecimal compatibilityScore;
    
    /**
     * Additional metadata for recommendation tracking
     */
    private String recommendationId;
    
    /**
     * Reason for recommendation (for UI display)
     */
    private String recommendationReason;
    
    /**
     * Match factors breakdown for analytics
     */
    private CompatibilityBreakdown compatibilityBreakdown;
    
    /**
     * Whether this recommendation has been viewed by the user
     */
    private Boolean viewed;
    
    /**
     * Whether this recommendation has been clicked by the user
     */
    private Boolean clicked;
    
    /**
     * Nested class for compatibility score breakdown
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CompatibilityBreakdown {
        private BigDecimal ageScore;
        private BigDecimal lifestyleScore;
        private BigDecimal budgetScore;
        private BigDecimal locationScore;
        private BigDecimal studyFieldScore;
        
        /**
         * Generate a human-readable explanation of the compatibility score
         */
        public String getExplanation() {
            StringBuilder explanation = new StringBuilder();
            
            if (ageScore != null && ageScore.compareTo(BigDecimal.valueOf(0.8)) >= 0) {
                explanation.append("Great age compatibility. ");
            }
            
            if (lifestyleScore != null && lifestyleScore.compareTo(BigDecimal.valueOf(0.7)) >= 0) {
                explanation.append("Similar lifestyle preferences. ");
            }
            
            if (budgetScore != null && budgetScore.compareTo(BigDecimal.valueOf(0.8)) >= 0) {
                explanation.append("Budget fits well. ");
            }
            
            if (locationScore != null && locationScore.compareTo(BigDecimal.valueOf(0.7)) >= 0) {
                explanation.append("Good location match. ");
            }
            
            if (studyFieldScore != null && studyFieldScore.compareTo(BigDecimal.valueOf(0.7)) >= 0) {
                explanation.append("Related study fields. ");
            }
            
            return explanation.length() > 0 ? explanation.toString().trim() : "Good overall compatibility.";
        }
    }
    
    /**
     * Helper method to get compatibility level as string
     */
    public String getCompatibilityLevel() {
        if (compatibilityScore == null) {
            return "Unknown";
        }
        
        if (compatibilityScore.compareTo(BigDecimal.valueOf(0.90)) >= 0) {
            return "Excellent";
        } else if (compatibilityScore.compareTo(BigDecimal.valueOf(0.75)) >= 0) {
            return "Very Good";
        } else if (compatibilityScore.compareTo(BigDecimal.valueOf(0.60)) >= 0) {
            return "Good";
        } else if (compatibilityScore.compareTo(BigDecimal.valueOf(0.40)) >= 0) {
            return "Fair";
        } else {
            return "Poor";
        }
    }
    
    /**
     * Get percentage representation of compatibility score
     */
    public Integer getCompatibilityPercentage() {
        return compatibilityScore != null ? 
               compatibilityScore.multiply(BigDecimal.valueOf(100)).intValue() : 0;
    }
} 