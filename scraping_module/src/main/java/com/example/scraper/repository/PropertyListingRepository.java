package com.example.scraper.repository;

import com.example.scraper.model.PropertyListing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PropertyListingRepository extends JpaRepository<PropertyListing, Long> {
    
    List<PropertyListing> findByActiveTrue();
    
    List<PropertyListing> findByActiveTrueAndCityIgnoreCase(String city);
    
    List<PropertyListing> findByActiveTrueAndPriceLessThanEqual(java.math.BigDecimal maxPrice);
    
    @Query("SELECT p FROM PropertyListing p WHERE p.active = true AND " +
           "p.latitude IS NOT NULL AND p.longitude IS NOT NULL")
    List<PropertyListing> findAllWithCoordinates();
} 