package com.example.spring_security.service;

import com.example.spring_security.model.RoommateAnnouncement;
import com.example.spring_security.model.User;
import com.example.spring_security.model.UserProfile;
import com.example.spring_security.model.UserRoommatePreferences;
import com.example.spring_security.dao.UserRoommatePreferencesRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

/**
 * Academic-Focused ML Compatibility Service for University-Based Roommate Matching
 * Prioritizes university → field of study → education level → age compatibility
 */
@Service
public class CompatibilityService {
    
    private static final Logger logger = LoggerFactory.getLogger(CompatibilityService.class);
    
    @Autowired
    private UserRoommatePreferencesRepository preferencesRepository;
    
    // NEW ACADEMIC-FOCUSED WEIGHTS for university-based roommate matching (must sum to 1.0)
    private static final double UNIVERSITY_WEIGHT = 0.40;      // 40% - PRIMARY: Same university students
    private static final double STUDY_FIELD_WEIGHT = 0.25;     // 25% - SECONDARY: Same field of study  
    private static final double EDUCATION_LEVEL_WEIGHT = 0.20; // 20% - TERTIARY: Same education level
    private static final double AGE_WEIGHT = 0.15;             // 15% - QUATERNARY: Similar age
    
    // NEW: ROOMMATE ANNOUNCEMENT WEIGHTS (includes preferences) - must sum to 1.0
    private static final double RA_ACADEMIC_WEIGHT = 0.50;     // 50% - Academic compatibility (university + field)
    private static final double RA_LIFESTYLE_WEIGHT = 0.25;    // 25% - Lifestyle preferences compatibility
    private static final double RA_BUDGET_WEIGHT = 0.15;       // 15% - Budget compatibility
    private static final double RA_PERSONAL_WEIGHT = 0.10;     // 10% - Personal traits (cleanliness, social level)
    
    // Field categories for study field similarity
    private static final Map<String, Set<String>> FIELD_CATEGORIES = Map.of(
        "ENGINEERING", Set.of("Computer Science", "Electrical Engineering", "Mechanical Engineering", 
                             "Civil Engineering", "Software Engineering", "Information Technology"),
        "SCIENCES", Set.of("Biology", "Chemistry", "Physics", "Mathematics", "Statistics", 
                          "Environmental Science", "Geology"),
        "BUSINESS", Set.of("Business Administration", "Economics", "Finance", "Marketing", 
                          "Management", "Accounting", "International Business"),
        "HUMANITIES", Set.of("Literature", "History", "Philosophy", "Languages", "Art", 
                            "Music", "Psychology", "Sociology"),
        "MEDICAL", Set.of("Medicine", "Dentistry", "Pharmacy", "Nursing", "Veterinary", 
                         "Public Health", "Physiotherapy"),
        "LAW", Set.of("Law", "Legal Studies", "International Law", "Criminal Justice")
    );
    
    /**
     * Calculate overall compatibility score between applicant and announcement poster
     * NEW ALGORITHM: Focuses on academic compatibility
     * Returns score between 0.00 and 1.00
     */
    public BigDecimal calculateCompatibility(User applicant, RoommateAnnouncement announcement) {
        logger.debug("Calculating ACADEMIC compatibility between user {} and announcement poster {}", 
                    applicant.getId(), announcement.getPoster().getId());
        
        double totalScore = 0.0;
        
        // 1. University compatibility (weight: 40% - PRIMARY)
        double universityScore = calculateUniversityCompatibility(applicant, announcement.getPoster());
        totalScore += universityScore * UNIVERSITY_WEIGHT;
        
        // 2. Field of study compatibility (weight: 25% - SECONDARY)
        double fieldScore = calculateFieldOfStudyCompatibility(applicant, announcement.getPoster());
        totalScore += fieldScore * STUDY_FIELD_WEIGHT;
        
        // 3. Education level compatibility (weight: 20% - TERTIARY)
        double educationScore = calculateEducationLevelCompatibility(applicant, announcement.getPoster());
        totalScore += educationScore * EDUCATION_LEVEL_WEIGHT;
        
        // 4. Age compatibility (weight: 15% - QUATERNARY)
        double ageScore = calculateAgeCompatibility(applicant, announcement.getPoster());
        totalScore += ageScore * AGE_WEIGHT;
        
        // Ensure score is between 0 and 1
        double finalScore = Math.min(Math.max(totalScore, 0.0), 1.0);
        
        logger.debug("ACADEMIC compatibility scores - University: {}, Field: {}, Education: {}, Age: {}, Total: {}",
                    universityScore, fieldScore, educationScore, ageScore, finalScore);
        
        return BigDecimal.valueOf(finalScore).setScale(2, RoundingMode.HALF_UP);
    }
    
    /**
     * Calculate university compatibility score (0.0 to 1.0) - PRIMARY FACTOR
     * Same university = perfect match, different universities = significant penalty
     */
    private double calculateUniversityCompatibility(User applicant, User poster) {
        String applicantUniversity = getUniversityFromProfile(applicant);
        String posterUniversity = getUniversityFromProfile(poster);
        
        if (applicantUniversity == null || posterUniversity == null) {
            logger.debug("Missing university data - applicant: {}, poster: {}", applicantUniversity, posterUniversity);
            return 0.1; // Heavy penalty for missing university data
        }
        
        // Exact university match - perfect score
        if (applicantUniversity.equalsIgnoreCase(posterUniversity)) {
            logger.debug("Perfect university match: {}", applicantUniversity);
            return 1.0;
        }
        
        // Different universities - major penalty since this is primary factor
        logger.debug("Different universities - applicant: {}, poster: {}", applicantUniversity, posterUniversity);
        return 0.2; // Significant penalty but not zero (may still be roommates from different unis)
    }
    
    /**
     * Calculate field of study compatibility score (0.0 to 1.0) - SECONDARY FACTOR
     */
    private double calculateFieldOfStudyCompatibility(User applicant, User poster) {
        String applicantField = getFieldOfStudyFromProfile(applicant);
        String posterField = getFieldOfStudyFromProfile(poster);
        
        if (applicantField == null || posterField == null) {
            logger.debug("Missing field of study data - applicant: {}, poster: {}", applicantField, posterField);
            return 0.3; // Moderate penalty for missing field data
        }
        
        // Exact field match
        if (applicantField.equalsIgnoreCase(posterField)) {
            logger.debug("Perfect field match: {}", applicantField);
            return 1.0;
        }
        
        // Check for related fields within same category
        double categoryScore = calculateFieldCategorySimilarity(applicantField, posterField);
        if (categoryScore > 0.5) {
            logger.debug("Related fields - applicant: {}, poster: {}, score: {}", 
                        applicantField, posterField, categoryScore);
            return categoryScore;
        }
        
        // Different fields
        logger.debug("Different fields - applicant: {}, poster: {}", applicantField, posterField);
        return 0.3; // Moderate penalty for different fields
    }
    
    /**
     * Calculate education level compatibility score (0.0 to 1.0) - TERTIARY FACTOR
     */
    private double calculateEducationLevelCompatibility(User applicant, User poster) {
        UserProfile.EducationLevel applicantLevel = getEducationLevelFromProfile(applicant);
        UserProfile.EducationLevel posterLevel = getEducationLevelFromProfile(poster);
        
        if (applicantLevel == null || posterLevel == null) {
            logger.debug("Missing education level data - applicant: {}, poster: {}", applicantLevel, posterLevel);
            return 0.5; // Neutral for missing education level data
        }
        
        // Same education level - perfect match
        if (applicantLevel == posterLevel) {
            logger.debug("Perfect education level match: {}", applicantLevel);
            return 1.0;
        }
        
        // Calculate compatibility based on education level proximity
        double levelScore = calculateEducationLevelProximity(applicantLevel, posterLevel);
        logger.debug("Education level compatibility - applicant: {}, poster: {}, score: {}", 
                    applicantLevel, posterLevel, levelScore);
        return levelScore;
    }
    
    /**
     * Calculate education level proximity score
     */
    private double calculateEducationLevelProximity(UserProfile.EducationLevel level1, UserProfile.EducationLevel level2) {
        // Define education level hierarchy: BACHELOR -> MASTERS -> PHD
        int level1Value = getEducationLevelValue(level1);
        int level2Value = getEducationLevelValue(level2);
        
        int distance = Math.abs(level1Value - level2Value);
        
        switch (distance) {
            case 0: return 1.0;  // Same level
            case 1: return 0.7;  // Adjacent levels (e.g., Bachelor-Masters)
            case 2: return 0.4;  // Distant levels (e.g., Bachelor-PhD)
            default: return 0.2; // Very different levels
        }
    }
    
    /**
     * Get numeric value for education level
     */
    private int getEducationLevelValue(UserProfile.EducationLevel level) {
        switch (level) {
            case BACHELOR: return 1;
            case MASTERS: return 2;
            case PHD: return 3;
            default: return 0;
        }
    }
    
    /**
     * Calculate age compatibility score (0.0 to 1.0) - QUATERNARY FACTOR
     */
    private double calculateAgeCompatibility(User applicant, User poster) {
        Integer applicantAge = applicant.getAge();
        Integer posterAge = poster.getAge();
        
        if (applicantAge == null || posterAge == null) {
            logger.debug("Missing age data - applicant: {}, poster: {}", applicantAge, posterAge);
            return 0.5; // Neutral score if age data not available
        }
        
        int ageDifference = Math.abs(applicantAge - posterAge);
        
        // Age compatibility scoring
        if (ageDifference <= 1) return 1.0;    // Within 1 year - perfect
        if (ageDifference <= 2) return 0.9;    // Within 2 years - excellent  
        if (ageDifference <= 3) return 0.8;    // Within 3 years - very good
        if (ageDifference <= 5) return 0.6;    // Within 5 years - good
        if (ageDifference <= 8) return 0.4;    // Within 8 years - fair
        return 0.2; // More than 8 years difference - poor
    }
    
    /**
     * Calculate field category similarity score
     */
    private double calculateFieldCategorySimilarity(String field1, String field2) {
        for (Set<String> category : FIELD_CATEGORIES.values()) {
            if (category.contains(field1) && category.contains(field2)) {
                return 0.7; // Same category
            }
        }
        
        // Check for related categories (e.g., Engineering and Sciences)
        boolean field1Engineering = FIELD_CATEGORIES.get("ENGINEERING").contains(field1);
        boolean field2Sciences = FIELD_CATEGORIES.get("SCIENCES").contains(field2);
        boolean field1Sciences = FIELD_CATEGORIES.get("SCIENCES").contains(field1);
        boolean field2Engineering = FIELD_CATEGORIES.get("ENGINEERING").contains(field2);
        
        if ((field1Engineering && field2Sciences) || (field1Sciences && field2Engineering)) {
            return 0.5; // Related technical fields
        }
        
        return 0.3; // Different categories
    }
    
    // Helper methods to extract data from UserProfile
    
    /**
     * Get institute from user profile
     */
    private String getUniversityFromProfile(User user) {
        if (user != null && user.getUserProfile() != null) {
            return user.getUserProfile().getInstitute();
        }
        return null;
    }
    
    /**
     * Get field of study from user profile
     */
    private String getFieldOfStudyFromProfile(User user) {
        if (user != null && user.getUserProfile() != null) {
            return user.getUserProfile().getFieldOfStudy();
        }
        // Fallback to User entity field if available
        if (user != null) {
            return user.getStudyField();
        }
        return null;
    }
    
    /**
     * Get education level from user profile
     */
    private UserProfile.EducationLevel getEducationLevelFromProfile(User user) {
        if (user != null && user.getUserProfile() != null) {
            return user.getUserProfile().getEducationLevel();
        }
        return null;
    }
    
    /**
     * NEW: Calculate compatibility between two users directly (not announcement-based)
     * Used for general student recommendations
     * Uses the same academic-focused scoring system
     */
    public BigDecimal calculateUserToUserCompatibility(User user1, User user2) {
        logger.debug("Calculating direct ACADEMIC compatibility between user {} and user {}", 
                    user1.getId(), user2.getId());
        
        double totalScore = 0.0;
        
        // 1. University compatibility (weight: 40% - PRIMARY)
        double universityScore = calculateUniversityCompatibility(user1, user2);
        totalScore += universityScore * UNIVERSITY_WEIGHT;
        
        // 2. Field of study compatibility (weight: 25% - SECONDARY)
        double fieldScore = calculateFieldOfStudyCompatibility(user1, user2);
        totalScore += fieldScore * STUDY_FIELD_WEIGHT;
        
        // 3. Education level compatibility (weight: 20% - TERTIARY)
        double educationScore = calculateEducationLevelCompatibility(user1, user2);
        totalScore += educationScore * EDUCATION_LEVEL_WEIGHT;
        
        // 4. Age compatibility (weight: 15% - QUATERNARY)
        double ageScore = calculateAgeCompatibilityBetweenUsers(user1, user2);
        totalScore += ageScore * AGE_WEIGHT;
        
        // Ensure score is between 0 and 1
        double finalScore = Math.min(Math.max(totalScore, 0.0), 1.0);
        
        logger.debug("Direct ACADEMIC compatibility scores - University: {}, Field: {}, Education: {}, Age: {}, Total: {}",
                    universityScore, fieldScore, educationScore, ageScore, finalScore);
        
        return BigDecimal.valueOf(finalScore).setScale(2, RoundingMode.HALF_UP);
    }
    
    /**
     * Calculate age compatibility between two users directly
     */
    private double calculateAgeCompatibilityBetweenUsers(User user1, User user2) {
        Integer age1 = user1.getAge();
        Integer age2 = user2.getAge();
        
        if (age1 == null || age2 == null) {
            logger.debug("Missing age data - user1: {}, user2: {}", age1, age2);
            return 0.5; // Neutral for missing age data
        }
        
        int ageDifference = Math.abs(age1 - age2);
        
        // Age compatibility scoring
        if (ageDifference == 0) {
            return 1.0; // Same age - perfect match
        } else if (ageDifference <= 1) {
            return 0.9; // Within 1 year
        } else if (ageDifference <= 2) {
            return 0.8; // Within 2 years  
        } else if (ageDifference <= 3) {
            return 0.6; // Within 3 years
        } else if (ageDifference <= 5) {
            return 0.4; // Within 5 years
        } else {
            return 0.2; // More than 5 years difference
        }
    }

    /**
     * NEW: Calculate compatibility for roommate announcements (includes preferences)
     * This method considers both academic profile AND roommate preferences
     * Used specifically for roommate announcement recommendations
     */
    public BigDecimal calculateRoommateAnnouncementCompatibility(User applicant, RoommateAnnouncement announcement) {
        logger.debug("Calculating ROOMMATE ANNOUNCEMENT compatibility between user {} and announcement poster {}", 
                    applicant.getId(), announcement.getPoster().getId());
        
        double totalScore = 0.0;
        
        // 1. Academic compatibility (50% weight) - combines university and field
        double academicScore = calculateAcademicCompatibilityForAnnouncement(applicant, announcement.getPoster());
        totalScore += academicScore * RA_ACADEMIC_WEIGHT;
        
        // 2. Lifestyle compatibility (25% weight) - uses roommate preferences
        double lifestyleScore = calculateLifestyleCompatibilityForAnnouncement(applicant, announcement);
        totalScore += lifestyleScore * RA_LIFESTYLE_WEIGHT;
        
        // 3. Budget compatibility (15% weight) - announcement rent vs user budget
        double budgetScore = calculateBudgetCompatibilityForAnnouncement(applicant, announcement);
        totalScore += budgetScore * RA_BUDGET_WEIGHT;
        
        // 4. Personal traits compatibility (10% weight) - cleanliness, social level
        double personalScore = calculatePersonalTraitsCompatibility(applicant, announcement);
        totalScore += personalScore * RA_PERSONAL_WEIGHT;
        
        // Ensure score is between 0 and 1
        double finalScore = Math.min(Math.max(totalScore, 0.0), 1.0);
        
        logger.debug("ROOMMATE ANNOUNCEMENT compatibility scores - Academic: {}, Lifestyle: {}, Budget: {}, Personal: {}, Total: {}",
                    academicScore, lifestyleScore, budgetScore, personalScore, finalScore);
        
        return BigDecimal.valueOf(finalScore).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calculate academic compatibility for announcements (university + field combined)
     */
    private double calculateAcademicCompatibilityForAnnouncement(User applicant, User poster) {
        // University compatibility (70% of academic score)
        double universityScore = calculateUniversityCompatibility(applicant, poster);
        
        // Field compatibility (30% of academic score)
        double fieldScore = calculateFieldOfStudyCompatibility(applicant, poster);
        
        return (universityScore * 0.7) + (fieldScore * 0.3);
    }

    /**
     * Calculate lifestyle compatibility using roommate preferences
     */
    private double calculateLifestyleCompatibilityForAnnouncement(User applicant, RoommateAnnouncement announcement) {
        try {
            // Get applicant's roommate preferences
            UserRoommatePreferences applicantPrefs = preferencesRepository.findByUserIdWithUser(applicant.getId())
                    .orElse(null);
            
            if (applicantPrefs == null || applicantPrefs.getLifestyleTags() == null || applicantPrefs.getLifestyleTags().isEmpty()) {
                logger.debug("No lifestyle preferences found for applicant {}, using neutral score", applicant.getId());
                return 0.5; // Neutral score if no preferences
            }
            
            // Get announcement lifestyle preferences
            Set<String> announcementLifestyle = announcement.getLifestyleTags();
            if (announcementLifestyle == null || announcementLifestyle.isEmpty()) {
                logger.debug("No lifestyle preferences in announcement {}, using neutral score", announcement.getId());
                return 0.5; // Neutral score if announcement has no lifestyle preferences
            }
            
            // Calculate Jaccard similarity between lifestyle tags
            Set<String> applicantLifestyle = applicantPrefs.getLifestyleTags();
            Set<String> intersection = new HashSet<>(applicantLifestyle);
            intersection.retainAll(announcementLifestyle);
            
            Set<String> union = new HashSet<>(applicantLifestyle);
            union.addAll(announcementLifestyle);
            
            double similarity = union.isEmpty() ? 0.5 : (double) intersection.size() / union.size();
            
            logger.debug("Lifestyle compatibility - applicant tags: {}, announcement tags: {}, similarity: {}", 
                        applicantLifestyle, announcementLifestyle, similarity);
            
            return similarity;
            
        } catch (Exception e) {
            logger.warn("Error calculating lifestyle compatibility for user {} and announcement {}: {}", 
                       applicant.getId(), announcement.getId(), e.getMessage());
            return 0.5; // Neutral score on error
        }
    }

    /**
     * Calculate budget compatibility between user preferences and announcement rent
     */
    private double calculateBudgetCompatibilityForAnnouncement(User applicant, RoommateAnnouncement announcement) {
        try {
            // Get applicant's budget preferences
            UserRoommatePreferences applicantPrefs = preferencesRepository.findByUserIdWithUser(applicant.getId())
                    .orElse(null);
            
            if (applicantPrefs == null || applicantPrefs.getBudgetMax() == null) {
                logger.debug("No budget preferences found for applicant {}, using neutral score", applicant.getId());
                return 0.5; // Neutral score if no budget preferences
            }
            
            BigDecimal announcementRent = announcement.getRentPerPerson();
            BigDecimal userMaxBudget = applicantPrefs.getBudgetMax();
            BigDecimal userMinBudget = applicantPrefs.getBudgetMin();
            
            // Check if rent is within user's budget range
            if (announcementRent.compareTo(userMaxBudget) > 0) {
                // Rent exceeds max budget - calculate penalty
                double exceedRatio = announcementRent.divide(userMaxBudget, 4, RoundingMode.HALF_UP).doubleValue();
                double penalty = Math.max(0.0, 1.0 - (exceedRatio - 1.0) * 2); // Steep penalty for exceeding budget
                logger.debug("Rent {} exceeds max budget {}, penalty score: {}", announcementRent, userMaxBudget, penalty);
                return penalty;
            }
            
            if (userMinBudget != null && announcementRent.compareTo(userMinBudget) < 0) {
                // Rent is below minimum budget - might be suspicious, moderate penalty
                logger.debug("Rent {} below min budget {}, moderate score", announcementRent, userMinBudget);
                return 0.7;
            }
            
            // Rent is within budget - calculate comfort score
            double budgetUtilization = announcementRent.divide(userMaxBudget, 4, RoundingMode.HALF_UP).doubleValue();
            double comfortScore = 1.0 - (budgetUtilization * 0.3); // Better score for lower utilization
            
            logger.debug("Budget compatibility - rent: {}, max budget: {}, utilization: {}, score: {}", 
                        announcementRent, userMaxBudget, budgetUtilization, comfortScore);
            
            return Math.max(0.7, comfortScore); // Minimum 0.7 for within-budget properties
            
        } catch (Exception e) {
            logger.warn("Error calculating budget compatibility for user {} and announcement {}: {}", 
                       applicant.getId(), announcement.getId(), e.getMessage());
            return 0.5; // Neutral score on error
        }
    }

    /**
     * Calculate personal traits compatibility (cleanliness, social level)
     */
    private double calculatePersonalTraitsCompatibility(User applicant, RoommateAnnouncement announcement) {
        try {
            // Get applicant's personal preferences
            UserRoommatePreferences applicantPrefs = preferencesRepository.findByUserIdWithUser(applicant.getId())
                    .orElse(null);
            
            if (applicantPrefs == null) {
                logger.debug("No personal preferences found for applicant {}, using neutral score", applicant.getId());
                return 0.5; // Neutral score if no preferences
            }
            
            double totalScore = 0.0;
            int factorCount = 0;
            
            // Cleanliness level compatibility (if both specified)
            if (applicantPrefs.getCleanlinessLevel() != null && announcement.getCleanlinessLevel() != null) {
                int cleanlinessDistance = Math.abs(applicantPrefs.getCleanlinessLevel() - announcement.getCleanlinessLevel());
                double cleanlinessScore = Math.max(0.0, 1.0 - (cleanlinessDistance * 0.2)); // 0.2 penalty per level difference
                totalScore += cleanlinessScore;
                factorCount++;
                
                logger.debug("Cleanliness compatibility - applicant: {}, announcement: {}, score: {}", 
                            applicantPrefs.getCleanlinessLevel(), announcement.getCleanlinessLevel(), cleanlinessScore);
            }
            
            // Social level compatibility (if applicant has preference)
            if (applicantPrefs.getSocialLevel() != null) {
                // Estimate announcement social level from lifestyle tags
                double estimatedSocialLevel = estimateSocialLevelFromLifestyle(announcement.getLifestyleTags());
                if (estimatedSocialLevel > 0) {
                    double socialDistance = Math.abs(applicantPrefs.getSocialLevel() - estimatedSocialLevel);
                    double socialScore = Math.max(0.0, 1.0 - (socialDistance * 0.15)); // 0.15 penalty per level difference
                    totalScore += socialScore;
                    factorCount++;
                    
                    logger.debug("Social level compatibility - applicant: {}, estimated announcement: {}, score: {}", 
                                applicantPrefs.getSocialLevel(), estimatedSocialLevel, socialScore);
                }
            }
            
            return factorCount > 0 ? totalScore / factorCount : 0.5; // Average of available factors
            
        } catch (Exception e) {
            logger.warn("Error calculating personal traits compatibility for user {} and announcement {}: {}", 
                       applicant.getId(), announcement.getId(), e.getMessage());
            return 0.5; // Neutral score on error
        }
    }

    /**
     * Estimate social level from lifestyle tags
     */
    private double estimateSocialLevelFromLifestyle(Set<String> lifestyleTags) {
        if (lifestyleTags == null || lifestyleTags.isEmpty()) {
            return 0; // No estimation possible
        }
        
        double socialScore = 3.0; // Start with neutral (middle)
        
        // Adjust based on lifestyle tags
        if (lifestyleTags.contains("SOCIAL") || lifestyleTags.contains("PARTY")) {
            socialScore += 1.5;
        }
        if (lifestyleTags.contains("QUIET") || lifestyleTags.contains("STUDIOUS")) {
            socialScore -= 1.0;
        }
        if (lifestyleTags.contains("NIGHT_OWL")) {
            socialScore += 0.5;
        }
        if (lifestyleTags.contains("EARLY_BIRD")) {
            socialScore -= 0.5;
        }
        
        return Math.max(1.0, Math.min(5.0, socialScore)); // Clamp to 1-5 range
    }
} 