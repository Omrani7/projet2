package com.example.spring_security.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Entity representing a roommate announcement posted by students
 * Type A: Based on closed property deals (auto-populated)
 * Type B: Manual property details entry
 */
@Entity
@Table(name = "roommate_announcements",
    indexes = {
        @Index(name = "idx_roommate_announcements_poster", columnList = "poster_id"),
        @Index(name = "idx_roommate_announcements_status", columnList = "status"),
        @Index(name = "idx_roommate_announcements_location", columnList = "property_latitude, property_longitude"),
        @Index(name = "idx_roommate_announcements_move_in_date", columnList = "move_in_date"),
        @Index(name = "idx_roommate_announcements_created_at", columnList = "created_at")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoommateAnnouncement {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * The student who posted this roommate announcement
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "poster_id", nullable = false)
    private User poster;
    
    /**
     * Reference to existing property listing for Type A announcements (from closed deals)
     * Null for Type B announcements (manual property entry)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_listing_id")
    private PropertyListing propertyListing;
    
    // Property Details (auto-filled for Type A, manual for Type B)
    @Column(name = "property_title", length = 500, nullable = false)
    private String propertyTitle;
    
    @Column(name = "property_address", columnDefinition = "TEXT", nullable = false)
    private String propertyAddress;
    
    @Column(name = "property_latitude")
    private Double propertyLatitude;
    
    @Column(name = "property_longitude")
    private Double propertyLongitude;
    
    @Column(name = "total_rent", precision = 10, scale = 2, nullable = false)
    private BigDecimal totalRent;
    
    @Column(name = "total_rooms", nullable = false)
    private Integer totalRooms;
    
    @Column(name = "available_rooms", nullable = false)
    private Integer availableRooms;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "property_type", length = 50, nullable = false)
    private PropertyType propertyType;
    
    @ElementCollection
    @CollectionTable(name = "roommate_announcement_amenities", 
                    joinColumns = @JoinColumn(name = "announcement_id"))
    @Column(name = "amenity")
    private Set<String> amenities = new HashSet<>();
    
    @ElementCollection
    @CollectionTable(name = "roommate_announcement_images", 
                    joinColumns = @JoinColumn(name = "announcement_id"))
    @Column(name = "image_url", columnDefinition = "TEXT")
    private List<String> imageUrls = new ArrayList<>();
    
    // Roommate Preferences
    @Column(name = "max_roommates", nullable = false)
    private Integer maxRoommates;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "gender_preference", length = 20, nullable = false)
    @Builder.Default
    private GenderPreference genderPreference = GenderPreference.NO_PREFERENCE;
    
    @Column(name = "age_min", nullable = false)
    @Builder.Default
    private Integer ageMin = 18;
    
    @Column(name = "age_max", nullable = false)
    @Builder.Default
    private Integer ageMax = 35;
    
    @ElementCollection
    @CollectionTable(name = "roommate_announcement_lifestyle_tags", 
                    joinColumns = @JoinColumn(name = "announcement_id"))
    @Column(name = "lifestyle_tag")
    private Set<String> lifestyleTags = new HashSet<>();
    
    @Column(name = "smoking_allowed", nullable = false)
    @Builder.Default
    private Boolean smokingAllowed = false;
    
    @Column(name = "pets_allowed", nullable = false)
    @Builder.Default
    private Boolean petsAllowed = false;
    
    @Column(name = "cleanliness_level", nullable = false)
    @Builder.Default
    private Integer cleanlinessLevel = 3;
    
    // Financial Details
    @Column(name = "rent_per_person", precision = 10, scale = 2, nullable = false)
    private BigDecimal rentPerPerson;
    
    @Column(name = "security_deposit", precision = 10, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal securityDeposit = BigDecimal.ZERO;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "utilities_split", length = 20, nullable = false)
    @Builder.Default
    private UtilitiesSplit utilitiesSplit = UtilitiesSplit.EQUAL;
    
    @Column(name = "additional_costs", columnDefinition = "TEXT")
    private String additionalCosts;
    
    // Posting Details
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "move_in_date", nullable = false)
    private LocalDate moveInDate;
    
    @Column(name = "lease_duration_months", nullable = false)
    private Integer leaseDurationMonths;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private AnnouncementStatus status = AnnouncementStatus.ACTIVE;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    // Relationships
    @OneToMany(mappedBy = "announcement", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference("announcement-applications")
    private List<RoommateApplication> applications = new ArrayList<>();
    
    @OneToMany(mappedBy = "announcement", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference("announcement-conversations")
    private List<Conversation> conversations = new ArrayList<>();
    
    // Enums
    public enum PropertyType {
        APARTMENT, HOUSE, STUDIO
    }
    
    public enum GenderPreference {
        MALE, FEMALE, MIXED, NO_PREFERENCE
    }
    
    public enum UtilitiesSplit {
        EQUAL, USAGE_BASED
    }
    
    public enum AnnouncementStatus {
        ACTIVE, PAUSED, FILLED, EXPIRED
    }
    
    /**
     * Helper method to check if announcement is from a closed deal (Type A)
     */
    public boolean isTypeA() {
        return propertyListing != null;
    }
    
    /**
     * Helper method to check if announcement is manual entry (Type B)
     */
    public boolean isTypeB() {
        return propertyListing == null;
    }
    
    /**
     * Check if the announcement is still active and within expiration date
     */
    public boolean isActive() {
        return status == AnnouncementStatus.ACTIVE && 
               expiresAt != null && 
               expiresAt.isAfter(LocalDateTime.now());
    }
    
    /**
     * Calculate the number of remaining spots available
     */
    public int getRemainingSpots() {
        int applicationCount = (applications != null) ? applications.size() : 0;
        return Math.max(0, maxRoommates - applicationCount);
    }
} 