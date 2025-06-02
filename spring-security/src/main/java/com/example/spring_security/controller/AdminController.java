package com.example.spring_security.controller;

import com.example.spring_security.dto.AdminUserDto;
import com.example.spring_security.dto.AdminStatsDto;
import com.example.spring_security.dto.SystemHealthDto;
import com.example.spring_security.dto.AdminPropertyDto;
import com.example.spring_security.dto.AdminRoommateAnnouncementDto;
import com.example.spring_security.model.User;
import com.example.spring_security.model.PropertyListing;
import com.example.spring_security.model.Inquiry;
import com.example.spring_security.model.RoommateAnnouncement;
import com.example.spring_security.service.AdminService;
import com.example.spring_security.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);

    @Autowired
    private AdminService adminService;
    
    @Autowired
    private UserService userService;

    // ==================== DASHBOARD STATS ====================
    
    @GetMapping("/test")
    public ResponseEntity<String> testEndpoint() {
        return ResponseEntity.ok("Admin controller is working!");
    }
    
    @GetMapping("/stats/overview")
    public ResponseEntity<AdminStatsDto> getOverviewStats() {
        try {
        logger.info("Admin requesting overview stats");
        AdminStatsDto stats = adminService.getOverviewStats();
            logger.info("Successfully generated stats: totalUsers={}, totalProperties={}", 
                       stats.getTotalUsers(), stats.getTotalProperties());
        return ResponseEntity.ok(stats);
        } catch (Exception e) {
            logger.error("Error getting overview stats: ", e);
            return ResponseEntity.internalServerError().body(null);
        }
    }
    
    @GetMapping("/stats/system-health")
    public ResponseEntity<SystemHealthDto> getSystemHealth() {
        logger.info("Admin requesting system health");
        SystemHealthDto health = adminService.getSystemHealth();
        return ResponseEntity.ok(health);
    }
    
    @GetMapping("/stats/user-growth")
    public ResponseEntity<Map<String, Object>> getUserGrowthStats(
            @RequestParam(defaultValue = "30") int days) {
        logger.info("Admin requesting user growth stats for {} days", days);
        Map<String, Object> growth = adminService.getUserGrowthStats(days);
        return ResponseEntity.ok(growth);
    }
    
    @GetMapping("/stats/activity")
    public ResponseEntity<Map<String, Object>> getActivityStats(
            @RequestParam(defaultValue = "7") int days) {
        logger.info("Admin requesting activity stats for {} days", days);
        Map<String, Object> activity = adminService.getActivityStats(days);
        return ResponseEntity.ok(activity);
    }

    // ==================== USER MANAGEMENT ====================
    
    @GetMapping("/users")
    public ResponseEntity<Page<AdminUserDto>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String role) {
        
        logger.info("Admin requesting users - page: {}, size: {}, search: {}, role: {}", page, size, search, role);
        
        Sort sort = Sort.by(sortDir.equals("desc") ? Sort.Direction.DESC : Sort.Direction.ASC, sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<AdminUserDto> users = adminService.getAllUsers(pageable, search, role);
        return ResponseEntity.ok(users);
    }
    
    @GetMapping("/users/{userId}")
    public ResponseEntity<AdminUserDto> getUserDetails(@PathVariable int userId) {
        logger.info("Admin requesting user details for ID: {}", userId);
        Optional<AdminUserDto> userDto = adminService.getUserDetails(userId);
        return userDto.map(ResponseEntity::ok)
                     .orElse(ResponseEntity.notFound().build());
    }
    
    @PutMapping("/users/{userId}/role")
    public ResponseEntity<String> updateUserRole(
            @PathVariable int userId,
            @RequestBody Map<String, String> request) {
        
        String newRole = request.get("role");
        logger.info("Admin updating user {} role to {}", userId, newRole);
        
        try {
            adminService.updateUserRole(userId, newRole);
            return ResponseEntity.ok("User role updated successfully");
        } catch (Exception e) {
            logger.error("Error updating user role: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Error updating user role: " + e.getMessage());
        }
    }
    
    @PutMapping("/users/{userId}/status")
    public ResponseEntity<String> updateUserStatus(
            @PathVariable int userId,
            @RequestBody Map<String, Boolean> request) {
        
        Boolean enabled = request.get("enabled");
        logger.info("Admin updating user {} status to {}", userId, enabled ? "enabled" : "disabled");
        
        try {
            adminService.updateUserStatus(userId, enabled);
            return ResponseEntity.ok("User status updated successfully");
        } catch (Exception e) {
            logger.error("Error updating user status: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Error updating user status: " + e.getMessage());
        }
    }
    
    @DeleteMapping("/users/{userId}")
    public ResponseEntity<String> deleteUser(@PathVariable int userId) {
        logger.info("Admin requesting deletion of user ID: {}", userId);
        
        try {
            adminService.deleteUser(userId);
            return ResponseEntity.ok("User deleted successfully");
        } catch (Exception e) {
            logger.error("Error deleting user: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Error deleting user: " + e.getMessage());
        }
    }

    // ==================== PROPERTY MANAGEMENT ====================
    
    @GetMapping("/properties")
    public ResponseEntity<Page<AdminPropertyDto>> getAllProperties(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status) {
        
        logger.info("Admin requesting properties - page: {}, size: {}, search: {}, status: {}", page, size, search, status);
        
        Sort sort = Sort.by(sortDir.equals("desc") ? Sort.Direction.DESC : Sort.Direction.ASC, sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<AdminPropertyDto> properties = adminService.getAllProperties(pageable, search, status);
        return ResponseEntity.ok(properties);
    }
    
    @DeleteMapping("/properties/{propertyId}")
    public ResponseEntity<String> deleteProperty(@PathVariable Long propertyId) {
        logger.info("Admin requesting deletion of property ID: {}", propertyId);
        
        try {
            adminService.deleteProperty(propertyId);
            return ResponseEntity.ok("Property deleted successfully");
        } catch (Exception e) {
            logger.error("Error deleting property: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Error deleting property: " + e.getMessage());
        }
    }
    
    @PutMapping("/properties/{propertyId}/status")
    public ResponseEntity<String> updatePropertyStatus(
            @PathVariable Long propertyId,
            @RequestBody Map<String, String> request) {
        
        String status = request.get("status");
        logger.info("Admin updating property {} status to {}", propertyId, status);
        
        try {
            adminService.updatePropertyStatus(propertyId, status);
            return ResponseEntity.ok("Property status updated successfully");
        } catch (Exception e) {
            logger.error("Error updating property status: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Error updating property status: " + e.getMessage());
        }
    }

    // ==================== ROOMMATE MANAGEMENT ====================
    
    @GetMapping("/roommate-announcements")
    public ResponseEntity<Page<AdminRoommateAnnouncementDto>> getAllRoommateAnnouncements(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status) {
        
        logger.info("Admin requesting roommate announcements - page: {}, size: {}, search: {}, status: {}", page, size, search, status);
        
        Sort sort = Sort.by(sortDir.equals("desc") ? Sort.Direction.DESC : Sort.Direction.ASC, sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<AdminRoommateAnnouncementDto> announcements = adminService.getAllRoommateAnnouncements(pageable, search, status);
        return ResponseEntity.ok(announcements);
    }
    
    @GetMapping("/roommate-announcements/{announcementId}")
    public ResponseEntity<AdminRoommateAnnouncementDto> getRoommateAnnouncementDetails(@PathVariable Long announcementId) {
        logger.info("Admin requesting roommate announcement details for ID: {}", announcementId);
        Optional<AdminRoommateAnnouncementDto> announcementDto = adminService.getRoommateAnnouncementDetails(announcementId);
        return announcementDto.map(ResponseEntity::ok)
                             .orElse(ResponseEntity.notFound().build());
    }
    
    @PutMapping("/roommate-announcements/{announcementId}/status")
    public ResponseEntity<String> updateRoommateAnnouncementStatus(
            @PathVariable Long announcementId,
            @RequestBody Map<String, String> request) {
        
        String status = request.get("status");
        logger.info("Admin updating roommate announcement {} status to {}", announcementId, status);
        
        try {
            adminService.updateRoommateAnnouncementStatus(announcementId, status);
            return ResponseEntity.ok("Roommate announcement status updated successfully");
        } catch (Exception e) {
            logger.error("Error updating roommate announcement status: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Error updating status: " + e.getMessage());
        }
    }
    
    @DeleteMapping("/roommate-announcements/{announcementId}")
    public ResponseEntity<String> deleteRoommateAnnouncement(@PathVariable Long announcementId) {
        logger.info("Admin requesting deletion of roommate announcement ID: {}", announcementId);
        
        try {
            adminService.deleteRoommateAnnouncement(announcementId);
            return ResponseEntity.ok("Roommate announcement deleted successfully");
        } catch (Exception e) {
            logger.error("Error deleting roommate announcement: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Error deleting announcement: " + e.getMessage());
        }
    }
    
    @GetMapping("/roommate-announcements/stats")
    public ResponseEntity<Map<String, Object>> getRoommateAnnouncementStats() {
        logger.info("Admin requesting roommate announcement statistics");
        Map<String, Object> stats = adminService.getRoommateAnnouncementStats();
        return ResponseEntity.ok(stats);
    }

    // ==================== INQUIRY MANAGEMENT ====================
    
    @GetMapping("/inquiries")
    public ResponseEntity<Page<Inquiry>> getAllInquiries(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status) {
        
        logger.info("Admin requesting inquiries - page: {}, size: {}", page, size);
        
        Sort sort = Sort.by(sortDir.equals("desc") ? Sort.Direction.DESC : Sort.Direction.ASC, sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<Inquiry> inquiries = adminService.getAllInquiries(pageable, search, status);
        return ResponseEntity.ok(inquiries);
    }

    // ==================== SYSTEM ACTIONS ====================
    
    @PostMapping("/system/clear-cache")
    public ResponseEntity<String> clearSystemCache() {
        logger.info("Admin requesting system cache clear");
        
        try {
            adminService.clearSystemCache();
            return ResponseEntity.ok("System cache cleared successfully");
        } catch (Exception e) {
            logger.error("Error clearing system cache: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Error clearing cache: " + e.getMessage());
        }
    }
    
    // ==================== DATA EXPORT ENDPOINTS ====================
    
    @PostMapping("/export/users")
    public ResponseEntity<String> exportUsersReport(@RequestBody Map<String, String> request) {
        String format = request.getOrDefault("format", "csv");
        logger.info("Admin requesting users export in {} format", format);
        
        try {
            String fileName = adminService.exportUsersReport(format);
            String absolutePath = new java.io.File("reports/" + fileName).getAbsolutePath();
            return ResponseEntity.ok("Users report generated successfully!\nFile saved to: " + absolutePath);
        } catch (Exception e) {
            logger.error("Error exporting users report: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Error generating users report: " + e.getMessage());
        }
    }
    
    @PostMapping("/export/properties")
    public ResponseEntity<String> exportPropertiesReport(@RequestBody Map<String, String> request) {
        String format = request.getOrDefault("format", "csv");
        logger.info("Admin requesting properties export in {} format", format);
        
        try {
            String fileName = adminService.exportPropertiesReport(format);
            String absolutePath = new java.io.File("reports/" + fileName).getAbsolutePath();
            return ResponseEntity.ok("Properties report generated successfully!\nFile saved to: " + absolutePath);
        } catch (Exception e) {
            logger.error("Error exporting properties report: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Error generating properties report: " + e.getMessage());
        }
    }
    
    @PostMapping("/export/inquiries")
    public ResponseEntity<String> exportInquiriesReport(@RequestBody Map<String, String> request) {
        String format = request.getOrDefault("format", "csv");
        logger.info("Admin requesting inquiries export in {} format", format);
        
        try {
            String fileName = adminService.exportInquiriesReport(format);
            String absolutePath = new java.io.File("reports/" + fileName).getAbsolutePath();
            return ResponseEntity.ok("Inquiries report generated successfully!\nFile saved to: " + absolutePath);
        } catch (Exception e) {
            logger.error("Error exporting inquiries report: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Error generating inquiries report: " + e.getMessage());
        }
    }
    
    @PostMapping("/export/announcements")
    public ResponseEntity<String> exportAnnouncementsReport(@RequestBody Map<String, String> request) {
        String format = request.getOrDefault("format", "csv");
        logger.info("Admin requesting announcements export in {} format", format);
        
        try {
            String fileName = adminService.exportAnnouncementsReport(format);
            String absolutePath = new java.io.File("reports/" + fileName).getAbsolutePath();
            return ResponseEntity.ok("Announcements report generated successfully!\nFile saved to: " + absolutePath);
        } catch (Exception e) {
            logger.error("Error exporting announcements report: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Error generating announcements report: " + e.getMessage());
        }
    }
    
    @PostMapping("/export/analytics")
    public ResponseEntity<String> exportAnalyticsReport(@RequestBody Map<String, String> request) {
        String format = request.getOrDefault("format", "csv");
        logger.info("Admin requesting analytics export in {} format", format);
        
        try {
            String fileName = adminService.exportSystemAnalyticsReport(format);
            String absolutePath = new java.io.File("reports/" + fileName).getAbsolutePath();
            return ResponseEntity.ok("Analytics report generated successfully!\nFile saved to: " + absolutePath);
        } catch (Exception e) {
            logger.error("Error exporting analytics report: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Error generating analytics report: " + e.getMessage());
        }
    }
    
    @PostMapping("/system/backup")
    public ResponseEntity<String> backupDatabase() {
        logger.info("Admin requesting database backup");
        
        try {
            String backupFile = adminService.backupDatabase();
            return ResponseEntity.ok("Database backup created: " + backupFile);
        } catch (Exception e) {
            logger.error("Error creating database backup: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Error creating backup: " + e.getMessage());
        }
    }
    
    @GetMapping("/logs/recent")
    public ResponseEntity<List<String>> getRecentLogs(
            @RequestParam(defaultValue = "100") int lines) {
        logger.info("Admin requesting recent logs - {} lines", lines);
        
        try {
            List<String> logs = adminService.getRecentLogs(lines);
            return ResponseEntity.ok(logs);
        } catch (Exception e) {
            logger.error("Error retrieving logs: {}", e.getMessage());
            return ResponseEntity.badRequest().body(List.of("Error retrieving logs: " + e.getMessage()));
        }
    }
} 