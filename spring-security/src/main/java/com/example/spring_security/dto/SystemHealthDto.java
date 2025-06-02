package com.example.spring_security.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemHealthDto {
    
    private String overallStatus; // UP, DOWN, DEGRADED
    private LocalDateTime timestamp;
    
    // Database Health
    private DatabaseHealth database;
    
    // Application Health
    private ApplicationHealth application;
    
    // External Services Health
    private ExternalServicesHealth externalServices;
    
    // System Resources
    private SystemResources systemResources;
    
    // Recent Errors
    private List<HealthAlert> recentAlerts;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DatabaseHealth {
        private String status;
        private long connectionPoolSize;
        private long activeConnections;
        private long idleConnections;
        private double averageResponseTime;
        private long totalQueries;
        private long slowQueries;
        private String lastBackup;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ApplicationHealth {
        private String status;
        private String version;
        private String buildDate;
        private long uptime;
        private long heapMemoryUsed;
        private long heapMemoryMax;
        private long nonHeapMemoryUsed;
        private int threadCount;
        private int activeThreads;
        private double cpuUsage;
        private String javaVersion;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ExternalServicesHealth {
        private String emailServiceStatus;
        private String scraperServiceStatus;
        private String websocketServiceStatus;
        private String fileStorageStatus;
        private Map<String, String> thirdPartyServices;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SystemResources {
        private double diskSpaceUsed;
        private double diskSpaceTotal;
        private double diskSpacePercent;
        private double memoryUsed;
        private double memoryTotal;
        private double memoryPercent;
        private double networkIn;
        private double networkOut;
        private int processCount;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class HealthAlert {
        private String severity; // LOW, MEDIUM, HIGH, CRITICAL
        private String message;
        private String component;
        private LocalDateTime timestamp;
        private boolean resolved;
    }
} 