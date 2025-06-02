package com.example.spring_security.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Locale;

@RestController
@RequestMapping("/api/v1")
public class ImageProxyController {

    private static final Logger logger = LoggerFactory.getLogger(ImageProxyController.class);
    private final RestTemplate restTemplate;

    public ImageProxyController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @GetMapping("/image-proxy")
    public ResponseEntity<byte[]> proxyImage(@RequestParam("url") String imageUrl) {
        logger.info("Processing image proxy request for URL: {}", imageUrl);
        
        try {
            // Validate URL
            URI uri = new URI(imageUrl);
            String host = uri.getHost();
            
            logger.info("Processing host: {}", host);
            
            // Special handling for tayara.tn (their CDN might require specific headers)
            if (host != null && host.contains("tayara.tn")) {
                logger.info("Special handling for tayara.tn domain");
                
                try {
                    // Create custom headers required for tayara.tn
                    HttpHeaders requestHeaders = new HttpHeaders();
                    requestHeaders.set(HttpHeaders.USER_AGENT, "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
                    requestHeaders.set(HttpHeaders.REFERER, "https://www.tayara.tn/");
                    requestHeaders.set(HttpHeaders.ACCEPT, "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8");
                    requestHeaders.set(HttpHeaders.ACCEPT_LANGUAGE, "en-US,en;q=0.9");
                    
                    // Create request entity with headers
                    HttpEntity<String> entity = new HttpEntity<>(requestHeaders);
                    
                    logger.info("Attempting to fetch external Tayara image URL: {}", uri.toString());
                    // Fetch the image with custom headers
                    ResponseEntity<byte[]> response = restTemplate.exchange(
                        uri,
                        HttpMethod.GET,
                        entity,
                        byte[].class
                    );
                    
                    logger.info("External Tayara fetch response status: {}", response.getStatusCode());
                    logger.info("External Tayara fetch response headers: {}", response.getHeaders());
                    if (response.getBody() != null) {
                        logger.info("External Tayara fetch response body length: {}", response.getBody().length);
                        if (response.getBody().length < 1024 && response.getBody().length > 0) { // Log small non-empty bodies
                            logger.info("External Tayara fetch response body (first {} chars): {}", Math.min(response.getBody().length, 200), new String(response.getBody(), 0, Math.min(response.getBody().length, 200)));
                        }
                    } else {
                        logger.info("External Tayara fetch response body is NULL");
                    }

                    // Check response status
                    if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null && response.getBody().length > 0) {
                        logger.info("Successfully retrieved image from tayara.tn, size: {} bytes", response.getBody().length);
                        
                        // Set response headers
                        HttpHeaders headers = new HttpHeaders();
                        MediaType contentType = response.getHeaders().getContentType();
                        if (contentType != null) {
                            headers.setContentType(contentType);
                            logger.info("Content type from response: {}", contentType);
                        } else {
                            // Default to image/jpeg if content type isn't provided
                            headers.setContentType(MediaType.IMAGE_JPEG);
                            logger.info("No content type in response, defaulting to image/jpeg");
                        }
                        
                        // Allow cross-origin
                        headers.add("Access-Control-Allow-Origin", "*");
                        
                        // Cache headers
                        headers.setCacheControl("public, max-age=86400"); // Cache for 1 day
                        
                        return new ResponseEntity<>(response.getBody(), headers, HttpStatus.OK);
                    } else {
                        logger.warn("Received empty or error response from tayara.tn: {}", response.getStatusCode());
                        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
                    }
                } catch (RestClientException e) {
                    logger.error("Error fetching from tayara.tn: {}", e.getMessage());
                    
                    // Try one more time with a different approach
                    try {
                        logger.info("Trying alternative approach for tayara.tn");
                        logger.info("Attempting to fetch external Tayara image URL (fallback): {}", uri.toString());
                        ResponseEntity<byte[]> response = restTemplate.getForEntity(uri, byte[].class);
                        
                        logger.info("External Tayara fetch response status (fallback): {}", response.getStatusCode());
                        logger.info("External Tayara fetch response headers (fallback): {}", response.getHeaders());
                        if (response.getBody() != null) {
                            logger.info("External Tayara fetch response body length (fallback): {}", response.getBody().length);
                            if (response.getBody().length < 1024 && response.getBody().length > 0) { // Log small non-empty bodies
                                logger.info("External Tayara fetch response body (fallback, first {} chars): {}", Math.min(response.getBody().length, 200), new String(response.getBody(), 0, Math.min(response.getBody().length, 200)));
                            }
                        } else {
                            logger.info("External Tayara fetch response body (fallback) is NULL");
                        }

                        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                            HttpHeaders headers = new HttpHeaders();
                            headers.setContentType(MediaType.IMAGE_JPEG);
                            headers.add("Access-Control-Allow-Origin", "*");
                            headers.setCacheControl("public, max-age=86400");
                            
                            return new ResponseEntity<>(response.getBody(), headers, HttpStatus.OK);
                        }
                    } catch (Exception fallbackEx) {
                        logger.error("Alternative approach also failed: {}", fallbackEx.getMessage());
                    }
                    
                    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
                }
            }
            // Check if the URL is allowed (other domains)
            else if (host != null && (
                host.endsWith("mubawab.tn") || 
                host.contains("cdn.") ||
                host.contains("img.") ||
                host.contains("image."))) {
                
                try {
                    // Create request with standard browser headers
                    HttpHeaders requestHeaders = new HttpHeaders();
                    requestHeaders.set(HttpHeaders.USER_AGENT, "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
                    HttpEntity<String> entity = new HttpEntity<>(requestHeaders);
                    
                    logger.info("Attempting to fetch external image URL (other allowed domain): {}", uri.toString());
                    // Fetch the image
                    ResponseEntity<byte[]> response = restTemplate.exchange(
                        uri,
                        HttpMethod.GET,
                        entity,
                        byte[].class
                    );

                    logger.info("External fetch response status (other domain): {}", response.getStatusCode());
                    logger.info("External fetch response headers (other domain): {}", response.getHeaders());
                    if (response.getBody() != null) {
                        logger.info("External fetch response body length (other domain): {}", response.getBody().length);
                        if (response.getBody().length < 1024 && response.getBody().length > 0) { // Log small non-empty bodies
                            logger.info("External fetch response body (other domain, first {} chars): {}", Math.min(response.getBody().length, 200), new String(response.getBody(), 0, Math.min(response.getBody().length, 200)));
                        }
                    } else {
                        logger.info("External fetch response body (other domain) is NULL");
                    }
                    
                    // Set appropriate headers
                    HttpHeaders headers = new HttpHeaders();
                    
                    // Try to determine content type
                    MediaType contentType = response.getHeaders().getContentType();
                    if (contentType != null) {
                        headers.setContentType(contentType);
                    } else {
                        // Default to image/jpeg if content type isn't provided
                        headers.setContentType(MediaType.IMAGE_JPEG);
                    }
                    
                    // Allow cross-origin
                    headers.add("Access-Control-Allow-Origin", "*");
                    
                    // Add cache headers
                    headers.setCacheControl("public, max-age=86400"); // Cache for 1 day
                    
                    logger.info("Successfully proxied image from: {}", imageUrl);
                    return new ResponseEntity<>(response.getBody(), headers, HttpStatus.OK);
                } catch (Exception e) {
                    logger.error("Error proxying from {}: {}", host, e.getMessage());
                    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
                }
            } else {
                logger.warn("Rejected proxy request for non-allowed domain: {}", host);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        } catch (URISyntaxException e) {
            logger.error("Invalid image URL: {}", imageUrl, e);
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Error proxying image from URL: {}", imageUrl, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
} 