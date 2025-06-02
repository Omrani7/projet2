package com.example.spring_security.service;

import com.example.spring_security.dao.ConnectionRequestRepository;
import com.example.spring_security.dao.UserRepo;
import com.example.spring_security.dto.*;
import com.example.spring_security.exception.ResourceNotFoundException;
import com.example.spring_security.exception.BadRequestException;
import com.example.spring_security.model.ConnectionRequest;
import com.example.spring_security.model.User;
import com.example.spring_security.model.UserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * Service for managing connection requests between students
 * Handles sending, receiving, accepting, and rejecting connection requests
 */
@Service
@Transactional
public class ConnectionRequestService {
    
    private static final Logger logger = LoggerFactory.getLogger(ConnectionRequestService.class);
    
    private final ConnectionRequestRepository connectionRequestRepository;
    private final UserRepo userRepository;
    private final WebSocketNotificationService webSocketNotificationService;
    
    @Autowired
    public ConnectionRequestService(ConnectionRequestRepository connectionRequestRepository,
                                  UserRepo userRepository,
                                  WebSocketNotificationService webSocketNotificationService) {
        this.connectionRequestRepository = connectionRequestRepository;
        this.userRepository = userRepository;
        this.webSocketNotificationService = webSocketNotificationService;
    }
    
    /**
     * Send a connection request to another student
     */
    public ConnectionRequestDTO sendConnectionRequest(ConnectionRequestCreateDTO createDTO, UserPrincipal currentUser) {
        logger.info("User {} sending connection request to user {}", currentUser.getId(), createDTO.getReceiverId());
        
        // Validate that sender and receiver are different
        if (currentUser.getId() == createDTO.getReceiverId()) {
            throw new BadRequestException("You cannot send a connection request to yourself");
        }
        
        // Get sender and receiver users
        User sender = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Sender not found"));
        
        User receiver = userRepository.findById(createDTO.getReceiverId())
                .orElseThrow(() -> new ResourceNotFoundException("Receiver not found"));
        
        // Validate that receiver is a student
        if (receiver.getRole() != User.Role.STUDENT) {
            throw new BadRequestException("Connection requests can only be sent to students");
        }
        
        // Check if a connection request already exists between these users
        if (connectionRequestRepository.existsConnectionBetweenUsers(currentUser.getId(), createDTO.getReceiverId())) {
            throw new BadRequestException("A connection request already exists between you and this user");
        }
        
        // Create the connection request
        ConnectionRequest connectionRequest = ConnectionRequest.builder()
                .sender(sender)
                .receiver(receiver)
                .message(createDTO.getMessage())
                .status(ConnectionRequest.ConnectionStatus.PENDING)
                .build();
        
        connectionRequest = connectionRequestRepository.save(connectionRequest);
        
        // Send WebSocket notification to receiver
        try {
            webSocketNotificationService.sendNotification(
                    Long.valueOf(receiver.getId()),
                    "NEW_CONNECTION_REQUEST",
                    String.format("New connection request from %s", sender.getUsername()),
                    convertToDTO(connectionRequest, receiver)
            );
        } catch (Exception e) {
            logger.warn("Failed to send WebSocket notification for connection request {}: {}", 
                       connectionRequest.getId(), e.getMessage());
        }
        
        logger.info("Connection request {} sent successfully from user {} to user {}", 
                   connectionRequest.getId(), currentUser.getId(), createDTO.getReceiverId());
        
        return convertToDTO(connectionRequest, sender);
    }
    
    /**
     * Get connection requests sent by the current user
     */
    @Transactional(readOnly = true)
    public Page<ConnectionRequestDTO> getSentRequests(UserPrincipal currentUser, Pageable pageable) {
        logger.debug("Getting sent connection requests for user {}", currentUser.getId());
        
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        Page<ConnectionRequest> requests = connectionRequestRepository
                .findBySenderIdOrderByCreatedAtDesc(currentUser.getId(), pageable);
        
        return requests.map(request -> convertToDTO(request, user));
    }
    
    /**
     * Get connection requests received by the current user
     */
    @Transactional(readOnly = true)
    public Page<ConnectionRequestDTO> getReceivedRequests(UserPrincipal currentUser, Pageable pageable) {
        logger.debug("Getting received connection requests for user {}", currentUser.getId());
        
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        Page<ConnectionRequest> requests = connectionRequestRepository
                .findByReceiverIdOrderByCreatedAtDesc(currentUser.getId(), pageable);
        
        return requests.map(request -> convertToDTO(request, user));
    }
    
    /**
     * Get pending connection requests received by the current user
     */
    @Transactional(readOnly = true)
    public Page<ConnectionRequestDTO> getPendingReceivedRequests(UserPrincipal currentUser, Pageable pageable) {
        logger.debug("Getting pending received connection requests for user {}", currentUser.getId());
        
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        Page<ConnectionRequest> requests = connectionRequestRepository
                .findPendingByReceiverId(currentUser.getId(), pageable);
        
        return requests.map(request -> convertToDTO(request, user));
    }
    
    /**
     * Respond to a connection request (accept or reject)
     */
    public ConnectionRequestDTO respondToConnectionRequest(Long requestId, 
                                                         ConnectionRequestResponseDTO responseDTO, 
                                                         UserPrincipal currentUser) {
        logger.info("User {} responding to connection request {} with status {}", 
                   currentUser.getId(), requestId, responseDTO.getStatus());
        
        ConnectionRequest connectionRequest = connectionRequestRepository.findByIdWithDetails(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Connection request not found"));
        
        // Validate that current user is the receiver
        if (connectionRequest.getReceiver().getId() != currentUser.getId()) {
            throw new BadRequestException("You can only respond to connection requests sent to you");
        }
        
        // Validate that request is still pending
        if (!connectionRequest.isPending()) {
            throw new BadRequestException("This connection request has already been responded to");
        }
        
        // Update the connection request
        if ("ACCEPTED".equals(responseDTO.getStatus())) {
            connectionRequest.accept(responseDTO.getResponseMessage());
        } else if ("REJECTED".equals(responseDTO.getStatus())) {
            connectionRequest.reject(responseDTO.getResponseMessage());
        } else {
            throw new BadRequestException("Invalid response status");
        }
        
        connectionRequest = connectionRequestRepository.save(connectionRequest);
        
        // Send WebSocket notification to sender
        try {
            boolean accepted = "ACCEPTED".equals(responseDTO.getStatus());
            String notificationType = accepted ? "CONNECTION_REQUEST_ACCEPTED" : "CONNECTION_REQUEST_REJECTED";
            String message = accepted ? 
                String.format("%s accepted your connection request!", connectionRequest.getReceiver().getUsername()) :
                String.format("%s declined your connection request", connectionRequest.getReceiver().getUsername());
            
            webSocketNotificationService.sendNotification(
                    Long.valueOf(connectionRequest.getSender().getId()),
                    notificationType,
                    message,
                    convertToDTO(connectionRequest, connectionRequest.getSender())
            );
        } catch (Exception e) {
            logger.warn("Failed to send WebSocket notification for connection request response {}: {}", 
                       connectionRequest.getId(), e.getMessage());
        }
        
        logger.info("Connection request {} {} by user {}", 
                   requestId, responseDTO.getStatus().toLowerCase(), currentUser.getId());
        
        return convertToDTO(connectionRequest, connectionRequest.getReceiver());
    }
    
    /**
     * Get a specific connection request by ID
     */
    @Transactional(readOnly = true)
    public ConnectionRequestDTO getConnectionRequestById(Long requestId, UserPrincipal currentUser) {
        logger.debug("Getting connection request {} for user {}", requestId, currentUser.getId());
        
        ConnectionRequest connectionRequest = connectionRequestRepository.findByIdWithDetails(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Connection request not found"));
        
        // Validate that current user is involved in this request
        if (connectionRequest.getSender().getId() != currentUser.getId() && 
            connectionRequest.getReceiver().getId() != currentUser.getId()) {
            throw new BadRequestException("You don't have access to this connection request");
        }
        
        User currentUserEntity = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        return convertToDTO(connectionRequest, currentUserEntity);
    }
    
    /**
     * Get count of pending connection requests received by user
     */
    @Transactional(readOnly = true)
    public long getPendingRequestsCount(UserPrincipal currentUser) {
        return connectionRequestRepository.countPendingByReceiverId(currentUser.getId());
    }
    
    /**
     * Check if a connection exists between current user and another user
     */
    @Transactional(readOnly = true)
    public boolean hasConnectionWith(Integer otherUserId, UserPrincipal currentUser) {
        return connectionRequestRepository.existsConnectionBetweenUsers(currentUser.getId(), otherUserId);
    }
    
    /**
     * Check if an ACCEPTED connection exists between current user and another user
     * This is used for messaging validation
     */
    @Transactional(readOnly = true)
    public boolean hasAcceptedConnectionWith(Integer otherUserId, UserPrincipal currentUser) {
        return connectionRequestRepository.hasAcceptedConnectionBetweenUsers(currentUser.getId(), otherUserId);
    }
    
    /**
     * Get accepted connections for a user (their network)
     */
    @Transactional(readOnly = true)
    public Page<ConnectionRequestDTO> getAcceptedConnections(UserPrincipal currentUser, Pageable pageable) {
        logger.debug("Getting accepted connections for user {}", currentUser.getId());
        
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        Page<ConnectionRequest> connections = connectionRequestRepository
                .findAcceptedByUserId(currentUser.getId(), pageable);
        
        return connections.map(connection -> convertToDTO(connection, user));
    }
    
    /**
     * Convert ConnectionRequest entity to DTO
     */
    private ConnectionRequestDTO convertToDTO(ConnectionRequest connectionRequest, User currentUser) {
        User otherUser = connectionRequest.getOtherUser(currentUser);
        
        return ConnectionRequestDTO.builder()
                .id(connectionRequest.getId())
                .sender(convertUserToBasicDTO(connectionRequest.getSender()))
                .receiver(convertUserToBasicDTO(connectionRequest.getReceiver()))
                .message(connectionRequest.getMessage())
                .status(connectionRequest.getStatus().name())
                .createdAt(connectionRequest.getCreatedAt())
                .respondedAt(connectionRequest.getRespondedAt())
                .responseMessage(connectionRequest.getResponseMessage())
                .canBeWithdrawn(connectionRequest.canBeWithdrawn())
                .isSender(connectionRequest.isSender(currentUser))
                .isReceiver(connectionRequest.isReceiver(currentUser))
                .otherUser(otherUser != null ? convertUserToBasicDTO(otherUser) : null)
                .statusDisplayText(getStatusDisplayText(connectionRequest.getStatus()))
                .timeAgo(getTimeAgo(connectionRequest.getCreatedAt()))
                .isPending(connectionRequest.isPending())
                .isAccepted(connectionRequest.isAccepted())
                .isRejected(connectionRequest.isRejected())
                .build();
    }
    
    /**
     * Convert User entity to UserBasicDTO
     */
    private UserBasicDTO convertUserToBasicDTO(User user) {
        return UserBasicDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole().name())
                .institute(user.getUserProfile() != null ? user.getUserProfile().getInstitute() : null)
                .fieldOfStudy(user.getUserProfile() != null ? user.getUserProfile().getFieldOfStudy() : user.getStudyField())
                .educationLevel(user.getUserProfile() != null && user.getUserProfile().getEducationLevel() != null ? 
                               user.getUserProfile().getEducationLevel().name() : null)
                .build();
    }
    
    /**
     * Get display text for connection request status
     */
    private String getStatusDisplayText(ConnectionRequest.ConnectionStatus status) {
        switch (status) {
            case PENDING:
                return "Pending Response";
            case ACCEPTED:
                return "Accepted";
            case REJECTED:
                return "Declined";
            default:
                return status.name();
        }
    }
    
    /**
     * Get human-readable time ago string
     */
    private String getTimeAgo(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }
        
        LocalDateTime now = LocalDateTime.now();
        long minutes = ChronoUnit.MINUTES.between(dateTime, now);
        long hours = ChronoUnit.HOURS.between(dateTime, now);
        long days = ChronoUnit.DAYS.between(dateTime, now);
        
        if (minutes < 1) {
            return "Just now";
        } else if (minutes < 60) {
            return minutes + " minute" + (minutes == 1 ? "" : "s") + " ago";
        } else if (hours < 24) {
            return hours + " hour" + (hours == 1 ? "" : "s") + " ago";
        } else if (days < 7) {
            return days + " day" + (days == 1 ? "" : "s") + " ago";
        } else {
            return dateTime.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"));
        }
    }
} 