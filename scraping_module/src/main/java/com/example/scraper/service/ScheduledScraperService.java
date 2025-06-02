package com.example.scraper.service;

import com.example.scraper.model.ImmobilierProperty;
import com.example.scraper.model.PropertyListing;
import com.example.scraper.repository.PropertyListingRepository;
import com.example.scraper.service.scraper.playwright.Converter;
import com.example.scraper.service.scraper.playwright.immobilier.ImmobilierScraper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Profile("scheduled")
public class ScheduledScraperService {
    private static final Logger log = LoggerFactory.getLogger(ScheduledScraperService.class);
    
    @Autowired
    private ImmobilierScraper immobilierScraper;
    
    @Autowired
    private PropertyListingRepository propertyRepository;
    
    @Scheduled(cron = "${scraper.schedule.cron}")
    public void runScheduledScraping() {
        log.info("Running scheduled scraping task");
        
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
            
            log.info("Scheduled scraping completed successfully");
        } catch (Exception e) {
            log.error("Error during scheduled scraping: {}", e.getMessage(), e);
        }
    }
} 