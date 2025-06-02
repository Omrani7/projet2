package com.example.spring_security.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * Data Transfer Object for RoommateAnnouncement entity
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoommateAnnouncementDTO {
    private Long id;
    private UserBasicDTO poster;
    
    // Property Details
    private String propertyTitle;
    private String propertyAddress;
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
    private String status;
    
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    
    // Helper fields for frontend
    private Integer remainingSpots;
    private Integer applicationCount;
} 