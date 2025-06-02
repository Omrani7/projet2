package com.example.spring_security.dao;

import com.example.spring_security.model.RoommateApplication;
import com.example.spring_security.model.RoommateApplication.ApplicationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for RoommateApplication entity operations
 */
@Repository
public interface RoommateApplicationRepository extends JpaRepository<RoommateApplication, Long> {
    
    /**
     * Find all applications for a specific announcement, ordered by application date descending
     * @param announcementId the ID of the announcement
     * @param pageable pagination information
     * @return page of applications
     */
    @Query("SELECT ra FROM RoommateApplication ra " +
           "LEFT JOIN FETCH ra.applicant " +
           "LEFT JOIN FETCH ra.announcement " +
           "WHERE ra.announcement.id = :announcementId " +
           "ORDER BY ra.appliedAt DESC")
    Page<RoommateApplication> findByAnnouncementIdOrderByAppliedAtDesc(@Param("announcementId") Long announcementId, Pageable pageable);
    
    /**
     * Find all applications by a specific applicant, ordered by application date descending
     * @param applicantId the ID of the applicant
     * @param pageable pagination information
     * @return page of applications
     */
    @Query("SELECT ra FROM RoommateApplication ra " +
           "LEFT JOIN FETCH ra.announcement " +
           "LEFT JOIN FETCH ra.poster " +
           "WHERE ra.applicant.id = :applicantId " +
           "ORDER BY ra.appliedAt DESC")
    Page<RoommateApplication> findByApplicantIdOrderByAppliedAtDesc(@Param("applicantId") Integer applicantId, Pageable pageable);
    
    /**
     * Find all applications received by a specific poster, ordered by application date descending
     * @param posterId the ID of the poster
     * @param pageable pagination information
     * @return page of applications
     */
    @Query("SELECT ra FROM RoommateApplication ra " +
           "LEFT JOIN FETCH ra.applicant " +
           "LEFT JOIN FETCH ra.announcement " +
           "WHERE ra.poster.id = :posterId " +
           "ORDER BY ra.appliedAt DESC")
    Page<RoommateApplication> findByPosterIdOrderByAppliedAtDesc(@Param("posterId") Integer posterId, Pageable pageable);
    
    /**
     * Find application by ID with all related entities eagerly fetched
     * @param id the application ID
     * @return optional application
     */
    @Query("SELECT ra FROM RoommateApplication ra " +
           "LEFT JOIN FETCH ra.applicant " +
           "LEFT JOIN FETCH ra.poster " +
           "LEFT JOIN FETCH ra.announcement " +
           "WHERE ra.id = :id")
    Optional<RoommateApplication> findByIdWithDetails(@Param("id") Long id);
    
    /**
     * Find applications by status for a specific poster
     * @param posterId the ID of the poster
     * @param status the application status
     * @param pageable pagination information
     * @return page of applications with specified status
     */
    @Query("SELECT ra FROM RoommateApplication ra " +
           "LEFT JOIN FETCH ra.applicant " +
           "LEFT JOIN FETCH ra.announcement " +
           "WHERE ra.poster.id = :posterId AND ra.status = :status " +
           "ORDER BY ra.appliedAt DESC")
    Page<RoommateApplication> findByPosterIdAndStatusOrderByAppliedAtDesc(
            @Param("posterId") Integer posterId,
            @Param("status") ApplicationStatus status,
            Pageable pageable);
    
    /**
     * Find applications by status for a specific applicant
     * @param applicantId the ID of the applicant
     * @param status the application status
     * @param pageable pagination information
     * @return page of applications with specified status
     */
    @Query("SELECT ra FROM RoommateApplication ra " +
           "LEFT JOIN FETCH ra.announcement " +
           "LEFT JOIN FETCH ra.poster " +
           "WHERE ra.applicant.id = :applicantId AND ra.status = :status " +
           "ORDER BY ra.appliedAt DESC")
    Page<RoommateApplication> findByApplicantIdAndStatusOrderByAppliedAtDesc(
            @Param("applicantId") Integer applicantId,
            @Param("status") ApplicationStatus status,
            Pageable pageable);
    
    /**
     * Check if a user has already applied to a specific announcement
     * @param announcementId the announcement ID
     * @param applicantId the applicant ID
     * @return true if application exists
     */
    boolean existsByAnnouncementIdAndApplicantId(Long announcementId, Integer applicantId);
    
    /**
     * Find existing application by announcement and applicant
     * @param announcementId the announcement ID
     * @param applicantId the applicant ID
     * @return optional application
     */
    Optional<RoommateApplication> findByAnnouncementIdAndApplicantId(Long announcementId, Integer applicantId);
    
    /**
     * Count pending applications for a specific poster
     * @param posterId the ID of the poster
     * @return count of pending applications
     */
    @Query("SELECT COUNT(ra) FROM RoommateApplication ra " +
           "WHERE ra.poster.id = :posterId AND ra.status = 'PENDING'")
    long countPendingApplicationsForPoster(@Param("posterId") Integer posterId);
    
    /**
     * Count applications for a specific announcement
     * @param announcementId the announcement ID
     * @return count of applications
     */
    long countByAnnouncementId(Long announcementId);
    
    /**
     * Find accepted applications for a specific announcement
     * @param announcementId the announcement ID
     * @return list of accepted applications
     */
    @Query("SELECT ra FROM RoommateApplication ra " +
           "LEFT JOIN FETCH ra.applicant " +
           "WHERE ra.announcement.id = :announcementId AND ra.status = 'ACCEPTED' " +
           "ORDER BY ra.respondedAt DESC")
    List<RoommateApplication> findAcceptedApplicationsByAnnouncementId(@Param("announcementId") Long announcementId);
    
    /**
     * Find applications with high compatibility scores (>= threshold)
     * @param announcementId the announcement ID
     * @param minCompatibilityScore minimum compatibility score
     * @param pageable pagination information
     * @return page of high-compatibility applications
     */
    @Query("SELECT ra FROM RoommateApplication ra " +
           "LEFT JOIN FETCH ra.applicant " +
           "WHERE ra.announcement.id = :announcementId " +
           "AND ra.compatibilityScore >= :minCompatibilityScore " +
           "ORDER BY ra.compatibilityScore DESC, ra.appliedAt DESC")
    Page<RoommateApplication> findHighCompatibilityApplications(
            @Param("announcementId") Long announcementId,
            @Param("minCompatibilityScore") java.math.BigDecimal minCompatibilityScore,
            Pageable pageable);
    
    /**
     * Update application status by ID
     * @param applicationId the application ID
     * @param newStatus the new status
     * @return number of updated records
     */
    @Modifying
    @Query("UPDATE RoommateApplication ra SET ra.status = :newStatus, ra.respondedAt = CURRENT_TIMESTAMP " +
           "WHERE ra.id = :applicationId")
    int updateApplicationStatus(@Param("applicationId") Long applicationId, @Param("newStatus") ApplicationStatus newStatus);
    
    /**
     * Find applications by announcement and status
     * @param announcementId the announcement ID
     * @param status the application status
     * @return list of applications
     */
    List<RoommateApplication> findByAnnouncementIdAndStatus(Long announcementId, ApplicationStatus status);
    
    /**
     * Delete applications by announcement ID (cascade when announcement is deleted)
     * @param announcementId the announcement ID
     * @return number of deleted applications
     */
    @Modifying
    @Query("DELETE FROM RoommateApplication ra WHERE ra.announcement.id = :announcementId")
    int deleteByAnnouncementId(@Param("announcementId") Long announcementId);
} 