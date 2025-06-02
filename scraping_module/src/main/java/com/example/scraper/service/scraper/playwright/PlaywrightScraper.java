package com.example.scraper.service.scraper.playwright;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Interface for scrapers using Playwright automation framework.
 * 
 * @param <T> the type of data being scraped
 */
public interface PlaywrightScraper<T> extends AutoCloseable {
    
    /**
     * Scrapes data from the provided URL synchronously.
     * 
     * @param url the URL to scrape
     * @return List of scraped items
     */
    List<T> scrape(String url);
    
    /**
     * Scrapes data from the provided URL asynchronously.
     * 
     * @param url the URL to scrape
     * @return CompletableFuture containing the list of scraped items
     */
    CompletableFuture<List<T>> scrapeAsync(String url);
    
    /**
     * Gets the name of this scraper implementation.
     * Useful for logging and diagnostics.
     * 
     * @return the name of the scraper
     */
    String getScraperName();
    
    /**
     * Closes resources used by this scraper.
     * This method should be idempotent.
     */
    @Override
    void close();
} 