package com.example.spring_security.service;

import com.example.spring_security.dao.InquiryRepository;
import com.example.spring_security.dao.PropertyListingRepository;
import com.example.spring_security.dao.UserRepo;
import com.example.spring_security.dto.*;
import com.example.spring_security.exception.ResourceNotFoundException;
import com.example.spring_security.exception.UnauthorizedAccessException;
import com.example.spring_security.model.*;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing property inquiries between students and owners
 */
@Service
@Transactional(readOnly = true)
public class InquiryService {
    
    private static final Logger logger = LoggerFactory.getLogger(InquiryService.class);
    
    private final InquiryRepository inquiryRepository;
    private final PropertyListingRepository propertyListingRepository;
    private final UserRepo userRepository;
    private final EmailService emailService;
    private final ModelMapper modelMapper;
    private final WebSocketNotificationService webSocketNotificationService;
    
    @Autowired
    public InquiryService(InquiryRepository inquiryRepository,
                         PropertyListingRepository propertyListingRepository,
                         UserRepo userRepository,
                         EmailService emailService,
                         ModelMapper modelMapper,
                         WebSocketNotificationService webSocketNotificationService) {
        this.inquiryRepository = inquiryRepository;
        this.propertyListingRepository = propertyListingRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.modelMapper = modelMapper;
        this.webSocketNotificationService = webSocketNotificationService;
    }
    
    /**
     * Create a new inquiry from a student to a property owner
     */
    @Transactional
    public InquiryDTO createInquiry(InquiryCreateDTO createDTO, UserPrincipal currentUser) {
        logger.info("Creating inquiry for property {} by user {}", createDTO.getPropertyId(), currentUser.getId());
        
        // Fetch current user
        User student = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        // Validate that the user has STUDENT role (or allow any authenticated user)
        // Note: Backend will handle role validation, frontend just needs authentication
        
        // Fetch property listing
        PropertyListing property = propertyListingRepository.findById(createDTO.getPropertyId())
                .orElseThrow(() -> new ResourceNotFoundException("Property not found"));
        
        // Ensure property is listed by an owner (not scraped)
        if (property.getSourceType() != PropertyListing.SourceType.OWNER) {
            throw new IllegalStateException("Inquiries can only be made for properties listed by owners");
        }
        
        // Ensure property has an owner
        if (property.getUser() == null) {
            throw new IllegalStateException("Property does not have an associated owner");
        }
        
        // Create new inquiry
        Inquiry inquiry = Inquiry.builder()
                .student(student)
                .owner(property.getUser())
                .property(property)
                .message(createDTO.getMessage())
                .studentPhoneNumber(createDTO.getPhoneNumber())
                .status(InquiryStatus.PENDING_REPLY)
                .build();
        
        inquiry = inquiryRepository.save(inquiry);
        
        // Send email notification to owner
        sendInquiryReceivedEmail(inquiry);
        
        // Send WebSocket notification to owner
        InquiryDTO inquiryDTO = convertToDTO(inquiry);
        webSocketNotificationService.notifyOwnerOfNewInquiry(
                Long.valueOf(inquiry.getOwner().getId()), inquiryDTO);
        
        return inquiryDTO;
    }
    
    /**
     * Get inquiries for the authenticated owner
     */
    public Page<InquiryDTO> getInquiriesForOwner(Long ownerId, UserPrincipal currentUser, Pageable pageable) {
        logger.info("Fetching inquiries for owner {}", ownerId);
        
        // Authorize: Ensure currentUser.id matches ownerId
        if (currentUser.getId() != ownerId.intValue()) {
            throw new AccessDeniedException("You can only view your own inquiries");
        }
        
        // Verify user has OWNER role
        User owner = userRepository.findById(ownerId.intValue())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        if (owner.getRole() != User.Role.OWNER) {
            throw new AccessDeniedException("Only property owners can access this resource");
        }
        
        Page<Inquiry> inquiries = inquiryRepository.findByOwnerIdOrderByTimestampDesc(ownerId, pageable);
        return inquiries.map(this::convertToDTO);
    }
    
    /**
     * Get inquiries for the authenticated student
     */
    public Page<InquiryDTO> getInquiriesForStudent(Long studentId, UserPrincipal currentUser, Pageable pageable) {
        logger.info("Fetching inquiries for student {}", studentId);
        
        // Authorize: Ensure currentUser.id matches studentId
        if (currentUser.getId() != studentId.intValue()) {
            throw new AccessDeniedException("You can only view your own inquiries");
        }
        
        Page<Inquiry> inquiries = inquiryRepository.findByStudentIdOrderByTimestampDesc(studentId, pageable);
        return inquiries.map(this::convertToDTO);
    }
    
    /**
     * Get closed deals for the authenticated student (inquiries with CLOSED status)
     */
    public Page<InquiryDTO> getClosedDealsForStudent(Long studentId, UserPrincipal currentUser, Pageable pageable) {
        logger.info("Fetching closed deals for student {}", studentId);
        
        // Authorize: Ensure currentUser.id matches studentId
        if (currentUser.getId() != studentId.intValue()) {
            throw new AccessDeniedException("You can only view your own closed deals");
        }
        
        Page<Inquiry> closedDeals = inquiryRepository.findByStudentIdAndStatusOrderByReplyTimestampDesc(
                studentId, InquiryStatus.CLOSED, pageable);
        return closedDeals.map(this::convertToDTO);
    }
    
    /**
     * Owner replies to an inquiry
     */
    @Transactional
    public InquiryDTO replyToInquiry(Long inquiryId, InquiryReplyDTO replyDTO, UserPrincipal currentUser) {
        logger.info("Owner {} replying to inquiry {}", currentUser.getId(), inquiryId);
        
        // Fetch inquiry with details
        Inquiry inquiry = inquiryRepository.findByIdWithDetails(inquiryId)
                .orElseThrow(() -> new ResourceNotFoundException("Inquiry not found"));
        
        // Authorize: Ensure currentUser is the owner of the property
        if (inquiry.getOwner().getId() != currentUser.getId()) {
            throw new AccessDeniedException("Only the property owner can reply to this inquiry");
        }
        
        // Update inquiry with reply
        inquiry.setReply(replyDTO.getReply());
        // setReply method automatically updates replyTimestamp and status
        
        // Set owner's phone number for direct contact
        // Try to get from User's phoneNumber or from their profile's contactNumber
        if (inquiry.getOwnerPhoneNumber() == null) {
            String ownerPhone = inquiry.getOwner().getPhoneNumber();
            // If not available from User, try from UserProfile contactNumber
            if (ownerPhone == null || ownerPhone.trim().isEmpty()) {
                if (inquiry.getOwner().getUserProfile() != null) {
                    ownerPhone = inquiry.getOwner().getUserProfile().getContactNumber();
                }
            }
            inquiry.setOwnerPhoneNumber(ownerPhone);
        }
        
        inquiry = inquiryRepository.save(inquiry);
        
        // Send email notification to student
        sendInquiryReplyEmail(inquiry);
        
        // Send WebSocket notification to student
        InquiryDTO inquiryDTO = convertToDTO(inquiry);
        webSocketNotificationService.notifyStudentOfReply(
                Long.valueOf(inquiry.getStudent().getId()), inquiryDTO);
        
        return inquiryDTO;
    }
    
    /**
     * Update inquiry status (for internal use or admin/moderation)
     * When status is set to CLOSED, also deactivates the property
     */
    @Transactional
    public InquiryDTO markInquiryStatus(Long inquiryId, InquiryStatus status, UserPrincipal currentUser) {
        logger.info("Updating inquiry {} status to {}", inquiryId, status);
        
        Inquiry inquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new ResourceNotFoundException("Inquiry not found"));
        
        // Authorize: Only owner or admin can update status
        if (inquiry.getOwner().getId() != currentUser.getId()) {
            throw new AccessDeniedException("You don't have permission to update this inquiry status");
        }
        
        inquiry.setStatus(status);
        inquiry = inquiryRepository.save(inquiry);
        
        // If marking as CLOSED, deactivate the property (deal is finalized)
        if (status == InquiryStatus.CLOSED) {
            PropertyListing property = inquiry.getProperty();
            if (property != null && property.isActive()) {
                property.setActive(false);
                propertyListingRepository.save(property);
                logger.info("Property {} has been deactivated due to closed inquiry {}", 
                           property.getId(), inquiryId);
                
                // Send notification emails about deal closure
                sendDealClosedNotifications(inquiry);
            }
        }
        
        return convertToDTO(inquiry);
    }
    
    /**
     * Get unread inquiry count for an owner
     */
    public long getUnreadInquiryCount(Long ownerId, UserPrincipal currentUser) {
        // Authorize
        if (currentUser.getId() != ownerId.intValue()) {
            throw new AccessDeniedException("You can only view your own inquiry count");
        }
        
        return inquiryRepository.countUnreadInquiriesForOwner(ownerId);
    }
    
    /**
     * Send email notification to owner when inquiry is received
     */
    private void sendInquiryReceivedEmail(Inquiry inquiry) {
        try {
            String subject = "New Inquiry for Your Property: " + inquiry.getProperty().getTitle();
            String body = String.format(
                "Hello %s,\n\n" +
                "You have received a new inquiry for your property '%s'.\n\n" +
                "From: %s\n" +
                "Email: %s\n" +
                "Phone: %s\n" +
                "Message: %s\n\n" +
                "Please log in to your dashboard to respond to this inquiry.\n\n" +
                "Best regards,\nUniNest Team",
                inquiry.getOwner().getUsername(),
                inquiry.getProperty().getTitle(),
                inquiry.getStudent().getUsername(),
                inquiry.getStudent().getEmail(),
                inquiry.getStudentPhoneNumber() != null ? inquiry.getStudentPhoneNumber() : "Not provided",
                inquiry.getMessage()
            );
            
            emailService.sendEmail(inquiry.getOwner().getEmail(), subject, body);
            
        } catch (Exception e) {
            logger.error("Failed to send inquiry received email: {}", e.getMessage());
        }
    }
    
    /**
     * Send email notification to student when owner replies
     */
    private void sendInquiryReplyEmail(Inquiry inquiry) {
        try {
            String subject = "Reply to Your Inquiry: " + inquiry.getProperty().getTitle();
            String body = String.format(
                "Hello %s,\n\n" +
                "The owner has replied to your inquiry for the property '%s'.\n\n" +
                "Owner's Reply: %s\n\n" +
                "Property Details:\n" +
                "- Location: %s\n" +
                "- Price: %s\n" +
                "- Owner Contact: %s\n\n" +
                "Please log in to view the full conversation and property details.\n\n" +
                "Best regards,\nUniNest Team",
                inquiry.getStudent().getUsername(),
                inquiry.getProperty().getTitle(),
                inquiry.getReply(),
                inquiry.getProperty().getLocation(),
                inquiry.getProperty().getPrice(),
                inquiry.getOwnerPhoneNumber() != null ? inquiry.getOwnerPhoneNumber() : "Contact via platform"
            );
            
            emailService.sendEmail(inquiry.getStudent().getEmail(), subject, body);
            
        } catch (Exception e) {
            logger.error("Failed to send inquiry reply email: {}", e.getMessage());
        }
    }
    
    /**
     * Send email notifications when a deal is closed
     */
    private void sendDealClosedNotifications(Inquiry inquiry) {
        try {
            // Email to student (congratulating on successful rental)
            String studentSubject = "🎉 Congratulations! Your Property Rental is Confirmed";
            String studentBody = String.format(
                "Hello %s,\n\n" +
                "Great news! Your rental application for '%s' has been confirmed.\n\n" +
                "Property Details:\n" +
                "- Location: %s\n" +
                "- Price: %s TND/month\n" +
                "- Owner: %s\n\n" +
                "You can now view this property in your 'Closed Deals' section in your profile.\n" +
                "In the future, you'll be able to recruit roommates for this property.\n\n" +
                "Congratulations on your new home!\n\n" +
                "Best regards,\nUniNest Team",
                inquiry.getStudent().getUsername(),
                inquiry.getProperty().getTitle(),
                inquiry.getProperty().getLocation(),
                inquiry.getProperty().getPrice(),
                inquiry.getOwner().getUsername()
            );
            
            emailService.sendEmail(inquiry.getStudent().getEmail(), studentSubject, studentBody);
            
            // Email to owner (confirming deal closure)
            String ownerSubject = "✅ Property Rental Confirmed - " + inquiry.getProperty().getTitle();
            String ownerBody = String.format(
                "Hello %s,\n\n" +
                "Your property '%s' has been successfully rented.\n\n" +
                "Tenant Details:\n" +
                "- Name: %s\n" +
                "- Email: %s\n\n" +
                "The property has been automatically removed from the discovery page.\n" +
                "Thank you for using UniNest!\n\n" +
                "Best regards,\nUniNest Team",
                inquiry.getOwner().getUsername(),
                inquiry.getProperty().getTitle(),
                inquiry.getStudent().getUsername(),
                inquiry.getStudent().getEmail()
            );
            
            emailService.sendEmail(inquiry.getOwner().getEmail(), ownerSubject, ownerBody);
            
        } catch (Exception e) {
            logger.error("Failed to send deal closed notification emails: {}", e.getMessage());
        }
    }
    
    /**
     * Send email notifications to students when property is no longer available
     */
    private void sendPropertyNoLongerAvailableNotifications(List<Inquiry> affectedInquiries, 
                                                          PropertyListing property, 
                                                          User winningStudent) {
        try {
            for (Inquiry inquiry : affectedInquiries) {
                String subject = "Property No Longer Available - " + property.getTitle();
                String body = String.format(
                    "Hello %s,\n\n" +
                    "We regret to inform you that the property '%s' is no longer available.\n\n" +
                    "Property Details:\n" +
                    "- Location: %s\n" +
                    "- Price: %s TND/month\n\n" +
                    "The property owner has closed a deal with another applicant. " +
                    "We understand this may be disappointing, but we encourage you to continue " +
                    "browsing our platform for other suitable properties.\n\n" +
                    "Thank you for your interest, and we hope to help you find your perfect home soon!\n\n" +
                    "Best regards,\nUniNest Team",
                    inquiry.getStudent().getUsername(),
                    property.getTitle(),
                    property.getLocation(),
                    property.getPrice()
                );
                
                emailService.sendEmail(inquiry.getStudent().getEmail(), subject, body);
                logger.info("Sent 'property no longer available' email to student {}", 
                           inquiry.getStudent().getUsername());
            }
        } catch (Exception e) {
            logger.error("Failed to send 'property no longer available' notification emails: {}", e.getMessage());
        }
    }

    /**
     * Send WebSocket notifications to students when property is no longer available
     */
    private void sendPropertyNoLongerAvailableWebSocketNotifications(List<Inquiry> affectedInquiries) {
        try {
            for (Inquiry inquiry : affectedInquiries) {
                // Create updated inquiry DTO with new status
                InquiryDTO inquiryDTO = convertToDTO(inquiry);
                inquiryDTO.setStatus(InquiryStatus.PROPERTY_NO_LONGER_AVAILABLE.name());
                
                // Send WebSocket notification
                webSocketNotificationService.notifyStudentOfPropertyUnavailable(
                        Long.valueOf(inquiry.getStudent().getId()), inquiryDTO);
                
                logger.info("Sent WebSocket 'property no longer available' notification to student {}", 
                           inquiry.getStudent().getUsername());
            }
        } catch (Exception e) {
            logger.error("Failed to send WebSocket 'property no longer available' notifications: {}", e.getMessage());
        }
    }
    
    /**
     * Convert Inquiry entity to InquiryDTO
     */
    private InquiryDTO convertToDTO(Inquiry inquiry) {
        // Create DTO manually to avoid ModelMapper conflicts with phone number fields
        InquiryDTO dto = InquiryDTO.builder()
                .id(inquiry.getId())
                .student(convertUserToBasicDTO(inquiry.getStudent()))
                .owner(convertUserToBasicDTO(inquiry.getOwner()))
                .property(convertPropertyToBasicDTO(inquiry.getProperty()))
                .message(inquiry.getMessage())
                .timestamp(inquiry.getTimestamp())
                .reply(inquiry.getReply())
                .replyTimestamp(inquiry.getReplyTimestamp())
                .status(inquiry.getStatus().name())
                .studentPhoneNumber(inquiry.getStudentPhoneNumber())
                .ownerPhoneNumber(inquiry.getOwnerPhoneNumber())
                .build();
        
        return dto;
    }
    
    /**
     * Convert User to UserBasicDTO
     */
    private UserBasicDTO convertUserToBasicDTO(User user) {
        return UserBasicDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .role(user.getRole().name())
                .build();
    }
    
    /**
     * Convert PropertyListing to PropertyListingBasicDTO
     */
    private PropertyListingBasicDTO convertPropertyToBasicDTO(PropertyListing property) {
        return PropertyListingBasicDTO.builder()
                .id(property.getId())
                .title(property.getTitle())
                .price(property.getPrice())
                .location(property.getLocation())
                .city(property.getCity())
                .propertyType(property.getPropertyType())
                .bedrooms(property.getBedrooms())
                .bathrooms(property.getBathrooms())
                .imageUrls(property.getImageUrls())
                .build();
    }
    
    /**
     * Close deal with a specific student and notify all other students 
     * that the property is no longer available
     */
    @Transactional
    public InquiryDTO closeDealWithStudent(Long inquiryId, UserPrincipal currentUser) {
        logger.info("Owner {} closing deal with inquiry {}", currentUser.getId(), inquiryId);
        
        // Fetch the winning inquiry with details
        Inquiry winningInquiry = inquiryRepository.findByIdWithDetails(inquiryId)
                .orElseThrow(() -> new ResourceNotFoundException("Inquiry not found"));
        
        // Authorize: Only the property owner can close deals
        if (winningInquiry.getOwner().getId() != currentUser.getId()) {
            throw new AccessDeniedException("Only the property owner can close deals for their properties");
        }
        
        // Validate current status (only PENDING_REPLY or REPLIED inquiries can be closed)
        if (winningInquiry.getStatus() != InquiryStatus.PENDING_REPLY && 
            winningInquiry.getStatus() != InquiryStatus.REPLIED) {
            throw new IllegalStateException("Only pending or replied inquiries can be closed as deals");
        }
        
        // Get the property
        PropertyListing property = winningInquiry.getProperty();
        
        // Find all other active inquiries for the same property (excluding the winning one)
        List<Inquiry> activeInquiries = inquiryRepository.findActiveInquiriesByPropertyId(property.getId());
        List<Inquiry> otherInquiries = activeInquiries.stream()
                .filter(inquiry -> !inquiry.getId().equals(inquiryId))
                .collect(Collectors.toList());
        
        // Mark the winning inquiry as CLOSED (successful deal)
        winningInquiry.setStatus(InquiryStatus.CLOSED);
        winningInquiry = inquiryRepository.save(winningInquiry);
        
        // Bulk update other inquiries to PROPERTY_NO_LONGER_AVAILABLE status
        if (!otherInquiries.isEmpty()) {
            List<Long> otherInquiryIds = otherInquiries.stream()
                    .map(Inquiry::getId)
                    .collect(Collectors.toList());
            
            int updatedCount = inquiryRepository.updateInquiryStatusByIds(
                    otherInquiryIds, InquiryStatus.PROPERTY_NO_LONGER_AVAILABLE);
            
            logger.info("Updated {} inquiries to PROPERTY_NO_LONGER_AVAILABLE for property {}", 
                       updatedCount, property.getId());
            
            // Send notification emails to affected students
            sendPropertyNoLongerAvailableNotifications(otherInquiries, property, winningInquiry.getStudent());
            
            // Send WebSocket notifications to affected students
            sendPropertyNoLongerAvailableWebSocketNotifications(otherInquiries);
        }
        
        // Deactivate the property (remove from search results)
        if (property.isActive()) {
            property.setActive(false);
            propertyListingRepository.save(property);
            logger.info("Property {} has been deactivated due to successful deal closure", property.getId());
        }
        
        // Send success notifications to winner and owner
        sendDealClosedNotifications(winningInquiry);
        
        return convertToDTO(winningInquiry);
    }
} 