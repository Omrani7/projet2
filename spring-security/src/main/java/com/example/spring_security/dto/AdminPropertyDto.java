package com.example.spring_security.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminPropertyDto {
    private Long id;
    private String title;
    private String description;
    private String location;
    private BigDecimal price;
    private Integer rooms;
    private Integer bathrooms;
    private Double area;
    private String propertyType;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Owner information (from User entity)
    private Integer userId;
    private String ownerUsername;
    private String ownerEmail;
    
    // Activity metrics
    private Long totalInquiries;
    private Long viewCount;
    
    // Images
    private List<String> images;
} 