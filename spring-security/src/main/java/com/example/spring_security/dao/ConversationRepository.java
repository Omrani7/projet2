package com.example.spring_security.dao;

import com.example.spring_security.model.Conversation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Conversation entity operations
 */
@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    
    /**
     * Find all conversations for a specific user, ordered by last update descending
     * @param userId the ID of the user
     * @param pageable pagination information
     * @return page of conversations
     */
    @Query("SELECT DISTINCT c FROM Conversation c " +
           "JOIN c.participants p " +
           "LEFT JOIN FETCH c.announcement " +
           "WHERE p.id = :userId " +
           "ORDER BY c.updatedAt DESC")
    Page<Conversation> findByParticipantIdOrderByUpdatedAtDesc(@Param("userId") Integer userId, Pageable pageable);
    
    /**
     * Find conversation by ID with all related entities eagerly fetched
     * @param id the conversation ID
     * @return optional conversation
     */
    @Query("SELECT c FROM Conversation c " +
           "LEFT JOIN FETCH c.participants " +
           "LEFT JOIN FETCH c.announcement " +
           "LEFT JOIN FETCH c.messages " +
           "WHERE c.id = :id")
    Optional<Conversation> findByIdWithDetails(@Param("id") Long id);
    
    /**
     * Find conversation between two specific users
     * @param userId1 the ID of the first user
     * @param userId2 the ID of the second user
     * @return optional conversation
     */
    @Query("SELECT c FROM Conversation c " +
           "JOIN c.participants p1 " +
           "JOIN c.participants p2 " +
           "WHERE p1.id = :userId1 AND p2.id = :userId2 " +
           "AND SIZE(c.participants) = 2")
    Optional<Conversation> findConversationBetweenUsers(@Param("userId1") Integer userId1, @Param("userId2") Integer userId2);
    
    /**
     * Find conversations related to a specific announcement
     * @param announcementId the announcement ID
     * @param pageable pagination information
     * @return page of conversations
     */
    @Query("SELECT c FROM Conversation c " +
           "LEFT JOIN FETCH c.participants " +
           "WHERE c.announcement.id = :announcementId " +
           "ORDER BY c.updatedAt DESC")
    Page<Conversation> findByAnnouncementIdOrderByUpdatedAtDesc(@Param("announcementId") Long announcementId, Pageable pageable);
    
    /**
     * Find conversations for a user with unread messages
     * @param userId the ID of the user
     * @return list of conversations with unread messages
     */
    @Query("SELECT DISTINCT c FROM Conversation c " +
           "JOIN c.participants p " +
           "JOIN c.messages m " +
           "WHERE p.id = :userId " +
           "AND m.sender.id != :userId " +
           "AND m.isRead = false " +
           "ORDER BY c.updatedAt DESC")
    List<Conversation> findConversationsWithUnreadMessages(@Param("userId") Integer userId);
    
    /**
     * Count conversations for a specific user
     * @param userId the ID of the user
     * @return count of conversations
     */
    @Query("SELECT COUNT(DISTINCT c) FROM Conversation c " +
           "JOIN c.participants p " +
           "WHERE p.id = :userId")
    long countConversationsForUser(@Param("userId") Integer userId);
    
    /**
     * Count conversations with unread messages for a user
     * @param userId the ID of the user
     * @return count of conversations with unread messages
     */
    @Query("SELECT COUNT(DISTINCT c) FROM Conversation c " +
           "JOIN c.participants p " +
           "JOIN c.messages m " +
           "WHERE p.id = :userId " +
           "AND m.sender.id != :userId " +
           "AND m.isRead = false")
    long countConversationsWithUnreadMessagesForUser(@Param("userId") Integer userId);
    
    /**
     * Find conversations where user is participant and announcement is specific
     * @param userId the ID of the user
     * @param announcementId the announcement ID
     * @return list of conversations
     */
    @Query("SELECT c FROM Conversation c " +
           "JOIN c.participants p " +
           "WHERE p.id = :userId " +
           "AND c.announcement.id = :announcementId " +
           "ORDER BY c.updatedAt DESC")
    List<Conversation> findByParticipantIdAndAnnouncementId(@Param("userId") Integer userId, @Param("announcementId") Long announcementId);
    
    /**
     * Check if a conversation exists between specific users
     * @param userId1 the ID of the first user
     * @param userId2 the ID of the second user
     * @return true if conversation exists
     */
    @Query("SELECT COUNT(c) > 0 FROM Conversation c " +
           "JOIN c.participants p1 " +
           "JOIN c.participants p2 " +
           "WHERE p1.id = :userId1 AND p2.id = :userId2 " +
           "AND SIZE(c.participants) = 2")
    boolean existsConversationBetweenUsers(@Param("userId1") Integer userId1, @Param("userId2") Integer userId2);
} 