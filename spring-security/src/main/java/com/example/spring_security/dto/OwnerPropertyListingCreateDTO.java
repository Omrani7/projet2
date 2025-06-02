package com.example.spring_security.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
// import java.util.List; // No longer needed for imageUrls here
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OwnerPropertyListingCreateDTO {
    // Fields that MUST be provided by the user when creating a listing
    private String title;
    private BigDecimal price;
    private String propertyType; // e.g., "Apartment", "House", "Studio"
    private String fullAddress; // Full address string for geocoding
    private String city; // Consider if this is derivable or should be explicit
    private String district; // Consider if this is derivable or should be explicit

    // Optional fields
    private String description;
    private Integer rooms;
    private Integer bedrooms;
    private Integer bathrooms;
    private Double area;
    private Integer floor;
    private Boolean hasBalcony;
    private BigDecimal securityDeposit;
    private LocalDate availableFrom;
    private LocalDate availableTo;
    private String paymentFrequency;
    private Integer minimumStayMonths;
    private Set<String> amenities;
    private String contactInfo;
    // imageUrls removed, will be handled by service after file upload
} 