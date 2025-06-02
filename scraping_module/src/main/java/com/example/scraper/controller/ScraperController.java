package com.example.scraper.controller;

import com.example.scraper.dto.PropertyDto;
import com.example.scraper.model.ImmobilierProperty;
import com.example.scraper.model.PropertyListing;
import com.example.scraper.repository.PropertyListingRepository;
import com.example.scraper.service.scraper.playwright.Converter;
import com.example.scraper.service.scraper.playwright.immobilier.ImmobilierScraper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/scrape")
public class ScraperController {
    
    private static final Logger log = LoggerFactory.getLogger(ScraperController.class);
    
    @Autowired
    private ImmobilierScraper immobilierScraper;
    
    @Autowired
    private PropertyListingRepository propertyRepository;
    
    @Autowired
    @Qualifier("playwrightScraperExecutor")
    private ThreadPoolTaskExecutor taskExecutor;
    
    private static final String IMMOBILIER_BASE_URL = "https://www.immobilier.com.tn/";
    
    @GetMapping("/properties")
    public ResponseEntity<List<PropertyDto>> getProperties() {
        log.info("Getting all properties");
        
        try {
            List<PropertyListing> listings = propertyRepository.findByActiveTrue();
            List<PropertyDto> dtos = listings.stream()
                .map(Converter::toPropertyDto)
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(dtos);
        } catch (Exception e) {
            log.error("Error retrieving properties: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(List.of());
        }
    }
    
    @GetMapping("/status")
    public ResponseEntity<String> getStatus() {
        long count = propertyRepository.count();
        return ResponseEntity.ok("Scraper module running. Database contains " + count + " property listings.");
    }
    
    @GetMapping("/properties/{id}")
    public ResponseEntity<PropertyDto> getProperty(@PathVariable Long id) {
        return propertyRepository.findById(id)
            .map(Converter::toPropertyDto)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/immobilier")
    public ResponseEntity<Void> runImmobilierScraper() {
        log.info("Received trigger request for Immobilier scraper.");
        try {
            CompletableFuture<List<ImmobilierProperty>> scrapingFuture = immobilierScraper.scrapeAsync(IMMOBILIER_BASE_URL);

            scrapingFuture.thenAcceptAsync(properties -> {
                log.info("Asynchronous scrape completed. Received {} properties. Starting conversion and save.", properties.size());
                List<PropertyListing> listings = new ArrayList<>();
                for (ImmobilierProperty property : properties) {
                    try {
                        PropertyListing listing = Converter.toPropertyListing(property);
                        if (listing != null) {
                            listings.add(listing);
                        }
                    } catch (Exception convEx) {
                        log.error("Error converting property with URL {}: {}", 
                                  (property != null ? property.getUrl() : "unknown"), convEx.getMessage(), convEx);
                    }
                }
                
                if (!listings.isEmpty()) {
                    log.info("Saving {} converted property listings to database.", listings.size());
                    try {
                        propertyRepository.saveAll(listings);
                        log.info("Successfully saved {} listings.", listings.size());
                    } catch (Exception dbEx) {
                        log.error("Error saving listings to database: {}", dbEx.getMessage(), dbEx);
                    }
                } else {
                    log.info("No properties to save after conversion.");
                }
            }, taskExecutor)
            .exceptionally(ex -> {
                log.error("Asynchronous scraping task failed: {}", ex.getMessage(), ex);
                return null;
            });

            log.info("Immobilier scraper task submitted successfully. Conversion and saving will occur in background.");
            return ResponseEntity.status(HttpStatus.ACCEPTED).build();
            
        } catch (Exception e) {
            log.error("Error submitting Immobilier scraper task: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
} 