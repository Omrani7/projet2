package com.example.spring_security.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "property_listings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PropertyListing {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(columnDefinition = "TEXT")
    private String title;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    private BigDecimal price;
    private String location;
    private Double area;
    private String propertyType;
    private String contactInfo;
    
    private String city;
    private String district;
    
    @Column(columnDefinition = "TEXT")
    private String fullAddress;
    
    private Double latitude;
    private Double longitude;
    
    @Column(columnDefinition = "TEXT")
    private String formattedAddress;
    
    private Integer rooms;
    private Integer bedrooms;
    private Integer bathrooms;
    
    @ElementCollection
    @CollectionTable(name = "property_listing_image_urls", joinColumns = @JoinColumn(name = "property_listing_id"))
    @Column(name = "image_urls", columnDefinition = "TEXT")
    private List<String> imageUrls = new ArrayList<>();
    
    @Column(columnDefinition = "TEXT")
    private String sourceUrl;
    
    private String sourceWebsite;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = true)
    private User user;
    
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    private LocalDateTime updatedAt;
    
    private LocalDateTime listingDate;
    private boolean active = true;
} 