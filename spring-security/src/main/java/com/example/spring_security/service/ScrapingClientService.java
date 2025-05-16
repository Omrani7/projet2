package com.example.spring_security.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import java.net.URI;

@Service
public class ScrapingClientService {

    private static final Logger log = LoggerFactory.getLogger(ScrapingClientService.class);

    private final RestTemplate restTemplate;
    private final String scraperBaseUrl;

    @Autowired
    public ScrapingClientService(RestTemplate restTemplate, 
                                 @Value("${scraper.module.base-url:http://localhost:8081}") String scraperBaseUrl) {
        this.restTemplate = restTemplate;
        this.scraperBaseUrl = scraperBaseUrl;
        log.info("Scraping Client Service configured with scraper base URL: {}", this.scraperBaseUrl);
    }

    /**
     * Sends a request to the scraping module to trigger the Immobilier scraper.
     */
    public void triggerImmobilierScrape() {
        String endpointPath = "/api/v1/scrape/immobilier";
        String targetUrl = scraperBaseUrl + endpointPath;
        log.info("Attempting to trigger Immobilier scrape via POST request to: {}", targetUrl);

        try {
            // Send POST request, expecting a 202 Accepted status and no response body
            ResponseEntity<Void> response = restTemplate.postForEntity(targetUrl, null, Void.class);

            if (response.getStatusCode() == HttpStatus.ACCEPTED) {
                log.info("Successfully sent trigger request for Immobilier scrape. Status: {}", response.getStatusCode());
            } else {
                // Log unexpected success codes
                log.warn("Trigger request for Immobilier scrape sent, but received unexpected status: {}. Expected 202 Accepted.", 
                         response.getStatusCode());
            }

        } catch (RestClientException e) {
            // Handle network errors, connection refused, etc.
            log.error("Failed to send trigger request to scraper module at {}: {}. Is the module running?", 
                      targetUrl, e.getMessage());
            // Optionally, re-throw, return a status, or notify an admin system
        } catch (Exception e) {
            // Catch any other unexpected errors during the request
            log.error("An unexpected error occurred while trying to trigger Immobilier scrape at {}: {}", 
                      targetUrl, e.getMessage(), e);
        }
    }

    /**
     * Sends a request to the scraping module to trigger the Tayara scraper with a custom URL.
     */
    public void triggerTayaraScrape(String url) {
        String endpointPath = "/api/v1/scrape/tayara";
        URI targetUri = null; // Initialize to null for error handling

        try {
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(scraperBaseUrl)
                    .path(endpointPath); // Set base URL and path first

            if (url != null && !url.isEmpty()) {
                // Make sure url is properly encoded for passing as a query parameter
                log.info("Original URL for Tayara scraping: {}", url);
                
                // Replace all & with %26 to ensure they're properly encoded as part of the URL parameter
                // Only do this if the URL actually contains & characters and isn't already encoded
                if (url.contains("&") && !url.contains("%26")) {
                    log.info("URL contains & characters, encoding them properly");
                    // We're careful to only encode the & characters, not the entire URL
                    String encodedUrl = url.replace("&", "%26");
                    log.info("Encoded URL for Tayara scraping: {}", encodedUrl);
                    builder.queryParam("url", encodedUrl);
                } else {
                    // No & characters or already encoded, use as is
                    builder.queryParam("url", url);
                }
            }

            // Build the URI, letting the builder handle necessary encoding of the query param value
            targetUri = builder.build(false) // Build without treating components as templates
                               .encode()      // Apply standard percent-encoding
                               .toUri();

            log.info("Attempting to trigger Tayara scrape via GET request to URI: {}", targetUri);

            // Send GET request using the URI object
            ResponseEntity<String> response = restTemplate.getForEntity(targetUri, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                log.info("Successfully triggered Tayara scrape. Status: {}. Message: {}", response.getStatusCode(), response.getBody());
            } else {
                log.warn("Trigger request for Tayara scrape sent, but received unexpected status: {}. Expected 200 OK.", response.getStatusCode());
            }

        } catch (RestClientException e) {
            // Log using targetUri if it was successfully built, otherwise use a placeholder
            String logUrl = (targetUri != null) ? targetUri.toString() : scraperBaseUrl + endpointPath + " (URI build failed)";
            log.error("Failed to send trigger request to Tayara scraper at {}: {}. Is the module running?", logUrl, e.getMessage());
        } catch (Exception e) {
            String logUrl = (targetUri != null) ? targetUri.toString() : scraperBaseUrl + endpointPath + " (URI build failed)";
            log.error("An unexpected error occurred while trying to trigger Tayara scrape at {}: {}", logUrl, e.getMessage(), e);
            // It's possible the IllegalArgumentException from builder.build() could land here
            if (e instanceof IllegalArgumentException) {
                 log.error(">>>>> URI building failed. Original URL parameter might contain unexpected characters: {}", url);
            }
        }
    }

    // TODO: Add methods for triggering other scrapers (e.g., triggerTayaraScrape) later

} 