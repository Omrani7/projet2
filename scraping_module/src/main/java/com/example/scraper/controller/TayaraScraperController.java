package com.example.scraper.controller;

import com.example.scraper.model.TayaraProperty;
import com.example.scraper.model.PropertyListing;
import com.example.scraper.repository.PropertyListingRepository;
import com.example.scraper.service.scraper.playwright.tayara.TayaraScraper;
import com.example.scraper.service.scraper.playwright.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/v1/scrape/tayara")
public class TayaraScraperController {

    private static final Logger log = LoggerFactory.getLogger(TayaraScraperController.class);
    private static final String BASE_TAYARA_URL = "https://www.tayara.tn/ads/c/Immobilier/Appartements/";
    private static final String DEFAULT_TAYARA_URL = "https://www.tayara.tn/ads/c/Immobilier/Appartements/?maxPrice=900&Type+de+transaction=%C3%80+Louer&page=1";
    private static final int MAX_PAGES = 60;

    private final TayaraScraper tayaraScraper;
    private final PropertyListingRepository propertyListingRepository;

    @Autowired
    public TayaraScraperController(@Qualifier("playwrightTayaraScraper") TayaraScraper tayaraScraper,
                                   PropertyListingRepository propertyListingRepository) {
        this.tayaraScraper = tayaraScraper;
        this.propertyListingRepository = propertyListingRepository;
    }

    /**
     * Original endpoint that accepts a full URL parameter
     */
    @GetMapping
    public ResponseEntity<?> triggerTayaraScraper(@RequestParam(required = false) String url) {
        log.info("Received request to trigger Tayara scraper.");
        
        try {
            if (url != null && !url.isEmpty()) {
                // Make sure the URL is properly decoded before logging and passing to the scraper
                String decodedUrl = URLDecoder.decode(url, StandardCharsets.UTF_8);
                log.info("Target URL specified (decoded): {}", decodedUrl);
                
                // Log any specific parameters we're interested in for debugging
                if (decodedUrl.contains("Type+de+transaction=") || decodedUrl.contains("Type%20de%20transaction=")) {
                    log.info("Transaction type parameter found in URL");
                }
                
                // Check if URL already contains a page parameter
                boolean hasPageParam = decodedUrl.contains("page=");
                
                if (hasPageParam) {
                    // Use the URL as is for scraping (single page)
                    List<TayaraProperty> properties = tayaraScraper.scrape(decodedUrl);
                    return processScrapingResults(properties);
                } else {
                    // Scrape multiple pages (up to MAX_PAGES)
                    return scrapeMultiplePages(decodedUrl);
                }
            } else {
                log.info("No target URL specified, using default rental apartments URL");
                // Default URL may already have a page parameter
                boolean hasPageParam = DEFAULT_TAYARA_URL.contains("page=");
                
                if (hasPageParam) {
                    // Use the default URL as is for scraping (single page)
                    List<TayaraProperty> properties = tayaraScraper.scrape(DEFAULT_TAYARA_URL);
                    return processScrapingResults(properties);
                } else {
                    // Scrape multiple pages (up to MAX_PAGES)
                    return scrapeMultiplePages(DEFAULT_TAYARA_URL);
                }
            }
        } catch (Exception e) {
            log.error("Error occurred while running Tayara scraper: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred during scraping: " + e.getMessage());
        }
    }
    
    /**
     * New endpoint that accepts individual search parameters and builds the URL
     * This avoids the need for manual URL encoding
     */
    @GetMapping("/search")
    public ResponseEntity<?> searchTayara(
            @RequestParam(required = false) Integer maxPrice,
            @RequestParam(required = false) String transactionType, // "À Louer" or "À Vendre" 
            @RequestParam(required = false) Integer minPrice,
            @RequestParam(required = false) Integer page) {
        
        log.info("Received parametrized search request for Tayara");
        
        try {
            // Build the URL with the parameters
            StringBuilder urlBuilder = new StringBuilder(BASE_TAYARA_URL);
            urlBuilder.append("?");
            
            boolean hasParam = false;
            
            if (maxPrice != null) {
                urlBuilder.append("maxPrice=").append(maxPrice);
                hasParam = true;
            }
            
            if (transactionType != null && !transactionType.isEmpty()) {
                if (hasParam) urlBuilder.append("&");
                urlBuilder.append("Type+de+transaction=").append(transactionType);
                hasParam = true;
            }
            
            if (minPrice != null) {
                if (hasParam) urlBuilder.append("&");
                urlBuilder.append("minPrice=").append(minPrice);
                hasParam = true;
            }
            
            if (page != null) {
                if (hasParam) urlBuilder.append("&");
                urlBuilder.append("page=").append(page);
                
                // If specific page is requested, only scrape that page
                String finalUrl = urlBuilder.toString();
                log.info("Built URL for Tayara search with specific page {}: {}", page, finalUrl);
                
                List<TayaraProperty> properties = tayaraScraper.scrape(finalUrl);
                return processScrapingResults(properties);
            } else {
                // No specific page requested, scrape multiple pages up to MAX_PAGES
                String baseSearchUrl = urlBuilder.toString();
                log.info("Built base search URL for Tayara (will scrape multiple pages): {}", baseSearchUrl);
                
                return scrapeMultiplePages(baseSearchUrl);
            }
            
        } catch (Exception e) {
            log.error("Error occurred while running parametrized Tayara search: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred during scraping: " + e.getMessage());
        }
    }
    
    /**
     * Helper method to scrape multiple pages, up to MAX_PAGES
     */
    private ResponseEntity<?> scrapeMultiplePages(String baseUrl) {
        log.info("Starting multi-page scraping with base URL: {}", baseUrl);
        
        List<TayaraProperty> allProperties = new ArrayList<>();
        int totalPages = 0;
        boolean hasMorePages = true;
        
        for (int currentPage = 1; currentPage <= MAX_PAGES && hasMorePages; currentPage++) {
            // Create the URL for the current page
            String pageUrl;
            
            // Check if the baseUrl already contains a page parameter
            if (baseUrl.contains("page=")) {
                // Remove existing page parameter and add the new one
                pageUrl = baseUrl.replaceAll("page=\\d+", "page=" + currentPage);
                log.info("URL with existing page parameter. Modified to: {}", pageUrl);
            } else {
                // Add the page parameter appropriately based on whether there are other parameters
                if (baseUrl.contains("?")) {
                    pageUrl = baseUrl + "&page=" + currentPage;
                } else {
                    pageUrl = baseUrl + "?page=" + currentPage;
                }
                log.info("Added page parameter to URL: {}", pageUrl);
            }
            
            log.info("Scraping page {} with URL: {}", currentPage, pageUrl);
            
            try {
                List<TayaraProperty> pageProperties = tayaraScraper.scrape(pageUrl);
                
                if (pageProperties == null || pageProperties.isEmpty()) {
                    log.info("No more properties found on page {}. Stopping pagination.", currentPage);
                    hasMorePages = false;
                } else {
                    log.info("Found {} properties on page {}. Total properties so far: {}", 
                            pageProperties.size(), currentPage, allProperties.size() + pageProperties.size());
                    allProperties.addAll(pageProperties);
                    totalPages = currentPage;
                }
                
                // Check if we've reached the maximum pages limit
                if (currentPage >= MAX_PAGES) {
                    log.info("Reached maximum page limit of {}. Stopping pagination.", MAX_PAGES);
                    hasMorePages = false;
                }
                
                // Small delay to avoid overwhelming the site
                if (hasMorePages) {
                    log.info("Waiting 1 second before proceeding to next page...");
                    Thread.sleep(1000);
                }
                
            } catch (Exception e) {
                log.error("Error scraping page {}: {}", currentPage, e.getMessage(), e);
                hasMorePages = false;
            }
        }
        
        log.info("Multi-page scraping completed. Scraped {} pages, collected {} properties total.",
                totalPages, allProperties.size());
        
        return processScrapingResults(allProperties);
    }
    
    private ResponseEntity<?> processScrapingResults(List<TayaraProperty> properties) {
        if (properties != null && !properties.isEmpty()) {
            log.info("Tayara scraper returned {} properties.", properties.size());
            // Map and save
            List<PropertyListing> listings = new java.util.ArrayList<>();
            for (TayaraProperty tp : properties) {
                PropertyListing pl = Converter.toPropertyListing(tp);
                if (pl != null) {
                    listings.add(pl);
                }
            }
            propertyListingRepository.saveAll(listings);
            return ResponseEntity.ok("Scraping finished. " + listings.size() + 
                                    " properties saved to the database from " + 
                                    (properties.size() > listings.size() ? 
                                     "original " + properties.size() + " scraped properties." : 
                                     "scraping."));
        } else {
            log.warn("Tayara scraper returned no properties or an empty list.");
            return ResponseEntity.ok("Scraping finished. No properties found or list was empty.");
        }
    }
} 