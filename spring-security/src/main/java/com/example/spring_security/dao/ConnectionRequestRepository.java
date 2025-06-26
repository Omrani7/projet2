package com.example.spring_security.dao;

import com.example.spring_security.model.ConnectionRequest;
import com.example.spring_security.model.ConnectionRequest.ConnectionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface ConnectionRequestRepository extends JpaRepository<ConnectionRequest, Long> {
    

    @Query("SELECT cr FROM ConnectionRequest cr " +
           "LEFT JOIN FETCH cr.sender " +
           "LEFT JOIN FETCH cr.receiver " +
           "WHERE cr.id = :id")
    Optional<ConnectionRequest> findByIdWithDetails(@Param("id") Long id);
    

    @Query("SELECT cr FROM ConnectionRequest cr " +
           "LEFT JOIN FETCH cr.receiver " +
           "WHERE cr.sender.id = :senderId " +
           "ORDER BY cr.createdAt DESC")
    Page<ConnectionRequest> findBySenderIdOrderByCreatedAtDesc(@Param("senderId") Integer senderId, Pageable pageable);
    

    @Query("SELECT cr FROM ConnectionRequest cr " +
           "LEFT JOIN FETCH cr.sender " +
           "WHERE cr.receiver.id = :receiverId " +
           "ORDER BY cr.createdAt DESC")
    Page<ConnectionRequest> findByReceiverIdOrderByCreatedAtDesc(@Param("receiverId") Integer receiverId, Pageable pageable);
    
    /**
     * Find connection request between two specific users
     * @param senderId the ID of the sender
     * @param receiverId the ID of the receiver
     * @return optional connection request
     */
    @Query("SELECT cr FROM ConnectionRequest cr " +
           "LEFT JOIN FETCH cr.sender " +
           "LEFT JOIN FETCH cr.receiver " +
           "WHERE cr.sender.id = :senderId AND cr.receiver.id = :receiverId")
    Optional<ConnectionRequest> findBySenderIdAndReceiverId(@Param("senderId") Integer senderId, 
                                                           @Param("receiverId") Integer receiverId);
    
    /**
     * Check if a connection request exists between two users (in either direction)
     * @param userId1 the ID of the first user
     * @param userId2 the ID of the second user
     * @return true if a connection request exists
     */
    @Query("SELECT COUNT(cr) > 0 FROM ConnectionRequest cr " +
           "WHERE (cr.sender.id = :userId1 AND cr.receiver.id = :userId2) " +
           "OR (cr.sender.id = :userId2 AND cr.receiver.id = :userId1)")
    boolean existsConnectionBetweenUsers(@Param("userId1") Integer userId1, @Param("userId2") Integer userId2);
    
    /**
     * Check if an ACCEPTED connection exists between two users (in either direction)
     * @param userId1 the ID of the first user
     * @param userId2 the ID of the second user
     * @return true if an accepted connection exists
     */
    @Query("SELECT COUNT(cr) > 0 FROM ConnectionRequest cr " +
           "WHERE (cr.sender.id = :userId1 AND cr.receiver.id = :userId2) " +
           "OR (cr.sender.id = :userId2 AND cr.receiver.id = :userId1) " +
           "AND cr.status = 'ACCEPTED'")
    boolean hasAcceptedConnectionBetweenUsers(@Param("userId1") Integer userId1, @Param("userId2") Integer userId2);
    
    /**
     * Find pending connection requests sent by a user
     * @param senderId the ID of the sender
     * @param pageable pagination information
     * @return page of pending sent requests
     */
    @Query("SELECT cr FROM ConnectionRequest cr " +
           "LEFT JOIN FETCH cr.receiver " +
           "WHERE cr.sender.id = :senderId AND cr.status = 'PENDING' " +
           "ORDER BY cr.createdAt DESC")
    Page<ConnectionRequest> findPendingBySenderId(@Param("senderId") Integer senderId, Pageable pageable);
    
    /**
     * Find pending connection requests received by a user
     * @param receiverId the ID of the receiver
     * @param pageable pagination information
     * @return page of pending received requests
     */
    @Query("SELECT cr FROM ConnectionRequest cr " +
           "LEFT JOIN FETCH cr.sender " +
           "WHERE cr.receiver.id = :receiverId AND cr.status = 'PENDING' " +
           "ORDER BY cr.createdAt DESC")
    Page<ConnectionRequest> findPendingByReceiverId(@Param("receiverId") Integer receiverId, Pageable pageable);
    
    /**
     * Find connection requests by status for a user (sent or received)
     * @param userId the ID of the user
     * @param status the status to filter by
     * @param pageable pagination information
     * @return page of connection requests with specified status
     */
    @Query("SELECT cr FROM ConnectionRequest cr " +
           "LEFT JOIN FETCH cr.sender " +
           "LEFT JOIN FETCH cr.receiver " +
           "WHERE (cr.sender.id = :userId OR cr.receiver.id = :userId) " +
           "AND cr.status = :status " +
           "ORDER BY cr.createdAt DESC")
    Page<ConnectionRequest> findByUserIdAndStatus(@Param("userId") Integer userId, 
                                                 @Param("status") ConnectionStatus status, 
                                                 Pageable pageable);
    
    /**
     * Count pending connection requests received by a user
     * @param receiverId the ID of the receiver
     * @return count of pending received requests
     */
    @Query("SELECT COUNT(cr) FROM ConnectionRequest cr " +
           "WHERE cr.receiver.id = :receiverId AND cr.status = 'PENDING'")
    long countPendingByReceiverId(@Param("receiverId") Integer receiverId);
    
    /**
     * Count pending connection requests sent by a user
     * @param senderId the ID of the sender
     * @return count of pending sent requests
     */
    @Query("SELECT COUNT(cr) FROM ConnectionRequest cr " +
           "WHERE cr.sender.id = :senderId AND cr.status = 'PENDING'")
    long countPendingBySenderId(@Param("senderId") Integer senderId);
    
    /**
     * Find accepted connection requests for a user (both sent and received)
     * @param userId the ID of the user
     * @param pageable pagination information
     * @return page of accepted connection requests
     */
    @Query("SELECT cr FROM ConnectionRequest cr " +
           "LEFT JOIN FETCH cr.sender " +
           "LEFT JOIN FETCH cr.receiver " +
           "WHERE (cr.sender.id = :userId OR cr.receiver.id = :userId) " +
           "AND cr.status = 'ACCEPTED' " +
           "ORDER BY cr.respondedAt DESC")
    Page<ConnectionRequest> findAcceptedByUserId(@Param("userId") Integer userId, Pageable pageable);
    
    /**
     * Find all connection requests involving a specific user (sent or received)
     * @param userId the ID of the user
     * @param pageable pagination information
     * @return page of all connection requests for the user
     */
    @Query("SELECT cr FROM ConnectionRequest cr " +
           "LEFT JOIN FETCH cr.sender " +
           "LEFT JOIN FETCH cr.receiver " +
           "WHERE cr.sender.id = :userId OR cr.receiver.id = :userId " +
           "ORDER BY cr.createdAt DESC")
    Page<ConnectionRequest> findAllByUserId(@Param("userId") Integer userId, Pageable pageable);
    
    /**
     * Delete connection requests by sender ID (for user cleanup)
     * @param senderId the ID of the sender
     * @return number of deleted requests
     */
    long deleteBySenderId(Integer senderId);
    
    /**
     * Delete connection requests by receiver ID (for user cleanup)
     * @param receiverId the ID of the receiver
     * @return number of deleted requests
     */
    long deleteByReceiverId(Integer receiverId);
} 