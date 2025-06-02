package com.example.spring_security.exception;

/**
 * Exception thrown when a user tries to access a resource they don't have permission for
 */
public class AccessDeniedException extends RuntimeException {
    
    public AccessDeniedException(String message) {
        super(message);
    }
    
    public AccessDeniedException(String message, Throwable cause) {
        super(message, cause);
    }
} 