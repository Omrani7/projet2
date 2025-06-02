package com.example.spring_security.controller;

import com.example.spring_security.service.ScrapingClientService;
import com.example.spring_security.service.ScraperIntegrationService;
import com.example.spring_security.service.ScraperServiceUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/scrape")
public class AdminScraperController {

    private static final Logger log = LoggerFactory.getLogger(AdminScraperController.class);

    @Autowired
    private ScrapingClientService scrapingClientService;

    @Autowired
    private ScraperIntegrationService scraperIntegrationService;

    /**
     * Endpoint to manually trigger the Immobilier scraper.
     * Requires ADMIN role.
     * Checks scraper service status before triggering.
     * @return ResponseEntity indicating success or failure of the trigger submission.
     */
    @PostMapping("/trigger/immobilier")
    @PreAuthorize("hasRole('ADMIN')") // Ensure only admins can call this
    public ResponseEntity<String> triggerImmobilier() {
        log.info("Admin request received to trigger Immobilier scraper.");
        
        try {
            // Step 1: Check scraper service status
            log.debug("Checking scraper service status...");
            String status = scraperIntegrationService.getScraperStatus(); // This will throw ScraperServiceUnavailableException if down
            log.info("Scraper service status: {}", status); // Log status if successful

            // Step 2: If status check passed (no exception), trigger the scrape
            scrapingClientService.triggerImmobilierScrape();
            return ResponseEntity.ok("Immobilier scraper trigger request submitted successfully. Service status: " + status);

        } catch (ScraperServiceUnavailableException e) {
            log.warn("Scraper service is unavailable: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body("Scraping service is not available. Please ensure it is started. Details: " + e.getMessage());
        } catch (Exception e) {
            // Catch other potential exceptions if the client service re-throws or other issues occur during trigger
            log.error("Error encountered while processing admin trigger for Immobilier scraper: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to submit Immobilier scraper trigger request after status check. Error: " + e.getMessage());
        }
    }

    /**
     * Endpoint to manually trigger the Tayara scraper with a custom URL.
     * Requires ADMIN role.
     * Checks scraper service status before triggering.
     * @param url The Tayara URL to scrape (optional)
     * @return ResponseEntity indicating success or failure of the trigger submission.
     */
    @PostMapping("/trigger/tayara")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> triggerTayara(@RequestParam(required = false) String url) {
        log.info("Admin request received to trigger Tayara scraper. URL: {}", url);
        try {
            // Step 1: Check scraper service status
            log.debug("Checking scraper service status...");
            String status = scraperIntegrationService.getScraperStatus();
            log.info("Scraper service status: {}", status);

            // Step 2: If status check passed (no exception), trigger the scrape
            scrapingClientService.triggerTayaraScrape(url);
            return ResponseEntity.ok("Tayara scraper trigger request submitted successfully");

        } catch (ScraperServiceUnavailableException e) {
            log.warn("Scraper service is unavailable: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body("Scraping service is not available. Please ensure it is started. Details: " + e.getMessage());
        } catch (Exception e) {
            log.error("Error encountered while processing admin trigger for Tayara scraper: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to submit Tayara scraper trigger request after status check. Error: " + e.getMessage());
        }
    }

    /**
     * Endpoint to get scraper service status.
     * Requires ADMIN role.
     * @return ResponseEntity with scraper status information.
     */
    @GetMapping("/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> getScraperStatus() {
        log.info("Admin request received to get scraper status.");
        try {
            String status = scraperIntegrationService.getScraperStatus();
            return ResponseEntity.ok(status);
        } catch (ScraperServiceUnavailableException e) {
            log.warn("Scraper service is unavailable: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body("Scraper service is unavailable: " + e.getMessage());
        } catch (Exception e) {
            log.error("Error encountered while getting scraper status: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to get scraper status. Error: " + e.getMessage());
        }
    }

    // TODO: Add endpoints for triggering other scrapers later
} 