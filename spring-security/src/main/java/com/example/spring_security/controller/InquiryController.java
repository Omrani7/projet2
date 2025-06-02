package com.example.spring_security.controller;

import com.example.spring_security.dto.*;
import com.example.spring_security.model.InquiryStatus;
import com.example.spring_security.model.UserPrincipal;
import com.example.spring_security.service.InquiryService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * REST controller for managing property inquiries
 */
@RestController
@RequestMapping("/api/v1/inquiries")
public class InquiryController {
    
    private static final Logger logger = LoggerFactory.getLogger(InquiryController.class);
    
    private final InquiryService inquiryService;
    
    @Autowired
    public InquiryController(InquiryService inquiryService) {
        this.inquiryService = inquiryService;
    }
    
    /**
     * Create a new inquiry
     * POST /api/v1/inquiries
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<InquiryDTO> createInquiry(
            @Valid @RequestBody InquiryCreateDTO createDTO,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        
        logger.info("Creating inquiry for property {} by user {}", createDTO.getPropertyId(), currentUser.getId());
        
        InquiryDTO createdInquiry = inquiryService.createInquiry(createDTO, currentUser);
        
        URI location = URI.create(String.format("/api/v1/inquiries/%s", createdInquiry.getId()));
        return ResponseEntity.created(location).body(createdInquiry);
    }
    
    /**
     * Get inquiries for the authenticated owner
     * GET /api/v1/inquiries/owner
     */
    @GetMapping("/owner")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<Page<InquiryDTO>> getOwnerInquiries(
            @AuthenticationPrincipal UserPrincipal currentUser,
            Pageable pageable) {
        
        logger.info("Fetching inquiries for owner {}", currentUser.getId());
        
        Page<InquiryDTO> inquiries = inquiryService.getInquiriesForOwner(
                (long) currentUser.getId(), currentUser, pageable);
        
        return ResponseEntity.ok(inquiries);
    }
    
    /**
     * Get inquiries for the authenticated student
     * GET /api/v1/inquiries/student
     */
    @GetMapping("/student")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<InquiryDTO>> getStudentInquiries(
            @AuthenticationPrincipal UserPrincipal currentUser,
            Pageable pageable) {
        
        logger.info("Fetching inquiries for student {}", currentUser.getId());
        
        Page<InquiryDTO> inquiries = inquiryService.getInquiriesForStudent(
                (long) currentUser.getId(), currentUser, pageable);
        
        return ResponseEntity.ok(inquiries);
    }
    
    /**
     * Get closed deals for the authenticated student
     * GET /api/v1/inquiries/student/closed-deals
     */
    @GetMapping("/student/closed-deals")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<Page<InquiryDTO>> getStudentClosedDeals(
            @AuthenticationPrincipal UserPrincipal currentUser,
            Pageable pageable) {
        
        logger.info("Fetching closed deals for student {}", currentUser.getId());
        
        Page<InquiryDTO> closedDeals = inquiryService.getClosedDealsForStudent(
                (long) currentUser.getId(), currentUser, pageable);
        
        return ResponseEntity.ok(closedDeals);
    }
    
    /**
     * Owner replies to an inquiry
     * PUT /api/v1/inquiries/{inquiryId}/reply
     */
    @PutMapping("/{inquiryId}/reply")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<InquiryDTO> replyToInquiry(
            @PathVariable Long inquiryId,
            @Valid @RequestBody InquiryReplyDTO replyDTO,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        
        logger.info("Owner {} replying to inquiry {}", currentUser.getId(), inquiryId);
        
        InquiryDTO updatedInquiry = inquiryService.replyToInquiry(inquiryId, replyDTO, currentUser);
        
        return ResponseEntity.ok(updatedInquiry);
    }
    
    /**
     * Update inquiry status (consider deferring)
     * PUT /api/v1/inquiries/{inquiryId}/status
     */
    @PutMapping("/{inquiryId}/status")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<InquiryDTO> updateInquiryStatus(
            @PathVariable Long inquiryId,
            @RequestParam InquiryStatus status,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        
        logger.info("Updating inquiry {} status to {}", inquiryId, status);
        
        InquiryDTO updatedInquiry = inquiryService.markInquiryStatus(inquiryId, status, currentUser);
        
        return ResponseEntity.ok(updatedInquiry);
    }
    
    /**
     * Close deal with a specific student (NEW ENHANCED FEATURE)
     * This will mark the specified inquiry as CLOSED and automatically notify 
     * all other students with pending inquiries that the property is no longer available
     * POST /api/v1/inquiries/{inquiryId}/close-deal
     */
    @PostMapping("/{inquiryId}/close-deal")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<InquiryDTO> closeDealWithStudent(
            @PathVariable Long inquiryId,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        
        logger.info("Owner {} closing deal with inquiry {}", currentUser.getId(), inquiryId);
        
        InquiryDTO closedDeal = inquiryService.closeDealWithStudent(inquiryId, currentUser);
        
        return ResponseEntity.ok(closedDeal);
    }
    
    /**
     * Get unread inquiry count for owner
     * GET /api/v1/inquiries/owner/unread-count
     */
    @GetMapping("/owner/unread-count")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<Map<String, Long>> getUnreadInquiryCount(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        
        long count = inquiryService.getUnreadInquiryCount(
                (long) currentUser.getId(), currentUser);
        
        Map<String, Long> response = new HashMap<>();
        response.put("unreadCount", count);
        
        return ResponseEntity.ok(response);
    }
} 