package com.example.spring_security.dto;

import com.example.spring_security.model.User;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserDto {
    private int id;
    private String email;
    private String username;
    private String phoneNumber;
    private String role;
    private String provider;
    private boolean enabled;
    private LocalDateTime lastLoginAt;
    private String studyField;
    private Integer age;
    private String institute;
    private String profileStatus;
    private int totalProperties;
    private int totalInquiries;
    private int totalAnnouncements;

    // Factory method to create DTO from User entity
    public static AdminUserDto fromUser(User user) {
        AdminUserDto dto = new AdminUserDto();
        dto.setId(user.getId());
        dto.setEmail(user.getEmail());
        dto.setUsername(user.getUsername());
        dto.setPhoneNumber(user.getPhoneNumber());
        dto.setRole(user.getRole().name());
        dto.setProvider(user.getProvider().name());
        dto.setEnabled(user.isEnabled());
        dto.setStudyField(user.getStudyField());
        dto.setAge(user.getAge());
        
        // Set profile status based on UserProfile existence
        if (user.getUserProfile() != null) {
            dto.setProfileStatus("COMPLETE");
            dto.setInstitute(user.getUserProfile().getInstitute());
        } else {
            dto.setProfileStatus("INCOMPLETE");
            dto.setInstitute("Not Set");
        }
        
        // These will be set by the service layer
        dto.setTotalProperties(0);
        dto.setTotalInquiries(0);
        dto.setTotalAnnouncements(0);
        
        return dto;
    }
} 