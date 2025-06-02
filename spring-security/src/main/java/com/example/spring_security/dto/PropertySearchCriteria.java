package com.example.spring_security.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data // Lombok's @Data includes @Getter, @Setter, @ToString, @EqualsAndHashCode, @RequiredArgsConstructor
@NoArgsConstructor
@AllArgsConstructor
public class PropertySearchCriteria {

    // From GET /announcements?univId=&instId=&type=&minPrice=&maxPrice=&radius=&lat=&lng=&page=&size=
    // We'll use instituteId to fetch lat/lng for the selected institute.
    // The user's current lat/lng (if used for a general search without an institute)
    // would be separate parameters if needed for that use case.

    private Long instituteId;       // To link to an Institute for proximity search
    private String propertyType;    // e.g., "Studio", "S+1", "Apartment"
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private Double radiusKm;        // Search radius in kilometers around the institute or a lat/lng point
    private Integer bedrooms; // Added
    private BigDecimal minArea; // Added
    private BigDecimal maxArea; // Added
    private Long ownerId;      // To filter by property owner

    // Note: page, size, and sort parameters are typically handled by a Pageable object
    // passed directly to the service/repository method from the controller.
    // If we need explicit lat/lng for searching not centered on an institute,
    // we could add them here:
    // private Double searchLatitude;
    // private Double searchLongitude;
    
    public PropertySearchCriteria(Long instituteId, String propertyType, 
                                  BigDecimal minPrice, BigDecimal maxPrice, 
                                  Double radiusKm, Integer bedrooms,
                                  BigDecimal minArea, BigDecimal maxArea) {
        this.instituteId = instituteId;
        this.propertyType = propertyType;
        this.minPrice = minPrice;
        this.maxPrice = maxPrice;
        this.radiusKm = radiusKm;
        this.bedrooms = bedrooms;
        this.minArea = minArea;
        this.maxArea = maxArea;
    }
} 