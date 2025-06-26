package com.example.spring_security.controller;

import com.example.spring_security.model.UserPrincipal;
import com.example.spring_security.dto.ConnectionRequestCreateDTO;
import com.example.spring_security.dto.ConnectionRequestDTO;
import com.example.spring_security.dto.ConnectionRequestResponseDTO;
import com.example.spring_security.service.ConnectionRequestService;
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


@RestController
@RequestMapping("/api/v1/connections")
public class ConnectionRequestController {
    
    private static final Logger logger = LoggerFactory.getLogger(ConnectionRequestController.class);
    
    private final ConnectionRequestService connectionRequestService;
    
    @Autowired
    public ConnectionRequestController(ConnectionRequestService connectionRequestService) {
        this.connectionRequestService = connectionRequestService;
    }
    

    @PostMapping("/request")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ConnectionRequestDTO> sendConnectionRequest(
            @Valid @RequestBody ConnectionRequestCreateDTO createDTO,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        
        logger.info("User {} sending connection request to user {}", 
                   currentUser.getId(), createDTO.getReceiverId());
        
        ConnectionRequestDTO connectionRequest = connectionRequestService
                .sendConnectionRequest(createDTO, currentUser);
        
        URI location = URI.create(String.format("/api/v1/connections/%s", connectionRequest.getId()));
        return ResponseEntity.created(location).body(connectionRequest);
    }
    

    @GetMapping("/sent")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<Page<ConnectionRequestDTO>> getSentRequests(
            @AuthenticationPrincipal UserPrincipal currentUser,
            Pageable pageable) {
        
        logger.info("Getting sent connection requests for user {}", currentUser.getId());
        
        Page<ConnectionRequestDTO> sentRequests = connectionRequestService
                .getSentRequests(currentUser, pageable);
        
        return ResponseEntity.ok(sentRequests);
    }
    

    @GetMapping("/received")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<Page<ConnectionRequestDTO>> getReceivedRequests(
            @AuthenticationPrincipal UserPrincipal currentUser,
            Pageable pageable) {
        
        logger.info("Getting received connection requests for user {}", currentUser.getId());
        
        Page<ConnectionRequestDTO> receivedRequests = connectionRequestService
                .getReceivedRequests(currentUser, pageable);
        
        return ResponseEntity.ok(receivedRequests);
    }
    

    @GetMapping("/pending")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<Page<ConnectionRequestDTO>> getPendingReceivedRequests(
            @AuthenticationPrincipal UserPrincipal currentUser,
            Pageable pageable) {
        
        logger.info("Getting pending received connection requests for user {}", currentUser.getId());
        
        Page<ConnectionRequestDTO> pendingRequests = connectionRequestService
                .getPendingReceivedRequests(currentUser, pageable);
        
        return ResponseEntity.ok(pendingRequests);
    }
    

    @PutMapping("/{id}/respond")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ConnectionRequestDTO> respondToConnectionRequest(
            @PathVariable Long id,
            @Valid @RequestBody ConnectionRequestResponseDTO responseDTO,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        
        logger.info("User {} responding to connection request {} with status {}", 
                   currentUser.getId(), id, responseDTO.getStatus());
        
        ConnectionRequestDTO updatedRequest = connectionRequestService
                .respondToConnectionRequest(id, responseDTO, currentUser);
        
        return ResponseEntity.ok(updatedRequest);
    }
    

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ConnectionRequestDTO> getConnectionRequestById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        
        logger.info("Getting connection request {} for user {}", id, currentUser.getId());
        
        ConnectionRequestDTO connectionRequest = connectionRequestService
                .getConnectionRequestById(id, currentUser);
        
        return ResponseEntity.ok(connectionRequest);
    }
    

    @GetMapping("/pending/count")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<Map<String, Long>> getPendingRequestsCount(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        
        logger.debug("Getting pending requests count for user {}", currentUser.getId());
        
        long count = connectionRequestService.getPendingRequestsCount(currentUser);
        
        Map<String, Long> response = new HashMap<>();
        response.put("pendingCount", count);
        
        return ResponseEntity.ok(response);
    }
    

    @GetMapping("/exists/{userId}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<Map<String, Boolean>> checkConnectionExists(
            @PathVariable Integer userId,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        
        logger.debug("Checking if connection exists between user {} and user {}", 
                    currentUser.getId(), userId);
        
        boolean exists = connectionRequestService.hasConnectionWith(userId, currentUser);
        
        Map<String, Boolean> response = new HashMap<>();
        response.put("connectionExists", exists);
        
        return ResponseEntity.ok(response);
    }
    

    @GetMapping("/network")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<Page<ConnectionRequestDTO>> getAcceptedConnections(
            @AuthenticationPrincipal UserPrincipal currentUser,
            Pageable pageable) {
        
        logger.info("Getting accepted connections (network) for user {}", currentUser.getId());
        
        Page<ConnectionRequestDTO> connections = connectionRequestService
                .getAcceptedConnections(currentUser, pageable);
        
        return ResponseEntity.ok(connections);
    }
} 