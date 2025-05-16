package com.example.spring_security.dao;

import com.example.spring_security.model.Institute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InstituteRepository extends JpaRepository<Institute, Long> {
    // Optional: Custom query to find by name, useful for autocomplete or checking existence
    Optional<Institute> findByName(String name);

    // Optional: Custom query for autocomplete search (case-insensitive)
    List<Institute> findByNameContainingIgnoreCase(String nameFragment);
} 