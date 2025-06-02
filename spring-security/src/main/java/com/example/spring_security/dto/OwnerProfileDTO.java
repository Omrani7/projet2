package com.example.spring_security.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OwnerProfileDTO {
    private Long id; // Profile ID
    private Long userId; // Associated User ID
    private String fullName;
    private String contactNumber; // This was "Mobile Number" in the form
    private String state; // Changed from Country to State as requested
    private String accommodationType;
    private String propertyManagementSystem;
    private String additionalInformation;
    private Boolean isAgency; // From previous discussions
    // userType will be implicitly OWNER when using this DTO context
} 