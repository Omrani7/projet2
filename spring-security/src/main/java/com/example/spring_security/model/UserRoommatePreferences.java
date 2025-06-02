package com.example.spring_security.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * Entity for storing user roommate preferences for ML compatibility scoring
 * As specified in the comprehensive roommate plan
 */
@Entity
@Table(name = "user_roommate_preferences")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRoommatePreferences {
    
    /**
     * User ID serves as primary key (one-to-one relationship)
     */
    @Id
    @Column(name = "user_id")
    private Integer userId;
    
    /**
     * One-to-one relationship with User entity
     */
    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;
    
    /**
     * Lifestyle preference tags for compatibility matching
     * Examples: QUIET, SOCIAL, STUDIOUS, PARTY, NIGHT_OWL, EARLY_BIRD
     */
    @ElementCollection
    @CollectionTable(name = "user_lifestyle_tags", 
                    joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "lifestyle_tag")
    private Set<String> lifestyleTags;
    
    /**
     * Cleanliness level (1-5 scale)
     * 1 = Very relaxed, 5 = Very strict
     */
    @Column(name = "cleanliness_level")
    private Integer cleanlinessLevel;
    
    /**
     * Social level (1-5 scale)
     * 1 = Very introverted, 5 = Very extroverted
     */
    @Column(name = "social_level")
    private Integer socialLevel;
    
    /**
     * Study habits preferences for compatibility
     * Examples: QUIET_STUDY, GROUP_STUDY, LIBRARY_PREFERRED, HOME_STUDY
     */
    @ElementCollection
    @CollectionTable(name = "user_study_habits", 
                    joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "study_habit")
    private Set<String> studyHabits;
    
    /**
     * Minimum acceptable rent budget
     */
    @Column(name = "budget_min", precision = 10, scale = 2)
    private BigDecimal budgetMin;
    
    /**
     * Maximum rent budget
     */
    @Column(name = "budget_max", precision = 10, scale = 2)
    private BigDecimal budgetMax;
    
    /**
     * Preferred location latitude for proximity scoring
     */
    @Column(name = "preferred_location_latitude")
    private Double preferredLocationLatitude;
    
    /**
     * Preferred location longitude for proximity scoring
     */
    @Column(name = "preferred_location_longitude")
    private Double preferredLocationLongitude;
    
    /**
     * Preferred location description (e.g., "Near University", "Downtown")
     */
    @Column(name = "preferred_location_description")
    private String preferredLocationDescription;
    
    /**
     * Maximum acceptable distance from preferred location (in km)
     */
    @Column(name = "max_distance_km")
    private Integer maxDistanceKm;
    
    /**
     * Gender preference for roommates
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "gender_preference")
    private GenderPreference genderPreference;
    
    /**
     * Minimum acceptable age for roommates
     */
    @Column(name = "age_preference_min")
    private Integer agePreferenceMin;
    
    /**
     * Maximum acceptable age for roommates
     */
    @Column(name = "age_preference_max")
    private Integer agePreferenceMax;
    
    /**
     * Whether smoking is acceptable
     */
    @Column(name = "smoking_acceptable")
    private Boolean smokingAcceptable;
    
    /**
     * Whether pets are acceptable
     */
    @Column(name = "pets_acceptable")
    private Boolean petsAcceptable;
    
    /**
     * Preferred property types
     * Examples: APARTMENT, HOUSE, STUDIO
     */
    @ElementCollection
    @CollectionTable(name = "user_preferred_property_types", 
                    joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "property_type")
    private Set<String> preferredPropertyTypes;
    
    /**
     * Preferred amenities
     * Examples: PARKING, POOL, GYM, LAUNDRY, WIFI
     */
    @ElementCollection
    @CollectionTable(name = "user_preferred_amenities", 
                    joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "amenity")
    private Set<String> preferredAmenities;
    
    /**
     * Minimum number of rooms preferred
     */
    @Column(name = "min_rooms")
    private Integer minRooms;
    
    /**
     * Maximum number of rooms preferred
     */
    @Column(name = "max_rooms")
    private Integer maxRooms;
    
    /**
     * Preferred lease duration in months
     */
    @Column(name = "preferred_lease_duration_months")
    private Integer preferredLeaseDurationMonths;
    
    /**
     * Whether user is open to sharing with multiple roommates
     */
    @Column(name = "open_to_multiple_roommates")
    private Boolean openToMultipleRoommates;
    
    /**
     * Maximum number of total roommates acceptable
     */
    @Column(name = "max_total_roommates")
    private Integer maxTotalRoommates;
    
    /**
     * Additional notes or special requirements
     */
    @Column(name = "additional_notes", columnDefinition = "TEXT")
    private String additionalNotes;
    
    /**
     * Whether these preferences are active for ML matching
     */
    @Column(name = "active_for_matching")
    @Builder.Default
    private Boolean activeForMatching = true;
    
    /**
     * Timestamp when preferences were last updated
     */
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    /**
     * Gender preference enumeration
     */
    public enum GenderPreference {
        MALE_ONLY,
        FEMALE_ONLY,
        MIXED,
        NO_PREFERENCE
    }
    
    // Helper methods for ML calculations
    
    /**
     * Check if budget is within acceptable range
     */
    public boolean isBudgetAcceptable(BigDecimal rentAmount) {
        if (budgetMin != null && rentAmount.compareTo(budgetMin) < 0) {
            return false;
        }
        if (budgetMax != null && rentAmount.compareTo(budgetMax) > 0) {
            return false;
        }
        return true;
    }
    
    /**
     * Check if age is within acceptable range
     */
    public boolean isAgeAcceptable(Integer age) {
        if (age == null) return true; // No age specified
        
        if (agePreferenceMin != null && age < agePreferenceMin) {
            return false;
        }
        if (agePreferenceMax != null && age > agePreferenceMax) {
            return false;
        }
        return true;
    }
    
    /**
     * Calculate lifestyle compatibility score with another user's preferences
     */
    public double calculateLifestyleCompatibility(Set<String> otherLifestyleTags) {
        if (this.lifestyleTags == null || this.lifestyleTags.isEmpty() ||
            otherLifestyleTags == null || otherLifestyleTags.isEmpty()) {
            return 0.5; // Neutral if no preferences
        }
        
        // Calculate Jaccard similarity
        long intersection = this.lifestyleTags.stream()
                .mapToLong(tag -> otherLifestyleTags.contains(tag) ? 1 : 0)
                .sum();
        
        long union = this.lifestyleTags.size() + otherLifestyleTags.size() - intersection;
        
        return union > 0 ? (double) intersection / union : 0.0;
    }
    
    /**
     * Check if property type is preferred
     */
    public boolean isPropertyTypePreferred(String propertyType) {
        return preferredPropertyTypes == null || 
               preferredPropertyTypes.isEmpty() || 
               preferredPropertyTypes.contains(propertyType);
    }
    
    /**
     * Calculate distance-based compatibility score
     */
    public double calculateLocationCompatibility(Double lat, Double lon) {
        if (preferredLocationLatitude == null || preferredLocationLongitude == null ||
            lat == null || lon == null) {
            return 0.5; // Neutral if location not specified
        }
        
        // Simple distance calculation
        double distance = Math.sqrt(
            Math.pow(lat - preferredLocationLatitude, 2) + 
            Math.pow(lon - preferredLocationLongitude, 2)
        ) * 111; // Rough km conversion
        
        if (maxDistanceKm != null && distance > maxDistanceKm) {
            return 0.0; // Outside acceptable range
        }
        
        // Score based on distance
        if (distance <= 2) return 1.0;
        if (distance <= 5) return 0.8;
        if (distance <= 10) return 0.6;
        if (distance <= 20) return 0.4;
        return 0.2;
    }
} 