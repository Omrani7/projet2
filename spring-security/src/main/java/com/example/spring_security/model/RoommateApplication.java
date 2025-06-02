package com.example.spring_security.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity representing an application from a student to a roommate announcement
 * Similar to Inquiry but for roommate matching
 */
@Entity
@Table(name = "roommate_applications",
    indexes = {
        @Index(name = "idx_roommate_applications_announcement", columnList = "announcement_id"),
        @Index(name = "idx_roommate_applications_applicant", columnList = "applicant_id"),
        @Index(name = "idx_roommate_applications_poster", columnList = "poster_id"),
        @Index(name = "idx_roommate_applications_status", columnList = "status"),
        @Index(name = "idx_roommate_applications_applied_at", columnList = "applied_at")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_announcement_applicant", 
                         columnNames = {"announcement_id", "applicant_id"})
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoommateApplication {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * The roommate announcement being applied to
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "announcement_id", nullable = false)
    @JsonBackReference("announcement-applications")
    private RoommateAnnouncement announcement;
    
    /**
     * The student who is applying to be a roommate
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "applicant_id", nullable = false)
    private User applicant;
    
    /**
     * The student who posted the announcement (for quick access)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "poster_id", nullable = false)
    private User poster;
    
    /**
     * The application message from the applicant
     */
    @Column(columnDefinition = "TEXT", nullable = false)
    private String message;
    
    /**
     * Compatibility score calculated by ML algorithm (0.00 to 1.00)
     */
    @Column(name = "compatibility_score", precision = 3, scale = 2)
    private BigDecimal compatibilityScore;
    
    /**
     * Current status of the application
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ApplicationStatus status = ApplicationStatus.PENDING;
    
    /**
     * Timestamp when the application was submitted
     */
    @CreationTimestamp
    @Column(name = "applied_at", nullable = false, updatable = false)
    private LocalDateTime appliedAt;
    
    /**
     * Timestamp when the poster responded to the application
     */
    @Column(name = "responded_at")
    private LocalDateTime respondedAt;
    
    /**
     * Additional notes from the poster when accepting/rejecting
     */
    @Column(name = "response_message", columnDefinition = "TEXT")
    private String responseMessage;
    
    // Application status enum
    public enum ApplicationStatus {
        /**
         * Application submitted but not yet reviewed by poster
         */
        PENDING,
        
        /**
         * Application accepted by poster - conversation can begin
         */
        ACCEPTED,
        
        /**
         * Application rejected by poster
         */
        REJECTED,
        
        /**
         * Application withdrawn by applicant before poster response
         */
        WITHDRAWN
    }
    
    /**
     * Helper method to accept the application
     */
    public void accept(String responseMessage) {
        this.status = ApplicationStatus.ACCEPTED;
        this.responseMessage = responseMessage;
        this.respondedAt = LocalDateTime.now();
    }
    
    /**
     * Helper method to reject the application
     */
    public void reject(String responseMessage) {
        this.status = ApplicationStatus.REJECTED;
        this.responseMessage = responseMessage;
        this.respondedAt = LocalDateTime.now();
    }
    
    /**
     * Helper method to withdraw the application
     */
    public void withdraw() {
        this.status = ApplicationStatus.WITHDRAWN;
        this.respondedAt = LocalDateTime.now();
    }
    
    /**
     * Check if the application is still pending
     */
    public boolean isPending() {
        return status == ApplicationStatus.PENDING;
    }
    
    /**
     * Check if the application was accepted
     */
    public boolean isAccepted() {
        return status == ApplicationStatus.ACCEPTED;
    }
    
    /**
     * Check if the application can be withdrawn (only if pending)
     */
    public boolean canBeWithdrawn() {
        return status == ApplicationStatus.PENDING;
    }
    
    /**
     * Get compatibility score as percentage (for UI display)
     */
    public Integer getCompatibilityPercentage() {
        if (compatibilityScore == null) {
            return null;
        }
        return compatibilityScore.multiply(new BigDecimal("100")).intValue();
    }
} 