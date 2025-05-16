package com.example.spring_security.dao;

import com.example.spring_security.model.PropertyListing;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface PropertyListingRepository extends JpaRepository<PropertyListing, Long>, JpaSpecificationExecutor<PropertyListing> {
    List<PropertyListing> findByLocationContainingIgnoreCase(String location);
    Page<PropertyListing> findByLocationContainingIgnoreCase(String location, Pageable pageable);
    List<PropertyListing> findBySourceWebsite(String sourceWebsite);
    List<PropertyListing> findByPropertyType(String propertyType);
    List<PropertyListing> findBySourceUrlIn(List<String> sourceUrls);
    
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
} 