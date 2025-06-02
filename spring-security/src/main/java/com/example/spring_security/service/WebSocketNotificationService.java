package com.example.spring_security.service;

import com.example.spring_security.dto.InquiryDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Service for sending real-time notifications via WebSocket
 */
@Service
public class WebSocketNotificationService {
    
    private static final Logger logger = LoggerFactory.getLogger(WebSocketNotificationService.class);
    
    private final SimpMessagingTemplate messagingTemplate;
    
    @Autowired
    public WebSocketNotificationService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }
    
    /**
     * Notify owner of a new inquiry on their property
     * @param ownerUserId The ID of the owner to notify
     * @param inquiry The inquiry details
     */
    public void notifyOwnerOfNewInquiry(Long ownerUserId, InquiryDTO inquiry) {
        try {
            String destination = "/user/" + ownerUserId + "/queue/inquiries";
            
            Map<String, Object> notification = new HashMap<>();
            notification.put("type", "NEW_INQUIRY");
            notification.put("inquiry", inquiry);
            notification.put("message", "New inquiry on your property: " + inquiry.getProperty().getTitle());
            
            messagingTemplate.convertAndSend(destination, notification);
            
            logger.info("Sent new inquiry notification to owner {} for property {}", 
                       ownerUserId, inquiry.getProperty().getTitle());
        } catch (Exception e) {
            logger.error("Failed to send WebSocket notification to owner {}: {}", ownerUserId, e.getMessage());
        }
    }
    
    /**
     * Notify student that owner has replied to their inquiry
     * @param studentUserId The ID of the student to notify
     * @param inquiry The inquiry details with reply
     */
    public void notifyStudentOfReply(Long studentUserId, InquiryDTO inquiry) {
        try {
            String destination = "/user/" + studentUserId + "/queue/inquiries";
            
            Map<String, Object> notification = new HashMap<>();
            notification.put("type", "INQUIRY_REPLY");
            notification.put("inquiry", inquiry);
            notification.put("message", "Owner replied to your inquiry for: " + inquiry.getProperty().getTitle());
            
            messagingTemplate.convertAndSend(destination, notification);
            
            logger.info("Sent reply notification to student {} for inquiry {}", 
                       studentUserId, inquiry.getId());
        } catch (Exception e) {
            logger.error("Failed to send WebSocket notification to student {}: {}", studentUserId, e.getMessage());
        }
    }
    
    /**
     * Notify student that a property is no longer available
     * @param studentUserId The ID of the student to notify
     * @param inquiry The inquiry details with updated status
     */
    public void notifyStudentOfPropertyUnavailable(Long studentUserId, InquiryDTO inquiry) {
        try {
            String destination = "/user/" + studentUserId + "/queue/inquiries";
            
            Map<String, Object> notification = new HashMap<>();
            notification.put("type", "PROPERTY_NO_LONGER_AVAILABLE");
            notification.put("inquiry", inquiry);
            notification.put("message", "Property is no longer available: " + inquiry.getProperty().getTitle());
            
            messagingTemplate.convertAndSend(destination, notification);
            
            logger.info("Sent property unavailable notification to student {} for inquiry {}", 
                       studentUserId, inquiry.getId());
        } catch (Exception e) {
            logger.error("Failed to send WebSocket property unavailable notification to student {}: {}", 
                        studentUserId, e.getMessage());
        }
    }
    
    /**
     * Send a general notification to a user
     * @param userId The ID of the user to notify
     * @param type The type of notification
     * @param message The notification message
     * @param data Additional data to send with the notification
     */
    public void sendNotification(Long userId, String type, String message, Object data) {
        try {
            String destination = "/user/" + userId + "/queue/notifications";
            
            Map<String, Object> notification = new HashMap<>();
            notification.put("type", type);
            notification.put("message", message);
            notification.put("timestamp", System.currentTimeMillis());
            if (data != null) {
                notification.put("data", data);
            }
            
            messagingTemplate.convertAndSend(destination, notification);
            
            logger.debug("Sent {} notification to user {}", type, userId);
        } catch (Exception e) {
            logger.error("Failed to send notification to user {}: {}", userId, e.getMessage());
        }
    }
    
    // ========== ROOMMATE NOTIFICATION METHODS ==========
    
    /**
     * Notify announcement poster of a new roommate application with ML compatibility score
     * @param posterId The ID of the announcement poster to notify
     * @param application The roommate application details
     * @param compatibilityScore ML-calculated compatibility score
     */
    public void notifyOfNewRoommateApplication(Integer posterId, Object application, Double compatibilityScore) {
        try {
            String destination = "/user/" + posterId + "/queue/notifications";
            
            // Create enhanced notification with ML score
            String compatibilityLevel = getCompatibilityLevel(compatibilityScore);
            String message = String.format("New roommate application with %s compatibility (%d%%)", 
                                          compatibilityLevel, (int)(compatibilityScore * 100));
            
            Map<String, Object> notification = new HashMap<>();
            notification.put("type", "NEW_ROOMMATE_APPLICATION");
            notification.put("roommateApplication", application);
            notification.put("message", message);
            notification.put("compatibilityScore", compatibilityScore);
            notification.put("compatibilityLevel", compatibilityLevel);
            notification.put("timestamp", System.currentTimeMillis());
            
            messagingTemplate.convertAndSend(destination, notification);
            
            logger.info("Sent new roommate application notification to user {} with {}% compatibility", 
                       posterId, (int)(compatibilityScore * 100));
        } catch (Exception e) {
            logger.error("Failed to send roommate application notification to user {}: {}", posterId, e.getMessage());
        }
    }
    
    /**
     * Notify applicant of response to their roommate application
     * @param applicantId The ID of the applicant to notify
     * @param application The roommate application details with response
     * @param accepted Whether the application was accepted or rejected
     */
    public void notifyOfRoommateApplicationResponse(Integer applicantId, Object application, boolean accepted) {
        try {
            String destination = "/user/" + applicantId + "/queue/notifications";
            
            String message = accepted ? 
                "Great news! Your roommate application has been accepted!" :
                "Your roommate application has been declined. Keep looking for other matches!";
            
            Map<String, Object> notification = new HashMap<>();
            notification.put("type", "ROOMMATE_APPLICATION_RESPONSE");
            notification.put("roommateApplication", application);
            notification.put("message", message);
            notification.put("accepted", accepted);
            notification.put("timestamp", System.currentTimeMillis());
            
            messagingTemplate.convertAndSend(destination, notification);
            
            logger.info("Sent roommate application {} notification to user {}", 
                       accepted ? "accepted" : "rejected", applicantId);
        } catch (Exception e) {
            logger.error("Failed to send roommate application response notification to user {}: {}", 
                        applicantId, e.getMessage());
        }
    }
    
    /**
     * Notify user of a high compatibility roommate match found by ML algorithm
     * @param userId The ID of the user to notify
     * @param matchData The matching roommate/announcement data
     * @param compatibilityScore ML-calculated compatibility score
     */
    public void notifyOfHighCompatibilityMatch(Integer userId, Object matchData, Double compatibilityScore) {
        try {
            String destination = "/user/" + userId + "/queue/notifications";
            
            if (compatibilityScore >= 0.8) { // Only send for high compatibility matches
                String compatibilityLevel = getCompatibilityLevel(compatibilityScore);
                String message = String.format("🎯 %s roommate match found! %d%% compatibility", 
                                              compatibilityLevel, (int)(compatibilityScore * 100));
                
                Map<String, Object> notification = new HashMap<>();
                notification.put("type", "ROOMMATE_MATCH_FOUND");
                notification.put("matchData", matchData);
                notification.put("message", message);
                notification.put("compatibilityScore", compatibilityScore);
                notification.put("compatibilityLevel", compatibilityLevel);
                notification.put("timestamp", System.currentTimeMillis());
                
                messagingTemplate.convertAndSend(destination, notification);
                
                logger.info("Sent high compatibility match notification to user {} with {}% compatibility", 
                           userId, (int)(compatibilityScore * 100));
            }
        } catch (Exception e) {
            logger.error("Failed to send high compatibility match notification to user {}: {}", userId, e.getMessage());
        }
    }
    
    /**
     * Get compatibility level description from score
     */
    private String getCompatibilityLevel(Double score) {
        if (score == null) return "Unknown";
        
        if (score >= 0.90) return "Excellent";
        if (score >= 0.75) return "Very Good";
        if (score >= 0.60) return "Good";
        if (score >= 0.40) return "Fair";
        return "Poor";
    }
} 