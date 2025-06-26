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
        String errorParam = request.getParameter("error");
        String errorDescriptionParam = request.getParameter("error_description");

        if (errorDescriptionParam != null && !errorDescriptionParam.isEmpty()) {
            errorMessage = errorDescriptionParam;
        } else if (errorParam != null && !errorParam.isEmpty()) {
            errorMessage = errorParam;
        }

        System.err.println("OAuth2 Authentication Failed: " + errorMessage);
        exception.printStackTrace();
        
        authorizationRequestRepository.removeAuthorizationRequest(request, response);
        
        String targetUrl = appConfig.getOAuth2PopupCallbackUrlWithError(errorMessage);
        response.sendRedirect(targetUrl);
    }
} 