package com.example.spring_security.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminRoommateAnnouncementDto {
    private Long id;
    
    // Poster information
    private Integer posterId;
    private String posterUsername;
    private String posterEmail;
    private String posterRole;
    
    // Property details
    private String propertyTitle;
    private String propertyAddress;
    private Double propertyLatitude;
    private Double propertyLongitude;
    private BigDecimal totalRent;
    private Integer totalRooms;
    private Integer availableRooms;
    private String propertyType;
    private Set<String> amenities;
    private List<String> imageUrls;
    
    // Roommate preferences
    private Integer maxRoommates;
    private String genderPreference;
    private Integer ageMin;
    private Integer ageMax;
    private Set<String> lifestyleTags;
    private Boolean smokingAllowed;
    private Boolean petsAllowed;
    private Integer cleanlinessLevel;
    
    // Financial details
    private BigDecimal rentPerPerson;
    private BigDecimal securityDeposit;
    private String utilitiesSplit;
    private String additionalCosts;
    
    // Posting details
    private String description;
    private LocalDate moveInDate;
    private Integer leaseDurationMonths;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private LocalDateTime updatedAt;
    
    // Activity metrics
    private Long totalApplications;
    private Long pendingApplications;
    private Long acceptedApplications;
    private Long viewCount;
    
    // Type information
    private Boolean isTypeA; // Based on closed deal
    private Long propertyListingId; // Reference to original property if Type A
    
    // Helper methods for display
    public String getAnnouncementType() {
        return isTypeA != null && isTypeA ? "Property Owner" : "Looking for Property";
    }
    
    public String getStatusDisplay() {
        if (status == null) return "UNKNOWN";
        return switch (status.toUpperCase()) {
            case "ACTIVE" -> "Active";
            case "PAUSED" -> "Paused";
            case "FILLED" -> "Filled";
            case "EXPIRED" -> "Expired";
            default -> status;
        };
    }
    
    public String getGenderPreferenceDisplay() {
        if (genderPreference == null) return "No Preference";
        return switch (genderPreference.toUpperCase()) {
            case "MALE" -> "Male Only";
            case "FEMALE" -> "Female Only";
            case "MIXED" -> "Mixed";
            case "NO_PREFERENCE" -> "No Preference";
            default -> genderPreference;
        };
    }
    
    public String getUtilitiesSplitDisplay() {
        if (utilitiesSplit == null) return "Equal";
        return switch (utilitiesSplit.toUpperCase()) {
            case "EQUAL" -> "Equal Split";
            case "USAGE_BASED" -> "Usage Based";
            default -> utilitiesSplit;
        };
    }
    
    public int getRemainingSpots() {
        return availableRooms != null ? availableRooms : 0;
    }
    
    public boolean isExpiringSoon() {
        if (expiresAt == null) return false;
        return expiresAt.isBefore(LocalDateTime.now().plusDays(7));
    }
    
    public boolean isExpired() {
        if (expiresAt == null) return false;
        return expiresAt.isBefore(LocalDateTime.now());
    }
} 