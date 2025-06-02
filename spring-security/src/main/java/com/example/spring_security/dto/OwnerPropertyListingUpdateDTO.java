package com.example.spring_security.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OwnerPropertyListingUpdateDTO {
    private String title;
    private String description;
    private BigDecimal price;
    private String propertyType;
    private String fullAddress; // If address changes, re-geocoding will be needed
    private String city;
    private String district;
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
    private List<String> imageUrls; // Could be a list of new/updated URLs
    private Boolean active; // To activate/deactivate the listing
} 