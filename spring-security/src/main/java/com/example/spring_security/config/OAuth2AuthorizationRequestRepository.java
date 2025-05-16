package com.example.spring_security.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.SerializationUtils;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component
public class OAuth2AuthorizationRequestRepository implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {
    public static final String OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME = "oauth2_auth_request";
    public static final String REDIRECT_URI_PARAM_COOKIE_NAME = "redirect_uri";
    private static final int COOKIE_EXPIRE_SECONDS = 180;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        return getCookieValue(request, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME)
                .map(this::deserialize)
                .orElse(null);
    }

    @Override
    public void saveAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest, HttpServletRequest request, HttpServletResponse response) {
        if (authorizationRequest == null) {
            removeCookie(request, response, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME);
            removeCookie(request, response, REDIRECT_URI_PARAM_COOKIE_NAME);
            return;
        }
        
        addCookie(response, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME, serialize(authorizationRequest), COOKIE_EXPIRE_SECONDS);
        
        String redirectUri = request.getParameter(REDIRECT_URI_PARAM_COOKIE_NAME);
        if (redirectUri != null && !redirectUri.isEmpty()) {
            addCookie(response, REDIRECT_URI_PARAM_COOKIE_NAME, redirectUri, COOKIE_EXPIRE_SECONDS);
        }
    }

    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(HttpServletRequest request, HttpServletResponse response) {
        OAuth2AuthorizationRequest authRequest = this.loadAuthorizationRequest(request);
        if (authRequest != null) {
            removeCookie(request, response, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME);
            removeCookie(request, response, REDIRECT_URI_PARAM_COOKIE_NAME);
        }
        return authRequest;
    }
    
    private Optional<String> getCookieValue(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        
        if (cookies != null && cookies.length > 0) {
            for (Cookie cookie : cookies) {
                if (name.equals(cookie.getName())) {
                    return Optional.of(cookie.getValue());
                }
            }
        }
        
        return Optional.empty();
    }
    
    private void addCookie(HttpServletResponse response, String name, String value, int maxAge) {
        Cookie cookie = new Cookie(name, value);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setMaxAge(maxAge);
        response.addCookie(cookie);
    }
    
    private void removeCookie(HttpServletRequest request, HttpServletResponse response, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null && cookies.length > 0) {
            for (Cookie cookie: cookies) {
                if (name.equals(cookie.getName())) {
                    cookie.setValue("");
                    cookie.setPath("/");
                    cookie.setMaxAge(0);
                    response.addCookie(cookie);
                }
            }
        }
    }
    
    private String serialize(OAuth2AuthorizationRequest authorizationRequest) {
        try {
            Map<String, Object> jsonMap = new HashMap<>();
            
            jsonMap.put("authorizationUri", authorizationRequest.getAuthorizationUri());
            jsonMap.put("clientId", authorizationRequest.getClientId());
            jsonMap.put("redirectUri", authorizationRequest.getRedirectUri());
            jsonMap.put("scopes", authorizationRequest.getScopes());
            jsonMap.put("state", authorizationRequest.getState());
            jsonMap.put("additionalParameters", authorizationRequest.getAdditionalParameters());
            jsonMap.put("authorizationRequestUri", authorizationRequest.getAuthorizationRequestUri());
            
            if (authorizationRequest.getGrantType() != null) {
                jsonMap.put("grantType", authorizationRequest.getGrantType().getValue());
            }
            
            if (authorizationRequest.getResponseType() != null) {
                jsonMap.put("responseType", authorizationRequest.getResponseType().getValue());
            }
            
            return Base64.getUrlEncoder().encodeToString(
                    objectMapper.writeValueAsBytes(jsonMap)
            );
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize authorization request", e);
        }
    }
    
    private OAuth2AuthorizationRequest deserialize(String cookie) {
        try {
            byte[] decodedBytes = Base64.getUrlDecoder().decode(cookie);
            Map<String, Object> jsonMap = objectMapper.readValue(decodedBytes, Map.class);
            
            OAuth2AuthorizationRequest.Builder builder = OAuth2AuthorizationRequest.authorizationCode()
                    .authorizationUri((String) jsonMap.get("authorizationUri"))
                    .clientId((String) jsonMap.get("clientId"))
                    .redirectUri((String) jsonMap.get("redirectUri"))
                    .state((String) jsonMap.get("state"));
            
            if (jsonMap.containsKey("scopes")) {
                builder.scopes((java.util.Set<String>) jsonMap.get("scopes"));
            }
            
            if (jsonMap.containsKey("additionalParameters")) {
                builder.additionalParameters((Map<String, Object>) jsonMap.get("additionalParameters"));
            }
            
            return builder.build();
        } catch (Exception e) {
            System.err.println("Error deserializing OAuth2AuthorizationRequest: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
} 