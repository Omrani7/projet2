package com.example.spring_security.dao;

import com.example.spring_security.model.Inquiry;
import com.example.spring_security.model.InquiryStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Inquiry entity operations
 */
@Repository
public interface InquiryRepository extends JpaRepository<Inquiry, Long>, JpaSpecificationExecutor<Inquiry> {
    
    /**
     * Find all inquiries for a specific owner, ordered by timestamp descending
     * @param ownerId the ID of the property owner
     * @param pageable pagination information
     * @return page of inquiries
     */
    Page<Inquiry> findByOwnerIdOrderByTimestampDesc(Long ownerId, Pageable pageable);
    
    /**
     * Find all inquiries for a specific property
     * @param propertyId the ID of the property
     * @return list of inquiries
     */
    List<Inquiry> findByPropertyId(Long propertyId);
    
    /**
     * Find inquiry by ID with all related entities eagerly fetched
     * @param id the inquiry ID
     * @return optional inquiry
     */
    @Query("SELECT i FROM Inquiry i " +
           "LEFT JOIN FETCH i.student " +
           "LEFT JOIN FETCH i.owner " +
           "LEFT JOIN FETCH i.property " +
           "WHERE i.id = :id")
    Optional<Inquiry> findByIdWithDetails(@Param("id") Long id);
    
    /**
     * Count unread inquiries for an owner (status = PENDING_REPLY)
     * @param ownerId the ID of the property owner
     * @return count of unread inquiries
     */
    @Query("SELECT COUNT(i) FROM Inquiry i WHERE i.owner.id = :ownerId AND i.status = 'PENDING_REPLY'")
    long countUnreadInquiriesForOwner(@Param("ownerId") Long ownerId);

    /**
     * Find inquiries for a specific student, ordered by timestamp descending
     * @param studentId the ID of the student
     * @param pageable pagination parameters
     * @return paginated list of inquiries for the student
     */
    @Query("SELECT i FROM Inquiry i " +
           "LEFT JOIN FETCH i.student " +
           "LEFT JOIN FETCH i.owner " +
           "LEFT JOIN FETCH i.property " +
           "WHERE i.student.id = :studentId " +
           "ORDER BY i.timestamp DESC")
    Page<Inquiry> findByStudentIdOrderByTimestampDesc(@Param("studentId") Long studentId, Pageable pageable);

    /**
     * Find closed deals for a specific student (inquiries with CLOSED status)
     * @param studentId the ID of the student
     * @param status the inquiry status (CLOSED)
     * @param pageable pagination parameters
     * @return paginated list of closed deals for the student
     */
    @Query("SELECT i FROM Inquiry i " +
           "LEFT JOIN FETCH i.student " +
           "LEFT JOIN FETCH i.owner " +
           "LEFT JOIN FETCH i.property " +
           "WHERE i.student.id = :studentId AND i.status = :status " +
           "ORDER BY i.replyTimestamp DESC")
    Page<Inquiry> findByStudentIdAndStatusOrderByReplyTimestampDesc(
            @Param("studentId") Long studentId, 
            @Param("status") InquiryStatus status, 
            Pageable pageable);

    /**
     * Find all active inquiries for a specific property (PENDING_REPLY or REPLIED status)
     * @param propertyId the ID of the property
     * @return list of active inquiries for the property
     */
    @Query("SELECT i FROM Inquiry i " +
           "LEFT JOIN FETCH i.student " +
           "LEFT JOIN FETCH i.owner " +
           "LEFT JOIN FETCH i.property " +
           "WHERE i.property.id = :propertyId " +
           "AND (i.status = 'PENDING_REPLY' OR i.status = 'REPLIED') " +
           "ORDER BY i.timestamp ASC")
    List<Inquiry> findActiveInquiriesByPropertyId(@Param("propertyId") Long propertyId);

    /**
     * Update status for multiple inquiries by their IDs
     * @param inquiryIds list of inquiry IDs to update
     * @param newStatus the new status to set
     * @return number of updated records
     */
    @Modifying
    @Query("UPDATE Inquiry i SET i.status = :newStatus WHERE i.id IN :inquiryIds")
    int updateInquiryStatusByIds(@Param("inquiryIds") List<Long> inquiryIds, @Param("newStatus") InquiryStatus newStatus);
    
    // Admin dashboard statistics methods
    long countByStatus(InquiryStatus status);
    long countByStudentId(Integer studentId);
    long countByTimestampAfter(LocalDateTime date);
    long countByTimestampBetween(LocalDateTime startDate, LocalDateTime endDate);
} 