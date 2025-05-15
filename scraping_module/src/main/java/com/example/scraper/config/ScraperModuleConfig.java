package com.example.scraper.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
public class ScraperModuleConfig {
    
    @Bean
    @Profile("scraper")
    public boolean enableScraper() {
        return true;
    }
} 