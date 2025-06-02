package com.example.spring_security.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

/**
 * DTO for recommendation statistics that matches frontend interface
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecommendationStatsDTO {
    
    private Long totalRecommendations;
    private Long highQualityMatches; // 70%+ compatibility
    private BigDecimal averageCompatibility; // 0.0 to 1.0
    private BigDecimal topCompatibilityScore; // Highest score found
    
    /**
     * Breakdown of recommendation types by compatibility factors
     */
    private Map<String, Integer> recommendationTypes;
    
    /**
     * Additional analytics data
     */
    private Long unviewedRecommendations;
    private Long successfulMatches;
    private BigDecimal successRate;
} 