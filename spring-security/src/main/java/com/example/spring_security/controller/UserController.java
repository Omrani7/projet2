package com.example.spring_security.controller;

import com.example.spring_security.dao.UserProfileRepo;
import com.example.spring_security.dto.UserInfoResponse;
import com.example.spring_security.model.User;
import com.example.spring_security.model.UserProfile;
import com.example.spring_security.model.UserPrincipal;
import com.example.spring_security.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {
    @Autowired
    private UserService userService;
    
    @Autowired
    private UserProfileRepo userProfileRepo;
    
    @GetMapping("/profile")
    public ResponseEntity<?> getUserProfile() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        
        User user = userService.findByUsername(username);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(user);
    }
    
    @GetMapping("/{userId}/profile")
    public ResponseEntity<UserProfile> getUserProfileById(@PathVariable int userId) {
        UserProfile profile = userProfileRepo.findByUserId(userId);
        if (profile == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(profile);
    }
    
    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(@RequestBody UserProfile profileUpdate) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        
        User user = userService.findByUsername(username);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        
        UserProfile existingProfile = user.getUserProfile();
        if (existingProfile == null) {
            existingProfile = new UserProfile();
            existingProfile.setUser(user);
        }
        
        if (profileUpdate.getFullName() != null) {
            existingProfile.setFullName(profileUpdate.getFullName());
        }
        if (profileUpdate.getDateOfBirth() != null) {
            existingProfile.setDateOfBirth(profileUpdate.getDateOfBirth());
        }
        if (profileUpdate.getFieldOfStudy() != null) {
            existingProfile.setFieldOfStudy(profileUpdate.getFieldOfStudy());
        }
        if (profileUpdate.getInstitute() != null) {
            existingProfile.setInstitute(profileUpdate.getInstitute());
        }
        if (profileUpdate.getUserType() != null) {
            existingProfile.setUserType(profileUpdate.getUserType());
        }
        
        UserProfile savedProfile = userProfileRepo.save(existingProfile);
        return ResponseEntity.ok(savedProfile);
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()") // Ensure user is logged in
    public ResponseEntity<UserInfoResponse> getCurrentUser(@AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        
        UserInfoResponse userInfo = UserInfoResponse.builder()
                .id(principal.getId())
                .email(principal.getEmail())
                .username(principal.getUsername())
                .role(principal.getRole())
                .authProvider(principal.getUser().getProvider())
                .enabled(principal.isEnabled())
                .build();
                
        return ResponseEntity.ok(userInfo);
    }
}
