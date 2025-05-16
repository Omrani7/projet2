package com.example.spring_security.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PropertyListingDTO {
    private Long id;
    private String title;
    private String description; // For list view, this might be a summary. For detail, full.
    private BigDecimal price;
    // Simplified location for list view; more details can be added or kept for detailed view
    private String city;
    private String district;
    private String location; // General location string from entity
    private Double area;
    private String propertyType;
    private Double latitude;
    private Double longitude;
    private Integer rooms;
    private Integer bedrooms;
    private Integer bathrooms;
    private List<String> imageUrls; // Consider if only the main image is needed for list view
    private LocalDateTime listingDate; // Date it was listed
    private boolean active;
    private String ownerUsername; // Example of derived data, if user is loaded
    private String mainImageUrl; // Often, a single main image is useful for list views
    // Potentially: String formattedAddress; (if needed by frontend directly)
} 