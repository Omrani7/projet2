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


    @PostMapping("/trigger/immobilier")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> triggerImmobilier() {
        log.info("Admin request received to trigger Immobilier scraper.");
        
        try {
            log.debug("Checking scraper service status...");
            String status = scraperIntegrationService.getScraperStatus();
            log.info("Scraper service status: {}", status);

            scrapingClientService.triggerImmobilierScrape();
            return ResponseEntity.ok("Immobilier scraper trigger request submitted successfully. Service status: " + status);

        } catch (ScraperServiceUnavailableException e) {
            log.warn("Scraper service is unavailable: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body("Scraping service is not available. Please ensure it is started. Details: " + e.getMessage());
        } catch (Exception e) {
            log.error("Error encountered while processing admin trigger for Immobilier scraper: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to submit Immobilier scraper trigger request after status check. Error: " + e.getMessage());
        }
    }


    @PostMapping("/trigger/tayara")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> triggerTayara(@RequestParam(required = false) String url) {
        log.info("Admin request received to trigger Tayara scraper. URL: {}", url);
        try {
            log.debug("Checking scraper service status...");
            String status = scraperIntegrationService.getScraperStatus();
            log.info("Scraper service status: {}", status);

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

}