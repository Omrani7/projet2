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
public class OwnerPropertyListingDTO {
    private Long id;
    private String title;
    private String description;
    private BigDecimal price;
    private String propertyType;
    private String fullAddress;
    private String city;
    private String district;
    private Double latitude;
    private Double longitude;
    private String formattedAddress;
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
    private List<String> imageUrls;
    private Long ownerId;
    private String ownerUsername; // Or any other relevant user detail
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime listingDate;
    private boolean active;
} 