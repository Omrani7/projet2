package com.example.spring_security.dto;

import com.example.spring_security.model.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for users with ML compatibility scores
 * Used by RecommendationService for ranking applicants and similar users
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserWithScore {
    
    /**
     * Custom user DTO to avoid circular references
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UserDTO {
        private Integer id;
        private String username;
        private String email;
        private String role;
        private String institute;
        private String fieldOfStudy;
        private String educationLevel;
        private Integer age;
    }
    
    /**
     * The user being scored (flattened to avoid circular references)
     */
    private UserDTO user;
    
    /**
     * ML-calculated compatibility score (0.00 to 1.00)
     */
    private BigDecimal compatibilityScore;
    
    /**
     * Associated application ID (if applicable)
     */
    private Long applicationId;
    
    /**
     * Application status (if applicable)
     */
    private String applicationStatus;
    
    /**
     * When the user applied (if applicable)
     */
    private LocalDateTime appliedAt;
    
    /**
     * Match factors breakdown for analytics
     */
    private CompatibilityBreakdown compatibilityBreakdown;
    
    /**
     * Recommendation rank (1 = best match)
     */
    private Integer rank;
    
    /**
     * Reason for recommendation (for UI display)
     */
    private String recommendationReason;
    
    /**
     * Whether this user has been contacted
     */
    private Boolean contacted;
    
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
                explanation.append("Similar age group. ");
            }
            
            if (lifestyleScore != null && lifestyleScore.compareTo(BigDecimal.valueOf(0.7)) >= 0) {
                explanation.append("Compatible lifestyle. ");
            }
            
            if (budgetScore != null && budgetScore.compareTo(BigDecimal.valueOf(0.8)) >= 0) {
                explanation.append("Good budget fit. ");
            }
            
            if (locationScore != null && locationScore.compareTo(BigDecimal.valueOf(0.7)) >= 0) {
                explanation.append("Convenient location. ");
            }
            
            if (studyFieldScore != null && studyFieldScore.compareTo(BigDecimal.valueOf(0.7)) >= 0) {
                explanation.append("Similar academic interests. ");
            }
            
            return explanation.length() > 0 ? explanation.toString().trim() : "Generally compatible.";
        }
        
        /**
         * Get the strongest compatibility factor
         */
        public String getStrongestFactor() {
            BigDecimal maxScore = BigDecimal.ZERO;
            String strongestFactor = "Overall compatibility";
            
            if (ageScore != null && ageScore.compareTo(maxScore) > 0) {
                maxScore = ageScore;
                strongestFactor = "Age compatibility";
            }
            
            if (lifestyleScore != null && lifestyleScore.compareTo(maxScore) > 0) {
                maxScore = lifestyleScore;
                strongestFactor = "Lifestyle match";
            }
            
            if (budgetScore != null && budgetScore.compareTo(maxScore) > 0) {
                maxScore = budgetScore;
                strongestFactor = "Budget alignment";
            }
            
            if (locationScore != null && locationScore.compareTo(maxScore) > 0) {
                maxScore = locationScore;
                strongestFactor = "Location proximity";
            }
            
            if (studyFieldScore != null && studyFieldScore.compareTo(maxScore) > 0) {
                maxScore = studyFieldScore;
                strongestFactor = "Academic similarity";
            }
            
            return strongestFactor;
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
            return "Excellent Match";
        } else if (compatibilityScore.compareTo(BigDecimal.valueOf(0.75)) >= 0) {
            return "Very Good Match";
        } else if (compatibilityScore.compareTo(BigDecimal.valueOf(0.60)) >= 0) {
            return "Good Match";
        } else if (compatibilityScore.compareTo(BigDecimal.valueOf(0.40)) >= 0) {
            return "Fair Match";
        } else {
            return "Poor Match";
        }
    }
    
    /**
     * Get percentage representation of compatibility score
     */
    public Integer getCompatibilityPercentage() {
        return compatibilityScore != null ? 
               compatibilityScore.multiply(BigDecimal.valueOf(100)).intValue() : 0;
    }
    
    /**
     * Get color code for UI representation of compatibility level
     */
    public String getCompatibilityColor() {
        if (compatibilityScore == null) {
            return "gray";
        }
        
        if (compatibilityScore.compareTo(BigDecimal.valueOf(0.75)) >= 0) {
            return "green";
        } else if (compatibilityScore.compareTo(BigDecimal.valueOf(0.50)) >= 0) {
            return "yellow";
        } else {
            return "red";
        }
    }
    
    /**
     * Check if this is a high-quality match
     */
    public boolean isHighQualityMatch() {
        return compatibilityScore != null && 
               compatibilityScore.compareTo(BigDecimal.valueOf(0.70)) >= 0;
    }
    
    /**
     * Check if this user is recommended for contact
     */
    public boolean isRecommendedForContact() {
        return compatibilityScore != null && 
               compatibilityScore.compareTo(BigDecimal.valueOf(0.60)) >= 0;
    }
    
    /**
     * Create UserWithScore from User entity
     */
    public static UserWithScore fromUser(User user, BigDecimal compatibilityScore) {
        UserDTO userDTO = UserDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole().name())
                .institute(user.getUserProfile() != null ? user.getUserProfile().getInstitute() : null)
                .fieldOfStudy(user.getUserProfile() != null ? user.getUserProfile().getFieldOfStudy() : null)
                .educationLevel(user.getUserProfile() != null && user.getUserProfile().getEducationLevel() != null ? 
                               user.getUserProfile().getEducationLevel().name() : null)
                .age(user.getAge())
                .build();
        
        return UserWithScore.builder()
                .user(userDTO)
                .compatibilityScore(compatibilityScore)
                .build();
    }
    
    /**
     * Create UserWithScore with additional details
     */
    public static UserWithScore fromUserWithDetails(User user, BigDecimal compatibilityScore, 
                                                   String recommendationReason, Integer rank) {
        UserWithScore userWithScore = fromUser(user, compatibilityScore);
        userWithScore.setRecommendationReason(recommendationReason);
        userWithScore.setRank(rank);
        return userWithScore;
    }
} 