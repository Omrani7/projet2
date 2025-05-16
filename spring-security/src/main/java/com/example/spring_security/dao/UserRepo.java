package com.example.spring_security.dao;

import com.example.spring_security.model.User;
import org.springframework.data.jpa.repository.JpaRepository;


public interface UserRepo extends JpaRepository<User,Integer> {
    User findUserByUsername(String username);
    
    User findByEmail(String email);
    
    User findByEmailAndProvider(String email, User.AuthProvider provider);
    
    boolean existsByEmail(String email);
    
    User findByProviderIdAndProvider(String providerId, User.AuthProvider provider);
}
