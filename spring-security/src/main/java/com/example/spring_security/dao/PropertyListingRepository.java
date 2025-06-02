package com.example.spring_security.dao;

import com.example.spring_security.model.PropertyListing;
import com.example.spring_security.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PropertyListingRepository extends JpaRepository<PropertyListing, Long>, JpaSpecificationExecutor<PropertyListing> {
    List<PropertyListing> findByLocationContainingIgnoreCase(String location);
    Page<PropertyListing> findByLocationContainingIgnoreCase(String location, Pageable pageable);
    List<PropertyListing> findBySourceWebsite(String sourceWebsite);
    List<PropertyListing> findByPropertyType(String propertyType);
    List<PropertyListing> findBySourceUrlIn(List<String> sourceUrls);
    
    // Find properties by the ID of the associated User entity
    List<PropertyListing> findByUserId(Integer userId);
    
    @Modifying
    @Transactional
    @Query("DELETE FROM PropertyListing p WHERE p.sourceWebsite = :website")
    void deleteAllBySourceWebsite(@Param("website") String website);
    
    @Query("SELECT p FROM PropertyListing p WHERE " +
           "p.location LIKE %:query% OR " +
           "p.title LIKE %:query% OR " +
           "p.description LIKE %:query% OR " +
           "p.propertyType LIKE %:query%")
    Page<PropertyListing> search(@Param("query") String query, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"imageUrls"})
    Page<PropertyListing> findAll(Specification<PropertyListing> spec, Pageable pageable);

    @Query("SELECT p FROM PropertyListing p WHERE " +
           "(:city IS NULL OR p.city = :city) AND " +
           "(:district IS NULL OR p.district = :district) AND " +
           "(:minPrice IS NULL OR p.price >= :minPrice) AND " +
           "(:maxPrice IS NULL OR p.price <= :maxPrice) AND " +
           "(:minRooms IS NULL OR p.rooms >= :minRooms) AND " +
           "(:propertyType IS NULL OR p.propertyType = :propertyType)")
    Page<PropertyListing> findByCriteriaAdvanced(
            @Param("city") String city,
            @Param("district") String district,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("minRooms") Integer minRooms,
            @Param("propertyType") String propertyType,
            Pageable pageable
    );
    
    List<PropertyListing> findByUser(User user);
    
    // Find properties by user ID and source type
    List<PropertyListing> findByUserIdAndSourceType(Integer userId, PropertyListing.SourceType sourceType);
    
    // Find active properties by source type with pagination and sorting
    Page<PropertyListing> findBySourceTypeAndActiveTrue(PropertyListing.SourceType sourceType, Pageable pageable);
    
    // Find active properties by user with pagination and sorting
    List<PropertyListing> findByUserAndActiveTrue(User user, Pageable pageable);
    
    // Find active properties by city (case insensitive) with pagination
    @Query("SELECT p FROM PropertyListing p WHERE " +
           "LOWER(p.city) = LOWER(:city) AND p.active = true " +
           "ORDER BY p.listingDate DESC")
    List<PropertyListing> findByCityIgnoreCaseAndActiveTrue(@Param("city") String city, Pageable pageable);
    
    // Find active properties by location containing city name (case insensitive) with pagination
    @Query("SELECT p FROM PropertyListing p WHERE " +
           "(LOWER(p.city) = LOWER(:city) OR LOWER(p.location) LIKE LOWER(CONCAT('%', :city, '%'))) " +
           "AND p.active = true " +
           "ORDER BY p.listingDate DESC")
    List<PropertyListing> findByCityOrLocationContainingIgnoreCaseAndActiveTrue(@Param("city") String city, Pageable pageable);
    
    // Admin dashboard statistics methods
    long countByActiveTrue();
    long countByActiveFalse();
    long countByUserId(Integer userId);
    long countByCreatedAtAfter(LocalDateTime date);
    long countByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
} 