package com.example.spring_security.dao;

import com.example.spring_security.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDateTime;
import java.util.List;

public interface UserRepo extends JpaRepository<User,Integer>, JpaSpecificationExecutor<User> {
    User findUserByUsername(String username);
    
    User findByEmail(String email);
    
    User findByEmailAndProvider(String email, User.AuthProvider provider);
    
    boolean existsByEmail(String email);
    
    User findByProviderIdAndProvider(String providerId, User.AuthProvider provider);
    
    // Additional methods for ML recommendations
    List<User> findByRoleAndIdNot(User.Role role, Integer id);
    
    // Admin dashboard statistics methods
    long countByEnabled(boolean enabled);
    long countByRole(User.Role role);
    long countByProvider(User.AuthProvider provider);
    long countByUpdatedAtAfter(LocalDateTime date);
    long countByUpdatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
}
