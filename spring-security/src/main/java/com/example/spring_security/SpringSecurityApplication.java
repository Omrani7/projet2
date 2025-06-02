package com.example.spring_security;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SpringBootApplication
@EnableScheduling
@EnableAsync
public class SpringSecurityApplication {
	
	private static final Logger logger = LoggerFactory.getLogger(SpringSecurityApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(SpringSecurityApplication.class, args);
	}
	
	@EventListener(ApplicationReadyEvent.class)
	public void logEmailConfig() {
		logger.info("Email configuration is loaded.");
		logger.info("If you're having trouble with emails, check these common issues:");
		logger.info("1. Make sure your Gmail account has 'Less secure app access' enabled or you're using an App Password");
		logger.info("2. Verify your email and password are correct in application-dev.properties");
		logger.info("3. Check that your Gmail account doesn't have captcha or 2FA preventing programmatic access");
		logger.info("4. For development, password reset links will be logged to console if email sending fails");
	}
}
