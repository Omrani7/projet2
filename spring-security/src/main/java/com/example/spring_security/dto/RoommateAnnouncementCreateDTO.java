package com.example.spring_security.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

/**
 * DTO for creating a new roommate announcement
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoommateAnnouncementCreateDTO {
    
    // Optional - for Type A announcements (based on closed deals)
    private Long propertyListingId;
    
    // Property Details (required for Type B, auto-filled for Type A)
    private String propertyTitle;
    private String propertyAddress;
    private Double propertyLatitude;
    private Double propertyLongitude;
    private BigDecimal totalRent;
    private Integer totalRooms;
    private Integer availableRooms;
    private String propertyType;
    
    // Roommate Preferences
    private Integer maxRoommates;
    private String genderPreference;
    private Integer ageMin;
    private Integer ageMax;
    private Set<String> lifestyleTags;
    private Boolean smokingAllowed;
    private Boolean petsAllowed;
    private Integer cleanlinessLevel;
    
    // Financial Details
    private BigDecimal rentPerPerson;
    private BigDecimal securityDeposit;
    private String utilitiesSplit;
    private String additionalCosts;
    
    // Posting Details
    private String description;
    private LocalDate moveInDate;
    private Integer leaseDurationMonths;
} 