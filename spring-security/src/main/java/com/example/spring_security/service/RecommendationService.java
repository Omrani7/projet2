package com.example.spring_security.service;

import com.example.spring_security.dao.RoommateAnnouncementRepository;
import com.example.spring_security.dao.RoommateMatchRepository;
import com.example.spring_security.dao.UserRepo;
import com.example.spring_security.dto.AnnouncementWithScore;
import com.example.spring_security.dto.UserWithScore;
import com.example.spring_security.dto.RecommendationStatsDTO;
import com.example.spring_security.exception.ResourceNotFoundException;
import com.example.spring_security.model.RoommateAnnouncement;
import com.example.spring_security.model.RoommateApplication;
import com.example.spring_security.model.RoommateMatch;
import com.example.spring_security.model.User;
import com.example.spring_security.model.UserProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Enhanced Recommendation Service for ML-powered roommate matching
 * Implements personalized recommendations using CompatibilityService
 * As specified in the comprehensive roommate plan
 */
@Service
@Transactional(readOnly = true)
public class RecommendationService {
    
    private static final Logger logger = LoggerFactory.getLogger(RecommendationService.class);
    
    // Minimum compatibility threshold for recommendations
    private static final BigDecimal MIN_COMPATIBILITY_THRESHOLD = BigDecimal.valueOf(0.30);
    
    // Default limits for recommendations
    private static final int DEFAULT_RECOMMENDATION_LIMIT = 20;
    private static final int MAX_RECOMMENDATION_LIMIT = 50;
    
    private final CompatibilityService compatibilityService;
    private final RoommateAnnouncementRepository announcementRepository;
    private final RoommateMatchRepository matchRepository;
    private final UserRepo userRepository;
    
    @Autowired
    public RecommendationService(CompatibilityService compatibilityService,
                               RoommateAnnouncementRepository announcementRepository,
                               RoommateMatchRepository matchRepository,
                               UserRepo userRepository) {
        this.compatibilityService = compatibilityService;
        this.announcementRepository = announcementRepository;
        this.matchRepository = matchRepository;
        this.userRepository = userRepository;
    }
    
    /**
     * Get personalized roommate announcement recommendations for a user
     * Uses ML compatibility scoring to rank announcements
     */
    public List<AnnouncementWithScore> getRecommendationsForUser(Integer userId, Integer limit) {
        logger.info("Generating recommendations for user {} with limit {}", userId, limit);
        
        // Validate input
        int actualLimit = Math.min(limit != null ? limit : DEFAULT_RECOMMENDATION_LIMIT, MAX_RECOMMENDATION_LIMIT);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        
        // Get all active announcements excluding user's own announcements
        // Use a large page to get all announcements for ML processing
        Page<RoommateAnnouncement> announcementsPage = announcementRepository
                .findByPosterIdNotAndStatusAndExpiresAtAfter(
                        userId,
                        RoommateAnnouncement.AnnouncementStatus.ACTIVE,
                        LocalDateTime.now(),
                        PageRequest.of(0, 1000) // Get up to 1000 announcements for ML processing
                );
        
        List<RoommateAnnouncement> allAnnouncements = announcementsPage.getContent();
        
        logger.debug("Found {} active announcements for user {}", allAnnouncements.size(), userId);
        
        // Calculate compatibility scores for each announcement (using preferences-enhanced algorithm)
        List<AnnouncementWithScore> scoredAnnouncements = allAnnouncements.stream()
                .map(announcement -> {
                    try {
                        // Use new roommate announcement compatibility that includes preferences
                        BigDecimal score = compatibilityService.calculateRoommateAnnouncementCompatibility(user, announcement);
                        return AnnouncementWithScore.builder()
                                .announcement(announcement)
                                .compatibilityScore(score)
                                .build();
                    } catch (Exception e) {
                        logger.warn("Error calculating compatibility for announcement {}: {}", 
                                   announcement.getId(), e.getMessage());
                        // Return with neutral score if calculation fails
                        return AnnouncementWithScore.builder()
                                .announcement(announcement)
                                .compatibilityScore(BigDecimal.valueOf(0.50))
                                .build();
                    }
                })
                // Filter by minimum compatibility threshold
                .filter(aws -> aws.getCompatibilityScore().compareTo(MIN_COMPATIBILITY_THRESHOLD) >= 0)
                // Sort by compatibility score descending, then by creation date descending
                .sorted((a, b) -> {
                    int scoreComparison = b.getCompatibilityScore().compareTo(a.getCompatibilityScore());
                    if (scoreComparison != 0) {
                        return scoreComparison;
                    }
                    return b.getAnnouncement().getCreatedAt().compareTo(a.getAnnouncement().getCreatedAt());
                })
                .limit(actualLimit)
                .collect(Collectors.toList());
        
        logger.info("Generated {} recommendations for user {} (filtered from {} announcements)", 
                   scoredAnnouncements.size(), userId, allAnnouncements.size());
        
        // Analytics saving temporarily disabled to prevent transaction issues
        // TODO: Implement analytics saving in a separate transaction or async process
        logger.debug("Analytics saving disabled - generated {} recommendations for user {}", 
                    scoredAnnouncements.size(), userId);
        
        return scoredAnnouncements;
    }
    
    /**
     * Get compatible applicants for a specific announcement
     * Ranks applicants by compatibility score
     */
    public List<UserWithScore> getCompatibleApplicants(Long announcementId) {
        logger.info("Finding compatible applicants for announcement {}", announcementId);
        
        RoommateAnnouncement announcement = announcementRepository.findByIdWithDetails(announcementId)
                .orElseThrow(() -> new ResourceNotFoundException("Announcement not found: " + announcementId));
        
        // Get all applications for this announcement
        List<RoommateApplication> applications = announcement.getApplications();
        
        logger.debug("Found {} applications for announcement {}", applications.size(), announcementId);
        
        // Calculate compatibility scores for each applicant (using preferences-enhanced algorithm)
        List<UserWithScore> compatibleApplicants = applications.stream()
                .map(application -> {
                    try {
                        // Use new roommate announcement compatibility that includes preferences
                        BigDecimal score = compatibilityService.calculateRoommateAnnouncementCompatibility(
                                application.getApplicant(), announcement);
                        
                        UserWithScore userWithScore = UserWithScore.fromUser(application.getApplicant(), score);
                        userWithScore.setApplicationId(application.getId());
                        userWithScore.setApplicationStatus(application.getStatus().name());
                        userWithScore.setAppliedAt(application.getAppliedAt());
                        
                        return userWithScore;
                    } catch (Exception e) {
                        logger.warn("Error calculating compatibility for applicant {}: {}", 
                                   application.getApplicant().getId(), e.getMessage());
                        
                        UserWithScore userWithScore = UserWithScore.fromUser(application.getApplicant(), BigDecimal.valueOf(0.50));
                        userWithScore.setApplicationId(application.getId());
                        userWithScore.setApplicationStatus(application.getStatus().name());
                        userWithScore.setAppliedAt(application.getAppliedAt());
                        
                        return userWithScore;
                    }
                })
                // Sort by compatibility score descending, then by application date
                .sorted((a, b) -> {
                    int scoreComparison = b.getCompatibilityScore().compareTo(a.getCompatibilityScore());
                    if (scoreComparison != 0) {
                        return scoreComparison;
                    }
                    return b.getAppliedAt().compareTo(a.getAppliedAt());
                })
                .collect(Collectors.toList());
        
        logger.info("Ranked {} applicants by compatibility for announcement {}", 
                   compatibleApplicants.size(), announcementId);
        
        return compatibleApplicants;
    }
    
    /**
     * Get high-quality matches for a user (compatibility score >= 0.7)
     */
    public List<AnnouncementWithScore> getHighQualityMatches(Integer userId, Integer limit) {
        logger.info("Finding high-quality matches for user {}", userId);
        
        List<AnnouncementWithScore> allRecommendations = getRecommendationsForUser(userId, MAX_RECOMMENDATION_LIMIT);
        
        int actualLimit = Math.min(limit != null ? limit : DEFAULT_RECOMMENDATION_LIMIT, MAX_RECOMMENDATION_LIMIT);
        
        List<AnnouncementWithScore> highQualityMatches = allRecommendations.stream()
                .filter(aws -> aws.getCompatibilityScore().compareTo(BigDecimal.valueOf(0.70)) >= 0)
                .limit(actualLimit)
                .collect(Collectors.toList());
        
        logger.info("Found {} high-quality matches for user {}", highQualityMatches.size(), userId);
        
        return highQualityMatches;
    }
    
    /**
     * Get similar users based on compatibility with a specific announcement
     * Useful for suggesting potential roommates
     */
    public List<UserWithScore> getSimilarUsers(Long announcementId, Integer limit) {
        logger.info("Finding similar users for announcement {}", announcementId);
        
        RoommateAnnouncement announcement = announcementRepository.findByIdWithDetails(announcementId)
                .orElseThrow(() -> new ResourceNotFoundException("Announcement not found: " + announcementId));
        
        int actualLimit = Math.min(limit != null ? limit : DEFAULT_RECOMMENDATION_LIMIT, MAX_RECOMMENDATION_LIMIT);
        
        // Get all active students except the poster
        List<User> allStudents = userRepository.findByRoleAndIdNot(User.Role.STUDENT, announcement.getPoster().getId());
        
        // Calculate compatibility scores for each user (using preferences-enhanced algorithm)
        List<UserWithScore> similarUsers = allStudents.stream()
                .map(user -> {
                    try {
                        // Use new roommate announcement compatibility that includes preferences
                        BigDecimal score = compatibilityService.calculateRoommateAnnouncementCompatibility(user, announcement);
                        return UserWithScore.fromUser(user, score);
                    } catch (Exception e) {
                        logger.warn("Error calculating compatibility for user {}: {}", 
                                   user.getId(), e.getMessage());
                        return UserWithScore.fromUser(user, BigDecimal.valueOf(0.50));
                    }
                })
                .filter(uws -> uws.getCompatibilityScore().compareTo(MIN_COMPATIBILITY_THRESHOLD) >= 0)
                .sorted((a, b) -> b.getCompatibilityScore().compareTo(a.getCompatibilityScore()))
                .limit(actualLimit)
                .collect(Collectors.toList());
        
        logger.info("Found {} similar users for announcement {}", similarUsers.size(), announcementId);
        
        return similarUsers;
    }
    
    /**
     * NEW: Get compatible students based on user profile alone
     * Recommends students even if they haven't applied to your announcements
     * Uses academic compatibility scoring (university, field, education level, age)
     */
    public List<UserWithScore> getCompatibleStudents(Integer userId, Integer limit) {
        logger.info("Finding compatible students for user {} (general compatibility)", userId);
        
        User currentUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        
        int actualLimit = Math.min(limit != null ? limit : DEFAULT_RECOMMENDATION_LIMIT, MAX_RECOMMENDATION_LIMIT);
        
        // Get all active students except the current user
        List<User> allStudents = userRepository.findByRoleAndIdNot(User.Role.STUDENT, userId);
        
        logger.debug("Found {} total students to evaluate for user {}", allStudents.size(), userId);
        
        // Calculate academic compatibility scores for each student
        List<UserWithScore> compatibleStudents = allStudents.stream()
                .map(student -> {
                    try {
                        // Use direct user-to-user compatibility (academic focused)
                        BigDecimal score = compatibilityService.calculateUserToUserCompatibility(currentUser, student);
                        
                        // Build recommendation reason based on profile similarity
                        String reason = buildRecommendationReason(currentUser, student, score);
                        
                        return UserWithScore.fromUserWithDetails(student, score, reason, null);
                    } catch (Exception e) {
                        logger.warn("Error calculating compatibility between user {} and student {}: {}", 
                                   userId, student.getId(), e.getMessage());
                        return UserWithScore.fromUserWithDetails(student, BigDecimal.valueOf(0.30), 
                                                               "Profile analysis unavailable", null);
                    }
                })
                .filter(uws -> uws.getCompatibilityScore().compareTo(MIN_COMPATIBILITY_THRESHOLD) >= 0)
                .sorted((a, b) -> b.getCompatibilityScore().compareTo(a.getCompatibilityScore()))
                .limit(actualLimit)
                .collect(Collectors.toList());
        
        // Add ranking information
        for (int i = 0; i < compatibleStudents.size(); i++) {
            compatibleStudents.get(i).setRank(i + 1);
        }
        
        logger.info("Found {} compatible students for user {} (filtered from {} total students)", 
                   compatibleStudents.size(), userId, allStudents.size());
        
        return compatibleStudents;
    }
    
    /**
     * Build a human-readable recommendation reason based on profile similarity
     */
    private String buildRecommendationReason(User currentUser, User recommendedUser, BigDecimal score) {
        StringBuilder reason = new StringBuilder();
        
        // Check institute match
        String currentInstitute = getInstitute(currentUser);
        String recommendedInstitute = getInstitute(recommendedUser);
        if (currentInstitute != null && currentInstitute.equalsIgnoreCase(recommendedInstitute)) {
            reason.append("Same institute");
        }
        
        // Check field of study match
        String currentField = getFieldOfStudy(currentUser);
        String recommendedField = getFieldOfStudy(recommendedUser);
        if (currentField != null && currentField.equalsIgnoreCase(recommendedField)) {
            if (reason.length() > 0) reason.append(" + ");
            reason.append("same field");
        } else if (currentField != null && recommendedField != null && 
                   areRelatedFields(currentField, recommendedField)) {
            if (reason.length() > 0) reason.append(" + ");
            reason.append("related field");
        }
        
        // Check education level match
        UserProfile.EducationLevel currentLevel = getEducationLevel(currentUser);
        UserProfile.EducationLevel recommendedLevel = getEducationLevel(recommendedUser);
        if (currentLevel != null && currentLevel == recommendedLevel) {
            if (reason.length() > 0) reason.append(" + ");
            reason.append("same level");
        }
        
        // Check age similarity
        Integer currentAge = currentUser.getAge();
        Integer recommendedAge = recommendedUser.getAge();
        if (currentAge != null && recommendedAge != null && 
            Math.abs(currentAge - recommendedAge) <= 2) {
            if (reason.length() > 0) reason.append(" + ");
            reason.append("similar age");
        }
        
        // Default reason if nothing specific
        if (reason.length() == 0) {
            if (score.compareTo(BigDecimal.valueOf(0.70)) >= 0) {
                reason.append("Strong academic compatibility");
            } else if (score.compareTo(BigDecimal.valueOf(0.50)) >= 0) {
                reason.append("Good academic match");
            } else {
                reason.append("Compatible profile");
            }
        }
        
        return reason.toString();
    }
    
    // Helper methods for recommendation reason building
    private String getInstitute(User user) {
        return user.getUserProfile() != null ? user.getUserProfile().getInstitute() : null;
    }
    
    private String getFieldOfStudy(User user) {
        return user.getUserProfile() != null ? user.getUserProfile().getFieldOfStudy() : user.getStudyField();
    }
    
    private UserProfile.EducationLevel getEducationLevel(User user) {
        return user.getUserProfile() != null ? user.getUserProfile().getEducationLevel() : null;
    }
    
    private boolean areRelatedFields(String field1, String field2) {
        // Simple field relationship check
        return (field1.toLowerCase().contains("informatique") && field2.toLowerCase().contains("informatique")) ||
               (field1.toLowerCase().contains("math") && field2.toLowerCase().contains("stat")) ||
               (field1.toLowerCase().contains("physique") && field2.toLowerCase().contains("math"));
    }
    
    /**
     * Get recommendation statistics for analytics
     */
    public RecommendationStats getRecommendationStats(Integer userId) {
        logger.debug("Getting recommendation statistics for user {}", userId);
        
        try {
            // Get total recommendations generated
            long totalRecommendations = matchRepository.countByUserId(userId);
            
            // Get unviewed recommendations
            long unviewedRecommendations = matchRepository.countByUserIdAndViewedFalse(userId);
            
            // Get successful matches (applied and accepted)
            long successfulMatches = matchRepository.countSuccessfulMatches(userId);
            
            // Calculate success rate
            double successRate = totalRecommendations > 0 ? 
                               (double) successfulMatches / totalRecommendations : 0.0;
            
            return RecommendationStats.builder()
                    .totalRecommendations(totalRecommendations)
                    .unviewedRecommendations(unviewedRecommendations)
                    .successfulMatches(successfulMatches)
                    .successRate(BigDecimal.valueOf(successRate).setScale(3, BigDecimal.ROUND_HALF_UP))
                    .build();
        } catch (Exception e) {
            logger.warn("Error getting recommendation stats for user {}: {}", userId, e.getMessage());
            // Return empty stats if analytics data is not available
            return RecommendationStats.builder()
                    .totalRecommendations(0L)
                    .unviewedRecommendations(0L)
                    .successfulMatches(0L)
                    .successRate(BigDecimal.ZERO)
                    .build();
        }
    }
    
    /**
     * Mark recommendation as viewed for analytics
     */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void markRecommendationAsViewed(Long matchId) {
        int updated = matchRepository.markAsViewed(matchId);
        if (updated > 0) {
            logger.debug("Marked recommendation {} as viewed", matchId);
        }
    }
    
    /**
     * Mark recommendation as clicked for analytics
     */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void markRecommendationAsClicked(Long matchId) {
        int updated = matchRepository.markAsClicked(matchId);
        if (updated > 0) {
            logger.debug("Marked recommendation {} as clicked", matchId);
        }
    }
    
    /**
     * Save recommendation matches for analytics and tracking
     */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    private void saveRecommendationMatches(User user, List<AnnouncementWithScore> recommendations) {
        try {
            for (AnnouncementWithScore aws : recommendations) {
                // Check if match already exists
                boolean exists = matchRepository.existsByUserIdAndRecommendedUserIdAndAnnouncementId(
                        user.getId(),
                        aws.getAnnouncement().getPoster().getId(),
                        aws.getAnnouncement().getId()
                );
                
                if (!exists) {
                    RoommateMatch match = RoommateMatch.builder()
                            .user(user)
                            .recommendedUser(aws.getAnnouncement().getPoster())
                            .announcement(aws.getAnnouncement())
                            .compatibilityScore(aws.getCompatibilityScore())
                            .matchFactors(buildMatchFactorsJson(aws.getCompatibilityScore()))
                            .viewed(false)
                            .clicked(false)
                            .applied(false)
                            .build();
                    
                    matchRepository.save(match);
                }
            }
            logger.debug("Saved {} recommendation matches for user {}", 
                        recommendations.size(), user.getId());
        } catch (Exception e) {
            logger.warn("Error saving recommendation matches for user {}: {}", 
                       user.getId(), e.getMessage());
        }
    }
    
    /**
     * Build match factors JSON for analytics
     */
    private String buildMatchFactorsJson(BigDecimal compatibilityScore) {
        return String.format("{\"overall_score\": %.2f, \"generated_at\": \"%s\"}", 
                           compatibilityScore.doubleValue(), 
                           LocalDateTime.now().toString());
    }
    
    /**
     * DTO for recommendation statistics
     */
    public static class RecommendationStats {
        private final long totalRecommendations;
        private final long unviewedRecommendations;
        private final long successfulMatches;
        private final BigDecimal successRate;
        
        private RecommendationStats(RecommendationStatsBuilder builder) {
            this.totalRecommendations = builder.totalRecommendations;
            this.unviewedRecommendations = builder.unviewedRecommendations;
            this.successfulMatches = builder.successfulMatches;
            this.successRate = builder.successRate;
        }
        
        // Getters
        public long getTotalRecommendations() { return totalRecommendations; }
        public long getUnviewedRecommendations() { return unviewedRecommendations; }
        public long getSuccessfulMatches() { return successfulMatches; }
        public BigDecimal getSuccessRate() { return successRate; }
        
        public static RecommendationStatsBuilder builder() {
            return new RecommendationStatsBuilder();
        }
        
        public static class RecommendationStatsBuilder {
            private long totalRecommendations;
            private long unviewedRecommendations;
            private long successfulMatches;
            private BigDecimal successRate;
            
            public RecommendationStatsBuilder totalRecommendations(long totalRecommendations) {
                this.totalRecommendations = totalRecommendations;
                return this;
            }
            
            public RecommendationStatsBuilder unviewedRecommendations(long unviewedRecommendations) {
                this.unviewedRecommendations = unviewedRecommendations;
                return this;
            }
            
            public RecommendationStatsBuilder successfulMatches(long successfulMatches) {
                this.successfulMatches = successfulMatches;
                return this;
            }
            
            public RecommendationStatsBuilder successRate(BigDecimal successRate) {
                this.successRate = successRate;
                return this;
            }
            
            public RecommendationStats build() {
                return new RecommendationStats(this);
            }
        }
    }
    
    /**
     * Get enhanced recommendation statistics for frontend display
     */
    public RecommendationStatsDTO getEnhancedRecommendationStats(Integer userId) {
        logger.debug("Getting enhanced recommendation statistics for user {}", userId);
        
        // Get user's current recommendations to calculate real-time stats
        List<AnnouncementWithScore> currentRecommendations = getRecommendationsForUser(userId, MAX_RECOMMENDATION_LIMIT);
        
        // Calculate statistics from current recommendations
        long totalRecommendations = currentRecommendations.size();
        
        // Count high-quality matches (>= 70% compatibility)
        long highQualityMatches = currentRecommendations.stream()
                .mapToLong(aws -> aws.getCompatibilityScore().compareTo(BigDecimal.valueOf(0.70)) >= 0 ? 1 : 0)
                .sum();
        
        // Calculate average compatibility
        BigDecimal averageCompatibility = totalRecommendations > 0 ? 
                currentRecommendations.stream()
                        .map(AnnouncementWithScore::getCompatibilityScore)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                        .divide(BigDecimal.valueOf(totalRecommendations), 4, BigDecimal.ROUND_HALF_UP) :
                BigDecimal.ZERO;
        
        // Find top compatibility score
        BigDecimal topCompatibilityScore = currentRecommendations.stream()
                .map(AnnouncementWithScore::getCompatibilityScore)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
        
        // Get historical data from matches table (with error handling)
        long unviewedRecommendations = 0L;
        long successfulMatches = 0L;
        
        try {
            unviewedRecommendations = matchRepository.countByUserIdAndViewedFalse(userId);
            successfulMatches = matchRepository.countSuccessfulMatches(userId);
        } catch (Exception e) {
            logger.warn("Error getting historical match data for user {}: {}", userId, e.getMessage());
        }
        
        // Calculate success rate
        BigDecimal successRate = totalRecommendations > 0 ? 
                BigDecimal.valueOf(successfulMatches).divide(BigDecimal.valueOf(totalRecommendations), 4, BigDecimal.ROUND_HALF_UP) :
                BigDecimal.ZERO;
        
        // Create recommendation types breakdown (simplified for now)
        java.util.Map<String, Integer> recommendationTypes = new java.util.HashMap<>();
        recommendationTypes.put("university", (int) (totalRecommendations * 0.4)); // 40% university matches
        recommendationTypes.put("fieldOfStudy", (int) (totalRecommendations * 0.25)); // 25% field matches  
        recommendationTypes.put("educationLevel", (int) (totalRecommendations * 0.20)); // 20% education matches
        recommendationTypes.put("age", (int) (totalRecommendations * 0.15)); // 15% age matches
        
        return RecommendationStatsDTO.builder()
                .totalRecommendations(totalRecommendations)
                .highQualityMatches(highQualityMatches)
                .averageCompatibility(averageCompatibility)
                .topCompatibilityScore(topCompatibilityScore)
                .recommendationTypes(recommendationTypes)
                .unviewedRecommendations(unviewedRecommendations)
                .successfulMatches(successfulMatches)
                .successRate(successRate)
                .build();
    }
} 