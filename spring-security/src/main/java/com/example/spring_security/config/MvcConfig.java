package com.example.spring_security.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class MvcConfig implements WebMvcConfigurer {

    private final FileStorageProperties fileStorageProperties;

    @Autowired
    public MvcConfig(FileStorageProperties fileStorageProperties) {
        this.fileStorageProperties = fileStorageProperties;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String staticPathPattern = fileStorageProperties.getStaticOwnerPathPattern();
        String resourceLocations = fileStorageProperties.getResourceOwnerLocations();

        if (staticPathPattern != null && !staticPathPattern.isEmpty() && 
            resourceLocations != null && !resourceLocations.isEmpty()) {
            registry.addResourceHandler(staticPathPattern)
                    .addResourceLocations(resourceLocations);
            System.out.println("Serving owner property images from: " + resourceLocations + " at " + staticPathPattern);
        } else {
            System.out.println("Owner property image serving paths not configured properly.");
        }
    }
} 