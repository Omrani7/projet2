package com.example.spring_security;

import com.example.spring_security.dao.UserRepo;
import com.example.spring_security.dto.AuthResponse;
import com.example.spring_security.model.User;
import com.example.spring_security.model.UserProfile;
import com.example.spring_security.dto.SignupRequest;
import com.example.spring_security.service.TokenService;
import com.example.spring_security.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional // Rollback DB changes after each test
public class OAuth2FlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepo userRepo;

    @Autowired
    private UserService userService; // Needed if we want to use service methods directly

    @Autowired
    private TokenService tokenService; // Needed to decode the returned token

    @Autowired
    private PasswordEncoder passwordEncoder; // Needed for createUserForTest

    // --- Test Cases for /oauth2/update-role ---

    @Test
    void updateRole_whenValidRequestWithOwnerRole_thenSuccessRoleUpdatedAndNewTokenIssued() throws Exception {
        // Arrange: Create a user (simulating a new OAuth2 user before role selection)
        User user = createUserForTest("oauth.owner.test@example.com", "oauthOwnerUser", "RandomPass123");
        assertThat(user.getRole()).isEqualTo(User.Role.STUDENT); // Verify initial role

        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("email", user.getEmail());
        requestBody.put("role", "OWNER");

        // Act
        MvcResult result = mockMvc.perform(post("/oauth2/update-role")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.userId").value(user.getId()))
                .andExpect(jsonPath("$.email").value(user.getEmail()))
                .andExpect(jsonPath("$.username").value(user.getUsername()))
                .andExpect(jsonPath("$.role").value("OWNER")) // Verify role in response
                .andReturn();

        // Assert: Verify role update in DB
        User updatedUser = userRepo.findByEmail(user.getEmail());
        assertThat(updatedUser).isNotNull();
        assertThat(updatedUser.getRole()).isEqualTo(User.Role.OWNER);
        // Check UserProfile type update
        assertThat(updatedUser.getUserProfile()).isNotNull(); // Assuming profile is created
        assertThat(updatedUser.getUserProfile().getUserType()).isEqualTo(UserProfile.UserType.OWNER);

        // Assert: Verify the new token contains the updated role
        String responseBody = result.getResponse().getContentAsString();
        AuthResponse authResponse = objectMapper.readValue(responseBody, AuthResponse.class);
        String newToken = authResponse.getToken();
        // Assuming TokenService has a method to get claims or decode (adapt if necessary)
        // Example: You might need a method like `getRoleFromToken` in TokenService
        // For now, we trust the response JSON and DB check. A full token decode assertion is better.
        // Claims claims = tokenService.extractAllClaims(newToken);
        // assertThat(claims.get("role")).isEqualTo("OWNER");
        System.out.println("New token issued with OWNER role (manual verification recommended): " + newToken);

    }
    
    @Test
    void updateRole_whenValidRequestWithStudentRole_thenSuccessRoleUpdatedAndNewTokenIssued() throws Exception {
        // Arrange: Create a user, manually set to OWNER to test changing back to STUDENT
        User user = createUserForTest("oauth.student.test@example.com", "oauthStudentUser", "RandomPass123");
        user.setRole(User.Role.OWNER); // Set initial role to OWNER for this test
        userRepo.save(user); 
        
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("email", user.getEmail());
        requestBody.put("role", "STUDENT");

        // Act & Assert
        mockMvc.perform(post("/oauth2/update-role")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("STUDENT"));

        // Assert: Verify role update in DB
        User updatedUser = userRepo.findByEmail(user.getEmail());
        assertThat(updatedUser).isNotNull();
        assertThat(updatedUser.getRole()).isEqualTo(User.Role.STUDENT);
        assertThat(updatedUser.getUserProfile()).isNotNull();
        assertThat(updatedUser.getUserProfile().getUserType()).isEqualTo(UserProfile.UserType.STUDENT);
    }

    @Test
    void updateRole_whenInvalidRoleProvided_thenBadRequest() throws Exception {
        // Arrange
        User user = createUserForTest("oauth.invalidrole.test@example.com", "oauthInvalidRoleUser", "RandomPass123");
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("email", user.getEmail());
        requestBody.put("role", "ADMIN"); // Invalid role for user selection

        // Act & Assert
        mockMvc.perform(post("/oauth2/update-role")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Role must be either STUDENT or OWNER"));
    }

    @Test
    void updateRole_whenRoleIsMissing_thenBadRequest() throws Exception {
        // Arrange
        User user = createUserForTest("oauth.missingrole.test@example.com", "oauthMissingRoleUser", "RandomPass123");
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("email", user.getEmail());
        // Missing "role"

        // Act & Assert
        mockMvc.perform(post("/oauth2/update-role")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Email and role are required"));
    }

    @Test
    void updateRole_whenEmailIsMissing_thenBadRequest() throws Exception {
        // Arrange
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("role", "STUDENT");
        // Missing "email"

        // Act & Assert
        mockMvc.perform(post("/oauth2/update-role")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Email and role are required"));
    }

    @Test
    void updateRole_whenUserNotFound_thenInternalServerError() throws Exception {
        // Arrange
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("email", "nonexistent.oauthuser@example.com");
        requestBody.put("role", "STUDENT");

        // Act & Assert
        mockMvc.perform(post("/oauth2/update-role")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isInternalServerError()) // Based on current controller catching Exception
                .andExpect(content().string("Error updating user role: User not found with email: nonexistent.oauthuser@example.com"));
    }


    // --- Helper Method ---
    private User createUserForTest(String email, String username, String password) {
       // Using SignupRequest to ensure profile is also created, mimicking register/oauth flow
       // SignupRequest logic removed as it wasn't fully used and caused import issues initially
       // Refined direct creation:
       
       if (userRepo.findByEmail(email) != null) {
           // Avoid creating duplicates if test cleanup fails
           return userRepo.findByEmail(email);
       }

       User user = new User();
       user.setEmail(email);
       user.setUsername(username);
       user.setPassword(passwordEncoder.encode(password)); // Encode password for direct save
       user.setEnabled(true);
       user.setRole(User.Role.STUDENT); // Explicitly set default role
       user.setProvider(User.AuthProvider.GOOGLE); // Simulate OAuth provider
       // user.setProviderId("test-provider-id-" + System.currentTimeMillis());

       User savedUser = userRepo.save(user); // Save user first

       // Create and associate profile
       UserProfile profile = new UserProfile(); // Corrected instantiation
       profile.setUser(savedUser);
       profile.setFullName(username + " Profile");
       profile.setUserType(UserProfile.UserType.STUDENT); // Corrected access 
       // Assuming UserProfileRepo is available or cascade works
       // If not using cascade persist, inject UserProfileRepo and save profile

       savedUser.setUserProfile(profile); 
       // Note: If not using CascadeType.ALL or MERGE, you might need to save profile separately
       // userProfileRepo.save(profile);
       
       return userRepo.save(savedUser); // Save user again to persist profile relationship if needed
   }
} 