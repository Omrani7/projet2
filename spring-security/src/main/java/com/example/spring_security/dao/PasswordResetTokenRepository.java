package com.example.spring_security.dao;

import com.example.spring_security.model.PasswordResetToken;
import com.example.spring_security.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    
    Optional<PasswordResetToken> findByToken(String token);
    
    Optional<PasswordResetToken> findByUser(User user);
    
    @Modifying
    @Query("DELETE FROM PasswordResetToken t WHERE t.expiryDate <= ?1")
    void deleteAllExpiredTokens(Date now);
} 