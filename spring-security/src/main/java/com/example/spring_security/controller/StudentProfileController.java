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


    @GetMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('STUDENT') and #userId == authentication.principal.id)")
    public ResponseEntity<UserProfileDto> getStudentProfile(@PathVariable int userId) {
        return userService.getStudentUserProfileByUserId(userId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }


    @PutMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('STUDENT') and #userId == authentication.principal.id)")
    public ResponseEntity<UserProfileDto> updateStudentProfile(
            @PathVariable int userId,
            @Valid @RequestBody UserProfileDto userProfileDto) {
        

        UserProfileDto updatedDto = userService.updateStudentUserProfile(userId, userProfileDto);
        return ResponseEntity.ok(updatedDto);
    }
} 