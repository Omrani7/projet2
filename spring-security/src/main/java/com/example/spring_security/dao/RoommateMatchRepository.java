package com.example.spring_security.dao;

import com.example.spring_security.model.RoommateMatch;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for RoommateMatch entity operations
 */
@Repository
public interface RoommateMatchRepository extends JpaRepository<RoommateMatch, Long> {
    
    /**
     * Find all matches for a specific user, ordered by compatibility score descending
     * @param userId the ID of the user
     * @param pageable pagination information
     * @return page of matches
     */
    @Query("SELECT rm FROM RoommateMatch rm " +
           "LEFT JOIN FETCH rm.recommendedUser " +
           "LEFT JOIN FETCH rm.announcement " +
           "WHERE rm.user.id = :userId " +
           "ORDER BY rm.compatibilityScore DESC, rm.createdAt DESC")
    Page<RoommateMatch> findByUserIdOrderByCompatibilityScoreDesc(@Param("userId") Integer userId, Pageable pageable);
    
    /**
     * Find matches for a specific announcement, ordered by compatibility score descending
     * @param announcementId the ID of the announcement
     * @param pageable pagination information
     * @return page of matches
     */
    @Query("SELECT rm FROM RoommateMatch rm " +
           "LEFT JOIN FETCH rm.user " +
           "LEFT JOIN FETCH rm.recommendedUser " +
           "WHERE rm.announcement.id = :announcementId " +
           "ORDER BY rm.compatibilityScore DESC, rm.createdAt DESC")
    Page<RoommateMatch> findByAnnouncementIdOrderByCompatibilityScoreDesc(@Param("announcementId") Long announcementId, Pageable pageable);
    
    /**
     * Find high-quality matches (compatibility score >= threshold)
     * @param userId the ID of the user
     * @param minScore minimum compatibility score
     * @param pageable pagination information
     * @return page of high-quality matches
     */
    @Query("SELECT rm FROM RoommateMatch rm " +
           "LEFT JOIN FETCH rm.recommendedUser " +
           "LEFT JOIN FETCH rm.announcement " +
           "WHERE rm.user.id = :userId " +
           "AND rm.compatibilityScore >= :minScore " +
           "ORDER BY rm.compatibilityScore DESC, rm.createdAt DESC")
    Page<RoommateMatch> findHighQualityMatches(
            @Param("userId") Integer userId,
            @Param("minScore") BigDecimal minScore,
            Pageable pageable);
    
    /**
     * Find unviewed matches for a user
     * @param userId the ID of the user
     * @param pageable pagination information
     * @return page of unviewed matches
     */
    @Query("SELECT rm FROM RoommateMatch rm " +
           "LEFT JOIN FETCH rm.recommendedUser " +
           "LEFT JOIN FETCH rm.announcement " +
           "WHERE rm.user.id = :userId " +
           "AND rm.viewed = false " +
           "ORDER BY rm.compatibilityScore DESC, rm.createdAt DESC")
    Page<RoommateMatch> findUnviewedMatches(@Param("userId") Integer userId, Pageable pageable);
    
    /**
     * Find existing match between two users for a specific announcement
     * @param userId the ID of the user
     * @param recommendedUserId the ID of the recommended user
     * @param announcementId the ID of the announcement
     * @return optional match
     */
    Optional<RoommateMatch> findByUserIdAndRecommendedUserIdAndAnnouncementId(
            Integer userId, Integer recommendedUserId, Long announcementId);
    
    /**
     * Check if a match exists between two users for a specific announcement
     * @param userId the ID of the user
     * @param recommendedUserId the ID of the recommended user
     * @param announcementId the ID of the announcement
     * @return true if match exists
     */
    boolean existsByUserIdAndRecommendedUserIdAndAnnouncementId(
            Integer userId, Integer recommendedUserId, Long announcementId);
    
    /**
     * Count total matches for a user
     * @param userId the ID of the user
     * @return count of matches
     */
    long countByUserId(Integer userId);
    
    /**
     * Count unviewed matches for a user
     * @param userId the ID of the user
     * @return count of unviewed matches
     */
    long countByUserIdAndViewedFalse(Integer userId);
    
    /**
     * Count successful matches (applied and accepted) for a user
     * @param userId the ID of the user
     * @return count of successful matches
     */
    @Query("SELECT COUNT(rm) FROM RoommateMatch rm " +
           "WHERE rm.user.id = :userId " +
           "AND rm.applied = true " +
           "AND rm.applicationSuccessful = true")
    long countSuccessfulMatches(@Param("userId") Integer userId);
    
    /**
     * Find matches that led to applications
     * @param userId the ID of the user
     * @param pageable pagination information
     * @return page of matches that resulted in applications
     */
    @Query("SELECT rm FROM RoommateMatch rm " +
           "LEFT JOIN FETCH rm.recommendedUser " +
           "LEFT JOIN FETCH rm.announcement " +
           "WHERE rm.user.id = :userId " +
           "AND rm.applied = true " +
           "ORDER BY rm.appliedAt DESC")
    Page<RoommateMatch> findMatchesThatLedToApplications(@Param("userId") Integer userId, Pageable pageable);
    
    /**
     * Find successful matches (applied and accepted)
     * @param userId the ID of the user
     * @param pageable pagination information
     * @return page of successful matches
     */
    @Query("SELECT rm FROM RoommateMatch rm " +
           "LEFT JOIN FETCH rm.recommendedUser " +
           "LEFT JOIN FETCH rm.announcement " +
           "WHERE rm.user.id = :userId " +
           "AND rm.applied = true " +
           "AND rm.applicationSuccessful = true " +
           "ORDER BY rm.appliedAt DESC")
    Page<RoommateMatch> findSuccessfulMatches(@Param("userId") Integer userId, Pageable pageable);
    
    /**
     * Mark a match as viewed
     * @param matchId the ID of the match
     * @return number of updated records
     */
    @Modifying
    @Query("UPDATE RoommateMatch rm SET rm.viewed = true, rm.viewedAt = CURRENT_TIMESTAMP " +
           "WHERE rm.id = :matchId AND rm.viewed = false")
    int markAsViewed(@Param("matchId") Long matchId);
    
    /**
     * Mark a match as clicked
     * @param matchId the ID of the match
     * @return number of updated records
     */
    @Modifying
    @Query("UPDATE RoommateMatch rm SET rm.clicked = true, rm.clickedAt = CURRENT_TIMESTAMP, " +
           "rm.viewed = true, rm.viewedAt = COALESCE(rm.viewedAt, CURRENT_TIMESTAMP) " +
           "WHERE rm.id = :matchId AND rm.clicked = false")
    int markAsClicked(@Param("matchId") Long matchId);
    
    /**
     * Mark a match as applied
     * @param matchId the ID of the match
     * @return number of updated records
     */
    @Modifying
    @Query("UPDATE RoommateMatch rm SET rm.applied = true, rm.appliedAt = CURRENT_TIMESTAMP, " +
           "rm.clicked = true, rm.clickedAt = COALESCE(rm.clickedAt, CURRENT_TIMESTAMP), " +
           "rm.viewed = true, rm.viewedAt = COALESCE(rm.viewedAt, CURRENT_TIMESTAMP) " +
           "WHERE rm.id = :matchId AND rm.applied = false")
    int markAsApplied(@Param("matchId") Long matchId);
    
    /**
     * Mark a match as successful (application accepted)
     * @param matchId the ID of the match
     * @return number of updated records
     */
    @Modifying
    @Query("UPDATE RoommateMatch rm SET rm.applicationSuccessful = true " +
           "WHERE rm.id = :matchId AND rm.applied = true")
    int markAsSuccessful(@Param("matchId") Long matchId);
    
    /**
     * Find matches created within a time period (for analytics)
     * @param startTime start of the time period
     * @param endTime end of the time period
     * @return list of matches created in the period
     */
    @Query("SELECT rm FROM RoommateMatch rm " +
           "WHERE rm.createdAt BETWEEN :startTime AND :endTime " +
           "ORDER BY rm.createdAt DESC")
    List<RoommateMatch> findMatchesCreatedBetween(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);
    
    /**
     * Calculate conversion rate for matches (applied / viewed)
     * @param userId the ID of the user
     * @return conversion rate as decimal
     */
    @Query("SELECT CASE WHEN COUNT(rm) = 0 THEN 0.0 " +
           "ELSE CAST(SUM(CASE WHEN rm.applied = true THEN 1 ELSE 0 END) AS DOUBLE) / COUNT(rm) END " +
           "FROM RoommateMatch rm " +
           "WHERE rm.user.id = :userId AND rm.viewed = true")
    Double calculateConversionRate(@Param("userId") Integer userId);
    
    /**
     * Find matches by quality category (high, medium, low)
     * @param userId the ID of the user
     * @param minScore minimum score for the category
     * @param maxScore maximum score for the category
     * @param pageable pagination information
     * @return page of matches in the quality range
     */
    @Query("SELECT rm FROM RoommateMatch rm " +
           "LEFT JOIN FETCH rm.recommendedUser " +
           "LEFT JOIN FETCH rm.announcement " +
           "WHERE rm.user.id = :userId " +
           "AND rm.compatibilityScore BETWEEN :minScore AND :maxScore " +
           "ORDER BY rm.compatibilityScore DESC, rm.createdAt DESC")
    Page<RoommateMatch> findMatchesByQualityRange(
            @Param("userId") Integer userId,
            @Param("minScore") BigDecimal minScore,
            @Param("maxScore") BigDecimal maxScore,
            Pageable pageable);
    
    /**
     * Delete matches by announcement ID (cascade when announcement is deleted)
     * @param announcementId the ID of the announcement
     * @return number of deleted matches
     */
    @Modifying
    @Query("DELETE FROM RoommateMatch rm WHERE rm.announcement.id = :announcementId")
    int deleteByAnnouncementId(@Param("announcementId") Long announcementId);
} 