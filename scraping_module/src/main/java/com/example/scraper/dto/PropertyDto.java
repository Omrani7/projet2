package com.example.scraper.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PropertyDto {
    private String id;
    private String title;
    private String description;
    private String price;
    private String location;
    private String area;
    private String propertyType;
    private String contactInfo;  // Will store phone number
    
    private String city;
    private String district;
    private String fullAddress;
    private Double latitude;
    private Double longitude;
    private String formattedAddress;
    
    private Integer rooms;
    private Integer bedrooms;
    private Integer bathrooms;
    
    private List<String> imageUrls = new ArrayList<>();
    
    private String sourceUrl;
    private String sourceWebsite;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime listingDate;
} 