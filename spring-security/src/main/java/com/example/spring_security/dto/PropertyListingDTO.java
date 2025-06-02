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
public class PropertyListingDTO {
    private Long id;
    private String title;
    private String description;
    private BigDecimal price;
    private String location;
    private Double area;
    private String propertyType;
    private String contactInfo;
    private String city;
    private String district;
    private String fullAddress;
    private Double latitude;
    private Double longitude;
    private String formattedAddress;
    private Integer rooms;
    private Integer bedrooms;
    private Integer bathrooms;
    private List<String> imageUrls;
    private String sourceUrl;
    private String sourceWebsite;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime listingDate;
    private boolean active;
    
    // Owner-specific fields
    private BigDecimal securityDeposit;
    private LocalDate availableTo;
    private String paymentFrequency;
    private Integer minimumStayMonths;
    private Boolean hasBalcony;
    private Integer floor;
    private Set<String> amenities;
    
    // Source type and owner information
    private String sourceType; // "OWNER" or "SCRAPED"
    private Long ownerId; // ID of the owner if sourceType is "OWNER"
} 