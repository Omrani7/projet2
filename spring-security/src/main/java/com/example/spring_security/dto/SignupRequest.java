package com.example.spring_security.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SignupRequest {
    private String email;
    private String username;
    private String password;
    private String phoneNumber;
    private ProfileDetails profileDetails;
    private String role;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProfileDetails {
        private String fullName;
        private String fieldOfStudy;
        private String institute;
        private String userType;
    }
} 