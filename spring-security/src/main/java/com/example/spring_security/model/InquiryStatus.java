package com.example.spring_security.model;

/**
 * Enum representing the different states of a property inquiry
 */
public enum InquiryStatus {
    /**
     * Inquiry has been sent but not yet replied to by the owner
     */
    PENDING_REPLY,
    
    /**
     * Owner has replied to the inquiry
     */
    REPLIED,
    
    /**
     * Inquiry has been closed (e.g., property rented, no longer available, etc.)
     */
    CLOSED,
    
    /**
     * Property is no longer available (deal closed with another student)
     * This status is automatically applied to other inquiries when an owner closes a deal
     */
    PROPERTY_NO_LONGER_AVAILABLE
} 