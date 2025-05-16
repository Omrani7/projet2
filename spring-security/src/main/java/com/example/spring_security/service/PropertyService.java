package com.example.spring_security.service;

import com.example.spring_security.dao.PropertyListingRepository;
import com.example.spring_security.model.PropertyListing;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for retrieving property listings with filtering
 */
@Service
@Slf4j
public class PropertyService {

    private final PropertyListingRepository propertyListingRepository;
    
    public PropertyService(PropertyListingRepository propertyListingRepository) {
        this.propertyListingRepository = propertyListingRepository;
    }
    
    /**
     * Get all property listings
     */
    public List<PropertyListing> getAllProperties() {
        return propertyListingRepository.findAll();
    }
    
    /**
     * Get property listings filtered by various criteria
     */
    public List<PropertyListing> getFilteredProperties(
            BigDecimal maxPrice, 
            Integer minRooms,
            String location,
            Double minArea) {
        
        List<PropertyListing> allProperties = propertyListingRepository.findAll();
        
        return allProperties.stream()
            .filter(p -> maxPrice == null || 
                (p.getPrice() != null && p.getPrice().compareTo(maxPrice) <= 0))
            .filter(p -> minRooms == null || 
                (p.getRooms() != null && p.getRooms() >= minRooms))
            .filter(p -> location == null || location.isEmpty() || 
                (p.getLocation() != null && p.getLocation().toLowerCase().contains(location.toLowerCase())))
            .filter(p -> minArea == null || 
                (p.getArea() != null && p.getArea() >= minArea))
            .collect(Collectors.toList());
    }
    
    /**
     * Get properties with geocoding information
     */
    public List<PropertyListing> getGeocodedProperties() {
        List<PropertyListing> allProperties = propertyListingRepository.findAll();
        
        return allProperties.stream()
            .filter(p -> p.getLatitude() != null && p.getLongitude() != null)
            .collect(Collectors.toList());
    }
    
    /**
     * Get a specific property by its ID
     */
    public Optional<PropertyListing> getPropertyById(Long id) {
        return propertyListingRepository.findById(id);
    }
} 