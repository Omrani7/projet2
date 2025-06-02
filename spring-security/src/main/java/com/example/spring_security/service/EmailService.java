package com.example.spring_security.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;

@Service
public class EmailService {
    
    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);
    
    private final JavaMailSender mailSender;
    
    @Value("${app.frontend-url:http://localhost:4200}")
    private String frontendUrl;
    
    @Value("${spring.mail.username:no-reply@tunitoit.com}")
    private String fromEmail;
    
    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }
    
    public void sendPasswordResetEmail(String to, String token) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject("Password Reset Request");
            
            String resetUrl = frontendUrl + "/auth/reset-password?token=" + token;
            
            message.setText("Hello,\n\n" +
                    "You have requested to reset your password. Please click on the link below to reset your password:\n\n" +
                    resetUrl + "\n\n" +
                    "If you did not request this, please ignore this email and your password will remain unchanged.\n\n" +
                    "Regards,\nUniNest Team");
            
            mailSender.send(message);
            logger.info("Password reset email sent to: {}", to);
        } catch (Exception e) {
            logger.error("Failed to send password reset email to {}: {}", to, e.getMessage());
        }
    }
    
    // Method for development/testing where email sending might not be configured
    public void logPasswordResetLink(String to, String token) {
        String resetUrl = frontendUrl + "/auth/reset-password?token=" + token;
        // Add console output for more visibility
        System.out.println("=========================================");
        System.out.println("PASSWORD RESET LINK:");
        System.out.println(resetUrl);
        System.out.println("For user: " + to);
        System.out.println("=========================================");
        
        logger.info("Password reset link for {}: {}", to, resetUrl);
    }
    
    /**
     * Send a generic email
     * @param to recipient email address
     * @param subject email subject
     * @param body email body text
     */
    @Async
    public void sendEmail(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            
            mailSender.send(message);
            logger.info("Email sent successfully to: {} with subject: {}", to, subject);
        } catch (Exception e) {
            logger.error("Failed to send email to {}: {}", to, e.getMessage());
            // For development, log the email content
            logEmail(to, subject, body);
        }
    }
    
    /**
     * Log email content for development/testing
     */
    private void logEmail(String to, String subject, String body) {
        System.out.println("=========================================");
        System.out.println("EMAIL NOTIFICATION:");
        System.out.println("To: " + to);
        System.out.println("Subject: " + subject);
        System.out.println("Body:");
        System.out.println(body);
        System.out.println("=========================================");
        
        logger.info("Email logged - To: {}, Subject: {}", to, subject);
    }
} 