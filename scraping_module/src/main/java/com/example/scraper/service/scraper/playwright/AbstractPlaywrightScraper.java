package com.example.scraper.service.scraper.playwright;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public abstract class AbstractPlaywrightScraper<T> implements PlaywrightScraper<T> {
    private static final Logger log = LoggerFactory.getLogger(AbstractPlaywrightScraper.class);
    
    @Autowired
    protected BrowserManager browserManager;
    
    @Autowired
    @Qualifier("playwrightScraperExecutor")
    protected ThreadPoolTaskExecutor taskExecutor;
    
    @Override
    public List<T> scrape(String url) {
        log.info("Starting scraping process for URL: {} with scraper: {}", url, getScraperName());
        
        try (BrowserContext context = browserManager.createContext();
             Page page = context.newPage()) {
             
            log.info("Navigating to URL: {}", url);
            page.navigate(url);
            waitForPageLoad(page);
            
            List<T> results = extractData(page);
            log.info("Scraping completed for URL: {}. Extracted {} items", url, results.size());
            
            return results;
        } catch (Exception e) {
            log.error("Error during scraping with {}: {}", getScraperName(), e.getMessage(), e);
            return Collections.emptyList();
        }
    }
    
    @Override
    public CompletableFuture<List<T>> scrapeAsync(String url) {
        return CompletableFuture.supplyAsync(() -> scrape(url), taskExecutor);
    }
    
    @Override
    public void close() {
        log.info("Closing resources for {}", getScraperName());
    }
    
    protected void waitForPageLoad(Page page) {
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);
        page.waitForLoadState(LoadState.NETWORKIDLE);
    }

    protected abstract List<T> extractData(Page page);
} 