package com.example.spring_security.service;

import com.example.spring_security.dto.OwnerProfileDTO;
import com.example.spring_security.model.User;
import com.example.spring_security.model.UserProfile;
import com.example.spring_security.dao.UserProfileRepo;
import com.example.spring_security.dao.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityNotFoundException;

@Service
public class OwnerProfileService {

    private final UserProfileRepo userProfileRepo;
    private final UserRepo userRepo; // To verify user exists and is an owner

    @Autowired
    public OwnerProfileService(UserProfileRepo userProfileRepo, UserRepo userRepo) {
        this.userProfileRepo = userProfileRepo;
        this.userRepo = userRepo;
    }

    @Transactional(readOnly = true)
    public OwnerProfileDTO getOwnerProfileByUserId(Long userId) {
        User user = userRepo.findById(userId.intValue())
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));

        // Ensure the user is an OWNER or ADMIN (if ADMINs can manage owner profiles)
        if (!(user.getRole() == User.Role.OWNER || user.getRole() == User.Role.ADMIN)) {
            throw new org.springframework.security.access.AccessDeniedException("User is not an owner or admin.");
        }

        UserProfile userProfile = userProfileRepo.findByUserId(userId.intValue());
        if (userProfile == null) {
            throw new EntityNotFoundException("Owner profile not found for user id: " + userId);
        }
        
        // Optional: Double check userType if your UserProfile entity has it
        if (userProfile.getUserType() != UserProfile.UserType.OWNER) {
            throw new IllegalStateException("Profile found is not an OWNER profile.");
        }

        return convertToDto(userProfile);
    }

    @Transactional
    public OwnerProfileDTO updateOwnerProfile(Long userId, OwnerProfileDTO ownerProfileDto) {
        User user = userRepo.findById(userId.intValue())
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));

        if (!(user.getRole() == User.Role.OWNER || user.getRole() == User.Role.ADMIN)) {
             throw new org.springframework.security.access.AccessDeniedException("User is not an owner or admin, or not authorized to update this profile.");
        }

        UserProfile userProfile = userProfileRepo.findByUserId(userId.intValue());
        if (userProfile == null) {
            userProfile = new UserProfile();
            userProfile.setUser(user);
            userProfile.setUserType(UserProfile.UserType.OWNER); // Ensure type is set if creating new
        }
        
        // Ensure profile belongs to the user and is of type OWNER if it already exists
        if (userProfile.getId() != 0 && (userProfile.getUserType() != UserProfile.UserType.OWNER || userProfile.getUser().getId() != userId.intValue())) {
            throw new IllegalStateException("Profile mismatch or not an owner profile.");
        }

        // Update fields
        userProfile.setFullName(ownerProfileDto.getFullName());
        userProfile.setContactNumber(ownerProfileDto.getContactNumber());
        userProfile.setState(ownerProfileDto.getState());
        userProfile.setAccommodationType(ownerProfileDto.getAccommodationType());
        userProfile.setPropertyManagementSystem(ownerProfileDto.getPropertyManagementSystem());
        userProfile.setAdditionalInformation(ownerProfileDto.getAdditionalInformation());
        userProfile.setIsAgency(ownerProfileDto.getIsAgency());
        userProfile.setUserType(UserProfile.UserType.OWNER); // Explicitly set/confirm user type

        UserProfile updatedProfile = userProfileRepo.save(userProfile);
        return convertToDto(updatedProfile);
    }

    private OwnerProfileDTO convertToDto(UserProfile userProfile) {
        OwnerProfileDTO dto = new OwnerProfileDTO();
        dto.setId((long)userProfile.getId());
        dto.setUserId((long)userProfile.getUser().getId());
        dto.setFullName(userProfile.getFullName());
        dto.setContactNumber(userProfile.getContactNumber());
        dto.setState(userProfile.getState());
        dto.setAccommodationType(userProfile.getAccommodationType());
        dto.setPropertyManagementSystem(userProfile.getPropertyManagementSystem());
        dto.setAdditionalInformation(userProfile.getAdditionalInformation());
        dto.setIsAgency(userProfile.getIsAgency());
        return dto;
    }
} 