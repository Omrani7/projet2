package com.example.spring_security.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for owner's reply to an inquiry
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InquiryReplyDTO {
    
    @NotBlank(message = "Reply message cannot be blank")
    private String reply;
} 