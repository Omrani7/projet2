package com.example.spring_security.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entity representing a connection request between students for roommate matching
 * Allows students to send connection requests to each other based on compatibility
 */
@Entity
@Table(name = "connection_requests",
    indexes = {
        @Index(name = "idx_connection_requests_sender", columnList = "sender_id"),
        @Index(name = "idx_connection_requests_receiver", columnList = "receiver_id"),
        @Index(name = "idx_connection_requests_status", columnList = "status"),
        @Index(name = "idx_connection_requests_created_at", columnList = "created_at")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_connection_requests_sender_receiver", 
                         columnNames = {"sender_id", "receiver_id"})
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConnectionRequest {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * The student who sent the connection request
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;
    
    /**
     * The student who received the connection request
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id", nullable = false)
    private User receiver;
    
    /**
     * Optional message from sender to receiver
     */
    @Column(columnDefinition = "TEXT")
    private String message;
    
    /**
     * Status of the connection request
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ConnectionStatus status = ConnectionStatus.PENDING;
    
    /**
     * Timestamp when the request was created
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    /**
     * Timestamp when the request was responded to (accepted/rejected)
     */
    @Column(name = "responded_at")
    private LocalDateTime respondedAt;
    
    /**
     * Optional response message when accepting/rejecting
     */
    @Column(name = "response_message", columnDefinition = "TEXT")
    private String responseMessage;
    
    /**
     * Enum for connection request status
     */
    public enum ConnectionStatus {
        PENDING,    // Request sent, waiting for response
        ACCEPTED,   // Request accepted by receiver
        REJECTED    // Request rejected by receiver
    }
    
    /**
     * Accept the connection request
     */
    public void accept(String responseMessage) {
        this.status = ConnectionStatus.ACCEPTED;
        this.respondedAt = LocalDateTime.now();
        this.responseMessage = responseMessage;
    }
    
    /**
     * Reject the connection request
     */
    public void reject(String responseMessage) {
        this.status = ConnectionStatus.REJECTED;
        this.respondedAt = LocalDateTime.now();
        this.responseMessage = responseMessage;
    }
    
    /**
     * Check if the request is still pending
     */
    public boolean isPending() {
        return status == ConnectionStatus.PENDING;
    }
    
    /**
     * Check if the request was accepted
     */
    public boolean isAccepted() {
        return status == ConnectionStatus.ACCEPTED;
    }
    
    /**
     * Check if the request was rejected
     */
    public boolean isRejected() {
        return status == ConnectionStatus.REJECTED;
    }
    
    /**
     * Check if the request can be withdrawn (only pending requests)
     */
    public boolean canBeWithdrawn() {
        return status == ConnectionStatus.PENDING;
    }
    
    /**
     * Get the other user in the connection (from perspective of given user)
     */
    public User getOtherUser(User currentUser) {
        if (currentUser.getId() == sender.getId()) {
            return receiver;
        } else if (currentUser.getId() == receiver.getId()) {
            return sender;
        }
        return null;
    }
    
    /**
     * Check if the given user is the sender
     */
    public boolean isSender(User user) {
        return sender.getId() == user.getId();
    }
    
    /**
     * Check if the given user is the receiver
     */
    public boolean isReceiver(User user) {
        return receiver.getId() == user.getId();
    }
} 