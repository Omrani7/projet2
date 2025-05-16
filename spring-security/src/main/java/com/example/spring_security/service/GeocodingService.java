package com.example.spring_security.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Service for geocoding addresses
 */
@Service
@Slf4j
public class GeocodingService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    @Value("${geocoding.locationiq.api-key:your-api-key}")
    private String apiKey;
    
    @Value("${geocoding.locationiq.enabled:false}")
    private boolean enabled;
    
    private static final String LOCATION_IQ_BASE_URL = "https://eu1.locationiq.com/v1/search.php";
    
    private long lastRequestTime = 0;
    private static final long MIN_REQUEST_INTERVAL_MS = 1100; // Add 100ms buffer
    
    // Hardcoded coordinates for common Tunisian locations
    private static final Map<String, double[]> TUNISIA_COORDINATES = new HashMap<>();
    static {
        TUNISIA_COORDINATES.put("tunis", new double[]{36.8065, 10.1815});
        TUNISIA_COORDINATES.put("sfax", new double[]{34.7406, 10.7603});
        TUNISIA_COORDINATES.put("sousse", new double[]{35.8245, 10.6346});
        TUNISIA_COORDINATES.put("kairouan", new double[]{35.6781, 10.1014});
        TUNISIA_COORDINATES.put("bizerte", new double[]{37.2746, 9.8748});
        TUNISIA_COORDINATES.put("gabes", new double[]{33.8869, 10.0982});
        TUNISIA_COORDINATES.put("ariana", new double[]{36.8625, 10.1956});
        TUNISIA_COORDINATES.put("gafsa", new double[]{34.4311, 8.7757});
        TUNISIA_COORDINATES.put("monastir", new double[]{35.7643, 10.8113});
        TUNISIA_COORDINATES.put("ben arous", new double[]{36.7533, 10.2282});
        TUNISIA_COORDINATES.put("kasserine", new double[]{35.1667, 8.8333});
        TUNISIA_COORDINATES.put("medenine", new double[]{33.3547, 10.5053});
        TUNISIA_COORDINATES.put("nabeul", new double[]{36.4513, 10.7357});
        TUNISIA_COORDINATES.put("tataouine", new double[]{32.9297, 10.4518});
        TUNISIA_COORDINATES.put("beja", new double[]{36.7333, 9.1833});
        TUNISIA_COORDINATES.put("jendouba", new double[]{36.5011, 8.7803});
        TUNISIA_COORDINATES.put("el kef", new double[]{36.1672, 8.7047});
        TUNISIA_COORDINATES.put("mahdia", new double[]{35.5047, 11.0622});
        TUNISIA_COORDINATES.put("sidi bouzid", new double[]{35.0381, 9.4858});
        TUNISIA_COORDINATES.put("tozeur", new double[]{33.9197, 8.1335});
        TUNISIA_COORDINATES.put("siliana", new double[]{36.0878, 9.3733});
        TUNISIA_COORDINATES.put("zaghouan", new double[]{36.4019, 10.1422});
        TUNISIA_COORDINATES.put("kebili", new double[]{33.7046, 8.9646});
        
        TUNISIA_COORDINATES.put("lac", new double[]{36.8317, 10.2475});
        TUNISIA_COORDINATES.put("marsa", new double[]{36.8842, 10.3230});
        TUNISIA_COORDINATES.put("sidi bou said", new double[]{36.8702, 10.3413});
        TUNISIA_COORDINATES.put("carthage", new double[]{36.8589, 10.3336});
        TUNISIA_COORDINATES.put("gammarth", new double[]{36.9181, 10.2903});
        TUNISIA_COORDINATES.put("menzah", new double[]{36.8345, 10.1686});
        TUNISIA_COORDINATES.put("bardo", new double[]{36.8088, 10.1400});
        TUNISIA_COORDINATES.put("lafayette", new double[]{36.8016, 10.1853});
        
        TUNISIA_COORDINATES.put("mornag", new double[]{36.6697, 10.2897});
        TUNISIA_COORDINATES.put("la soukra", new double[]{36.8982, 10.2378});
        TUNISIA_COORDINATES.put("borj louzir", new double[]{36.8982, 10.2378}); // Same as La Soukra, consider refining if different
        TUNISIA_COORDINATES.put("menzah 9", new double[]{36.8451, 10.1686});
        TUNISIA_COORDINATES.put("el manar", new double[]{36.8393, 10.1584});
        TUNISIA_COORDINATES.put("nabeul ville", new double[]{36.4513, 10.7357}); // Same as Nabeul, can be kept for alias
    }
    
    public GeocodingService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }
    
    /**
     * Geocode an address to get coordinates
     * @param address The address to geocode
     * @return Array of [latitude, longitude] or null if geocoding failed
     */
    public double[] geocode(String address) {
        if (address == null || address.trim().isEmpty()) {
            log.debug("Cannot geocode empty address");
            return null;
        }
        
        log.info("Geocoding address: {}", address);
        
        // Try API first if enabled and key is valid
        if (enabled && apiKey != null && !apiKey.isEmpty() && !"your-api-key".equals(apiKey)) {
            log.debug("Geocoding API is enabled with a key. Attempting API call.");
            try {
                throttleRequests();
                
                String encodedAddress = URLEncoder.encode(address, StandardCharsets.UTF_8.toString());
                
                UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(LOCATION_IQ_BASE_URL)
                    .queryParam("key", apiKey)
                    .queryParam("q", encodedAddress)
                    .queryParam("format", "json");
                
                // Add Tunisia as country filter
                builder.queryParam("countrycodes", "tn");
                
                String url = builder.build().toUriString();
                log.debug("Geocoding URL: {}", url.replace(apiKey, "API_KEY_HIDDEN"));
                
                ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
                String responseBody = response.getBody();
                
                if (response.getStatusCode().is2xxSuccessful() && responseBody != null) {
                    JsonNode root = objectMapper.readTree(responseBody);
                    
                    if (root.isArray() && root.size() > 0) {
                        JsonNode result = root.get(0);
                        
                        double latitude = Double.parseDouble(result.get("lat").asText());
                        double longitude = Double.parseDouble(result.get("lon").asText());
                        
                        log.info("Successfully geocoded via API: {} to lat={}, lon={}",
                                address, latitude, longitude);
                        
                        return new double[]{latitude, longitude};
                    }
                }
                
                // Log API failure but continue to fallback
                log.warn("API call failed for address: {}, response status: {}. Proceeding to fallback.", 
                        address, response.getStatusCode());
                
            } catch (Exception e) {
                // Log API error but continue to fallback
                log.warn("Error during API geocoding for address: {}. Proceeding to fallback. Error: {}", address, e.getMessage());
            }
        } else {
            log.debug("Geocoding API is disabled or missing valid API key. Using fallback coordinates directly.");
        }
        
        // If API call was skipped, disabled, or failed, use fallback
        log.info("Using fallback coordinates for address: {}", address);
        return getFallbackCoordinates(address);
    }
    
    /**
     * Get fallback coordinates from the predefined map
     */
    public double[] getFallbackCoordinates(String address) {
        if (address == null || address.isEmpty()) {
            return null;
        }
        
        String lowerAddress = address.toLowerCase();
        
        // Sort keys by length in descending order to match longer, more specific names first
        List<String> sortedKeys = TUNISIA_COORDINATES.keySet().stream()
            .sorted((a, b) -> Integer.compare(b.length(), a.length()))
            .collect(Collectors.toList());
            
        for (String location : sortedKeys) {
            if (lowerAddress.contains(location)) {
                double[] coordinates = TUNISIA_COORDINATES.get(location);
                log.info("Using fallback coordinates for '{}' in address: {}", location, address);
                return coordinates;
            }
        }
        
        // Default to Tunis if no match found
        log.debug("No fallback coordinates found for address: {}, using Tunis as default", address);
        return TUNISIA_COORDINATES.get("tunis"); // Ensure "tunis" is always in the map
    }
    
    /**
     * Apply rate limiting to API requests
     */
    private void throttleRequests() {
        long currentTime = System.currentTimeMillis();
        long elapsed = currentTime - lastRequestTime;
        
        if (elapsed < MIN_REQUEST_INTERVAL_MS) {
            try {
                long sleepTime = MIN_REQUEST_INTERVAL_MS - elapsed;
                log.debug("Rate limiting: sleeping for {}ms", sleepTime);
                TimeUnit.MILLISECONDS.sleep(sleepTime);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Thread interrupted during rate limiting", e);
            }
        }
        
        lastRequestTime = System.currentTimeMillis();
    }
} 