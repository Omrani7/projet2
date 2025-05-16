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
public class PropertyListingCreateDTO {

    @NotBlank(message = "Title is mandatory")
    @Size(min = 5, max = 200, message = "Title must be between 5 and 200 characters")
    private String title;

    @Size(max = 5000, message = "Description can be up to 5000 characters")
    private String description;

    @NotNull(message = "Price is mandatory")
    @DecimalMin(value = "0.0", inclusive = true, message = "Price must be a positive value or zero") // Allow free items
    private BigDecimal price;

    @NotBlank(message = "Property type is mandatory")
    private String propertyType; // Consider using an Enum for predefined types

    @NotBlank(message = "Full address is mandatory for geocoding")
    @Size(min = 10, max = 255, message = "Full address must be between 10 and 255 characters")
    private String fullAddress; // Used for geocoding to get latitude/longitude

    // Optional fields, but good to have placeholders
    @Min(value = 0, message = "Rooms cannot be negative")
    private Integer rooms;

    @Min(value = 0, message = "Bedrooms cannot be negative")
    private Integer bedrooms;

    @Min(value = 0, message = "Bathrooms cannot be negative")
    private Integer bathrooms;

    @DecimalMin(value = "0.0", inclusive = false, message = "Area must be a positive value")
    private Double area; // Square meters or other unit

    // Contact info can be a simple string or a more structured object if needed
    @Size(max = 255, message = "Contact info can be up to 255 characters")
    private String contactInfo;
    
    // Location details that might be explicitly provided by the user
    // City and District might also be derived from fullAddress post-geocoding if needed
    @Size(max = 100, message = "City name can be up to 100 characters")
    private String city;

    @Size(max = 100, message = "District name can be up to 100 characters")
    private String district;

    // List of image URLs provided by the user during creation
    private List<String> imageUrls = new ArrayList<>();

    // active status, listingDate, user (owner) will be set by the backend.
    // latitude, longitude will be set by geocoding fullAddress.
    // formattedAddress could also be set by geocoding service.
} 