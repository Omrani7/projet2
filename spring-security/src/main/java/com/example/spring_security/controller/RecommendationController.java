package com.example.spring_security.controller;

import com.example.spring_security.dto.AnnouncementWithScore;
import com.example.spring_security.dto.UserWithScore;
import com.example.spring_security.dto.RecommendationStatsDTO;
import com.example.spring_security.model.UserPrincipal;
import com.example.spring_security.service.RecommendationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST controller for ML-powered roommate recommendations
 * Implements recommendation endpoints as specified in the plan
 */
@RestController
@RequestMapping("/api/v1/recommendations")
public class RecommendationController {
    
    private static final Logger logger = LoggerFactory.getLogger(RecommendationController.class);
    
    private final RecommendationService recommendationService;
    
    @Autowired
    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }
    
    /**
     * Get personalized roommate recommendations for authenticated user
     * GET /api/v1/recommendations/roommates
     */
    @GetMapping("/roommates")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<List<AnnouncementWithScore>> getPersonalizedRecommendations(
            @RequestParam(defaultValue = "20") Integer limit,
            @RequestParam(defaultValue = "false") Boolean trackView,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        
        logger.info("Getting personalized recommendations for user {} with limit {}", 
                   currentUser.getId(), limit);
        
        List<AnnouncementWithScore> recommendations = recommendationService
                .getRecommendationsForUser(currentUser.getId(), limit);
        
        logger.info("Returning {} personalized recommendations for user {}", 
                   recommendations.size(), currentUser.getId());
        
        return ResponseEntity.ok(recommendations);
    }
    
    /**
     * Get high-quality matches (compatibility score >= 70%)
     * GET /api/v1/recommendations/high-quality
     */
    @GetMapping("/high-quality")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<List<AnnouncementWithScore>> getHighQualityMatches(
            @RequestParam(defaultValue = "10") Integer limit,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        
        logger.info("Getting high-quality matches for user {} with limit {}", 
                   currentUser.getId(), limit);
        
        List<AnnouncementWithScore> highQualityMatches = recommendationService
                .getHighQualityMatches(currentUser.getId(), limit);
        
        logger.info("Returning {} high-quality matches for user {}", 
                   highQualityMatches.size(), currentUser.getId());
        
        return ResponseEntity.ok(highQualityMatches);
    }
    
    /**
     * Get compatible applicants for a specific announcement
     * GET /api/v1/recommendations/announcements/{announcementId}/compatible-applicants
     */
    @GetMapping("/announcements/{announcementId}/compatible-applicants")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<List<UserWithScore>> getCompatibleApplicants(
            @PathVariable Long announcementId,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        
        logger.info("Getting compatible applicants for announcement {} by user {}", 
                   announcementId, currentUser.getId());
        
        List<UserWithScore> compatibleApplicants = recommendationService
                .getCompatibleApplicants(announcementId);
        
        logger.info("Returning {} compatible applicants for announcement {}", 
                   compatibleApplicants.size(), announcementId);
        
        return ResponseEntity.ok(compatibleApplicants);
    }
    
    /**
     * Get similar users for a specific announcement
     * GET /api/v1/recommendations/announcements/{announcementId}/similar-users
     */
    @GetMapping("/announcements/{announcementId}/similar-users")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<List<UserWithScore>> getSimilarUsers(
            @PathVariable Long announcementId,
            @RequestParam(defaultValue = "15") Integer limit,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        
        logger.info("Getting similar users for announcement {} by user {} with limit {}", 
                   announcementId, currentUser.getId(), limit);
        
        List<UserWithScore> similarUsers = recommendationService
                .getSimilarUsers(announcementId, limit);
        
        logger.info("Returning {} similar users for announcement {}", 
                   similarUsers.size(), announcementId);
        
        return ResponseEntity.ok(similarUsers);
    }
    
    /**
     * NEW: Get compatible students based on user profile (general recommendations)
     * Not tied to specific announcements - discover potential roommates
     * GET /api/v1/recommendations/compatible-students
     */
    @GetMapping("/compatible-students")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<List<UserWithScore>> getCompatibleStudents(
            @RequestParam(defaultValue = "15") Integer limit,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        
        logger.info("Getting compatible students for user {} with limit {}", 
                   currentUser.getId(), limit);
        
        List<UserWithScore> compatibleStudents = recommendationService
                .getCompatibleStudents(currentUser.getId(), limit);
        
        logger.info("Returning {} compatible students for user {}", 
                   compatibleStudents.size(), currentUser.getId());
        
        return ResponseEntity.ok(compatibleStudents);
    }
    
    /**
     * Get recommendation statistics for authenticated user
     * GET /api/v1/recommendations/stats
     */
    @GetMapping("/stats")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<RecommendationStatsDTO> getRecommendationStats(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        
        logger.info("Getting recommendation statistics for user {}", currentUser.getId());
        
        RecommendationStatsDTO stats = recommendationService
                .getEnhancedRecommendationStats(currentUser.getId());
        
        return ResponseEntity.ok(stats);
    }
    
    /**
     * Mark a recommendation as viewed
     * POST /api/v1/recommendations/{matchId}/view
     */
    @PostMapping("/{matchId}/view")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<Map<String, String>> markRecommendationAsViewed(
            @PathVariable Long matchId,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        
        logger.debug("Marking recommendation {} as viewed by user {}", matchId, currentUser.getId());
        
        recommendationService.markRecommendationAsViewed(matchId);
        
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Recommendation marked as viewed");
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Mark a recommendation as clicked
     * POST /api/v1/recommendations/{matchId}/click
     */
    @PostMapping("/{matchId}/click")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<Map<String, String>> markRecommendationAsClicked(
            @PathVariable Long matchId,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        
        logger.debug("Marking recommendation {} as clicked by user {}", matchId, currentUser.getId());
        
        recommendationService.markRecommendationAsClicked(matchId);
        
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Recommendation marked as clicked");
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get ML model performance metrics (for admin/analytics)
     * GET /api/v1/recommendations/model-metrics
     */
    @GetMapping("/model-metrics")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getModelMetrics() {
        
        logger.info("Getting ML model performance metrics");
        
        // This would typically fetch metrics from the ML service
        // For now, return basic placeholder metrics
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("recommendation_accuracy", 0.85);
        metrics.put("click_through_rate", 0.23);
        metrics.put("application_success_rate", 0.67);
        metrics.put("total_recommendations_generated", 15420);
        metrics.put("active_users_with_recommendations", 342);
        
        return ResponseEntity.ok(metrics);
    }
    
    /**
     * Health check for recommendation service
     * GET /api/v1/recommendations/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> healthCheck() {
        
        Map<String, String> health = new HashMap<>();
        health.put("status", "healthy");
        health.put("service", "RecommendationService");
        health.put("timestamp", java.time.LocalDateTime.now().toString());
        
        return ResponseEntity.ok(health);
    }
    
    /**
     * Get recommendation algorithm weights (for debugging)
     * GET /api/v1/recommendations/algorithm-info
     */
    @GetMapping("/algorithm-info")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getAlgorithmInfo() {
        
        logger.info("Getting recommendation algorithm information");
        
        Map<String, Object> algorithmInfo = new HashMap<>();
        algorithmInfo.put("algorithm_type", "Academic-Focused University-Based Scoring");
        algorithmInfo.put("description", "Prioritizes university → field of study → education level → age compatibility");
        algorithmInfo.put("factors", Map.of(
                "university_weight", 0.40,      // PRIMARY: Same university students
                "study_field_weight", 0.25,     // SECONDARY: Same field of study
                "education_level_weight", 0.20, // TERTIARY: Same education level  
                "age_weight", 0.15              // QUATERNARY: Similar age
        ));
        algorithmInfo.put("priority_order", new String[]{"University", "Field of Study", "Education Level", "Age"});
        algorithmInfo.put("min_compatibility_threshold", 0.30);
        algorithmInfo.put("high_quality_threshold", 0.70);
        algorithmInfo.put("version", "2.0 - Academic Focus");
        
        return ResponseEntity.ok(algorithmInfo);
    }
} 