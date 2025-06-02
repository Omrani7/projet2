package com.example.spring_security.controller;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/images")
public class ImageController {

    private final String UPLOAD_DIR = "uploads/owner-property-images/";

    @GetMapping("/{filename:.+}")
    public ResponseEntity<Resource> serveFile(@PathVariable String filename) {
        Path filePath = Paths.get(UPLOAD_DIR + filename);
        Resource file = new FileSystemResource(filePath);
        
        if (!file.exists()) {
            return ResponseEntity.notFound().build();
        }
        
        MediaType mediaType;
        try {
            String contentType = Files.probeContentType(filePath);
            mediaType = contentType != null ? MediaType.parseMediaType(contentType) : MediaType.IMAGE_JPEG;
        } catch (IOException e) {
            mediaType = MediaType.IMAGE_JPEG; // Default to JPEG if content type cannot be determined
        }
        
        return ResponseEntity.ok()
                .contentType(mediaType)
                .body(file);
    }
} 