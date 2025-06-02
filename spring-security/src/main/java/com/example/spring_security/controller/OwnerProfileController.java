package com.example.spring_security.controller;

import com.example.spring_security.dto.OwnerProfileDTO;
import com.example.spring_security.service.OwnerProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profiles/owners")
public class OwnerProfileController {

    private final OwnerProfileService ownerProfileService;

    @Autowired
    public OwnerProfileController(OwnerProfileService ownerProfileService) {
        this.ownerProfileService = ownerProfileService;
    }

    @GetMapping("/{userId}")
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN') or (#userId == authentication.principal.id and (hasRole('OWNER') or hasRole('ADMIN')))")
    public ResponseEntity<OwnerProfileDTO> getOwnerProfile(@PathVariable Long userId) {
        // The PreAuthorize should already ensure that if not an admin, the user can only fetch their own profile.
        // The service layer also has checks.
        OwnerProfileDTO ownerProfile = ownerProfileService.getOwnerProfileByUserId(userId);
        return ResponseEntity.ok(ownerProfile);
    }

    @PutMapping("/{userId}")
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN') or (#userId == authentication.principal.id and (hasRole('OWNER') or hasRole('ADMIN')))")
    public ResponseEntity<OwnerProfileDTO> updateOwnerProfile(@PathVariable Long userId, @RequestBody OwnerProfileDTO ownerProfileDto) {
        // PreAuthorize ensures user is owner of this profile or an admin.
        // Service layer performs additional checks.
        // Ensure DTO's userId matches path variable for consistency, or rely on path variable as source of truth.
        if (ownerProfileDto.getUserId() != null && !ownerProfileDto.getUserId().equals(userId)) {
            // Or, some might argue to ignore DTO's userId and always use path variable.
            // For now, let's consider it a bad request if they don't align or DTO's userId is not set to path variable.
            // Alternatively, always set dto.setUserId(userId) before passing to service.
            return ResponseEntity.badRequest().build(); // Or throw an exception
        }
        ownerProfileDto.setUserId(userId); // Ensure userId from path is used

        OwnerProfileDTO updatedProfile = ownerProfileService.updateOwnerProfile(userId, ownerProfileDto);
        return ResponseEntity.ok(updatedProfile);
    }
} 