package com.example.spring_security.config;

import com.example.spring_security.service.ScrapingClientService;
import com.example.spring_security.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableScheduling
public class SchedulingConfig {
    
    private static final Logger log = LoggerFactory.getLogger(SchedulingConfig.class);
    
    private final UserService userService;
    private final ScrapingClientService scrapingClientService;
    
    @Autowired
    public SchedulingConfig(UserService userService, ScrapingClientService scrapingClientService) {
        this.userService = userService;
        this.scrapingClientService = scrapingClientService;
    }
    

    @Scheduled(cron = "0 0 0 * * ?")
    public void cleanupExpiredTokens() {
        log.info("Running scheduled task: cleanupExpiredTokens");
        userService.cleanExpiredPasswordResetTokens();
        log.info("Scheduled task completed: cleanupExpiredTokens");
    }

    /**
     Scheduling for mobilier scraper
     */
    @Scheduled(cron = "0 0 2 ? * SUN")
    @ConditionalOnProperty(name = "scraper.scheduling.enabled", havingValue = "true", matchIfMissing = false)
    public void scheduleImmobilierScrape() {
        log.info("Running scheduled task: Triggering Immobilier Scraper");
        try {
            scrapingClientService.triggerImmobilierScrape();
            log.info("Scheduled task completed: Immobilier Scraper trigger request sent.");
        } catch (Exception e) {
            log.error("Scheduled task failed: Error triggering Immobilier scraper.", e);
        }
    }

}