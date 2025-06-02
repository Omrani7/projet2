package com.example.spring_security.controller;

import com.example.spring_security.dto.PropertySearchCriteria;
import com.example.spring_security.dto.PropertyListingDTO;
import com.example.spring_security.dto.PropertyListingCreateDTO;
import com.example.spring_security.dto.PropertyListingUpdateDTO;
import com.example.spring_security.model.User;
import com.example.spring_security.model.UserPrincipal;
import com.example.spring_security.service.PropertyListingService;
import com.example.spring_security.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.web.server.ResponseStatusException;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/properties")
public class PropertyListingController {
    
    private final PropertyListingService propertyListingService;
    private final UserService userService;
    private static final Logger logger = LoggerFactory.getLogger(PropertyListingController.class);
    
    @Autowired
    public PropertyListingController(PropertyListingService propertyListingService, 
                                       UserService userService) {
        this.propertyListingService = propertyListingService;
        this.userService = userService;
    }
    
    /**
     * Searches for property listings based on various criteria.
     *
     * Example: GET /api/v1/properties/search?propertyType=APARTMENT&minPrice=500&maxPrice=1000&instituteId=1&radiusKm=5&bedrooms=2&minArea=50&maxArea=100&page=0&size=10&sort=price,asc
     *
     * @param genericQuery Optional generic query for general search terms
     * @param instituteId Optional ID of the institute for proximity search.
     * @param propertyType Optional type of the property.
     * @param minPrice Optional minimum price.
     * @param maxPrice Optional maximum price.
     * @param radiusKm Optional search radius in kilometers (used with instituteId or lat/lon).
     * @param bedrooms Optional exact number of bedrooms.
     * @param minArea Optional minimum area (e.g., square meters).
     * @param maxArea Optional maximum area (e.g., square meters).
     * @param mine Optional filter to include only properties owned by the authenticated user
     * @param userPrincipal The principal of the authenticated user
     * @param pageable Pagination and sorting information (e.g., page, size, sort).
     * @return A page of matching property listings as DTOs.
     */
    @GetMapping("/search")
    public ResponseEntity<Page<PropertyListingDTO>> searchProperties(
            @RequestParam(required = false) String genericQuery,
            @RequestParam(required = false) Long instituteId,
            @RequestParam(required = false) String propertyType,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Double radiusKm,
            @RequestParam(required = false) Integer bedrooms,
            @RequestParam(required = false) BigDecimal minArea,
            @RequestParam(required = false) BigDecimal maxArea,
            @RequestParam(required = false) Boolean mine,
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            Pageable pageable) {
        
        // Extract sort direction for distance if it exists
        String distanceSortDirection = null;
        Pageable modifiedPageable = pageable;
        
        if (pageable.getSort().isSorted()) {
            for (org.springframework.data.domain.Sort.Order order : pageable.getSort()) {
                if ("distance".equals(order.getProperty())) {
                    // Store the sort direction for distance
                    distanceSortDirection = order.getDirection().toString();
                    
                    // Create a new pageable without the distance sort
                    List<org.springframework.data.domain.Sort.Order> filteredOrders = 
                        pageable.getSort().stream()
                            .filter(o -> !"distance".equals(o.getProperty()))
                            .collect(Collectors.toList());
                    
                    org.springframework.data.domain.Sort newSort;
                    if (filteredOrders.isEmpty()) {
                        // If no other sort properties remain, default to listingDate DESC
                        newSort = org.springframework.data.domain.Sort.by(
                            org.springframework.data.domain.Sort.Direction.DESC, "listingDate"
                        );
                    } else {
                        newSort = org.springframework.data.domain.Sort.by(filteredOrders);
                    }
                    
                    modifiedPageable = org.springframework.data.domain.PageRequest.of(
                        pageable.getPageNumber(),
                        pageable.getPageSize(),
                        newSort
                    );
                    break;
                }
            }
        }
        
        // Create search criteria
        PropertySearchCriteria criteria = new PropertySearchCriteria(
                instituteId, propertyType, minPrice, maxPrice, radiusKm,
                bedrooms, minArea, maxArea
        );
        
        // If 'mine' is true, add the owner ID to the search criteria
        if (Boolean.TRUE.equals(mine) && userPrincipal != null) {
            criteria.setOwnerId(Long.valueOf(userPrincipal.getId()));
        }
        
        // Call service with the modified pageable and distance sort direction
        Page<PropertyListingDTO> results = propertyListingService.searchPropertiesWithDistanceSort(
            criteria, modifiedPageable, distanceSortDirection
        );
        
        // Add debug logging to track source types
        long ownerCount = results.getContent().stream()
            .filter(p -> "OWNER".equals(p.getSourceType()))
            .count();
        long scrapedCount = results.getContent().stream()
            .filter(p -> "SCRAPED".equals(p.getSourceType()))
            .count();
        System.out.println("Search results: Total=" + results.getTotalElements() + 
                         ", OWNER=" + ownerCount + ", SCRAPED=" + scrapedCount);
        
        return ResponseEntity.ok(results);
    }
    
    /**
     * Gets a specific property listing by its ID.
     * @param id The ID of the property listing.
     * @return The property listing DTO if found, otherwise 404 Not Found.
     */
    @GetMapping("/{id}")
    public ResponseEntity<PropertyListingDTO> getPropertyById(@PathVariable Long id) {
        return propertyListingService.getPropertyById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Creates a new property listing.
     * Requires the user to be authenticated.
     * @param createDto The DTO containing information for the new property.
     * @param userPrincipal The principal of the authenticated user.
     * @return The created property listing DTO with HTTP status 201 (Created).
     */
    @PostMapping("/scraped")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PropertyListingDTO> createProperty(
            @Valid @RequestBody PropertyListingCreateDTO createDto,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        if (userPrincipal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        User currentUser = userService.findById(userPrincipal.getId())
                .orElseThrow(() -> new RuntimeException("Authenticated user not found in database"));

        PropertyListingDTO createdProperty = propertyListingService.createProperty(createDto, currentUser);
        
        URI location = URI.create(String.format("/api/v1/properties/%s", createdProperty.getId()));
        
        return ResponseEntity.created(location).body(createdProperty);
    }

    /**
     * Updates an existing property listing.
     * Requires the user to be authenticated. Authorization (owner or admin) is handled in the service layer.
     * @param id The ID of the property to update.
     * @param updateDto The DTO containing updated information for the property.
     * @param userPrincipal The principal of the authenticated user.
     * @return The updated property listing DTO.
     */
    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()") // Ensures only authenticated users can attempt to update
    public ResponseEntity<PropertyListingDTO> updateProperty(
            @PathVariable Long id,
            @Valid @RequestBody PropertyListingUpdateDTO updateDto,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        if (userPrincipal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User currentUser = userService.findById(userPrincipal.getId())
                .orElseThrow(() -> new RuntimeException("Authenticated user not found in database"));

        PropertyListingDTO updatedProperty = propertyListingService.updateProperty(id, updateDto, currentUser);
        return ResponseEntity.ok(updatedProperty);
    }

    /**
     * Deletes a property listing by its ID.
     * Requires the user to be authenticated. Authorization (owner or admin) is handled in the service layer.
     * @param id The ID of the property to delete.
     * @param userPrincipal The principal of the authenticated user.
     * @return HTTP status 204 (No Content) if successful.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()") // Ensures only authenticated users can attempt to delete
    public ResponseEntity<Void> deleteProperty(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        if (userPrincipal == null) {
            // Should be caught by @PreAuthorize, but as a safeguard
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User currentUser = userService.findById(userPrincipal.getId())
                .orElseThrow(() -> new RuntimeException("Authenticated user not found in database"));
        
        propertyListingService.deleteProperty(id, currentUser);
        
        return ResponseEntity.noContent().build(); // HTTP 204 No Content
    }
    
    /**
     * Uploads images for a specific property listing.
     * Requires the user to be authenticated and be the owner of the property or an admin.
     * @param id The ID of the property to upload images for.
     * @param images The list of image files to upload.
     * @param userPrincipal The principal of the authenticated user.
     * @return The updated property listing DTO with new image URLs.
     */
    @PostMapping("/scraped/{id}/images")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PropertyListingDTO> uploadPropertyImages(
            @PathVariable Long id,
            @RequestParam("images") List<MultipartFile> images,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        if (userPrincipal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        User currentUser = userService.findById(userPrincipal.getId())
                .orElseThrow(() -> new RuntimeException("Authenticated user not found in database"));

        if (images == null || images.isEmpty() || images.stream().allMatch(MultipartFile::isEmpty)) {
            return ResponseEntity.badRequest().build(); // Or handle as no-op success
        }

        PropertyListingDTO updatedProperty = propertyListingService.addImagesToProperty(id, images, currentUser);
        return ResponseEntity.ok(updatedProperty);
    }

    // Endpoint to get owner-specific listings for their profile
    @GetMapping("/mine")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<PropertyListingDTO>> getMyProperties(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        if (userPrincipal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        List<PropertyListingDTO> properties = propertyListingService.getPropertiesByOwnerId(userPrincipal.getId());
        if (properties.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(properties);
    }

    // New endpoint for owner property creation
    @PostMapping
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<PropertyListingDTO> createProperty(
            @Valid @RequestBody PropertyListingCreateDTO propertyListingDTO,
            Authentication authentication) {
        PropertyListingDTO createdProperty = propertyListingService.createPropertyByOwner(propertyListingDTO, authentication);
        return new ResponseEntity<>(createdProperty, HttpStatus.CREATED);
    }
    
    // Update property endpoint
    @PutMapping("/owner/{id}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<PropertyListingDTO> updateProperty(
            @PathVariable Long id,
            @Valid @RequestBody PropertyListingCreateDTO propertyListingDTO,
            Authentication authentication) {
        PropertyListingDTO updatedProperty = propertyListingService.updatePropertyByOwner(id, propertyListingDTO, authentication);
        return ResponseEntity.ok(updatedProperty);
    }
    
    // Delete property endpoint
    @DeleteMapping("/owner/{id}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<Void> deleteProperty(
            @PathVariable Long id,
            Authentication authentication) {
        propertyListingService.deletePropertyByOwner(id, authentication);
        return ResponseEntity.noContent().build();
    }
    
    // Get owner properties endpoint
    @GetMapping("/owner")
    // Temporarily remove PreAuthorize for testing
    // @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<List<PropertyListingDTO>> getOwnerProperties(Authentication authentication) {
        List<PropertyListingDTO> properties = propertyListingService.getPropertiesByOwner(authentication);
        return ResponseEntity.ok(properties);
    }

    // Endpoint for owner property creation WITH images (multipart form data)
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<PropertyListingDTO> createPropertyWithImages(
            @RequestParam("listingData") String propertyListingJsonString,
            @RequestParam(value = "images", required = false) List<MultipartFile> images,
            Authentication authentication) {
        
        try {
            // Configure ObjectMapper for proper date handling
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.registerModule(new JavaTimeModule());
            objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            
            PropertyListingCreateDTO propertyListingDTO = objectMapper.readValue(propertyListingJsonString, PropertyListingCreateDTO.class);
            
            // Create property first
            PropertyListingDTO createdProperty = propertyListingService.createPropertyByOwner(propertyListingDTO, authentication);
            
            // Then add images if provided
            if (images != null && !images.isEmpty()) {
                createdProperty = propertyListingService.addImagesToProperty(createdProperty.getId(), images, authentication);
            }
            
            return new ResponseEntity<>(createdProperty, HttpStatus.CREATED);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid property data format: " + e.getMessage());
        }
    }

    // Endpoint for owner property update WITH images (multipart form data)
    @PutMapping(path = "/owner/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<PropertyListingDTO> updatePropertyWithImages(
            @PathVariable Long id,
            @RequestParam("listingData") String propertyListingJsonString,
            @RequestParam(value = "images", required = false) List<MultipartFile> images,
            Authentication authentication) {
        
        try {
            // Configure ObjectMapper for proper date handling
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.registerModule(new JavaTimeModule());
            objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            
            PropertyListingCreateDTO propertyListingDTO = objectMapper.readValue(propertyListingJsonString, PropertyListingCreateDTO.class);
            
            // Update property first
            PropertyListingDTO updatedProperty = propertyListingService.updatePropertyByOwner(id, propertyListingDTO, authentication);
            
            // Then add images if provided
            if (images != null && !images.isEmpty()) {
                updatedProperty = propertyListingService.addImagesToProperty(id, images, authentication);
            }
            
            return ResponseEntity.ok(updatedProperty);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid property data format: " + e.getMessage());
        }
    }
    
    /**
     * Gets the latest properties posted by owners, sorted by listing date.
     * This endpoint is public and doesn't require authentication.
     * 
     * @param limit Optional parameter to limit the number of results (default is 6)
     * @return List of the latest owner property listings
     */
    @GetMapping("/latest-owner")
    public ResponseEntity<List<PropertyListingDTO>> getLatestOwnerProperties(
            @RequestParam(required = false, defaultValue = "6") int limit) {
        
        List<PropertyListingDTO> latestProperties = propertyListingService.getLatestPropertiesBySourceType("OWNER", limit);
        
        if (latestProperties.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        
        return ResponseEntity.ok(latestProperties);
    }

    /**
     * Gets properties for a specific city.
     * This endpoint returns properties based on the city name, with case-insensitive matching.
     * 
     * @param city The name of the city to filter by
     * @param limit The maximum number of properties to return (default: 10)
     * @return A list of property listings in the specified city
     */
    @GetMapping("/by-city")
    public ResponseEntity<List<PropertyListingDTO>> getPropertiesByCity(
            @RequestParam String city,
            @RequestParam(required = false, defaultValue = "10") int limit) {
        if (city == null || city.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        List<PropertyListingDTO> cityProperties = propertyListingService.getPropertiesByCity(city, limit);
        return ResponseEntity.ok(cityProperties);
    }

    /**
     * Archive a property listing (mark as inactive)
     * @param propertyId the ID of the property to archive
     * @param userPrincipal the authenticated user principal
     * @return the updated property listing DTO
     */
    @PutMapping("/{propertyId}/archive")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<PropertyListingDTO> archiveProperty(
            @PathVariable Long propertyId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        logger.info("Archiving property {} by owner {}", propertyId, userPrincipal.getId());
        
        PropertyListingDTO archivedProperty = propertyListingService.archiveProperty(propertyId, userPrincipal);
        
        return ResponseEntity.ok(archivedProperty);
    }
} 