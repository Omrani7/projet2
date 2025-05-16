package com.example.spring_security.service;

import com.example.spring_security.dao.InstituteRepository;
import com.example.spring_security.model.Institute;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class InstituteService {

    private final InstituteRepository instituteRepository;

    @Autowired
    public InstituteService(InstituteRepository instituteRepository) {
        this.instituteRepository = instituteRepository;
    }

    public List<Institute> getAllInstitutes() {
        return instituteRepository.findAll();
    }

    public List<Institute> searchInstitutesByName(String nameFragment) {
        if (nameFragment == null || nameFragment.trim().isEmpty()) {
            // Return an empty list if the search fragment is null or empty.
            // Alternatively, could return all institutes or a featured subset.
            return Collections.emptyList(); 
        }
        return instituteRepository.findByNameContainingIgnoreCase(nameFragment);
    }
} 