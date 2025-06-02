package com.example.spring_security.service;

/**
 * Custom exception to indicate that the Scraper service is unavailable.
 */
public class ScraperServiceUnavailableException extends RuntimeException {
    public ScraperServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }

    public ScraperServiceUnavailableException(String message) {
        super(message);
    }
} 