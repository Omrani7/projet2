package com.example.spring_security.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Data Transfer Object for Inquiry entity
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InquiryDTO {
    private Long id;
    private UserBasicDTO student;
    private UserBasicDTO owner;
    private PropertyListingBasicDTO property;
    private String message;
    private LocalDateTime timestamp;
    private String reply;
    private LocalDateTime replyTimestamp;
    private String status;
    private String studentPhoneNumber;
    private String ownerPhoneNumber;
    
    // Additional field for closed deals (roommate feature)
    private BigDecimal agreedPrice;
} 