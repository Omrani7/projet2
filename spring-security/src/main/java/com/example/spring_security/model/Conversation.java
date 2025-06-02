package com.example.spring_security.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Entity representing a conversation between students about a roommate announcement
 * Supports both 1-on-1 and group conversations
 */
@Entity
@Table(name = "conversations",
    indexes = {
        @Index(name = "idx_conversations_announcement", columnList = "announcement_id"),
        @Index(name = "idx_conversations_updated_at", columnList = "updated_at")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Conversation {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * The roommate announcement this conversation is related to (optional)
     * Can be null for direct conversations not related to specific announcements
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "announcement_id")
    @JsonBackReference("announcement-conversations")
    private RoommateAnnouncement announcement;
    
    /**
     * Creation timestamp
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    /**
     * Last update timestamp (automatically updated when new messages are added)
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    /**
     * Participants in this conversation (many-to-many relationship)
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "conversation_participants",
        joinColumns = @JoinColumn(name = "conversation_id"),
        inverseJoinColumns = @JoinColumn(name = "user_id"),
        indexes = {
            @Index(name = "idx_conversation_participants_conversation", columnList = "conversation_id"),
            @Index(name = "idx_conversation_participants_user", columnList = "user_id")
        }
    )
    private Set<User> participants = new HashSet<>();
    
    /**
     * Messages in this conversation
     */
    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("timestamp DESC")
    @JsonManagedReference("conversation-messages")
    private List<Message> messages = new ArrayList<>();
    
    /**
     * Add a participant to the conversation
     */
    public void addParticipant(User user) {
        this.participants.add(user);
    }
    
    /**
     * Remove a participant from the conversation
     */
    public void removeParticipant(User user) {
        this.participants.remove(user);
    }
    
    /**
     * Check if a user is a participant in this conversation
     */
    public boolean hasParticipant(User user) {
        return participants.contains(user);
    }
    
        /**     * Check if a user is a participant by ID     */    public boolean hasParticipantWithId(Integer userId) {        return participants.stream()                .anyMatch(participant -> participant.getId() == userId);    }        /**     * Get the other participant in a 1-on-1 conversation     */    public User getOtherParticipant(User currentUser) {        return participants.stream()                .filter(participant -> participant.getId() != currentUser.getId())                .findFirst()                .orElse(null);    }
    
    /**
     * Get the most recent message in the conversation
     */
    public Message getLatestMessage() {
        if (messages == null || messages.isEmpty()) {
            return null;
        }
        return messages.get(0); // Messages are ordered by timestamp DESC
    }
    
        /**     * Get count of unread messages for a specific user     */    public long getUnreadCountForUser(User user) {        if (messages == null) {            return 0;        }        return messages.stream()                .filter(message -> message.getSender().getId() != user.getId())                .filter(message -> !message.isRead())                .count();    }
    
    /**
     * Check if this is a 1-on-1 conversation
     */
    public boolean isOneOnOne() {
        return participants.size() == 2;
    }
    
    /**
     * Check if this is a group conversation
     */
    public boolean isGroup() {
        return participants.size() > 2;
    }
    
    /**
     * Get conversation title for display
     * For 1-on-1: other participant's name
     * For group: announcement title or "Group Chat"
     */
    public String getDisplayTitle(User currentUser) {
        if (isOneOnOne()) {
            User otherParticipant = getOtherParticipant(currentUser);
            return otherParticipant != null ? otherParticipant.getUsername() : "Unknown User";
        } else if (announcement != null) {
            return "Group: " + announcement.getPropertyTitle();
        } else {
            return "Group Chat";
        }
    }
} 