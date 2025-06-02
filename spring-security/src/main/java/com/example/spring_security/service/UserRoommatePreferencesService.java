package com.example.spring_security.service;

import com.example.spring_security.dao.UserRoommatePreferencesRepository;
import com.example.spring_security.dao.UserRepo;
import com.example.spring_security.dto.UserRoommatePreferencesDTO;
import com.example.spring_security.exception.ResourceNotFoundException;
import com.example.spring_security.model.User;
import com.example.spring_security.model.UserRoommatePreferences;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Optional;

/**
 * Service for managing user roommate preferences
 * Handles CRUD operations and validation for roommate preferences
 */
@Service
@Transactional(readOnly = true)
public class UserRoommatePreferencesService {
    
    private static final Logger logger = LoggerFactory.getLogger(UserRoommatePreferencesService.class);
    
    private final UserRoommatePreferencesRepository preferencesRepository;
    private final UserRepo userRepository;
    
    @Autowired
    public UserRoommatePreferencesService(UserRoommatePreferencesRepository preferencesRepository,
                                        UserRepo userRepository) {
        this.preferencesRepository = preferencesRepository;
        this.userRepository = userRepository;
    }
    
    /**
     * Get user's roommate preferences
     * Returns empty preferences if none exist
     */
    public UserRoommatePreferencesDTO getUserPreferences(Integer userId) {
        logger.debug("Getting roommate preferences for user {}", userId);
        
        // Verify user exists and is a student
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        
        if (user.getRole() != User.Role.STUDENT) {
            throw new IllegalArgumentException("Only students can have roommate preferences");
        }
        
        Optional<UserRoommatePreferences> preferencesOpt = preferencesRepository.findByUserIdWithUser(userId);
        
        if (preferencesOpt.isPresent()) {
            return convertToDTO(preferencesOpt.get());
        } else {
            // Return empty preferences with default values
            return createEmptyPreferencesDTO(userId);
        }
    }
    
    /**
     * Create or update user's roommate preferences
     */
    @Transactional
    public UserRoommatePreferencesDTO updateUserPreferences(Integer userId, UserRoommatePreferencesDTO preferencesDTO) {
        logger.info("Updating roommate preferences for user {}", userId);
        
        // Verify user exists and is a student
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        
        if (user.getRole() != User.Role.STUDENT) {
            throw new IllegalArgumentException("Only students can have roommate preferences");
        }
        
        // Find existing preferences or create new
        Optional<UserRoommatePreferences> existingOpt = preferencesRepository.findByUserIdWithUser(userId);
        
        UserRoommatePreferences preferences;
        if (existingOpt.isPresent()) {
            preferences = existingOpt.get();
            logger.debug("Updating existing preferences for user {}", userId);
        } else {
            // Create new preferences entity
            // With @MapsId, we should NOT set userId manually - it's derived from the User entity
            preferences = new UserRoommatePreferences();
            preferences.setUser(user);
            preferences.setActiveForMatching(true);
            logger.debug("Creating new preferences for user {}", userId);
        }
        
        // Update preferences from DTO
        updatePreferencesFromDTO(preferences, preferencesDTO);
        
        // Save preferences
        UserRoommatePreferences savedPreferences = preferencesRepository.save(preferences);
        
        logger.info("Successfully updated roommate preferences for user {}", userId);
        return convertToDTO(savedPreferences);
    }
    
    /**
     * Check if user has preferences set
     */
    public boolean hasPreferencesSet(Integer userId) {
        return preferencesRepository.hasPreferencesSet(userId);
    }
    
    /**
     * Delete user's roommate preferences
     */
    @Transactional
    public void deleteUserPreferences(Integer userId) {
        logger.info("Deleting roommate preferences for user {}", userId);
        
        Optional<UserRoommatePreferences> preferencesOpt = preferencesRepository.findByUserIdWithUser(userId);
        
        if (preferencesOpt.isPresent()) {
            preferencesRepository.delete(preferencesOpt.get());
            logger.info("Successfully deleted roommate preferences for user {}", userId);
        } else {
            logger.debug("No preferences found to delete for user {}", userId);
        }
    }
    
    /**
     * Convert entity to DTO
     */
    private UserRoommatePreferencesDTO convertToDTO(UserRoommatePreferences preferences) {
        return UserRoommatePreferencesDTO.builder()
                .userId(preferences.getUserId())
                .lifestyleTags(preferences.getLifestyleTags())
                .cleanlinessLevel(preferences.getCleanlinessLevel())
                .socialLevel(preferences.getSocialLevel())
                .studyHabits(preferences.getStudyHabits())
                .budgetMin(preferences.getBudgetMin())
                .budgetMax(preferences.getBudgetMax())
                .additionalPreferences(preferences.getAdditionalNotes())
                .updatedAt(preferences.getUpdatedAt())
                // Helper fields
                .isComplete(isPreferencesComplete(preferences))
                .build();
    }
    
    /**
     * Create empty preferences DTO with default values
     */
    private UserRoommatePreferencesDTO createEmptyPreferencesDTO(Integer userId) {
        return UserRoommatePreferencesDTO.builder()
                .userId(userId)
                .lifestyleTags(new HashSet<>())
                .studyHabits(new HashSet<>())
                .isComplete(false)
                .build();
    }
    
    /**
     * Update preferences entity from DTO
     */
    private void updatePreferencesFromDTO(UserRoommatePreferences preferences, UserRoommatePreferencesDTO dto) {
        // Lifestyle and study preferences
        if (dto.getLifestyleTags() != null) {
            preferences.setLifestyleTags(new HashSet<>(dto.getLifestyleTags()));
        }
        if (dto.getStudyHabits() != null) {
            preferences.setStudyHabits(new HashSet<>(dto.getStudyHabits()));
        }
        
        // Numeric preferences
        preferences.setCleanlinessLevel(dto.getCleanlinessLevel());
        preferences.setSocialLevel(dto.getSocialLevel());
        
        // Budget preferences
        preferences.setBudgetMin(dto.getBudgetMin());
        preferences.setBudgetMax(dto.getBudgetMax());
        
        // Additional notes
        preferences.setAdditionalNotes(dto.getAdditionalPreferences());
        
        // Always active for matching when updated
        preferences.setActiveForMatching(true);
    }
    
    /**
     * Check if preferences are complete enough for ML matching
     */
    private boolean isPreferencesComplete(UserRoommatePreferences preferences) {
        return preferences.getCleanlinessLevel() != null &&
               preferences.getSocialLevel() != null &&
               preferences.getBudgetMin() != null &&
               preferences.getBudgetMax() != null &&
               preferences.getLifestyleTags() != null && !preferences.getLifestyleTags().isEmpty();
    }
} 