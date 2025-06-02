package com.example.spring_security.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entity representing a message in a conversation between students
 * Supports text messages, images, and announcement references
 */
@Entity
@Table(name = "messages",
    indexes = {
        @Index(name = "idx_messages_conversation", columnList = "conversation_id"),
        @Index(name = "idx_messages_sender", columnList = "sender_id"),
        @Index(name = "idx_messages_timestamp", columnList = "timestamp"),
        @Index(name = "idx_messages_is_read", columnList = "is_read")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Message {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * The conversation this message belongs to
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    @JsonBackReference("conversation-messages")
    private Conversation conversation;
    
    /**
     * The user who sent this message
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;
    
    /**
     * The message content
     */
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;
    
    /**
     * Type of message (text, image, announcement reference, etc.)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", length = 30, nullable = false)
    @Builder.Default
    private MessageType messageType = MessageType.TEXT;
    
    /**
     * Timestamp when the message was sent
     */
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime timestamp;
    
    /**
     * Whether the message has been read by all recipients
     * For 1-on-1 conversations: true if read by the other participant
     * For group conversations: true if read by all other participants
     */
    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private Boolean isRead = false;
    
    /**
     * Reference to announcement if message type is ANNOUNCEMENT_REFERENCE
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "referenced_announcement_id")
    private RoommateAnnouncement referencedAnnouncement;
    
    /**
     * Additional metadata for the message (JSON format)
     * Can store image URLs, file paths, etc.
     */
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;
    
    // Message type enum
    public enum MessageType {
        /**
         * Regular text message
         */
        TEXT,
        
        /**
         * Image message (URL stored in content or metadata)
         */
        IMAGE,
        
        /**
         * Reference to a roommate announcement
         */
        ANNOUNCEMENT_REFERENCE,
        
        /**
         * System message (user joined, left, etc.)
         */
        SYSTEM
    }
    
    /**
     * Mark this message as read
     */
    public void markAsRead() {
        this.isRead = true;
    }
    
    /**
     * Check if this is a text message
     */
    public boolean isTextMessage() {
        return messageType == MessageType.TEXT;
    }
    
    /**
     * Check if this is an image message
     */
    public boolean isImageMessage() {
        return messageType == MessageType.IMAGE;
    }
    
    /**
     * Check if this is an announcement reference
     */
    public boolean isAnnouncementReference() {
        return messageType == MessageType.ANNOUNCEMENT_REFERENCE;
    }
    
    /**
     * Check if this is a system message
     */
    public boolean isSystemMessage() {
        return messageType == MessageType.SYSTEM;
    }
    
    /**
     * Get formatted timestamp for display
     */
    public String getFormattedTimestamp() {
        return timestamp.toString(); // Can be customized with proper formatting
        }        /**     * Check if message was sent by a specific user     */    public boolean isSentBy(User user) {        return sender.getId() == user.getId();    }        /**     * Check if message was sent by user ID     */    public boolean isSentByUserId(Integer userId) {        return sender.getId() == userId;    }        /**     * Check if the message is read     */    public boolean isRead() {        return isRead != null && isRead;    }        /**
     * Get sender display name
     */
    public String getSenderDisplayName() {
        return sender.getUsername();
    }
    
    /**
     * Get preview text for notifications (first 50 characters)
     */
    public String getPreviewText() {
        if (content == null) {
            return "";
        }
        if (isImageMessage()) {
            return "📷 Image";
        }
        if (isAnnouncementReference()) {
            return "📋 Shared an announcement";
        }
        if (isSystemMessage()) {
            return content;
        }
        return content.length() > 50 ? content.substring(0, 47) + "..." : content;
    }
} 