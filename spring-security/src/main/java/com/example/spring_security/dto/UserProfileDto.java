package com.example.spring_security.dto;

import com.example.spring_security.model.UserProfile;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true) // Ignore unknown fields like contactNumber
public class UserProfileDto {

    private int id;
    private String fullName;
    private LocalDate dateOfBirth;
    private String fieldOfStudy;
    private String institute;
    private UserProfile.UserType userType;
    private String studentYear;
    private UserProfile.EducationLevel educationLevel;
    private Set<Long> favoritePropertyIds;
    // We might also need userId for context in some scenarios, though UserProfile is tied to a User
    private int userId; 
} 