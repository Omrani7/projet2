package com.example.spring_security.controller;

import com.example.spring_security.dto.UserRoommatePreferencesDTO;
import com.example.spring_security.model.UserPrincipal;
import com.example.spring_security.service.UserRoommatePreferencesService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;


@RestController
@RequestMapping("/api/v1/users/roommate-preferences")
public class UserRoommatePreferencesController {
    
    private static final Logger logger = LoggerFactory.getLogger(UserRoommatePreferencesController.class);
    
    private final UserRoommatePreferencesService preferencesService;
    
    @Autowired
    public UserRoommatePreferencesController(UserRoommatePreferencesService preferencesService) {
        this.preferencesService = preferencesService;
    }
    

    @GetMapping
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<UserRoommatePreferencesDTO> getCurrentUserPreferences(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        
        logger.info("Getting roommate preferences for user {}", currentUser.getId());
        
        UserRoommatePreferencesDTO preferences = preferencesService.getUserPreferences(currentUser.getId());
        
        return ResponseEntity.ok(preferences);
    }
    

    @PutMapping
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<UserRoommatePreferencesDTO> updateCurrentUserPreferences(
            @Valid @RequestBody UserRoommatePreferencesDTO preferencesDTO,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        
        logger.info("Updating roommate preferences for user {}", currentUser.getId());
        
        UserRoommatePreferencesDTO updatedPreferences = preferencesService
                .updateUserPreferences(currentUser.getId(), preferencesDTO);
        
        return ResponseEntity.ok(updatedPreferences);
    }
    

    @GetMapping("/status")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<Map<String, Object>> getPreferencesStatus(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        
        logger.debug("Checking preferences status for user {}", currentUser.getId());
        
        boolean hasPreferences = preferencesService.hasPreferencesSet(currentUser.getId());
        
        Map<String, Object> status = new HashMap<>();
        status.put("hasPreferences", hasPreferences);
        status.put("userId", currentUser.getId());
        
        return ResponseEntity.ok(status);
    }
    

    @DeleteMapping
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<Map<String, String>> deleteCurrentUserPreferences(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        
        logger.info("Deleting roommate preferences for user {}", currentUser.getId());
        
        preferencesService.deleteUserPreferences(currentUser.getId());
        
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Roommate preferences deleted successfully");
        
        return ResponseEntity.ok(response);
    }
} 