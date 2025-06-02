package com.example.spring_security.controller;

import com.example.spring_security.dto.UserProfileDto;
import com.example.spring_security.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profiles/students")
@RequiredArgsConstructor
public class StudentProfileController {

    private final UserService userService;

    // GET /api/profiles/students/{userId}
    // Assuming {userId} is the ID of the User entity
    // Add authorization: e.g., only admin or the student themselves can access
    @GetMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('STUDENT') and #userId == authentication.principal.id)")
    public ResponseEntity<UserProfileDto> getStudentProfile(@PathVariable int userId) {
        return userService.getStudentUserProfileByUserId(userId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // PUT /api/profiles/students/{userId}
    // Add authorization: e.g., only admin or the student themselves can update
    @PutMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('STUDENT') and #userId == authentication.principal.id)")
    public ResponseEntity<UserProfileDto> updateStudentProfile(
            @PathVariable int userId,
            @Valid @RequestBody UserProfileDto userProfileDto) {
        
        // The userService.updateStudentUserProfile method already checks if the user is a student.
        // It also handles creating a profile if one doesn't exist for the student.
        UserProfileDto updatedDto = userService.updateStudentUserProfile(userId, userProfileDto);
        return ResponseEntity.ok(updatedDto);
    }
} 