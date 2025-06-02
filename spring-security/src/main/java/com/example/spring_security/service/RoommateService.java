package com.example.spring_security.service;

import com.example.spring_security.dao.RoommateAnnouncementRepository;
import com.example.spring_security.dao.RoommateApplicationRepository;
import com.example.spring_security.dao.UserRepo;
import com.example.spring_security.dao.PropertyListingRepository;
import com.example.spring_security.dao.InquiryRepository;
import com.example.spring_security.dto.RoommateAnnouncementCreateDTO;
import com.example.spring_security.dto.RoommateAnnouncementDTO;
import com.example.spring_security.dto.RoommateApplicationDTO;
import com.example.spring_security.dto.RoommateApplicationCreateDTO;
import com.example.spring_security.dto.RoommateApplicationResponseDTO;
import com.example.spring_security.dto.UserBasicDTO;import com.example.spring_security.dto.InquiryDTO;import com.example.spring_security.dto.PropertyListingDTO;import com.example.spring_security.dto.PropertyListingBasicDTO;
import com.example.spring_security.exception.AccessDeniedException;
import com.example.spring_security.exception.ResourceNotFoundException;
import com.example.spring_security.model.RoommateAnnouncement;
import com.example.spring_security.model.RoommateApplication;
import com.example.spring_security.model.User;
import com.example.spring_security.model.UserPrincipal;
import com.example.spring_security.model.PropertyListing;
import com.example.spring_security.model.Inquiry;
import com.example.spring_security.model.InquiryStatus;
import com.example.spring_security.service.CompatibilityService;
import com.example.spring_security.service.EmailService;
import com.example.spring_security.service.WebSocketNotificationService;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Service for managing roommate announcements and applications
 * Includes ML-powered compatibility scoring and recommendations
 */
@Service
@Transactional(readOnly = true)
public class RoommateService {
    
    private static final Logger logger = LoggerFactory.getLogger(RoommateService.class);
    
    private final RoommateAnnouncementRepository announcementRepository;
    private final RoommateApplicationRepository applicationRepository;
    private final UserRepo userRepository;
    private final PropertyListingRepository propertyListingRepository;
    private final InquiryRepository inquiryRepository;
    private final EmailService emailService;
    private final ModelMapper modelMapper;
    private final CompatibilityService compatibilityService;
    private final WebSocketNotificationService webSocketNotificationService;
    
    @Autowired
    public RoommateService(RoommateAnnouncementRepository announcementRepository,
                          RoommateApplicationRepository applicationRepository,
                          UserRepo userRepository,
                          PropertyListingRepository propertyListingRepository,
                          InquiryRepository inquiryRepository,
                          EmailService emailService,
                          ModelMapper modelMapper,
                          CompatibilityService compatibilityService,
                          WebSocketNotificationService webSocketNotificationService) {
        this.announcementRepository = announcementRepository;
        this.applicationRepository = applicationRepository;
        this.userRepository = userRepository;
        this.propertyListingRepository = propertyListingRepository;
        this.inquiryRepository = inquiryRepository;
        this.emailService = emailService;
        this.modelMapper = modelMapper;
        this.compatibilityService = compatibilityService;
        this.webSocketNotificationService = webSocketNotificationService;
    }
    
    /**
     * Create a new roommate announcement
     */
    @Transactional
    public RoommateAnnouncementDTO createAnnouncement(RoommateAnnouncementCreateDTO createDTO, UserPrincipal currentUser) {
        logger.info("Creating roommate announcement by user {}", currentUser.getId());
        
        User poster = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        if (poster.getRole() != User.Role.STUDENT) {
            throw new AccessDeniedException("Only students can post roommate announcements");
        }
        
        RoommateAnnouncement announcement = RoommateAnnouncement.builder()
                .poster(poster)
                // Property details
                .propertyTitle(createDTO.getPropertyTitle())
                .propertyAddress(createDTO.getPropertyAddress())
                .propertyLatitude(createDTO.getPropertyLatitude())
                .propertyLongitude(createDTO.getPropertyLongitude())
                .totalRent(createDTO.getTotalRent())
                .totalRooms(createDTO.getTotalRooms())
                .availableRooms(createDTO.getAvailableRooms())
                .propertyType(createDTO.getPropertyType() != null ? 
                    RoommateAnnouncement.PropertyType.valueOf(createDTO.getPropertyType()) : 
                    RoommateAnnouncement.PropertyType.APARTMENT)
                // Roommate preferences
                .maxRoommates(createDTO.getMaxRoommates())
                .genderPreference(createDTO.getGenderPreference() != null ? 
                    RoommateAnnouncement.GenderPreference.valueOf(createDTO.getGenderPreference()) : 
                    RoommateAnnouncement.GenderPreference.NO_PREFERENCE)
                .ageMin(createDTO.getAgeMin() != null ? createDTO.getAgeMin() : 18)
                .ageMax(createDTO.getAgeMax() != null ? createDTO.getAgeMax() : 35)
                .lifestyleTags(createDTO.getLifestyleTags() != null ? createDTO.getLifestyleTags() : new java.util.HashSet<>())
                .smokingAllowed(createDTO.getSmokingAllowed() != null ? createDTO.getSmokingAllowed() : false)
                .petsAllowed(createDTO.getPetsAllowed() != null ? createDTO.getPetsAllowed() : false)
                .cleanlinessLevel(createDTO.getCleanlinessLevel() != null ? createDTO.getCleanlinessLevel() : 3)
                // Financial details
                .rentPerPerson(createDTO.getRentPerPerson())
                .securityDeposit(createDTO.getSecurityDeposit() != null ? createDTO.getSecurityDeposit() : BigDecimal.ZERO)
                .utilitiesSplit(createDTO.getUtilitiesSplit() != null ? 
                    RoommateAnnouncement.UtilitiesSplit.valueOf(createDTO.getUtilitiesSplit()) : 
                    RoommateAnnouncement.UtilitiesSplit.EQUAL)
                .additionalCosts(createDTO.getAdditionalCosts())
                // Posting details
                .description(createDTO.getDescription())
                .moveInDate(createDTO.getMoveInDate())
                .leaseDurationMonths(createDTO.getLeaseDurationMonths())
                .status(RoommateAnnouncement.AnnouncementStatus.ACTIVE)
                .expiresAt(LocalDateTime.now().plusDays(90))
                .build();
        
        announcement = announcementRepository.save(announcement);
        return convertAnnouncementToDTO(announcement, currentUser.getId());
    }
    
    /**
     * Get announcements for browsing
     */
    public Page<RoommateAnnouncementDTO> getAnnouncementsForBrowsing(UserPrincipal currentUser, Pageable pageable) {
        logger.info("Fetching announcements for browsing by user {}", currentUser.getId());
        
        Page<RoommateAnnouncement> announcements = announcementRepository.findByPosterIdNotAndStatusAndExpiresAtAfter(
                currentUser.getId(),
                RoommateAnnouncement.AnnouncementStatus.ACTIVE,
                LocalDateTime.now(),
                pageable);
        
        return announcements.map(announcement -> convertAnnouncementToDTO(announcement, currentUser.getId()));
    }
    
    /**
     * Get my announcements
     */
    public Page<RoommateAnnouncementDTO> getMyAnnouncements(UserPrincipal currentUser, Pageable pageable) {
        Page<RoommateAnnouncement> announcements = announcementRepository.findByPosterIdOrderByCreatedAtDesc(
                currentUser.getId(), pageable);
        
        return announcements.map(announcement -> convertAnnouncementToDTO(announcement, currentUser.getId()));
    }
    
    /**
     * Get announcement by ID
     */
    public RoommateAnnouncementDTO getAnnouncementById(Long announcementId, UserPrincipal currentUser) {
        RoommateAnnouncement announcement = announcementRepository.findByIdWithDetails(announcementId)
                .orElseThrow(() -> new ResourceNotFoundException("Announcement not found"));
        
        return convertAnnouncementToDTO(announcement, currentUser.getId());
    }
    
    /**
     * Apply to announcement with ML compatibility scoring
     */
    @Transactional
    public RoommateApplicationDTO applyToAnnouncement(RoommateApplicationCreateDTO createDTO, UserPrincipal currentUser) {
        User applicant = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        RoommateAnnouncement announcement = announcementRepository.findByIdWithDetails(createDTO.getAnnouncementId())
                .orElseThrow(() -> new ResourceNotFoundException("Announcement not found"));
        
        // Basic validation
        if (announcement.getPoster().getId() == currentUser.getId()) {
            throw new IllegalStateException("You cannot apply to your own announcement");
        }
        
        // Calculate ML compatibility score
        BigDecimal compatibilityScore = null;
        try {
            compatibilityScore = compatibilityService.calculateCompatibility(applicant, announcement);
            logger.info("Calculated compatibility score {} for user {} applying to announcement {}", 
                       compatibilityScore, applicant.getId(), announcement.getId());
        } catch (Exception e) {
            logger.warn("Failed to calculate compatibility score for application: {}", e.getMessage());
            compatibilityScore = BigDecimal.valueOf(0.50); // Default neutral score
        }
        
        RoommateApplication application = RoommateApplication.builder()
                .announcement(announcement)
                .applicant(applicant)
                .poster(announcement.getPoster())
                .message(createDTO.getMessage())
                .compatibilityScore(compatibilityScore)
                .status(RoommateApplication.ApplicationStatus.PENDING)
                .build();
        
        application = applicationRepository.save(application);
        
        // Send notification to poster with compatibility score
        sendApplicationNotificationWithScore(application, compatibilityScore);
        
        return convertApplicationToDTO(application);
    }
    
    /**
     * Get applications for announcement
     */
    public Page<RoommateApplicationDTO> getApplicationsForAnnouncement(Long announcementId, UserPrincipal currentUser, Pageable pageable) {
        Page<RoommateApplication> applications = applicationRepository.findByAnnouncementIdOrderByAppliedAtDesc(
                announcementId, pageable);
        
        return applications.map(this::convertApplicationToDTO);
    }
    
    /**
     * Get my applications
     */
    public Page<RoommateApplicationDTO> getMyApplications(UserPrincipal currentUser, Pageable pageable) {
        Page<RoommateApplication> applications = applicationRepository.findByApplicantIdOrderByAppliedAtDesc(
                currentUser.getId(), pageable);
        
        return applications.map(this::convertApplicationToDTO);
    }
    
    /**
     * Respond to application
     */
    @Transactional
    public RoommateApplicationDTO respondToApplication(Long applicationId, RoommateApplicationResponseDTO responseDTO, UserPrincipal currentUser) {
        RoommateApplication application = applicationRepository.findByIdWithDetails(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found"));
        
        if (application.getPoster().getId() != currentUser.getId()) {
            throw new AccessDeniedException("You can only respond to applications for your own announcements");
        }
        
        RoommateApplication.ApplicationStatus newStatus = RoommateApplication.ApplicationStatus.valueOf(responseDTO.getStatus().toUpperCase());
        
        if (newStatus == RoommateApplication.ApplicationStatus.ACCEPTED) {
            application.accept(responseDTO.getResponseMessage());
        } else if (newStatus == RoommateApplication.ApplicationStatus.REJECTED) {
            application.reject(responseDTO.getResponseMessage());
        }
        
        application = applicationRepository.save(application);
        
        // Send WebSocket notification to applicant about response
        boolean accepted = newStatus == RoommateApplication.ApplicationStatus.ACCEPTED;
        webSocketNotificationService.notifyOfRoommateApplicationResponse(
            application.getApplicant().getId(),
            convertApplicationToDTO(application),
            accepted
        );
        
        return convertApplicationToDTO(application);
    }
    
    /**
     * Get closed deals for student
     */
    public Page<InquiryDTO> getClosedDealsForStudent(UserPrincipal currentUser, Pageable pageable) {
        Page<Inquiry> closedDeals = inquiryRepository.findByStudentIdAndStatusOrderByReplyTimestampDesc(
                Long.valueOf(currentUser.getId()), InquiryStatus.CLOSED, pageable);
        
        return closedDeals.map(this::convertInquiryToClosedDealDTO);
    }
    
    // Helper conversion methods
    private RoommateAnnouncementDTO convertAnnouncementToDTO(RoommateAnnouncement announcement, Integer currentUserId) {
        return RoommateAnnouncementDTO.builder()
                .id(announcement.getId())
                .poster(convertUserToBasicDTO(announcement.getPoster()))
                // Property details
                .propertyTitle(announcement.getPropertyTitle())
                .propertyAddress(announcement.getPropertyAddress())
                .totalRent(announcement.getTotalRent())
                .totalRooms(announcement.getTotalRooms())
                .availableRooms(announcement.getAvailableRooms())
                .propertyType(announcement.getPropertyType() != null ? announcement.getPropertyType().name() : null)
                // Roommate preferences
                .maxRoommates(announcement.getMaxRoommates())
                .genderPreference(announcement.getGenderPreference() != null ? announcement.getGenderPreference().name() : null)
                .ageMin(announcement.getAgeMin())
                .ageMax(announcement.getAgeMax())
                .lifestyleTags(announcement.getLifestyleTags())
                .smokingAllowed(announcement.getSmokingAllowed())
                .petsAllowed(announcement.getPetsAllowed())
                .cleanlinessLevel(announcement.getCleanlinessLevel())
                // Financial details
                .rentPerPerson(announcement.getRentPerPerson())
                .securityDeposit(announcement.getSecurityDeposit())
                .utilitiesSplit(announcement.getUtilitiesSplit() != null ? announcement.getUtilitiesSplit().name() : null)
                .additionalCosts(announcement.getAdditionalCosts())
                // Posting details
                .description(announcement.getDescription())
                .moveInDate(announcement.getMoveInDate())
                .leaseDurationMonths(announcement.getLeaseDurationMonths())
                .status(announcement.getStatus().name())
                .createdAt(announcement.getCreatedAt())
                .expiresAt(announcement.getExpiresAt())
                // Helper fields
                .remainingSpots(announcement.getRemainingSpots())
                .applicationCount(announcement.getApplications() != null ? announcement.getApplications().size() : 0)
                .build();
    }
    
    private RoommateApplicationDTO convertApplicationToDTO(RoommateApplication application) {
        return RoommateApplicationDTO.builder()
                .id(application.getId())
                .announcementId(application.getAnnouncement().getId())
                .announcementTitle(application.getAnnouncement().getPropertyTitle())
                .applicant(convertUserToBasicDTO(application.getApplicant()))
                .poster(convertUserToBasicDTO(application.getPoster()))
                .message(application.getMessage())
                .compatibilityScore(application.getCompatibilityScore())
                .status(application.getStatus().name())
                .appliedAt(application.getAppliedAt())
                .respondedAt(application.getRespondedAt())
                .responseMessage(application.getResponseMessage())
                .build();
    }
    
    private UserBasicDTO convertUserToBasicDTO(User user) {
        return UserBasicDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
    }
    
    private InquiryDTO convertInquiryToBasicDTO(Inquiry inquiry) {
        return InquiryDTO.builder()
                .id(inquiry.getId())
                .student(convertUserToBasicDTO(inquiry.getStudent()))
                .owner(convertUserToBasicDTO(inquiry.getOwner()))
                .message(inquiry.getMessage())
                .timestamp(inquiry.getTimestamp())
                .status(inquiry.getStatus().name())
                .build();
    }
    
    private InquiryDTO convertInquiryToClosedDealDTO(Inquiry inquiry) {
        PropertyListing property = inquiry.getProperty();
        
        return InquiryDTO.builder()
                .id(inquiry.getId())
                .student(convertUserToBasicDTO(inquiry.getStudent()))
                .owner(convertUserToBasicDTO(inquiry.getOwner()))
                .property(PropertyListingBasicDTO.builder()
                        .id(property.getId())
                        .title(property.getTitle())
                        .price(property.getPrice())
                        .location(property.getLocation())
                        .city(property.getCity())
                        .propertyType(property.getPropertyType())
                        .bedrooms(property.getBedrooms())
                        .bathrooms(property.getBathrooms())
                        .imageUrls(property.getImageUrls())
                        .build())
                .message(inquiry.getMessage())
                .timestamp(inquiry.getTimestamp())
                .replyTimestamp(inquiry.getReplyTimestamp())
                .status(inquiry.getStatus().name())
                .agreedPrice(property.getPrice()) // Use property price as agreed price
                .build();
    }
    
    /**
     * Send enhanced application notification with ML compatibility score
     */
    private void sendApplicationNotificationWithScore(RoommateApplication application, BigDecimal compatibilityScore) {
        try {
            String compatibilityLevel = getCompatibilityLevel(compatibilityScore);
            
            String subject = String.format("New Roommate Application - %s Compatibility Match", compatibilityLevel);
            
            String emailBody = String.format(
                "You have received a new roommate application for your announcement: %s\n\n" +
                "Applicant: %s\n" +
                "Compatibility Score: %d%% (%s)\n" +
                "Message: %s\n\n" +
                "View and respond to this application in your dashboard.",
                application.getAnnouncement().getPropertyTitle(),
                application.getApplicant().getUsername(),
                compatibilityScore.multiply(BigDecimal.valueOf(100)).intValue(),
                compatibilityLevel,
                application.getMessage()
            );
            
            // Send email notification (if emailService is properly implemented)
            // emailService.sendNotificationEmail(application.getPoster().getEmail(), subject, emailBody);
            
            // Send real-time WebSocket notification with ML compatibility score
            webSocketNotificationService.notifyOfNewRoommateApplication(
                application.getPoster().getId(),
                convertApplicationToDTO(application),
                compatibilityScore.doubleValue()
            );
            
            logger.info("Sent application notification with compatibility score to user {}", 
                       application.getPoster().getId());
            
        } catch (Exception e) {
            logger.error("Failed to send application notification: {}", e.getMessage());
        }
    }
    
    /**
     * Get compatibility level description from score
     */
    private String getCompatibilityLevel(BigDecimal score) {
        if (score == null) return "Unknown";
        
        if (score.compareTo(BigDecimal.valueOf(0.90)) >= 0) return "Excellent";
        if (score.compareTo(BigDecimal.valueOf(0.75)) >= 0) return "Very Good";
        if (score.compareTo(BigDecimal.valueOf(0.60)) >= 0) return "Good";
        if (score.compareTo(BigDecimal.valueOf(0.40)) >= 0) return "Fair";
        return "Poor";
    }
} 