package com.example.spring_security.dao;

import com.example.spring_security.model.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserProfileRepo extends JpaRepository<UserProfile, Integer> {
    UserProfile findByUserId(int userId);
    
    // Add this method to support the Long userId parameter in the service
    default UserProfile findByUserId(Long userId) {
        return findByUserId(userId.intValue());
    }
} 