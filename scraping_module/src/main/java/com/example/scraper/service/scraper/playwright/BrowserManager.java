package com.example.scraper.service.scraper.playwright;

import com.microsoft.playwright.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class BrowserManager {
    private static final Logger log = LoggerFactory.getLogger(BrowserManager.class);
    
    private Playwright playwright;
    private Browser browser;
    private final AtomicBoolean initialized = new AtomicBoolean(false);
    
    public synchronized BrowserContext createContext() {
        if (!initialized.get()) {
            initialize();
        }
        
        return browser.newContext(new Browser.NewContextOptions()
                .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                .setViewportSize(1920, 1080)
                .setIgnoreHTTPSErrors(true));
    }
    
    private void initialize() {
        try {
            log.info("Initializing Playwright and browser");
            playwright = Playwright.create();
            browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                    .setHeadless(true)
                    .setSlowMo(50));
            initialized.set(true);
            log.info("Playwright and browser initialized successfully");
        } catch (Exception e) {
            log.error("Failed to initialize Playwright and browser", e);
            throw new RuntimeException("Failed to initialize browser", e);
        }
    }
    
    @PreDestroy
    public synchronized void close() {
        if (initialized.get()) {
            log.info("Closing browser and Playwright resources");
            try {
                if (browser != null) {
                    browser.close();
                }
                if (playwright != null) {
                    playwright.close();
                }
                initialized.set(false);
                log.info("Browser and Playwright resources closed successfully");
            } catch (Exception e) {
                log.error("Error closing browser resources", e);
            }
        }
    }
    
    public boolean isInitialized() {
        return initialized.get();
    }
} 