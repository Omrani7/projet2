package com.example.spring_security.controller;

import com.example.spring_security.config.AppConfig;
import com.example.spring_security.dto.AuthResponse;
import com.example.spring_security.model.User;
import com.example.spring_security.service.TokenService;
import com.example.spring_security.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/oauth2")
public class OAuth2Controller {

    @Autowired
    private UserService userService;
    
    @Autowired
    private TokenService tokenService;
    
    @Autowired
    private AppConfig appConfig;
    
    @GetMapping("/success")
    public void handleOAuth2Success(HttpServletResponse response) throws IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication instanceof OAuth2AuthenticationToken) {
            try {
                OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
                OAuth2User oAuth2User = oauthToken.getPrincipal();
                
                String registrationId = oauthToken.getAuthorizedClientRegistrationId();
                User.AuthProvider provider = User.AuthProvider.valueOf(registrationId.toUpperCase());
                
                Map<String, Object> attributes = oAuth2User.getAttributes();
                
                System.out.println("OAuth2 Attributes: " + attributes);
                
                String email = (String) attributes.get("email");
                String providerId = (String) attributes.get("sub");
                String name = (String) attributes.get("name");
                
                if (email == null && provider == User.AuthProvider.GOOGLE) {
                    email = (String) attributes.get("sub") + "@gmail.com";
                }
                
                if (name == null && provider == User.AuthProvider.GOOGLE) {
                    name = (String) attributes.getOrDefault("given_name", "") + " " + 
                           (String) attributes.getOrDefault("family_name", "");
                    name = name.trim();
                    if (name.isEmpty()) {
                        name = email.substring(0, email.indexOf('@'));
                    }
                }
                
                System.out.println("OAuth2 Success - Email: " + email + ", Provider: " + provider + ", ID: " + providerId + ", Name: " + name);
                
                if (email == null) {
                    response.sendRedirect(appConfig.getOAuth2CallbackUrlWithError("No email found"));
                    return;
                }
                
                User user = userService.createOrUpdateOAuth2User(email, name, providerId, provider);
                
                String token = tokenService.generateToken(user);
                
                response.sendRedirect(appConfig.getOAuth2CallbackUrlWithToken(token));
                
            } catch (Exception e) {
                System.err.println("OAuth2 authentication error: " + e.getMessage());
                e.printStackTrace();
                response.sendRedirect(appConfig.getOAuth2CallbackUrlWithError(e.getMessage()));
            }
        } else {
            response.sendRedirect(appConfig.getOAuth2CallbackUrlWithError("Authentication failed"));
        }
    }

    @GetMapping("/error")
    public void handleOAuth2Error(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String error = request.getParameter("error");
        String errorDescription = request.getParameter("error_description");
        String errorUri = request.getParameter("error_uri");
        
        System.err.println("OAuth2 Error Details:");
        System.err.println("Error: " + error);
        System.err.println("Error Description: " + errorDescription);
        System.err.println("Error URI: " + errorUri);
        System.err.println("Request Parameters: " + request.getParameterMap());
        
        String errorMessage = errorDescription != null ? errorDescription : (error != null ? error : "OAuth2 authentication failed");
        response.sendRedirect(appConfig.getOAuth2CallbackUrlWithError(errorMessage));
    }
    
    @GetMapping("/logout")
    public void logout(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            new SecurityContextLogoutHandler().logout(request, response, auth);
        }
        response.sendRedirect("/");
    }
    
    // New endpoint for updating user role after OAuth2 login
    @PostMapping("/update-role")
    public ResponseEntity<?> updateUserRole(@RequestBody Map<String, Object> request) { // Accept Object for mixed types
        try {
            // Extract userId and role
            Object userIdObj = request.get("userId");
            String role = (String) request.get("role");
            
            if (userIdObj == null || role == null) {
                return ResponseEntity.badRequest().body("User ID and role are required");
            }

            int userId;
            try {
                // Ensure userId is an integer
                userId = Integer.parseInt(userIdObj.toString()); 
            } catch (NumberFormatException e) {
                return ResponseEntity.badRequest().body("Invalid User ID format");
            }
            
            // Validate role is either STUDENT or OWNER
            if (!role.equalsIgnoreCase("STUDENT") && !role.equalsIgnoreCase("OWNER")) {
                return ResponseEntity.badRequest().body("Role must be either STUDENT or OWNER");
            }
            
            // Update the user's role using userId
            User updatedUser = userService.updateOAuth2UserRole(userId, role);
            
            // Generate a fresh token with the updated role
            String newToken = tokenService.generateToken(updatedUser);
            
            // Create response with new token and user details
            AuthResponse response = new AuthResponse();
            response.setToken(newToken);
            response.setUserId(updatedUser.getId());
            response.setEmail(updatedUser.getEmail());
            response.setUsername(updatedUser.getUsername());
            response.setRole(updatedUser.getRole().name()); 
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) { // Catch specific exception for not found
             return ResponseEntity.status(HttpStatus.NOT_FOUND) // Return 404 if user not found by ID
                     .body(e.getMessage());
        } catch (Exception e) {
            // Log the exception for debugging
            // logger.error("Error updating user role", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error updating user role: " + e.getMessage());
        }
    }
} 