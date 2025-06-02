package com.example.spring_security.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for creating a new inquiry
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InquiryCreateDTO {
    
    @NotNull(message = "Property ID is required")
    private Long propertyId;
    
    @NotBlank(message = "Message cannot be blank")
    private String message;
    
    @NotBlank(message = "Phone number is required")
    private String phoneNumber;
} 