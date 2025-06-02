package com.example.spring_security.dao;

import com.example.spring_security.model.Message;
import com.example.spring_security.model.Message.MessageType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository interface for Message entity operations
 */
@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    
    /**
     * Find all messages for a specific conversation, ordered by timestamp descending
     * @param conversationId the ID of the conversation
     * @param pageable pagination information
     * @return page of messages
     */
    @Query("SELECT m FROM Message m " +
           "LEFT JOIN FETCH m.sender " +
           "WHERE m.conversation.id = :conversationId " +
           "ORDER BY m.timestamp DESC")
    Page<Message> findByConversationIdOrderByTimestampDesc(@Param("conversationId") Long conversationId, Pageable pageable);
    
    /**
     * Find all messages for a specific conversation, ordered by timestamp ascending (for chat display)
     * @param conversationId the ID of the conversation
     * @param pageable pagination information
     * @return page of messages
     */
    @Query("SELECT m FROM Message m " +
           "LEFT JOIN FETCH m.sender " +
           "WHERE m.conversation.id = :conversationId " +
           "ORDER BY m.timestamp ASC")
    Page<Message> findByConversationIdOrderByTimestampAsc(@Param("conversationId") Long conversationId, Pageable pageable);
    
    /**
     * Find unread messages for a specific user in a conversation
     * @param conversationId the ID of the conversation
     * @param userId the ID of the user (to exclude their own messages)
     * @return list of unread messages
     */
    @Query("SELECT m FROM Message m " +
           "LEFT JOIN FETCH m.sender " +
           "WHERE m.conversation.id = :conversationId " +
           "AND m.sender.id != :userId " +
           "AND m.isRead = false " +
           "ORDER BY m.timestamp ASC")
    List<Message> findUnreadMessagesInConversation(@Param("conversationId") Long conversationId, @Param("userId") Integer userId);
    
    /**
     * Count unread messages for a specific user in a conversation
     * @param conversationId the ID of the conversation
     * @param userId the ID of the user (to exclude their own messages)
     * @return count of unread messages
     */
    @Query("SELECT COUNT(m) FROM Message m " +
           "WHERE m.conversation.id = :conversationId " +
           "AND m.sender.id != :userId " +
           "AND m.isRead = false")
    long countUnreadMessagesInConversation(@Param("conversationId") Long conversationId, @Param("userId") Integer userId);
    
    /**
     * Find the latest message in a conversation
     * @param conversationId the ID of the conversation
     * @return the most recent message
     */
    @Query("SELECT m FROM Message m " +
           "LEFT JOIN FETCH m.sender " +
           "WHERE m.conversation.id = :conversationId " +
           "ORDER BY m.timestamp DESC " +
           "LIMIT 1")
    Message findLatestMessageInConversation(@Param("conversationId") Long conversationId);
    
    /**
     * Find messages by sender in a conversation
     * @param conversationId the ID of the conversation
     * @param senderId the ID of the sender
     * @param pageable pagination information
     * @return page of messages from the sender
     */
    @Query("SELECT m FROM Message m " +
           "WHERE m.conversation.id = :conversationId " +
           "AND m.sender.id = :senderId " +
           "ORDER BY m.timestamp DESC")
    Page<Message> findByConversationIdAndSenderIdOrderByTimestampDesc(
            @Param("conversationId") Long conversationId,
            @Param("senderId") Integer senderId,
            Pageable pageable);
    
    /**
     * Find messages by type in a conversation
     * @param conversationId the ID of the conversation
     * @param messageType the type of message
     * @param pageable pagination information
     * @return page of messages of specified type
     */
    @Query("SELECT m FROM Message m " +
           "LEFT JOIN FETCH m.sender " +
           "WHERE m.conversation.id = :conversationId " +
           "AND m.messageType = :messageType " +
           "ORDER BY m.timestamp DESC")
    Page<Message> findByConversationIdAndMessageTypeOrderByTimestampDesc(
            @Param("conversationId") Long conversationId,
            @Param("messageType") MessageType messageType,
            Pageable pageable);
    
    /**
     * Mark all messages in a conversation as read for a specific user
     * @param conversationId the ID of the conversation
     * @param userId the ID of the user (to exclude their own messages)
     * @return number of updated messages
     */
    @Modifying
    @Query("UPDATE Message m SET m.isRead = true " +
           "WHERE m.conversation.id = :conversationId " +
           "AND m.sender.id != :userId " +
           "AND m.isRead = false")
    int markAllMessagesAsReadInConversation(@Param("conversationId") Long conversationId, @Param("userId") Integer userId);
    
    /**
     * Mark a specific message as read
     * @param messageId the ID of the message
     * @return number of updated messages
     */
    @Modifying
    @Query("UPDATE Message m SET m.isRead = true WHERE m.id = :messageId")
    int markMessageAsRead(@Param("messageId") Long messageId);
    
    /**
     * Find messages sent after a specific timestamp
     * @param conversationId the ID of the conversation
     * @param timestamp the timestamp threshold
     * @param pageable pagination information
     * @return page of recent messages
     */
    @Query("SELECT m FROM Message m " +
           "LEFT JOIN FETCH m.sender " +
           "WHERE m.conversation.id = :conversationId " +
           "AND m.timestamp > :timestamp " +
           "ORDER BY m.timestamp ASC")
    Page<Message> findByConversationIdAndTimestampAfterOrderByTimestampAsc(
            @Param("conversationId") Long conversationId,
            @Param("timestamp") LocalDateTime timestamp,
            Pageable pageable);
    
    /**
     * Count total messages in a conversation
     * @param conversationId the ID of the conversation
     * @return count of messages
     */
    long countByConversationId(Long conversationId);
    
    /**
     * Delete messages by conversation ID (cascade when conversation is deleted)
     * @param conversationId the ID of the conversation
     * @return number of deleted messages
     */
    @Modifying
    @Query("DELETE FROM Message m WHERE m.conversation.id = :conversationId")
    int deleteByConversationId(@Param("conversationId") Long conversationId);
    
    /**
     * Find messages containing specific content (search functionality)
     * @param conversationId the ID of the conversation
     * @param searchTerm the search term
     * @param pageable pagination information
     * @return page of matching messages
     */
    @Query("SELECT m FROM Message m " +
           "LEFT JOIN FETCH m.sender " +
           "WHERE m.conversation.id = :conversationId " +
           "AND LOWER(m.content) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "ORDER BY m.timestamp DESC")
    Page<Message> searchMessagesInConversation(
            @Param("conversationId") Long conversationId,
            @Param("searchTerm") String searchTerm,
            Pageable pageable);
} 