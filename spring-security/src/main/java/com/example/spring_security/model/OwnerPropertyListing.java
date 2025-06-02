package com.example.spring_security.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "owner_property_listings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OwnerPropertyListing {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private BigDecimal price;

    @Column(nullable = false)
    private String propertyType;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String fullAddress;

    private String city;
    private String district;

    private Double latitude;
    private Double longitude;

    @Column(columnDefinition = "TEXT")
    private String formattedAddress; // Populated by geocoding service

    private Integer rooms;
    private Integer bedrooms;
    private Integer bathrooms;
    private Double area; // e.g., square meters
    private Integer floor; // Floor level (0 for ground floor)
    private Boolean hasBalcony; // Whether the property has a balcony

    // Payment and availability information
    private BigDecimal securityDeposit; // Security deposit amount
    private LocalDate availableFrom; // Date the property is available from
    private LocalDate availableTo; // Date until the property is available (can be null)
    private String paymentFrequency; // monthly, quarterly, yearly
    private Integer minimumStayMonths; // Minimum stay duration in months

    // Amenities as a collection of strings
    @ElementCollection
    @CollectionTable(name = "owner_property_amenities", joinColumns = @JoinColumn(name = "owner_property_listing_id"))
    @Column(name = "amenity")
    private Set<String> amenities = new HashSet<>();

    private String contactInfo; // Optional contact details

    @ElementCollection(fetch = FetchType.EAGER) // Eager fetch for simplicity, consider Lazy for performance with many images
    @CollectionTable(name = "owner_property_image_urls", joinColumns = @JoinColumn(name = "owner_property_listing_id"))
    @Column(name = "image_url", columnDefinition = "TEXT") // Storing relative paths to locally served images
    private List<String> imageUrls = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // The owner

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    private LocalDateTime listingDate; // Date the property was listed or made available
    private boolean active = true; // Is the listing currently active
} 