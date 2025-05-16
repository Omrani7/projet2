package com.example.spring_security.controller;

import com.example.spring_security.dto.PropertySearchCriteria;
import com.example.spring_security.dto.PropertyListingDTO;
import com.example.spring_security.dto.PropertyListingCreateDTO;
import com.example.spring_security.dto.PropertyListingUpdateDTO;
import com.example.spring_security.model.PropertyListing;
import com.example.spring_security.model.User;
import com.example.spring_security.model.UserPrincipal;
import com.example.spring_security.service.PropertyListingService;
import com.example.spring_security.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.net.URI;

@RestController
@RequestMapping("/api/v1/properties")
public class PropertyListingController {
    
    private final PropertyListingService propertyListingService;
    private final UserService userService;
    
    @Autowired
    public PropertyListingController(PropertyListingService propertyListingService, UserService userService) {
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
            Pageable pageable) {
        // For now, we're ignoring genericQuery since it isn't part of our search criteria
        // In a future enhancement, you could add it to PropertySearchCriteria and use it to
        // search across multiple fields like title, description, address, etc.
        PropertySearchCriteria criteria = new PropertySearchCriteria(
                instituteId, propertyType, minPrice, maxPrice, radiusKm,
                bedrooms, minArea, maxArea
        );
        Page<PropertyListingDTO> results = propertyListingService.searchProperties(criteria, pageable);
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
    @PostMapping
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

    // TODO: Implement DELETE /api/v1/properties/{id} for deletion (checking ownership or admin role)

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
} 