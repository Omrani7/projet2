package com.example.spring_security.dao;

import com.example.spring_security.model.RoommateAnnouncement;
import com.example.spring_security.model.RoommateAnnouncement.AnnouncementStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for RoommateAnnouncement entity operations
 */
@Repository
public interface RoommateAnnouncementRepository extends JpaRepository<RoommateAnnouncement, Long>, JpaSpecificationExecutor<RoommateAnnouncement> {
    
    /**
     * Find all announcements for a specific poster, ordered by creation date descending
     * @param posterId the ID of the poster
     * @param pageable pagination information
     * @return page of announcements
     */
    Page<RoommateAnnouncement> findByPosterIdOrderByCreatedAtDesc(Integer posterId, Pageable pageable);
    
    /**
     * Find all active announcements (status = ACTIVE and not expired)
     * @param pageable pagination information
     * @return page of active announcements
     */
    @Query("SELECT ra FROM RoommateAnnouncement ra " +
           "LEFT JOIN FETCH ra.poster " +
           "WHERE ra.status = 'ACTIVE' AND ra.expiresAt > :currentTime " +
           "ORDER BY ra.createdAt DESC")
    Page<RoommateAnnouncement> findActiveAnnouncements(@Param("currentTime") LocalDateTime currentTime, Pageable pageable);
    
    /**
     * Find announcement by ID with all related entities eagerly fetched
     * @param id the announcement ID
     * @return optional announcement
     */
    @Query("SELECT ra FROM RoommateAnnouncement ra " +
           "LEFT JOIN FETCH ra.poster " +
           "LEFT JOIN FETCH ra.propertyListing " +
           "LEFT JOIN FETCH ra.applications " +
           "WHERE ra.id = :id")
    Optional<RoommateAnnouncement> findByIdWithDetails(@Param("id") Long id);
    
    /**
     * Find announcements by status
     * @param status the announcement status
     * @param pageable pagination information
     * @return page of announcements with specified status
     */
    Page<RoommateAnnouncement> findByStatusOrderByCreatedAtDesc(AnnouncementStatus status, Pageable pageable);
    
    /**
     * Find announcements excluding a specific poster (for browsing)
     * @param posterId the ID of the poster to exclude
     * @param status the announcement status
     * @param currentTime current timestamp to check expiration
     * @param pageable pagination information
     * @return page of announcements
     */
    @Query("SELECT ra FROM RoommateAnnouncement ra " +
           "LEFT JOIN FETCH ra.poster " +
           "WHERE ra.poster.id != :posterId " +
           "AND ra.status = :status " +
           "AND ra.expiresAt > :currentTime " +
           "ORDER BY ra.createdAt DESC")
    Page<RoommateAnnouncement> findByPosterIdNotAndStatusAndExpiresAtAfter(
            @Param("posterId") Integer posterId,
            @Param("status") AnnouncementStatus status,
            @Param("currentTime") LocalDateTime currentTime,
            Pageable pageable);
    
    /**
     * Find announcements within a budget range
     * @param minRent minimum rent per person
     * @param maxRent maximum rent per person
     * @param status announcement status
     * @param currentTime current timestamp
     * @param pageable pagination information
     * @return page of announcements within budget range
     */
    @Query("SELECT ra FROM RoommateAnnouncement ra " +
           "LEFT JOIN FETCH ra.poster " +
           "WHERE ra.rentPerPerson BETWEEN :minRent AND :maxRent " +
           "AND ra.status = :status " +
           "AND ra.expiresAt > :currentTime " +
           "ORDER BY ra.createdAt DESC")
    Page<RoommateAnnouncement> findByRentPerPersonBetweenAndStatusAndExpiresAtAfter(
            @Param("minRent") BigDecimal minRent,
            @Param("maxRent") BigDecimal maxRent,
            @Param("status") AnnouncementStatus status,
            @Param("currentTime") LocalDateTime currentTime,
            Pageable pageable);
    
    /**
     * Find announcements by move-in date range
     * @param startDate earliest move-in date
     * @param endDate latest move-in date
     * @param status announcement status
     * @param currentTime current timestamp
     * @param pageable pagination information
     * @return page of announcements within date range
     */
    @Query("SELECT ra FROM RoommateAnnouncement ra " +
           "LEFT JOIN FETCH ra.poster " +
           "WHERE ra.moveInDate BETWEEN :startDate AND :endDate " +
           "AND ra.status = :status " +
           "AND ra.expiresAt > :currentTime " +
           "ORDER BY ra.moveInDate ASC")
    Page<RoommateAnnouncement> findByMoveInDateBetweenAndStatusAndExpiresAtAfter(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("status") AnnouncementStatus status,
            @Param("currentTime") LocalDateTime currentTime,
            Pageable pageable);
    
    /**
     * Find announcements based on property listing (Type A announcements)
     * @param propertyListingId the property listing ID
     * @return list of announcements for the property
     */
    List<RoommateAnnouncement> findByPropertyListingId(Long propertyListingId);
    
    /**
     * Count active announcements for a specific poster
     * @param posterId the ID of the poster
     * @param currentTime current timestamp
     * @return count of active announcements
     */
    @Query("SELECT COUNT(ra) FROM RoommateAnnouncement ra " +
           "WHERE ra.poster.id = :posterId " +
           "AND ra.status = 'ACTIVE' " +
           "AND ra.expiresAt > :currentTime")
    long countActiveAnnouncementsForPoster(@Param("posterId") Integer posterId, @Param("currentTime") LocalDateTime currentTime);
    
    /**
     * Find announcements that are about to expire (within next 24 hours)
     * @param currentTime current timestamp
     * @param expirationThreshold timestamp 24 hours from now
     * @return list of announcements about to expire
     */
    @Query("SELECT ra FROM RoommateAnnouncement ra " +
           "LEFT JOIN FETCH ra.poster " +
           "WHERE ra.status = 'ACTIVE' " +
           "AND ra.expiresAt BETWEEN :currentTime AND :expirationThreshold " +
           "ORDER BY ra.expiresAt ASC")
    List<RoommateAnnouncement> findAnnouncementsAboutToExpire(
            @Param("currentTime") LocalDateTime currentTime,
            @Param("expirationThreshold") LocalDateTime expirationThreshold);
    
    /**
     * Update announcement status by ID
     * @param announcementId the announcement ID
     * @param newStatus the new status
     * @return number of updated records
     */
    @Modifying
    @Query("UPDATE RoommateAnnouncement ra SET ra.status = :newStatus WHERE ra.id = :announcementId")
    int updateAnnouncementStatus(@Param("announcementId") Long announcementId, @Param("newStatus") AnnouncementStatus newStatus);
    
    /**
     * Find announcements with available spots (applications count < max roommates)
     * @param status announcement status
     * @param currentTime current timestamp
     * @param pageable pagination information
     * @return page of announcements with available spots
     */
    @Query("SELECT ra FROM RoommateAnnouncement ra " +
           "LEFT JOIN FETCH ra.poster " +
           "WHERE ra.status = :status " +
           "AND ra.expiresAt > :currentTime " +
           "AND (SELECT COUNT(app) FROM RoommateApplication app WHERE app.announcement = ra) < ra.maxRoommates " +
           "ORDER BY ra.createdAt DESC")
    Page<RoommateAnnouncement> findAnnouncementsWithAvailableSpots(
            @Param("status") AnnouncementStatus status,
            @Param("currentTime") LocalDateTime currentTime,
            Pageable pageable);
    
    // Admin dashboard statistics methods
    long countByStatus(String status);
    long countByPosterId(Integer posterId);
    long countByCreatedAtAfter(LocalDateTime date);
    long countByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    // Additional admin methods for comprehensive management
    long countByPropertyListingIsNotNull(); // Count Type A announcements (with property listing)
    long countByPropertyListingIsNull(); // Count Type B announcements (without property listing)
} 