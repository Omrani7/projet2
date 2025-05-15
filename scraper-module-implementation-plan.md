# Scraper Module Implementation Plan

## Overview
This plan outlines the steps to separate the web scraping functionality into a distinct module within the same Spring Boot project. This separation will:
1. Make the scraping code independent from the main application
2. Allow running scraping operations separately without authentication
3. Keep the MCP server integration working
4. Enable testing without Spring Security conflicts

## Step 1: Create Module Structure

### 1.1 Create Maven Multi-Module Project
Convert the existing project to a multi-module Maven project:

1. Rename the current `pom.xml` to `parent-pom.xml`
2. Create a new `pom.xml` in the root directory:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.4.3</version>
        <relativePath/>
    </parent>
    <groupId>com.example</groupId>
    <artifactId>property-platform</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <packaging>pom</packaging>
    <name>property-platform</name>
    <description>Property Platform with Scraping Capabilities</description>
    
    <modules>
        <module>spring-security</module>
        <module>property-scraper</module>
    </modules>
    
    <properties>
        <java.version>21</java.version>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>
</project>
```

3. Update the current `pom.xml` (spring-security):
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.example</groupId>
        <artifactId>property-platform</artifactId>
        <version>0.0.1-SNAPSHOT</version>
    </parent>
    
    <artifactId>spring-security</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>spring-security</name>
    <description>Main application with security and web interface</description>
    
    <!-- Keep existing dependencies but remove playwright and scraping related ones -->
    <dependencies>
        <!-- Remove:
        - com.microsoft.playwright:playwright
        - org.jsoup:jsoup 
        -->
    </dependencies>
    
    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
            <!-- Remove the playwright-maven-plugin -->
        </plugins>
    </build>
</project>
```

### 1.2 Create Scraper Module
Create a new module for scraping:

1. Create directory `property-scraper`
2. Create `property-scraper/pom.xml`:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.example</groupId>
        <artifactId>property-platform</artifactId>
        <version>0.0.1-SNAPSHOT</version>
    </parent>
    
    <artifactId>property-scraper</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>property-scraper</name>
    <description>Property scraping module</description>
    
    <dependencies>
        <!-- Spring Boot -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter</artifactId>
        </dependency>
        
        <!-- Database -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
        </dependency>
        
        <!-- Web (minimal for REST APIs) -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        
        <!-- Scraping Tools -->
        <dependency>
            <groupId>com.microsoft.playwright</groupId>
            <artifactId>playwright</artifactId>
            <version>1.40.0</version>
        </dependency>
        <dependency>
            <groupId>org.jsoup</groupId>
            <artifactId>jsoup</artifactId>
            <version>1.16.1</version>
        </dependency>
        
        <!-- Utilities -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <scope>provided</scope>
        </dependency>
        
        <!-- Testing -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
    
    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
            
            <!-- Playwright Maven Plugin for installation -->
            <plugin>
                <groupId>org.codehaus.mojo</groupId>
                <artifactId>exec-maven-plugin</artifactId>
                <version>3.1.0</version>
                <executions>
                    <execution>
                        <id>install-playwright</id>
                        <phase>generate-test-resources</phase>
                        <goals>
                            <goal>java</goal>
                        </goals>
                        <configuration>
                            <mainClass>com.microsoft.playwright.CLI</mainClass>
                            <arguments>
                                <argument>install</argument>
                                <argument>--with-deps</argument>
                            </arguments>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</project>
```

## Step 2: Shared Model/Domain Layer

### 2.1 Create Shared Entity Models
Create shared entity models that both modules will use:

1. Create a package structure in the scraper module:
```
property-scraper/src/main/java/com/example/scraper/model/
```

2. Move/copy property models from the main application to the scraper module:
```
ImmobilierProperty.java
PropertyListing.java (if exists)
```

### 2.2 Create Data Transfer Objects
Create DTOs for transferring data between modules:

1. Create a package for DTOs:
```
property-scraper/src/main/java/com/example/scraper/dto/
```

2. Create DTOs for property data:
```java
@Data
public class PropertyDto {
    private String id;
    private String title;
    private String description;
    private String location;
    private String price;
    private String type;
    private Double latitude;
    private Double longitude;
    // Other fields as needed
}
```

## Step 3: Move Scraper Code

### 3.1 Create Directory Structure
Set up the package structure in the scraper module:

```
property-scraper/src/main/java/com/example/scraper/
  ├── config/                  # Configuration
  ├── controller/              # REST endpoints
  ├── dto/                     # Data Transfer Objects  
  ├── model/                   # Entity models
  ├── repository/              # Database access
  ├── service/                 # Business logic
  │   ├── geocoding/           # Geocoding services
  │   └── scraper/             # Scraping services
  │       ├── playwright/      # Playwright implementation
  │       └── immobilier/      # Site-specific code
  └── ScraperApplication.java  # Main application class
```

### 3.2 Move Scraper Code
Move the scraper code from the main application to the scraper module:

1. Copy all code from:
```
spring-security/src/main/java/com/example/spring_security/service/scraper/
```

2. To:
```
property-scraper/src/main/java/com/example/scraper/service/scraper/
```

3. Update package declarations in all files
4. Update imports to reflect the new package structure

### 3.3 Create Main Application Class
Create a main class for the scraper module:

```java
package com.example.scraper;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ScraperApplication {
    public static void main(String[] args) {
        SpringApplication.run(ScraperApplication.class, args);
    }
}
```

## Step 4: Configure Spring Profiles

### 4.1 Create Configuration Class
Create a configuration class that enables/disables scraping based on profiles:

```java
package com.example.scraper.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
public class ScraperModuleConfig {
    
    @Bean
    @Profile("scraper")
    public boolean enableScraper() {
        return true;
    }
    
    // Additional configuration beans
}
```

### 4.2 Create Application Properties
Create application properties files for different environments:

1. `property-scraper/src/main/resources/application.properties`:
```properties
# Database configuration (same as main app)
spring.datasource.url=jdbc:postgresql://localhost:5432/property_db
spring.datasource.username=postgres
spring.datasource.password=password
spring.jpa.hibernate.ddl-auto=update

# MCP Configuration
playwright.mcp.headless=true
```

2. `property-scraper/src/main/resources/application-scraper.properties`:
```properties
# Scraper specific settings
scraper.schedule.enabled=true
scraper.schedule.cron=0 0 0 * * ? # Run daily at midnight
```

## Step 5: Create REST API for Scraper

### 5.1 Create REST Controller
Create a REST controller for triggering scraping operations:

```java
package com.example.scraper.controller;

import com.example.scraper.dto.PropertyDto;
import com.example.scraper.service.scraper.immobilier.ImmobilierScraperService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/scraper")
public class ScraperController {
    
    @Autowired
    private ImmobilierScraperService scraperService;
    
    @GetMapping("/run")
    public ResponseEntity<String> runScraper() {
        // Trigger scraping
        scraperService.searchRentalApartments(null);
        return ResponseEntity.ok("Scraping operation started");
    }
    
    @GetMapping("/status")
    public ResponseEntity<String> getStatus() {
        // Return status of scraping operations
        return ResponseEntity.ok("Scraper module running");
    }
    
    // Additional endpoints as needed
}
```

### 5.2 Configure Security (Optional)
If needed, add basic security for the scraper API:

```java
package com.example.scraper.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class ScraperSecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/scraper/**").permitAll()
                .anyRequest().authenticated()
            );
        
        return http.build();
    }
}
```

## Step 6: Running Options

### 6.1 Command Line Execution
Create a command line runner for direct execution:

```java
package com.example.scraper;

import com.example.scraper.service.scraper.immobilier.ImmobilierScraperService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("cli")
public class ScraperCommandLineRunner implements CommandLineRunner {

    @Autowired
    private ImmobilierScraperService scraperService;
    
    @Override
    public void run(String... args) throws Exception {
        System.out.println("Starting CLI scraper run");
        
        // Run the scraper
        scraperService.searchRentalApartments(null);
        
        System.out.println("Scraping completed, exiting");
        System.exit(0);
    }
}
```

### 6.2 Scheduled Execution
Setup a scheduler for periodic execution:

```java
package com.example.scraper.service;

import com.example.scraper.service.scraper.immobilier.ImmobilierScraperService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@Profile("scheduled")
public class ScheduledScraperService {

    @Autowired
    private ImmobilierScraperService scraperService;
    
    @Scheduled(cron = "${scraper.schedule.cron}")
    public void runScheduledScraping() {
        System.out.println("Running scheduled scraping task");
        scraperService.searchRentalApartments(null);
    }
}
```

## Step 7: Testing the Scraper Module

### 7.1 Create Test Configuration
Create a test configuration for the scraper:

```java
package com.example.scraper.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class ScraperTestConfig {
    // Test configuration beans
}
```

### 7.2 Create Mock Classes
Create mock classes for testing:

```java
package com.example.scraper.service.scraper.playwright;

import com.microsoft.playwright.Page;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Primary
@Profile("test")
public class MockPlaywrightMcpManager extends PlaywrightMcpManager {
    // Mock implementation
}
```

## Step 8: Integration with Main Application

### 8.1 Service Integration
Create service interfaces in the main application that call the scraper module:

```java
package com.example.spring_security.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class ScraperIntegrationService {
    
    private final RestTemplate restTemplate;
    private final String scraperBaseUrl = "http://localhost:8081/api/scraper";
    
    public ScraperIntegrationService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
    
    public void triggerScraping() {
        restTemplate.getForEntity(scraperBaseUrl + "/run", String.class);
    }
    
    public String getScraperStatus() {
        return restTemplate.getForObject(scraperBaseUrl + "/status", String.class);
    }
}
```

### 8.2 Controller for Admin Interface
Create a controller in the main application for admin access to scraping:

```java
package com.example.spring_security.controller;

import com.example.spring_security.service.ScraperIntegrationService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/scraper")
public class ScraperAdminController {
    
    private final ScraperIntegrationService scraperService;
    
    public ScraperAdminController(ScraperIntegrationService scraperService) {
        this.scraperService = scraperService;
    }
    
    @GetMapping("/run")
    @PreAuthorize("hasRole('ADMIN')")
    public String triggerScraping() {
        scraperService.triggerScraping();
        return "Scraping initiated";
    }
    
    @GetMapping("/status")
    @PreAuthorize("hasRole('ADMIN')")
    public String getStatus() {
        return scraperService.getScraperStatus();
    }
}
```

## Step 9: MCP Server Integration

### 9.1 Ensure MCP Server Starts with Scraper Module
Make sure the MCP server starts properly:

```java
package com.example.scraper.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.example.scraper.service.scraper.playwright.PlaywrightMcpManager;

@Configuration
public class MpcConfig {
    
    @Bean
    public PlaywrightMcpManager playwrightMcpManager() {
        PlaywrightMcpManager manager = new PlaywrightMcpManager();
        // Additional configuration
        return manager;
    }
}
```

## Step 10: Running the Modules

### 10.1 Run Main Application
```bash
cd spring-security
mvn spring-boot:run
```

### 10.2 Run Scraper Module
```bash
cd property-scraper
mvn spring-boot:run -Dspring-boot.run.profiles=scraper
```

### 10.3 Run Scraper CLI Mode
```bash
cd property-scraper
mvn spring-boot:run -Dspring-boot.run.profiles=cli
```

## Conclusion
After completing these steps, you'll have a dedicated scraper module that:

1. Can run independently of the main application
2. Doesn't require Spring Security authentication
3. Maintains MCP server integration
4. Allows easier testing without security concerns
5. Can be scheduled or manually triggered

The main application can still integrate with the scraper module through REST calls, and the database layer is shared between both components. 