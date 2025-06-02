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
            
            if (!(authentication instanceof OAuth2AuthenticationToken)) {
                logger.warn("Authentication is not an OAuth2AuthenticationToken, cannot proceed with OAuth2 success handling.");
                getRedirectStrategy().sendRedirect(request, response, appConfig.getOAuth2PopupCallbackUrlWithError("Invalid authentication type"));
                return;
            }

            OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
            OAuth2User oAuth2User = oauthToken.getPrincipal();
            
            String registrationId = oauthToken.getAuthorizedClientRegistrationId();
            User.AuthProvider provider = User.AuthProvider.valueOf(registrationId.toUpperCase());
            
            Map<String, Object> attributes = oAuth2User.getAttributes();
            
            System.out.println("OAuth2 Attributes: " + attributes);
            
            String email = (String) attributes.get("email");
            String providerId = oAuth2User.getName(); // Standard way to get unique ID for provider
            String name = (String) attributes.get("name");
            
            // Fallbacks for Google specific attributes if standard ones are missing
            if (email == null && provider == User.AuthProvider.GOOGLE) {
                email = (String) attributes.get("email"); // Retrying, as it should be present
            }
            if (name == null && provider == User.AuthProvider.GOOGLE) {
                name = (String) attributes.getOrDefault("given_name", "") + " " + 
                       (String) attributes.getOrDefault("family_name", "");
                name = name.trim();
                if (name.isEmpty() && email != null) {
                    name = email.substring(0, email.indexOf('@'));
                }
            }
            
            System.out.println("OAuth2 Success - Email: " + email + ", Provider: " + provider + ", ID: " + providerId + ", Name: " + name);
            
            if (email == null) {
                logger.error("No email found in OAuth2 attributes, cannot proceed.");
                getRedirectStrategy().sendRedirect(request, response, appConfig.getOAuth2PopupCallbackUrlWithError("No email found from provider"));
                return;
            }
            
            User user = userService.createOrUpdateOAuth2User(email, name, providerId, provider);
            String jwtToken = tokenService.generateToken(user);
            
            // Redirect the popup to the HTML page that will postMessage the token
            String targetUrl = appConfig.getOAuth2PopupCallbackUrlWithToken(jwtToken);
            logger.info("OAuth2 success, redirecting popup to: " + targetUrl);
            getRedirectStrategy().sendRedirect(request, response, targetUrl);
            
        } catch (Exception e) {
            logger.error("Exception in OAuth2SuccessHandler: " + e.getMessage(), e);
            getRedirectStrategy().sendRedirect(request, response, appConfig.getOAuth2PopupCallbackUrlWithError(e.getMessage()));
        }
    }
} 