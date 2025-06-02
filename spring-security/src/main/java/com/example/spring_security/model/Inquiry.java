package com.example.spring_security.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entity representing an inquiry from a student to a property owner
 */
@Entity
@Table(name = "inquiries", 
    indexes = {
        @Index(name = "idx_inquiry_student", columnList = "student_id"),
        @Index(name = "idx_inquiry_owner", columnList = "owner_id"),
        @Index(name = "idx_inquiry_property", columnList = "property_listing_id"),
        @Index(name = "idx_inquiry_timestamp", columnList = "timestamp"),
        @Index(name = "idx_inquiry_status", columnList = "status")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Inquiry {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * The student (user) who is making the inquiry
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;
    
    /**
     * The owner (user) who owns the property and will receive the inquiry
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;
    
    /**
     * The property listing being inquired about
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_listing_id", nullable = false)
    private PropertyListing property;
    
    /**
     * The inquiry message from the student
     */
    @Column(columnDefinition = "TEXT", nullable = false)
    private String message;
    
    /**
     * Student's phone number for direct contact
     */
    @Column(name = "student_phone_number")
    private String studentPhoneNumber;
    
    /**
     * Owner's phone number for direct contact (set when owner replies)
     */
    @Column(name = "owner_phone_number")
    private String ownerPhoneNumber;
    
    /**
     * Timestamp when the inquiry was created
     */
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime timestamp;
    
    /**
     * The owner's reply to the inquiry (optional)
     */
    @Column(columnDefinition = "TEXT")
    private String reply;
    
    /**
     * Timestamp when the owner replied (optional)
     */
    @Column(name = "reply_timestamp")
    private LocalDateTime replyTimestamp;
    
    /**
     * Current status of the inquiry
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private InquiryStatus status = InquiryStatus.PENDING_REPLY;
    
    /**
     * Set the reply and automatically update the reply timestamp and status
     */
    public void setReply(String reply) {
        this.reply = reply;
        if (reply != null && !reply.trim().isEmpty()) {
            this.replyTimestamp = LocalDateTime.now();
            this.status = InquiryStatus.REPLIED;
        }
    }
} 