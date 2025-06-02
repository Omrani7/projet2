package com.example.spring_security.config;

import com.example.spring_security.service.MyUserDetailsService;
import com.example.spring_security.service.TokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(JwtFilter.class);

    @Autowired
    private TokenService tokenService;
    @Autowired
    ApplicationContext context;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        final String authHeader = request.getHeader("Authorization");
        String token = null;
        String userName = null;

        log.debug("Request URI: {}", request.getRequestURI());
        log.debug("Authorization Header: {}", authHeader);

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
            log.debug("Extracted Token: {}", token);
            try {
                userName = tokenService.extractUserName(token);
                log.debug("Extracted Username from Token: {}", userName);
            } catch (Exception e) {
                log.error("Error extracting username from token: {}", e.getMessage());
            }
        } else {
            log.debug("No Bearer token found in Authorization header.");
        }

        if (userName != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            log.debug("Username is present and SecurityContext has no authentication. Attempting to load UserDetails for: {}", userName);
            UserDetails userDetails = null;
            try {
                userDetails = context.getBean(MyUserDetailsService.class).loadUserByUsername(userName);
            } catch (Exception e) {
                log.error("Error loading UserDetails for username {}: {}", userName, e.getMessage());
            }
            
            if (userDetails != null) {
                log.debug("UserDetails loaded successfully for username: {}. Authorities: {}", userName, userDetails.getAuthorities());
                boolean isTokenValid = false;
                try {
                    isTokenValid = tokenService.validateToken(token, userDetails);
                    log.debug("Token validation result for username {}: {}", userName, isTokenValid);
                } catch (Exception e) {
                    log.error("Error validating token for username {}: {}", userName, e.getMessage());
                }

                if (isTokenValid) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    log.info("Successfully authenticated user {} and set SecurityContext.", userName);
                } else {
                    log.warn("Token validation failed for user {}. SecurityContext not set.", userName);
                }
            } else {
                log.warn("UserDetails not found for username {}. SecurityContext not set.", userName);
            }
        } else {
            if (userName == null) {
                log.debug("Username is null, skipping SecurityContext setup.");
            }
            if (SecurityContextHolder.getContext().getAuthentication() != null) {
                log.debug("SecurityContext already has an authentication: {}. Skipping setup.", SecurityContextHolder.getContext().getAuthentication().getName());
            }
        }
        
        filterChain.doFilter(request, response);
    }
}
