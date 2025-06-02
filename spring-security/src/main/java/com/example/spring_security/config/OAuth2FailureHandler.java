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
        String errorMessage = exception.getMessage();
        // Try to get a more specific error from request parameters if available (e.g., from Google's error response)
        String errorParam = request.getParameter("error");
        String errorDescriptionParam = request.getParameter("error_description");

        if (errorDescriptionParam != null && !errorDescriptionParam.isEmpty()) {
            errorMessage = errorDescriptionParam;
        } else if (errorParam != null && !errorParam.isEmpty()) {
            errorMessage = errorParam;
        }

        System.err.println("OAuth2 Authentication Failed: " + errorMessage);
        exception.printStackTrace();
        
        // Clear the failed OAuth2 authorization request state
        authorizationRequestRepository.removeAuthorizationRequest(request, response);
        
        // Redirect the popup to the callback HTML page with error information
        String targetUrl = appConfig.getOAuth2PopupCallbackUrlWithError(errorMessage);
        response.sendRedirect(targetUrl);
    }
} 