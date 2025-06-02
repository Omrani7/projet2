package com.example.spring_security.service;

import com.example.spring_security.dao.PasswordResetTokenRepository;
import com.example.spring_security.dao.UserProfileRepo;
import com.example.spring_security.dao.UserRepo;
import com.example.spring_security.dto.SignupRequest;
import com.example.spring_security.dto.UserProfileDto;
import com.example.spring_security.exception.ResourceNotFoundException;
import com.example.spring_security.model.PasswordResetToken;
import com.example.spring_security.model.User;
import com.example.spring_security.model.UserProfile;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.HashSet;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
public class UserService {
    private BCryptPasswordEncoder encoder  = new  BCryptPasswordEncoder(12);
   
    // Regex for password validation: at least one lowercase, one uppercase, one digit, and 8+ characters
    private static final Pattern PASSWORD_PATTERN =
            Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$");
   
    @Autowired
    private UserRepo userRepo;
    
    @Autowired
    private UserProfileRepo userProfileRepo;
    
    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;
    
    @Autowired
    private EmailService emailService;

    @Autowired
    private ModelMapper modelMapper;
    
    @Transactional
    public User saveUser(User user){
        user.setPassword(encoder.encode(user.getPassword()));
        return userRepo.save(user);
    }
    
    @Transactional
    public User registerUser(SignupRequest signupRequest) {
        // 1. Improved Email Uniqueness Check
        if (userRepo.existsByEmail(signupRequest.getEmail())) {
            // Throw exception with a clearer message
            throw new IllegalArgumentException("An account with this email address already exists.");
        }

        // 2. Password Strength Validation
        String rawPassword = signupRequest.getPassword();
        if (rawPassword == null || !PASSWORD_PATTERN.matcher(rawPassword).matches()) {
            throw new IllegalArgumentException(
                "Password must be at least 8 characters long and contain at least one uppercase letter, one lowercase letter, and one number."
            );
        }

        User user = new User();
        user.setEmail(signupRequest.getEmail());
        user.setUsername(signupRequest.getUsername());
        // Encode the password *after* validation
        user.setPassword(encoder.encode(rawPassword));
        user.setPhoneNumber(signupRequest.getPhoneNumber());
        user.setProvider(User.AuthProvider.LOCAL);
        user.setEnabled(true);
        
        // Set Role based on request, defaulting to STUDENT
        String requestedRole = signupRequest.getRole();
        if (requestedRole != null && requestedRole.equalsIgnoreCase(User.Role.OWNER.name())) {
            user.setRole(User.Role.OWNER);
        } else {
            // Default to STUDENT if role is null, empty, "STUDENT" (case-insensitive), or anything else (like "ADMIN")
            user.setRole(User.Role.STUDENT); 
        }
        
        User savedUser = userRepo.save(user);
        
        if (signupRequest.getProfileDetails() != null) {
            UserProfile profile = new UserProfile();
            profile.setUser(savedUser);
            profile.setFullName(signupRequest.getProfileDetails().getFullName());
            profile.setFieldOfStudy(signupRequest.getProfileDetails().getFieldOfStudy());
            profile.setInstitute(signupRequest.getProfileDetails().getInstitute());
            
            String userType = signupRequest.getProfileDetails().getUserType();
            if (userType != null) {
                try {
                    profile.setUserType(UserProfile.UserType.valueOf(userType.toUpperCase()));
                } catch (IllegalArgumentException e) {
                    profile.setUserType(UserProfile.UserType.STUDENT); // Default
                }
            }
            
            userProfileRepo.save(profile);
            savedUser.setUserProfile(profile);
        }
        
        return savedUser;
    }
    
    @Transactional
    public PasswordResetToken createPasswordResetTokenForUser(User user) {
        passwordResetTokenRepository.findByUser(user).ifPresent(passwordResetTokenRepository::delete);
        
        PasswordResetToken token = new PasswordResetToken(user);
        passwordResetTokenRepository.save(token);
        
        return token;
    }
    
    @Transactional
    public boolean validatePasswordResetToken(String token) {
        Optional<PasswordResetToken> passwordResetToken = passwordResetTokenRepository.findByToken(token);
        
        if (passwordResetToken.isEmpty()) {
            return false;
        }
        
        if (passwordResetToken.get().isExpired()) {
            passwordResetTokenRepository.delete(passwordResetToken.get());
            return false;
        }
        
        return true;
    }
    
    @Transactional
    public boolean resetPassword(String token, String newPassword) {
        Optional<PasswordResetToken> passwordResetToken = passwordResetTokenRepository.findByToken(token);
        
        if (passwordResetToken.isEmpty() || passwordResetToken.get().isExpired()) {
            return false;
        }
        
        User user = passwordResetToken.get().getUser();
        user.setPassword(encoder.encode(newPassword));
        userRepo.save(user);
        
        passwordResetTokenRepository.delete(passwordResetToken.get());
        
        return true;
    }
    
    public void sendPasswordResetEmail(String email) {
        User user = findByEmail(email);
        System.out.println("Reset password request for email: " + email);
        
        if (user != null) {
            System.out.println("User found, creating reset token");
            PasswordResetToken token = createPasswordResetTokenForUser(user);
            
            try {
                // Try to send actual email
                emailService.sendPasswordResetEmail(user.getEmail(), token.getToken());
            } catch (Exception e) {
                System.out.println("Email sending failed, falling back to logging: " + e.getMessage());
                // Fallback to logging the link
                emailService.logPasswordResetLink(user.getEmail(), token.getToken());
            }
        } else {
            System.out.println("No user found with email: " + email);
        }
    }
    
    @Transactional
    public void cleanExpiredPasswordResetTokens() {
        passwordResetTokenRepository.deleteAllExpiredTokens(new Date());
    }
    
    @Transactional
    public User createOrUpdateOAuth2User(String email, String name, String providerId, User.AuthProvider provider) {
        User user = userRepo.findByEmail(email);
        boolean isNewUser = (user == null);
        
        if (isNewUser) {
            user = new User();
            user.setEmail(email);
            user.setUsername(email.substring(0, email.indexOf('@')));
            user.setProvider(provider);
            user.setProviderId(providerId);
            user.setEnabled(true);
            user.setRole(User.Role.STUDENT); // Default role, can be updated later
            user.setPassword(encoder.encode(generateRandomPassword()));
            
            User savedUser = userRepo.save(user);
            
            UserProfile profile = new UserProfile();
            profile.setUser(savedUser);
            profile.setFullName(name);
            profile.setUserType(UserProfile.UserType.STUDENT); // Default
            
            userProfileRepo.save(profile);
            savedUser.setUserProfile(profile);
            savedUser.setNewOAuth2User(true); // Mark as new user
            
            return savedUser;
        } else {
            user.setProvider(provider);
            user.setProviderId(providerId);
            user.setNewOAuth2User(false); // Existing user
            
            if (user.getUserProfile() != null && user.getUserProfile().getFullName() == null) {
                user.getUserProfile().setFullName(name);
                userProfileRepo.save(user.getUserProfile());
            }
            
            return userRepo.save(user);
        }
    }
    
    @Transactional
    public User updateOAuth2UserRole(int userId, String role) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));
        
        // Only allow STUDENT or OWNER roles
        if (role != null && role.equalsIgnoreCase(User.Role.OWNER.name())) {
            user.setRole(User.Role.OWNER);
            // Update the UserProfile type to match
            if (user.getUserProfile() != null) {
                user.getUserProfile().setUserType(UserProfile.UserType.OWNER);
                // No need to save profile separately if Cascade is appropriate
                // userProfileRepo.save(user.getUserProfile());
            }
        } else {
            user.setRole(User.Role.STUDENT);
            // Update the UserProfile type to match
            if (user.getUserProfile() != null) {
                user.getUserProfile().setUserType(UserProfile.UserType.STUDENT);
                 // No need to save profile separately if Cascade is appropriate
               // userProfileRepo.save(user.getUserProfile());
            }
        }
        
        return userRepo.save(user);
    }
    // In UserService.java
    public Optional<User> findById(int id) {
        return userRepo.findById(id);
    }
    
    private String generateRandomPassword() {
        return java.util.UUID.randomUUID().toString();
    }
    
    public User findByEmail(String email) {
        return userRepo.findByEmail(email);
    }
    
    public User findByUsername(String username) {
        return userRepo.findUserByUsername(username);
    }

    @Transactional(readOnly = true)
    public Optional<UserProfileDto> getStudentUserProfileByUserId(int userId) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        if (user.getRole() != User.Role.STUDENT) {
            // Or return Optional.empty() if preferred over exception for non-students
            throw new AccessDeniedException("User is not a student.");
        }

        UserProfile userProfile = user.getUserProfile();
        if (userProfile == null || userProfile.getUserType() != UserProfile.UserType.STUDENT) {
            // This case implies data inconsistency or a profile not yet fully set up for a student
            return Optional.empty(); 
        }
        
        UserProfileDto dto = modelMapper.map(userProfile, UserProfileDto.class);
        dto.setUserId(userId); // Ensure userId is set in DTO
        return Optional.of(dto);
    }

    @Transactional
    public UserProfileDto updateStudentUserProfile(int userId, UserProfileDto userProfileDto) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        if (user.getRole() != User.Role.STUDENT) {
            throw new AccessDeniedException("User is not a student, profile cannot be updated.");
        }

        UserProfile userProfile = user.getUserProfile();
        if (userProfile == null) {
            userProfile = new UserProfile();
            userProfile.setUser(user);
            userProfile.setUserType(UserProfile.UserType.STUDENT); // Ensure type is STUDENT
        } else if (userProfile.getUserType() != UserProfile.UserType.STUDENT) {
            // If profile exists but is not for a student type, this is an issue.
            // Forcing it to student, or throwing an error, depends on desired behavior.
            // For now, let's assume it should be a STUDENT profile.
            userProfile.setUserType(UserProfile.UserType.STUDENT);
        }

        // Map editable fields from DTO to entity
        // We use modelMapper to map, then explicitly set fields that require logic or protection
        userProfile.setFullName(userProfileDto.getFullName()); // Assuming fullName is editable here
        userProfile.setInstitute(userProfileDto.getInstitute());
        userProfile.setFieldOfStudy(userProfileDto.getFieldOfStudy()); // DTO's fieldOfStudy maps to entity's fieldOfStudy
        userProfile.setStudentYear(userProfileDto.getStudentYear());
        userProfile.setDateOfBirth(userProfileDto.getDateOfBirth()); // Assuming dateOfBirth is editable here
        
        // For collections like favorites, handle them carefully (e.g., clear and add, or more sophisticated merge)
        if (userProfileDto.getFavoritePropertyIds() != null) {
            userProfile.setFavoritePropertyIds(new HashSet<>(userProfileDto.getFavoritePropertyIds()));
        }

        UserProfile savedProfile = userProfileRepo.save(userProfile);
        user.setUserProfile(savedProfile); // Ensure User entity in memory has the updated profile reference
        userRepo.save(user); // Save user to ensure linkage, though Cascade might handle this.

        UserProfileDto resultDto = modelMapper.map(savedProfile, UserProfileDto.class);
        resultDto.setUserId(userId);
        return resultDto;
    }
}
