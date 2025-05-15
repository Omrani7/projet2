package com.example.scraper.model;

import lombok.Data;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Data
@ToString
public class TayaraProperty {
    private String id;                    // Extracted from URL
    private String url;                   // Full detail page URL
    private String title;
    private String price;                 // Raw price string, e.g., "140 DT"
    private String contactPhone;
    private String description;
    private Integer rooms;                // Total number of rooms (e.g., S+1 -> 2 rooms)
    private Integer bedrooms;             // Number of bedrooms (e.g., S+1 -> 1 bedroom)
    private String fullAddress;           // Formatted for geocoding
    private String mainImageUrl;
    private List<String> imageUrls = new ArrayList<>();
    private String sourceWebsite = "tayara.tn";

    // Potentially add these later if needed and available
    private String locationCity;
    private String locationDelegation; 
    // private String propertyType; // e.g., "Appartements"
    // private String transactionType; // e.g., "À Louer"
    // private String datePosted;
    // private String sellerName;
    // private String sellerType; // "Boutique" or individual
    private Double latitude;
    private Double longitude;
    // private String formattedAddressFromGeoApi; // Address confirmed by geocoding

} 