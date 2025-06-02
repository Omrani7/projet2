package com.example.spring_security.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity tracking ML-generated roommate matches and recommendations
 * Used for analytics, performance tracking, and recommendation improvements
 */
@Entity
@Table(name = "roommate_matches",
    indexes = {
        @Index(name = "idx_roommate_matches_user", columnList = "user_id"),
        @Index(name = "idx_roommate_matches_recommended_user", columnList = "recommended_user_id"),
        @Index(name = "idx_roommate_matches_announcement", columnList = "announcement_id"),
        @Index(name = "idx_roommate_matches_score", columnList = "compatibility_score")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_recommended_announcement", 
                         columnNames = {"user_id", "recommended_user_id", "announcement_id"})
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoommateMatch {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * The user who received this recommendation
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    /**
     * The user being recommended as a potential roommate
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recommended_user_id", nullable = false)
    private User recommendedUser;
    
    /**
     * The announcement this recommendation is based on (optional)
     * Can be null for general compatibility matches not tied to specific announcements
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "announcement_id")
    private RoommateAnnouncement announcement;
    
    /**
     * Compatibility score calculated by ML algorithm (0.00 to 1.00)
     */
    @Column(name = "compatibility_score", precision = 3, scale = 2, nullable = false)
    private BigDecimal compatibilityScore;
    
    /**
     * JSON string containing factors that contributed to the match
     * Example: {"age_compatibility": 0.9, "lifestyle_match": 0.8, "budget_compatibility": 0.95}
     */
    @Column(name = "match_factors", columnDefinition = "TEXT")
    private String matchFactors;
    
    /**
     * Timestamp when this match was generated
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    /**
     * Whether the user has viewed this recommendation
     */
    @Column(name = "viewed", nullable = false)
    @Builder.Default
    private Boolean viewed = false;
    
    /**
     * Whether the user clicked on this recommendation
     */
    @Column(name = "clicked", nullable = false)
    @Builder.Default
    private Boolean clicked = false;
    
    /**
     * Whether the user applied based on this recommendation
     */
    @Column(name = "applied", nullable = false)
    @Builder.Default
    private Boolean applied = false;
    
    /**
     * Whether the application was successful (accepted)
     */
    @Column(name = "application_successful", nullable = false)
    @Builder.Default
    private Boolean applicationSuccessful = false;
    
    /**
     * Timestamp when the user viewed this recommendation
     */
    @Column(name = "viewed_at")
    private LocalDateTime viewedAt;
    
    /**
     * Timestamp when the user clicked on this recommendation
     */
    @Column(name = "clicked_at")
    private LocalDateTime clickedAt;
    
    /**
     * Timestamp when the user applied based on this recommendation
     */
    @Column(name = "applied_at")
    private LocalDateTime appliedAt;
    
    /**
     * Additional metadata for analytics (JSON format)
     */
    @Column(name = "analytics_metadata", columnDefinition = "TEXT")
    private String analyticsMetadata;
    
    // Helper methods for tracking user interactions
    
    /**
     * Mark this recommendation as viewed
     */
    public void markAsViewed() {
        if (!viewed) {
            this.viewed = true;
            this.viewedAt = LocalDateTime.now();
        }
    }
    
    /**
     * Mark this recommendation as clicked
     */
    public void markAsClicked() {
        if (!clicked) {
            this.clicked = true;
            this.clickedAt = LocalDateTime.now();
            // Auto-mark as viewed if not already
            markAsViewed();
        }
    }
    
    /**
     * Mark that user applied based on this recommendation
     */
    public void markAsApplied() {
        if (!applied) {
            this.applied = true;
            this.appliedAt = LocalDateTime.now();
            // Auto-mark as clicked and viewed
            markAsClicked();
        }
    }
    
    /**
     * Mark the application as successful
     */
    public void markApplicationSuccessful() {
        this.applicationSuccessful = true;
        // Ensure applied is also marked
        markAsApplied();
    }
    
    /**
     * Get compatibility score as percentage (for UI display)
     */
    public Integer getCompatibilityPercentage() {
        if (compatibilityScore == null) {
            return 0;
        }
        return compatibilityScore.multiply(new BigDecimal("100")).intValue();
    }
    
    /**
     * Check if this is a high-quality match (>= 70% compatibility)
     */
    public boolean isHighQualityMatch() {
        return getCompatibilityPercentage() >= 70;
    }
    
    /**
     * Check if this is a medium-quality match (50-69% compatibility)
     */
    public boolean isMediumQualityMatch() {
        int percentage = getCompatibilityPercentage();
        return percentage >= 50 && percentage < 70;
    }
    
    /**
     * Check if this is a low-quality match (< 50% compatibility)
     */
    public boolean isLowQualityMatch() {
        return getCompatibilityPercentage() < 50;
    }
    
    /**
     * Calculate the conversion rate for this match type (for analytics)
     * Returns the ratio of applications to views
     */
    public double getConversionRate() {
        if (!viewed) {
            return 0.0;
        }
        return applied ? 1.0 : 0.0;
    }
    
    /**
     * Check if the recommendation led to a successful outcome
     */
    public boolean wasSuccessful() {
        return applied && applicationSuccessful;
    }
    
    /**
     * Get the quality category as string
     */
    public String getQualityCategory() {
        if (isHighQualityMatch()) {
            return "HIGH";
        } else if (isMediumQualityMatch()) {
            return "MEDIUM";
        } else {
            return "LOW";
        }
    }
    
    /**
     * Check if this match is for a specific announcement or general compatibility
     */
    public boolean isAnnouncementSpecific() {
        return announcement != null;
    }
    
    /**
     * Check if this is a general compatibility match
     */
    public boolean isGeneralCompatibility() {
        return announcement == null;
    }
    
    /**
     * Get display name for the recommended user
     */
    public String getRecommendedUserDisplayName() {
        return recommendedUser != null ? recommendedUser.getUsername() : "Unknown User";
    }
    
    /**
     * Calculate time since match was created (in hours)
     */
    public long getHoursSinceCreated() {
        return java.time.Duration.between(createdAt, LocalDateTime.now()).toHours();
    }
    
    /**
     * Check if this is a fresh recommendation (less than 24 hours old)
     */
    public boolean isFreshRecommendation() {
        return getHoursSinceCreated() < 24;
    }
} 