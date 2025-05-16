package com.example.spring_security.controller;

import com.example.spring_security.service.ScraperIntegrationService;
import com.example.spring_security.service.ScrapingClientService;
import com.example.spring_security.service.ScraperServiceUnavailableException;
import com.example.spring_security.service.TokenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
// Import for CSRF
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@WebMvcTest(AdminScraperController.class)
@EnableMethodSecurity(prePostEnabled = true)
public class AdminScraperControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ScrapingClientService scrapingClientService;

    @MockBean
    private ScraperIntegrationService scraperIntegrationService;

    @MockBean
    private TokenService tokenService;

    @MockBean
    private UserDetailsService userDetailsService;

    @Test
    @WithMockUser(roles = "ADMIN") // Simulate an authenticated admin user
    public void triggerImmobilier_whenServiceAvailable_shouldSucceed() throws Exception {
        // Arrange: Mock scraperIntegrationService to return a successful status
        String mockStatus = "Scraper module running. Database contains 10 property listings.";
        when(scraperIntegrationService.getScraperStatus()).thenReturn(mockStatus);
        // Mock scrapingClientService to do nothing (void method)
        doNothing().when(scrapingClientService).triggerImmobilierScrape();

        // Act & Assert
        mockMvc.perform(post("/api/admin/scrape/trigger/immobilier")
                .contentType(MediaType.APPLICATION_JSON)
                .with(csrf())) // Keep csrf() as it links @WithMockUser session to request
                .andExpect(status().isOk())
                .andExpect(content().string("Immobilier scraper trigger request submitted successfully. Service status: " + mockStatus));

        // Verify that getScraperStatus was called
        verify(scraperIntegrationService, times(1)).getScraperStatus();
        // Verify that triggerImmobilierScrape was called because status was OK
        verify(scrapingClientService, times(1)).triggerImmobilierScrape();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void triggerImmobilier_whenServiceUnavailable_shouldReturnServiceUnavailable() throws Exception {
        // Arrange: Mock scraperIntegrationService to throw ScraperServiceUnavailableException
        String errorMessage = "Connection refused";
        when(scraperIntegrationService.getScraperStatus()).thenThrow(new ScraperServiceUnavailableException(errorMessage, new RuntimeException()));

        // Act & Assert
        mockMvc.perform(post("/api/admin/scrape/trigger/immobilier")
                .contentType(MediaType.APPLICATION_JSON)
                .with(csrf())) // Keep csrf()
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().string("Scraping service is not available. Please ensure it is started. Details: " + errorMessage));

        // Verify that getScraperStatus was called
        verify(scraperIntegrationService, times(1)).getScraperStatus();
        // Verify that triggerImmobilierScrape was NOT called because service was unavailable
        verify(scrapingClientService, never()).triggerImmobilierScrape();
    }

    @Test
    @WithMockUser(roles = "USER") // Simulate an authenticated user without ADMIN role
    public void triggerImmobilier_whenUserNotAdmin_shouldReturnForbidden() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/admin/scrape/trigger/immobilier")
                .contentType(MediaType.APPLICATION_JSON)
                .with(csrf())) // Keep csrf()
                .andExpect(status().isForbidden());

        // Verify that no service methods were called (Access should be denied before controller method runs)
        verify(scraperIntegrationService, never()).getScraperStatus();
        verify(scrapingClientService, never()).triggerImmobilierScrape();
    }
    
    @Test
    // Test without @WithMockUser to simulate an unauthenticated user
    public void triggerImmobilier_whenUserUnauthenticated_shouldRedirectToLogin() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/admin/scrape/trigger/immobilier")
                .contentType(MediaType.APPLICATION_JSON)
                // Adding csrf() here too, although might be unnecessary if unauthenticated
                // requests are blocked before CSRF check by the AuthenticationEntryPoint redirect.
                 .with(csrf())
                 )
                // Expect a redirect to the configured login entry point
                .andExpect(status().isFound()) 
                .andExpect(redirectedUrlPattern("**/oauth2/authorization/google"));

        // Verify that no service methods were called as request should be intercepted before controller logic
        verify(scraperIntegrationService, never()).getScraperStatus();
        verify(scrapingClientService, never()).triggerImmobilierScrape();
    }
} 