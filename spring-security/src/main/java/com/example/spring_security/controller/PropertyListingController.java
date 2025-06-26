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
        
        String distanceSortDirection = null;
        Pageable modifiedPageable = pageable;
        
        if (pageable.getSort().isSorted()) {
            for (org.springframework.data.domain.Sort.Order order : pageable.getSort()) {
                if ("distance".equals(order.getProperty())) {
                    distanceSortDirection = order.getDirection().toString();
                    
                    List<org.springframework.data.domain.Sort.Order> filteredOrders =
                        pageable.getSort().stream()
                            .filter(o -> !"distance".equals(o.getProperty()))
                            .collect(Collectors.toList());
                    
                    org.springframework.data.domain.Sort newSort;
                    if (filteredOrders.isEmpty()) {
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
        
        PropertySearchCriteria criteria = new PropertySearchCriteria(
                instituteId, propertyType, minPrice, maxPrice, radiusKm,
                bedrooms, minArea, maxArea
        );
        
        if (Boolean.TRUE.equals(mine) && userPrincipal != null) {
            criteria.setOwnerId(Long.valueOf(userPrincipal.getId()));
        }
        
        Page<PropertyListingDTO> results = propertyListingService.searchPropertiesWithDistanceSort(
            criteria, modifiedPageable, distanceSortDirection
        );
        
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

    @GetMapping("/{id}")
    public ResponseEntity<PropertyListingDTO> getPropertyById(@PathVariable Long id) {
        return propertyListingService.getPropertyById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }


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


    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteProperty(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        if (userPrincipal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User currentUser = userService.findById(userPrincipal.getId())
                .orElseThrow(() -> new RuntimeException("Authenticated user not found in database"));
        
        propertyListingService.deleteProperty(id, currentUser);
        
        return ResponseEntity.noContent().build();
    }
    

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
            return ResponseEntity.badRequest().build();
        }

        PropertyListingDTO updatedProperty = propertyListingService.addImagesToProperty(id, images, currentUser);
        return ResponseEntity.ok(updatedProperty);
    }

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

    @PostMapping
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<PropertyListingDTO> createProperty(
            @Valid @RequestBody PropertyListingCreateDTO propertyListingDTO,
            Authentication authentication) {
        PropertyListingDTO createdProperty = propertyListingService.createPropertyByOwner(propertyListingDTO, authentication);
        return new ResponseEntity<>(createdProperty, HttpStatus.CREATED);
    }
    
    @PutMapping("/owner/{id}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<PropertyListingDTO> updateProperty(
            @PathVariable Long id,
            @Valid @RequestBody PropertyListingCreateDTO propertyListingDTO,
            Authentication authentication) {
        PropertyListingDTO updatedProperty = propertyListingService.updatePropertyByOwner(id, propertyListingDTO, authentication);
        return ResponseEntity.ok(updatedProperty);
    }
    
    @DeleteMapping("/owner/{id}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<Void> deleteProperty(
            @PathVariable Long id,
            Authentication authentication) {
        propertyListingService.deletePropertyByOwner(id, authentication);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/owner")

    public ResponseEntity<List<PropertyListingDTO>> getOwnerProperties(Authentication authentication) {
        List<PropertyListingDTO> properties = propertyListingService.getPropertiesByOwner(authentication);
        return ResponseEntity.ok(properties);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<PropertyListingDTO> createPropertyWithImages(
            @RequestParam("listingData") String propertyListingJsonString,
            @RequestParam(value = "images", required = false) List<MultipartFile> images,
            Authentication authentication) {
        
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.registerModule(new JavaTimeModule());
            objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            
            PropertyListingCreateDTO propertyListingDTO = objectMapper.readValue(propertyListingJsonString, PropertyListingCreateDTO.class);
            
            PropertyListingDTO createdProperty = propertyListingService.createPropertyByOwner(propertyListingDTO, authentication);
            
            if (images != null && !images.isEmpty()) {
                createdProperty = propertyListingService.addImagesToProperty(createdProperty.getId(), images, authentication);
            }
            
            return new ResponseEntity<>(createdProperty, HttpStatus.CREATED);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid property data format: " + e.getMessage());
        }
    }

    @PutMapping(path = "/owner/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<PropertyListingDTO> updatePropertyWithImages(
            @PathVariable Long id,
            @RequestParam("listingData") String propertyListingJsonString,
            @RequestParam(value = "images", required = false) List<MultipartFile> images,
            Authentication authentication) {
        
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.registerModule(new JavaTimeModule());
            objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            
            PropertyListingCreateDTO propertyListingDTO = objectMapper.readValue(propertyListingJsonString, PropertyListingCreateDTO.class);
            
            PropertyListingDTO updatedProperty = propertyListingService.updatePropertyByOwner(id, propertyListingDTO, authentication);
            
            if (images != null && !images.isEmpty()) {
                updatedProperty = propertyListingService.addImagesToProperty(id, images, authentication);
            }
            
            return ResponseEntity.ok(updatedProperty);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid property data format: " + e.getMessage());
        }
    }
    

    @GetMapping("/latest-owner")
    public ResponseEntity<List<PropertyListingDTO>> getLatestOwnerProperties(
            @RequestParam(required = false, defaultValue = "6") int limit) {
        
        List<PropertyListingDTO> latestProperties = propertyListingService.getLatestPropertiesBySourceType("OWNER", limit);
        
        if (latestProperties.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        
        return ResponseEntity.ok(latestProperties);
    }


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