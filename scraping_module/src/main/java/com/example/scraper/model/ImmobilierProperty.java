package com.example.scraper.model;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

/**
 * Data model representing a property listing from immobilier.com.tn
 */
@Data
public class ImmobilierProperty {
    private String id;
    private String title;
    private String url;
    private String price;
    private String type;
    private String transactionType;
    
    // Location details
    private String location;
    private String city;
    private String district;
    private String fullAddress;
    private Double latitude;
    private Double longitude;
    private String formattedAddress;
    
    // Property characteristics
    private String surface;
    private Integer rooms;
    private Integer bedrooms;
    private Integer bathrooms;
    
    // Additional details
    private String description;
    private List<String> amenities = new ArrayList<>();
    private String contactPhone;
    private String publishedDate;
    private String sourceWebsite;
    
    // Images
    private String mainImageUrl;
    private List<String> imageUrls = new ArrayList<>();
    
    @Override
    public String toString() {
        return "ImmobilierProperty{" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", price='" + price + '\'' +
                ", type='" + type + '\'' +
                ", location='" + location + '\'' +
                ", surface='" + surface + '\'' +
                ", rooms=" + rooms +
                ", contactPhone='" + contactPhone + '\'' +
                ", images=" + (imageUrls != null ? imageUrls.size() : 0) +
                '}';
    }
} 