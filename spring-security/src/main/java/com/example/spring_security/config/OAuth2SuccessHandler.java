package com.example.spring_security.config;

import com.example.spring_security.model.User;
import com.example.spring_security.service.TokenService;
import com.example.spring_security.service.UserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Component
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UserService userService;
    private final TokenService tokenService;
    private final StatelessOAuth2AuthorizationRequestRepository authorizationRequestRepository;
    private final AppConfig appConfig;

    public OAuth2SuccessHandler(UserService userService, TokenService tokenService, 
                                StatelessOAuth2AuthorizationRequestRepository authorizationRequestRepository,
                                AppConfig appConfig) {
        this.userService = userService;
        this.tokenService = tokenService;
        this.authorizationRequestRepository = authorizationRequestRepository;
        this.appConfig = appConfig;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        try {
            authorizationRequestRepository.removeAuthorizationRequest(request, response);
            
            if (authentication instanceof OAuth2AuthenticationToken) {
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
                    System.out.println("Email was null, created email from sub: " + email);
                }
                
                if (name == null && provider == User.AuthProvider.GOOGLE) {
                    name = (String) attributes.getOrDefault("given_name", "") + " " + 
                           (String) attributes.getOrDefault("family_name", "");
                    name = name.trim();
                    if (name.isEmpty()) {
                        name = email.substring(0, email.indexOf('@'));
                    }
                    System.out.println("Name was null, created name: " + name);
                }
                
                System.out.println("OAuth2 Success - Email: " + email + ", Provider: " + provider + ", ID: " + providerId + ", Name: " + name);
                
                if (email == null) {
                    System.err.println("No email found in OAuth2 attributes: " + attributes);
                    getRedirectStrategy().sendRedirect(request, response, 
                            appConfig.getOAuth2CallbackUrlWithError("No email found"));
                    return;
                }
                
                try {
                    User user = userService.createOrUpdateOAuth2User(email, name, providerId, provider);
                    
                    String token = tokenService.generateToken(user);
                    
                    System.out.println("OAuth2 login successful for: " + email);
                    
                    String redirectUrl;
                    if (user.isNewOAuth2User()) {
                        // For new users, include flag to trigger role selection
                        System.out.println("New OAuth2 user, redirecting to role selection page");
                        redirectUrl = appConfig.getOAuth2CallbackUrlWithToken(token) + "&newUser=true";
                    } else {
                        System.out.println("Existing OAuth2 user, redirecting to normal callback");
                        redirectUrl = appConfig.getOAuth2CallbackUrlWithToken(token);
                    }
                    
                    System.out.println("Redirecting to: " + redirectUrl);
                    
                    getRedirectStrategy().sendRedirect(request, response, redirectUrl);
                } catch (Exception e) {
                    System.err.println("Error creating/updating user: " + e.getMessage());
                    e.printStackTrace();
                    getRedirectStrategy().sendRedirect(request, response, 
                            appConfig.getOAuth2CallbackUrlWithError("User processing error: " + e.getMessage()));
                }
                
            } else {
                System.err.println("Authentication is not an OAuth2AuthenticationToken: " + authentication.getClass().getName());
                getRedirectStrategy().sendRedirect(request, response, 
                        appConfig.getOAuth2CallbackUrlWithError("Authentication failed"));
            }
        } catch (Exception e) {
            System.err.println("OAuth2 authentication error: " + e.getMessage());
            e.printStackTrace();
            getRedirectStrategy().sendRedirect(request, response, 
                    appConfig.getOAuth2CallbackUrlWithError(e.getMessage()));
        }
    }
} 