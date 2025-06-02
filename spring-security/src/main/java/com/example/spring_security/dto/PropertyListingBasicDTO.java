package com.example.spring_security.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Basic property listing information DTO for use in other DTOs
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PropertyListingBasicDTO {
    private Long id;
    private String title;
    private BigDecimal price;
    private String location;
    private String city;
    private String propertyType;
    private Integer bedrooms;
    private Integer bathrooms;
    private List<String> imageUrls;
} 