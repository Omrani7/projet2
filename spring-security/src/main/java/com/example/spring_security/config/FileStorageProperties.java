package com.example.spring_security.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "file")
public class FileStorageProperties {
    private String uploadOwnerDir;
    private String staticOwnerPathPattern;
    private String resourceOwnerLocations;

    public String getUploadOwnerDir() {
        return uploadOwnerDir;
    }

    public void setUploadOwnerDir(String uploadOwnerDir) {
        this.uploadOwnerDir = uploadOwnerDir;
    }

    public String getStaticOwnerPathPattern() {
        return staticOwnerPathPattern;
    }

    public void setStaticOwnerPathPattern(String staticOwnerPathPattern) {
        this.staticOwnerPathPattern = staticOwnerPathPattern;
    }

    public String getResourceOwnerLocations() {
        return resourceOwnerLocations;
    }

    public void setResourceOwnerLocations(String resourceOwnerLocations) {
        this.resourceOwnerLocations = resourceOwnerLocations;
    }
} 