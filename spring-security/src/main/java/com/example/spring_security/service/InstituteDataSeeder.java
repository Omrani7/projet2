package com.example.spring_security.service;

import com.example.spring_security.dao.InstituteRepository;
import com.example.spring_security.model.Institute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Service
public class InstituteDataSeeder implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(InstituteDataSeeder.class);

    @Autowired
    private InstituteRepository instituteRepository;

    // Path to the data file in src/main/resources
    private static final String DATA_FILE_PATH = "Establishments.txt";

    @Override
    @Transactional // Good practice for operations that modify the database
    public void run(String... args) throws Exception {
        long count = instituteRepository.count();
        if (count == 0) {
            logger.info("No institutes found in database. Starting to seed data from {}.", DATA_FILE_PATH);
            seedInstitutes();
        } else {
            logger.info("{} institutes already exist in the database. Skipping seeding.", count);
        }
    }

    private void seedInstitutes() {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new ClassPathResource(DATA_FILE_PATH).getInputStream(), StandardCharsets.UTF_8))) {

            String line;
            int lineNumber = 0;
            int institutesAdded = 0;

            // Skip header lines
            reader.readLine(); // Skip "Établissement | Latitude | Longitude"
            lineNumber++;
            reader.readLine(); // Skip "---|---|---"
            lineNumber++;

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.trim().isEmpty()) {
                    continue; // Skip empty lines
                }

                String[] parts = line.split("\\|"); // Escaped pipe for regex split

                // Expecting: | Name | Latitude | Longitude | (so 4 parts after split, first is empty)
                if (parts.length >= 4) { // Check for at least 4 parts
                    try {
                        String name = parts[1].trim();
                        String latitudeStr = parts[2].trim();
                        String longitudeStr = parts[3].trim();

                        if (name.isEmpty()) {
                            logger.warn("Skipping line {} due to empty institute name: '{}'", lineNumber, line);
                            continue;
                        }

                        Optional<Institute> existingInstitute = instituteRepository.findByName(name);
                        if (existingInstitute.isPresent()) {
                            // logger.debug("Institute '{}' already exists. Skipping.", name);
                            continue;
                        }

                        Double latitude = Double.parseDouble(latitudeStr);
                        Double longitude = Double.parseDouble(longitudeStr);

                        Institute institute = new Institute(name, latitude, longitude);
                        instituteRepository.save(institute);
                        institutesAdded++;

                    } catch (NumberFormatException e) {
                        logger.warn("Skipping line {} due to number format error for coordinates: '{}'. Error: {}", lineNumber, line, e.getMessage());
                    } catch (ArrayIndexOutOfBoundsException e) {
                        // This might happen if a line doesn't have enough parts after split, even if parts.length initially passed
                        logger.warn("Skipping line {} due to unexpected structure (ArrayIndexOutOfBounds after initial check): '{}'. Error: {}", lineNumber, line, e.getMessage());
                    } catch (Exception e) {
                        logger.error("Error processing line {}: '{}'", lineNumber, line, e);
                    }
                } else {
                    logger.warn("Skipping malformed line {}: '{}' - Expected at least 4 parts, got {}", lineNumber, line, parts.length);
                }
            }
            logger.info("Finished seeding. Added {} new institutes.", institutesAdded);

        } catch (Exception e) {
            logger.error("Failed to read or process the institute data file: {}", DATA_FILE_PATH, e);
        }
    }
} 