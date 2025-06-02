package com.example.scraper;

import com.example.scraper.model.ImmobilierProperty;
import com.example.scraper.model.PropertyListing;
import com.example.scraper.repository.PropertyListingRepository;
import com.example.scraper.service.scraper.playwright.Converter;
import com.example.scraper.service.scraper.playwright.immobilier.ImmobilierScraper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Profile("cli")
public class ScraperCommandLineRunner implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(ScraperCommandLineRunner.class);
    
    @Autowired
    private ImmobilierScraper immobilierScraper;
    
    @Autowired
    private PropertyListingRepository propertyRepository;
    
    @Override
    public void run(String... args) {
        log.info("Starting CLI scraper run");
        
        try {
            // Run Immobilier scraper
            log.info("Running Immobilier scraper");
            List<ImmobilierProperty> properties = immobilierScraper.scrape(null);
            
            List<PropertyListing> listings = new ArrayList<>();
            
            // Convert each property to a listing
            for (ImmobilierProperty property : properties) {
                PropertyListing listing = Converter.toPropertyListing(property);
                if (listing != null) {
                    listings.add(listing);
                }
            }
            
            // Save all listings to database
            log.info("Saving {} property listings to database", listings.size());
            propertyRepository.saveAll(listings);
            
            log.info("Scraping completed successfully");
        } catch (Exception e) {
            log.error("Error during CLI scraping: {}", e.getMessage(), e);
        } finally {
            // Exit after completion - only in CLI mode
            log.info("Exiting CLI mode");
            System.exit(0);
        }
    }
} 