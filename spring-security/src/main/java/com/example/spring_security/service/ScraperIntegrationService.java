package com.example.spring_security.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class ScraperIntegrationService {
    
    private static final Logger log = LoggerFactory.getLogger(ScraperIntegrationService.class);
    
    private final RestTemplate restTemplate;
    
    @Value("${scraper.base-url:http://localhost:8081}")
    private String scraperBaseUrl;
    
    public ScraperIntegrationService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
    
    /**
     * Trigger a scraping operation for Immobilier.
     * Note: Consider using ScrapingClientService for more specific trigger actions.
     * @return Response message from the scraper service
     */
    public String triggerScraping() {
        try {
            String targetUrl = scraperBaseUrl + "/api/v1/scrape/immobilier";
            log.info("Triggering Immobilier scraping operation via POST to {}", targetUrl);
            // The immobilier endpoint expects a POST and returns 202 ACCEPTED with no body.
            // For simplicity here, we'll still try to get a String response,
            // but ideally, this should align with the expected Void response.
            ResponseEntity<String> response = restTemplate.postForEntity(
                    targetUrl, 
                    null, // No request body for this trigger
                    String.class);
            
            // If the actual endpoint returns 202 with no body, response.getBody() might be null.
            // The original ScraperController returns void, which translates to no body on success.
            // So, returning a static message or status code might be more appropriate.
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Immobilier scraping triggered successfully, status: {}", response.getStatusCode());
                return "Immobilier scraper trigger request submitted successfully. Status: " + response.getStatusCode();
            } else {
                log.warn("Immobilier scraping trigger returned non-success status: {}", response.getStatusCode());
                return "Immobilier scraper trigger request returned status: " + response.getStatusCode();
            }
        } catch (Exception e) {
            log.error("Error triggering Immobilier scraping operation: {}", e.getMessage(), e);
            return "Error: " + e.getMessage();
        }
    }
    
    /**
     * Get the status of the scraper service
     * @return Status message or an error string
     */
    public String getScraperStatus() {
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(
                    scraperBaseUrl + "/api/v1/scrape/status", 
                    String.class);
            return response.getBody();
        } catch (Exception e) {
            log.error("Error getting scraper status: {}", e.getMessage(), e);
            // Propagate the error or return a clear error indicator
            // For the health check, throwing or returning a specific error object/string
            // that can be reliably checked is better than returning "Error: " + message.
            // For now, keeping it similar to original but this could be improved.
            throw new ScraperServiceUnavailableException("Failed to get scraper status: " + e.getMessage(), e);
        }
    }
    
    /**
     * Get all properties from the scraper module
     * @return List of property data
     */
    public List<Map<String, Object>> getAllProperties() {
        try {
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    scraperBaseUrl + "/api/v1/scrape/properties",
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {});
            
            return response.getBody();
        } catch (Exception e) {
            log.error("Error getting properties: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }
    
    /**
     * Get a specific property by ID
     * @param id Property ID
     * @return Property data or null if not found
     */
    public Map<String, Object> getPropertyById(Long id) {
        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    scraperBaseUrl + "/api/v1/scrape/properties/" + id,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Map<String, Object>>() {});
            
            return response.getBody();
        } catch (Exception e) {
            log.error("Error getting property with ID {}: {}", id, e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Get all scraped properties from the scraper module for review
     * @return List of scraped property data
     */
    public List<Map<String, Object>> getScrapedPropertiesForReview() {
        try {
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    scraperBaseUrl + "/api/v1/scrape/properties",
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {});
            
            List<Map<String, Object>> properties = response.getBody();
            log.info("Retrieved {} scraped properties for review", properties != null ? properties.size() : 0);
            return properties != null ? properties : Collections.emptyList();
        } catch (Exception e) {
            log.error("Error getting scraped properties for review: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }
    
    /**
     * Get scraped properties count for admin dashboard stats
     * @return Number of scraped properties available for review
     */
    public long getScrapedPropertiesCount() {
        try {
            String status = getScraperStatus();
            // Extract count from status message like "Database contains 60 property listings"
            if (status != null && status.contains("Database contains")) {
                String[] parts = status.split("Database contains ");
                if (parts.length > 1) {
                    String countPart = parts[1].split(" ")[0];
                    return Long.parseLong(countPart);
                }
            }
            return 0;
        } catch (Exception e) {
            log.error("Error getting scraped properties count: {}", e.getMessage());
            return 0;
        }
    }
} 