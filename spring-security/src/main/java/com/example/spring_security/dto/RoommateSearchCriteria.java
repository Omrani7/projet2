package com.example.spring_security.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

/**
 * DTO for roommate announcement search criteria
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoommateSearchCriteria {
    
    // Budget filters
    private BigDecimal minRentPerPerson;
    private BigDecimal maxRentPerPerson;
    
    // Date filters
    private LocalDate earliestMoveInDate;
    private LocalDate latestMoveInDate;
    
    // Property filters
    private Set<String> propertyTypes; // APARTMENT, HOUSE, STUDIO
    private Integer minTotalRooms;
    private Integer maxTotalRooms;
    private Integer minAvailableRooms;
    private Integer maxAvailableRooms;
    private Set<String> amenities;
    
    // Location filters
    private Double latitude;
    private Double longitude;
    private Integer radiusKm;
    private String city;
    
    // Roommate preference filters
    private Set<String> genderPreferences; // MALE, FEMALE, MIXED, NO_PREFERENCE
    private Integer minAge;
    private Integer maxAge;
    private Set<String> lifestyleTags;
    private Boolean smokingAllowed;
    private Boolean petsAllowed;
    private Integer minCleanlinessLevel;
    private Integer maxCleanlinessLevel;
    
    // Lease filters
    private Integer minLeaseDurationMonths;
    private Integer maxLeaseDurationMonths;
    
    // Availability filters
    private Boolean onlyWithAvailableSpots;
    private Boolean onlyActive;
    
    // Sorting
    private String sortBy; // createdAt, rentPerPerson, moveInDate, compatibilityScore
    private String sortDirection; // ASC, DESC
    
    // Recommendation filters
    private Boolean personalizedRecommendations; // Use ML scoring
    private BigDecimal minCompatibilityScore;
} 