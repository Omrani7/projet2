package com.example.spring_security.dao;

import com.example.spring_security.model.OwnerPropertyListing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OwnerPropertyListingRepository extends JpaRepository<OwnerPropertyListing, Long> {
    List<OwnerPropertyListing> findByUserId(Long userId);
} 