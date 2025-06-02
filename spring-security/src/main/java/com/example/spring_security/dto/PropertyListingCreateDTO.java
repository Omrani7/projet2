package com.example.spring_security.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PropertyListingCreateDTO {
    @NotBlank
    private String title;

    @NotBlank
    private String description;

    @NotNull
    @Min(1)
    private BigDecimal price;

    @NotBlank
    private String propertyType;
    
    @NotBlank
    private String fullAddress;
    
    private String city;
    private String district;
    
    // Optional fields
    private Integer rooms;
    private Integer bedrooms;
    private Integer bathrooms;
    private Double area;
    private Integer floor;
    private Boolean hasBalcony;

    // Payment and availability information
    private BigDecimal securityDeposit;
    private LocalDate availableFrom;
    private LocalDate availableTo;
    private String paymentFrequency; // monthly, quarterly, yearly
    private Integer minimumStayMonths;

    // Amenities
    private Set<String> amenities;

    private String contactInfo;
} 