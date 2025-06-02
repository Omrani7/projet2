package com.example.spring_security.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminStatsDto {
    
    // User Statistics
    private long totalUsers;
    private long activeUsers;
    private long newUsersToday;
    private long newUsersThisWeek;
    private long studentUsers;
    private long ownerUsers;
    private long adminUsers;
    
    // Property Statistics
    private long totalProperties;
    private long activeProperties;
    private long pendingProperties;
    private long propertiesListedToday;
    private long propertiesListedThisWeek;
    
    // Inquiry Statistics
    private long totalInquiries;
    private long pendingInquiries;
    private long acceptedInquiries;
    private long rejectedInquiries;
    private long inquiriesToday;
    private long inquiriesThisWeek;
    
    // Roommate Statistics
    private long totalAnnouncements;
    private long activeAnnouncements;
    private long totalApplications;
    private long successfulMatches;
    private long announcementsToday;
    private long applicationsToday;
    
    // System Statistics
    private double systemLoad;
    private long memoryUsage;
    private long diskUsage;
    private String uptimeHours;
    private int onlineUsers;
    
    // Growth Metrics
    private double userGrowthRate;
    private double propertyGrowthRate;
    private double inquiryGrowthRate;
    private double announcementGrowthRate;
    
    // Engagement Metrics
    private double averageSessionDuration;
    private double conversionRate;
    private double matchSuccessRate;
    
    // Platform Usage Distribution
    private Map<String, Long> usersByRole;
    private Map<String, Long> propertiesByType;
    private Map<String, Long> inquiriesByStatus;
    private Map<String, Long> announcementsByStatus;
    private Map<String, Long> usersByProvider;
    
    // Recent Activity
    private long activitiesLast24Hours;
    private long messagesLast24Hours;
    private long loginAttempts;
    private long failedLoginAttempts;
} 