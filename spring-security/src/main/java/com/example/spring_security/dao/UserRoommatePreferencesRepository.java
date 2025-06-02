package com.example.spring_security.dao;

import com.example.spring_security.model.UserRoommatePreferences;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for UserRoommatePreferences entity operations
 */
@Repository
public interface UserRoommatePreferencesRepository extends JpaRepository<UserRoommatePreferences, Integer> {
    
    /**
     * Find preferences by user ID with user details
     * @param userId the ID of the user
     * @return optional preferences
     */
    @Query("SELECT urp FROM UserRoommatePreferences urp " +
           "LEFT JOIN FETCH urp.user " +
           "WHERE urp.userId = :userId")
    Optional<UserRoommatePreferences> findByUserIdWithUser(@Param("userId") Integer userId);
    
    /**
     * Find users with similar cleanliness levels (within 1 level difference)
     * @param cleanlinessLevel the target cleanliness level
     * @param userId the user ID to exclude from results
     * @return list of users with similar cleanliness preferences
     */
    @Query("SELECT urp FROM UserRoommatePreferences urp " +
           "LEFT JOIN FETCH urp.user " +
           "WHERE urp.cleanlinessLevel BETWEEN :cleanlinessLevel - 1 AND :cleanlinessLevel + 1 " +
           "AND urp.userId != :userId " +
           "AND urp.cleanlinessLevel IS NOT NULL")
    List<UserRoommatePreferences> findUsersWithSimilarCleanliness(
            @Param("cleanlinessLevel") Integer cleanlinessLevel,
            @Param("userId") Integer userId);
    
    /**
     * Find users with similar social levels (within 1 level difference)
     * @param socialLevel the target social level
     * @param userId the user ID to exclude from results
     * @return list of users with similar social preferences
     */
    @Query("SELECT urp FROM UserRoommatePreferences urp " +
           "LEFT JOIN FETCH urp.user " +
           "WHERE urp.socialLevel BETWEEN :socialLevel - 1 AND :socialLevel + 1 " +
           "AND urp.userId != :userId " +
           "AND urp.socialLevel IS NOT NULL")
    List<UserRoommatePreferences> findUsersWithSimilarSocialLevel(
            @Param("socialLevel") Integer socialLevel,
            @Param("userId") Integer userId);
    
    /**
     * Find users within a budget range
     * @param minBudget minimum budget
     * @param maxBudget maximum budget
     * @param userId the user ID to exclude from results
     * @return list of users with overlapping budget ranges
     */
    @Query("SELECT urp FROM UserRoommatePreferences urp " +
           "LEFT JOIN FETCH urp.user " +
           "WHERE urp.userId != :userId " +
           "AND ((urp.budgetMin <= :maxBudget AND urp.budgetMax >= :minBudget) " +
           "OR (urp.budgetMin IS NULL OR urp.budgetMax IS NULL))")
    List<UserRoommatePreferences> findUsersWithOverlappingBudget(
            @Param("minBudget") BigDecimal minBudget,
            @Param("maxBudget") BigDecimal maxBudget,
            @Param("userId") Integer userId);
    
    /**
     * Find users with specific lifestyle tags
     * @param lifestyleTag the lifestyle tag to search for
     * @param userId the user ID to exclude from results
     * @return list of users with the specified lifestyle tag
     */
    @Query("SELECT urp FROM UserRoommatePreferences urp " +
           "LEFT JOIN FETCH urp.user " +
           "WHERE :lifestyleTag MEMBER OF urp.lifestyleTags " +
           "AND urp.userId != :userId")
    List<UserRoommatePreferences> findUsersWithLifestyleTag(
            @Param("lifestyleTag") String lifestyleTag,
            @Param("userId") Integer userId);
    
    /**
     * Find users with specific study habits
     * @param studyHabit the study habit to search for
     * @param userId the user ID to exclude from results
     * @return list of users with the specified study habit
     */
    @Query("SELECT urp FROM UserRoommatePreferences urp " +
           "LEFT JOIN FETCH urp.user " +
           "WHERE :studyHabit MEMBER OF urp.studyHabits " +
           "AND urp.userId != :userId")
    List<UserRoommatePreferences> findUsersWithStudyHabit(
            @Param("studyHabit") String studyHabit,
            @Param("userId") Integer userId);
    
    /**
     * Find users within a geographic radius (for location-based matching)
     * @param latitude the center latitude
     * @param longitude the center longitude
     * @param radiusKm the radius in kilometers
     * @param userId the user ID to exclude from results
     * @return list of users within the specified radius
     */
    @Query("SELECT urp FROM UserRoommatePreferences urp " +
           "LEFT JOIN FETCH urp.user " +
           "WHERE urp.userId != :userId " +
           "AND urp.preferredLocationLatitude IS NOT NULL " +
           "AND urp.preferredLocationLongitude IS NOT NULL " +
           "AND (6371 * acos(cos(radians(:latitude)) * cos(radians(urp.preferredLocationLatitude)) * " +
           "cos(radians(urp.preferredLocationLongitude) - radians(:longitude)) + " +
           "sin(radians(:latitude)) * sin(radians(urp.preferredLocationLatitude)))) <= :radiusKm")
    List<UserRoommatePreferences> findUsersWithinRadius(
            @Param("latitude") Double latitude,
            @Param("longitude") Double longitude,
            @Param("radiusKm") Integer radiusKm,
            @Param("userId") Integer userId);
    
    /**
     * Find all users with complete preferences (for ML algorithm)
     * @param userId the user ID to exclude from results
     * @return list of users with complete preference profiles
     */
    @Query("SELECT urp FROM UserRoommatePreferences urp " +
           "LEFT JOIN FETCH urp.user " +
           "WHERE urp.userId != :userId " +
           "AND urp.cleanlinessLevel IS NOT NULL " +
           "AND urp.socialLevel IS NOT NULL " +
           "AND SIZE(urp.lifestyleTags) > 0")
    List<UserRoommatePreferences> findUsersWithCompletePreferences(@Param("userId") Integer userId);
    
    /**
     * Count users with preferences set
     * @return count of users with roommate preferences
     */
    @Query("SELECT COUNT(urp) FROM UserRoommatePreferences urp " +
           "WHERE urp.cleanlinessLevel IS NOT NULL " +
           "OR urp.socialLevel IS NOT NULL " +
           "OR SIZE(urp.lifestyleTags) > 0")
    long countUsersWithPreferences();
    
    /**
     * Check if user has preferences set
     * @param userId the user ID
     * @return true if user has any preferences set
     */
    @Query("SELECT COUNT(urp) > 0 FROM UserRoommatePreferences urp " +
           "WHERE urp.userId = :userId " +
           "AND (urp.cleanlinessLevel IS NOT NULL " +
           "OR urp.socialLevel IS NOT NULL " +
           "OR SIZE(urp.lifestyleTags) > 0)")
    boolean hasPreferencesSet(@Param("userId") Integer userId);
} 