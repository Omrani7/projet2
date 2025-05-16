package com.example.spring_security.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public class OAuth2FailureHandler implements AuthenticationFailureHandler {
    
    private final AppConfig appConfig;
    private final StatelessOAuth2AuthorizationRequestRepository authorizationRequestRepository;
    
    public OAuth2FailureHandler(AppConfig appConfig, 
                             StatelessOAuth2AuthorizationRequestRepository authorizationRequestRepository) {
        this.appConfig = appConfig;
        this.authorizationRequestRepository = authorizationRequestRepository;
    }
    
    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, 
                                      AuthenticationException exception) throws IOException, ServletException {
        System.err.println("=== OAuth2 Authentication Failure ===");
        System.err.println("Error Message: " + exception.getMessage());
        System.err.println("Error Class: " + exception.getClass().getName());
        if (exception.getCause() != null) {
            System.err.println("Cause: " + exception.getCause().getMessage());
        }
        
        exception.printStackTrace();
        
        String errorMessage = "Authentication failed";
        if (exception.getMessage() != null) {
            errorMessage = exception.getMessage();
        }
        
        String encodedError = URLEncoder.encode(errorMessage, StandardCharsets.UTF_8.toString());
        
        // Clean up any saved authorization requests
        authorizationRequestRepository.removeAuthorizationRequest(request, response);
        
        // Redirect to the frontend with error details
        response.sendRedirect(appConfig.getOAuth2CallbackUrlWithError(encodedError));
    }
} 