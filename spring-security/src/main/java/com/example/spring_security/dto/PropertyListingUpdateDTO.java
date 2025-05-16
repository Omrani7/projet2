package com.example.spring_security.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PropertyListingUpdateDTO {

    @Size(min = 5, max = 200, message = "Title must be between 5 and 200 characters")
    private String title;

    @Size(max = 5000, message = "Description can be up to 5000 characters")
    private String description;

    @DecimalMin(value = "0.0", inclusive = true, message = "Price must be a positive value or zero")
    private BigDecimal price;

    private String propertyType; // Consider using an Enum for predefined types

    @Size(min = 10, max = 255, message = "Full address must be between 10 and 255 characters")
    private String fullAddress; // If provided, implies re-geocoding might be needed

    @Min(value = 0, message = "Rooms cannot be negative")
    private Integer rooms;

    @Min(value = 0, message = "Bedrooms cannot be negative")
    private Integer bedrooms;

    @Min(value = 0, message = "Bathrooms cannot be negative")
    private Integer bathrooms;

    @DecimalMin(value = "0.0", inclusive = false, message = "Area must be a positive value")
    private Double area; // Square meters or other unit

    @Size(max = 255, message = "Contact info can be up to 255 characters")
    private String contactInfo;
    
    @Size(max = 100, message = "City name can be up to 100 characters")
    private String city;

    @Size(max = 100, message = "District name can be up to 100 characters")
    private String district;

    // For image updates, the logic can be more complex (e.g., add/remove specific URLs).
    // For simplicity here, we allow replacing the list. A more granular approach might be needed in a real app.
    private List<String> imageUrls; // If null, images are not updated. If empty list, all images removed.

    private Boolean active; // Allow explicit activation/deactivation

    // Fields like id, owner, listingDate, createdAt, updatedAt, latitude, longitude (directly)
    // are generally not updatable via a DTO like this.
    // Latitude/Longitude are updated via geocoding fullAddress.
} 