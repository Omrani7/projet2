package com.example.spring_security.controller;

import com.example.spring_security.dto.*;
import com.example.spring_security.model.UserPrincipal;
import com.example.spring_security.service.RoommateService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * REST controller for managing roommate announcements and applications
 */
@RestController
@RequestMapping("/api/v1/roommates")
public class RoommateController {
    
    private static final Logger logger = LoggerFactory.getLogger(RoommateController.class);
    
    private final RoommateService roommateService;
    
    @Autowired
    public RoommateController(RoommateService roommateService) {
        this.roommateService = roommateService;
    }
    
    // ===== ROOMMATE ANNOUNCEMENT ENDPOINTS =====
    
    /**
     * Create a new roommate announcement
     * POST /api/v1/roommates/announcements
     */
    @PostMapping("/announcements")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<RoommateAnnouncementDTO> createAnnouncement(
            @Valid @RequestBody RoommateAnnouncementCreateDTO createDTO,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        
        logger.info("Creating roommate announcement by user {}", currentUser.getId());
        
        RoommateAnnouncementDTO createdAnnouncement = roommateService.createAnnouncement(createDTO, currentUser);
        
        URI location = URI.create(String.format("/api/v1/roommates/announcements/%s", createdAnnouncement.getId()));
        return ResponseEntity.created(location).body(createdAnnouncement);
    }
    
    /**
     * Get announcements for browsing (excluding current user's announcements)
     * GET /api/v1/roommates/announcements
     */
    @GetMapping("/announcements")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<Page<RoommateAnnouncementDTO>> getAnnouncementsForBrowsing(
            @AuthenticationPrincipal UserPrincipal currentUser,
            Pageable pageable) {
        
        logger.info("Fetching announcements for browsing by user {}", currentUser.getId());
        
        Page<RoommateAnnouncementDTO> announcements = roommateService.getAnnouncementsForBrowsing(currentUser, pageable);
        
        return ResponseEntity.ok(announcements);
    }
    
    /**
     * Get announcements posted by the current user
     * GET /api/v1/roommates/announcements/my
     */
    @GetMapping("/announcements/my")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<Page<RoommateAnnouncementDTO>> getMyAnnouncements(
            @AuthenticationPrincipal UserPrincipal currentUser,
            Pageable pageable) {
        
        logger.info("Fetching announcements for user {}", currentUser.getId());
        
        Page<RoommateAnnouncementDTO> announcements = roommateService.getMyAnnouncements(currentUser, pageable);
        
        return ResponseEntity.ok(announcements);
    }
    
    /**
     * Get announcement by ID with details
     * GET /api/v1/roommates/announcements/{id}
     */
    @GetMapping("/announcements/{id}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<RoommateAnnouncementDTO> getAnnouncementById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        
        logger.info("Fetching announcement {} for user {}", id, currentUser.getId());
        
        RoommateAnnouncementDTO announcement = roommateService.getAnnouncementById(id, currentUser);
        
        return ResponseEntity.ok(announcement);
    }
    
    // ===== ROOMMATE APPLICATION ENDPOINTS =====
    
    /**
     * Apply to a roommate announcement
     * POST /api/v1/roommates/applications
     */
    @PostMapping("/applications")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<RoommateApplicationDTO> applyToAnnouncement(
            @Valid @RequestBody RoommateApplicationCreateDTO createDTO,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        
        logger.info("User {} applying to announcement {}", currentUser.getId(), createDTO.getAnnouncementId());
        
        RoommateApplicationDTO createdApplication = roommateService.applyToAnnouncement(createDTO, currentUser);
        
        URI location = URI.create(String.format("/api/v1/roommates/applications/%s", createdApplication.getId()));
        return ResponseEntity.created(location).body(createdApplication);
    }
    
    /**
     * Get applications received for user's announcements
     * GET /api/v1/roommates/applications/received
     */
    @GetMapping("/applications/received")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<Page<RoommateApplicationDTO>> getReceivedApplications(
            @RequestParam Long announcementId,
            @AuthenticationPrincipal UserPrincipal currentUser,
            Pageable pageable) {
        
        logger.info("Fetching received applications for announcement {} by user {}", announcementId, currentUser.getId());
        
        Page<RoommateApplicationDTO> applications = roommateService.getApplicationsForAnnouncement(
                announcementId, currentUser, pageable);
        
        return ResponseEntity.ok(applications);
    }
    
    /**
     * Get applications sent by the current user
     * GET /api/v1/roommates/applications/sent
     */
    @GetMapping("/applications/sent")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<Page<RoommateApplicationDTO>> getSentApplications(
            @AuthenticationPrincipal UserPrincipal currentUser,
            Pageable pageable) {
        
        logger.info("Fetching sent applications for user {}", currentUser.getId());
        
        Page<RoommateApplicationDTO> applications = roommateService.getMyApplications(currentUser, pageable);
        
        return ResponseEntity.ok(applications);
    }
    
    /**
     * Respond to an application (accept/reject)
     * PUT /api/v1/roommates/applications/{id}/respond
     */
    @PutMapping("/applications/{id}/respond")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<RoommateApplicationDTO> respondToApplication(
            @PathVariable Long id,
            @Valid @RequestBody RoommateApplicationResponseDTO responseDTO,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        
        logger.info("User {} responding to application {}", currentUser.getId(), id);
        
        RoommateApplicationDTO updatedApplication = roommateService.respondToApplication(id, responseDTO, currentUser);
        
        return ResponseEntity.ok(updatedApplication);
    }
    
    // ===== CLOSED DEALS ENDPOINTS =====
    
    /**
     * Get closed deals for student to create Type A announcements
     * GET /api/v1/roommates/closed-deals
     */
    @GetMapping("/closed-deals")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<Page<InquiryDTO>> getClosedDealsForStudent(
            @AuthenticationPrincipal UserPrincipal currentUser,
            Pageable pageable) {
        
        logger.info("Fetching closed deals for student {}", currentUser.getId());
        
        Page<InquiryDTO> closedDeals = roommateService.getClosedDealsForStudent(currentUser, pageable);
        
        return ResponseEntity.ok(closedDeals);
    }
    
    // ===== UTILITY ENDPOINTS =====
    
    /**
     * Get application count for a specific announcement
     * GET /api/v1/roommates/announcements/{id}/application-count
     */
    @GetMapping("/announcements/{id}/application-count")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<Map<String, Long>> getApplicationCount(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        
        logger.info("Fetching application count for announcement {} by user {}", id, currentUser.getId());
        
        // This endpoint gets announcement details and extracts application count
        RoommateAnnouncementDTO announcement = roommateService.getAnnouncementById(id, currentUser);
        
        Map<String, Long> response = new HashMap<>();
        response.put("applicationCount", Long.valueOf(announcement.getApplicationCount()));
        response.put("remainingSpots", Long.valueOf(announcement.getRemainingSpots()));
        
        return ResponseEntity.ok(response);
    }
} 