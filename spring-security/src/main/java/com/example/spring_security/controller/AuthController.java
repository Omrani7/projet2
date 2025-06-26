package com.example.spring_security.controller;

import com.example.spring_security.dto.AuthResponse;
import com.example.spring_security.dto.LoginRequest;
import com.example.spring_security.dto.SignupRequest;
import com.example.spring_security.model.User;
import com.example.spring_security.service.TokenService;
import com.example.spring_security.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;
import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private TokenService tokenService;
    
    @Autowired
    private AuthenticationManager authenticationManager;
    
    @Value("${app.frontend-url:http://localhost:4200}")
    private String frontendUrl;
    
    @GetMapping("/login")
    public RedirectView handleLoginGet() {

        return new RedirectView(frontendUrl);
    }
    
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody SignupRequest signupRequest) {
        try {
            User registeredUser = userService.registerUser(signupRequest);
            
            String token = tokenService.generateToken(registeredUser);
            
            AuthResponse response = AuthResponse.builder()
                    .token(token)
                    .userId(registeredUser.getId())
                    .email(registeredUser.getEmail())
                    .username(registeredUser.getUsername())
                    .role(registeredUser.getRole().name())
                    .build();
                    
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Registration failed: " + e.getMessage());
        }
    }
    
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getEmail(),
                            loginRequest.getPassword()
                    )
            );
            
            if (authentication.isAuthenticated()) {
                User user = userService.findByEmail(loginRequest.getEmail());
                if (user == null) {
                    user = userService.findByUsername(loginRequest.getEmail());
                }
                
                String token = tokenService.generateToken(user);
                
                AuthResponse response = AuthResponse.builder()
                        .token(token)
                        .userId(user.getId())
                        .email(user.getEmail())
                        .username(user.getUsername())
                        .role(user.getRole().name())
                        .build();
                        
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("Authentication failed");
            }
        } catch (AuthenticationException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Authentication failed: " + e.getMessage());
        }
    }
    
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Logged out successfully");
        

        
        return ResponseEntity.ok(response);
    }
} 