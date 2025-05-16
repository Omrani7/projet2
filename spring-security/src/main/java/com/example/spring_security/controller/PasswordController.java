package com.example.spring_security.controller;

import com.example.spring_security.dto.ForgotPasswordRequest;
import com.example.spring_security.dto.PasswordResetRequest;
import com.example.spring_security.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth/password")
public class PasswordController {
    
    private final UserService userService;
    
    public PasswordController(UserService userService) {
        this.userService = userService;
    }
    
    @PostMapping("/forgot")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        try {
            userService.sendPasswordResetEmail(request.getEmail());
            

            Map<String, String> response = new HashMap<>();
            response.put("message", "If an account exists with that email, we have sent password reset instructions.");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "Failed to process request: " + e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    @PostMapping("/reset")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody PasswordResetRequest request) {
        Map<String, String> response = new HashMap<>();
        
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            response.put("error", "Passwords do not match");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
        
        if (userService.validatePasswordResetToken(request.getToken())) {
            boolean success = userService.resetPassword(request.getToken(), request.getPassword());
            
            if (success) {
                response.put("message", "Password has been reset successfully");
                return ResponseEntity.ok(response);
            }
        }
        
        response.put("error", "Invalid or expired password reset token");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
    
    @GetMapping("/validate")
    public ResponseEntity<?> validateToken(@RequestParam("token") String token) {
        Map<String, Boolean> response = new HashMap<>();
        response.put("valid", userService.validatePasswordResetToken(token));
        
        return ResponseEntity.ok(response);
    }
} 