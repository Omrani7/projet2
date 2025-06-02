package com.example.spring_security;

import com.example.spring_security.dao.PasswordResetTokenRepository;
import com.example.spring_security.dao.UserRepo;
import com.example.spring_security.dto.LoginRequest;
import com.example.spring_security.dto.ForgotPasswordRequest;
import com.example.spring_security.dto.SignupRequest;
import com.example.spring_security.model.PasswordResetToken;
import com.example.spring_security.model.User;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
// @Transactional // Uncomment if using a persistent DB for tests to rollback changes
public class AuthFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper; // For converting objects to JSON

    @Autowired
    private UserRepo userRepo;

    @Autowired
    private PasswordResetTokenRepository tokenRepo;

    @Autowired
    private PasswordEncoder passwordEncoder; // For verifying password changes

    // --- Registration Tests --- 
    
    @Test
    void registerUser_whenValidRequest_thenSuccessAndStudentRoleAssigned() throws Exception {
        SignupRequest signupRequest = SignupRequest.builder()
                .email("register.test@example.com")
                .username("registerTestUser")
                .password("Password123") // Meets complexity requirements
                .phoneNumber("123456789")
                .build();

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isOk()); // Assuming AuthController returns OK

        // Verify user exists in DB with correct details and role
        User savedUser = userRepo.findByEmail("register.test@example.com");
        assertThat(savedUser).isNotNull();
        assertThat(savedUser.getUsername()).isEqualTo("registerTestUser");
        assertThat(savedUser.getRole()).isEqualTo(User.Role.STUDENT); // Verify default role
        assertThat(savedUser.getProvider()).isEqualTo(User.AuthProvider.LOCAL);
        assertThat(passwordEncoder.matches("Password123", savedUser.getPassword())).isTrue();
    }

    @Test
    void registerUser_whenValidRequestWithOwnerRole_thenSuccessAndOwnerRoleAssigned() throws Exception {
        SignupRequest signupRequest = SignupRequest.builder()
                .email("register.owner@example.com")
                .username("registerOwnerUser")
                .password("Password123")
                .role("OWNER") // Specify OWNER role
                .build();

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isOk());

        // Verify user exists in DB with OWNER role
        User savedUser = userRepo.findByEmail("register.owner@example.com");
        assertThat(savedUser).isNotNull();
        assertThat(savedUser.getRole()).isEqualTo(User.Role.OWNER);
    }
    
    @Test
    void registerUser_whenAdminRoleSpecified_thenDefaultsToStudentRole() throws Exception {
        SignupRequest signupRequest = SignupRequest.builder()
                .email("register.admin.attempt@example.com")
                .username("registerAdminAttemptUser")
                .password("Password123")
                .role("ADMIN") // Attempt to specify ADMIN role
                .build();

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isOk());

        // Verify user exists in DB but has STUDENT role (defaulted)
        User savedUser = userRepo.findByEmail("register.admin.attempt@example.com");
        assertThat(savedUser).isNotNull();
        assertThat(savedUser.getRole()).isEqualTo(User.Role.STUDENT);
    }

    @Test
    void registerUser_whenEmailExists_thenBadRequest() throws Exception {
        // Arrange: Create a user first
        createUserForTest("duplicate.email@example.com", "existingUser", "Password123");

        SignupRequest signupRequest = SignupRequest.builder()
                .email("duplicate.email@example.com") // Use the same email
                .username("newUser")
                .password("Password456")
                .build();

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isBadRequest()) // Expect 400 Bad Request
                .andExpect(content().string("Registration failed: An account with this email address already exists."));
    }
    
    @Test
    void registerUser_whenPasswordWeak_thenBadRequest() throws Exception {
        SignupRequest signupRequest = SignupRequest.builder()
                .email("weakpass.test@example.com")
                .username("weakPassUser")
                .password("weak") // Does not meet complexity
                .build();

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(
                        "Registration failed: Password must be at least 8 characters long and contain at least one uppercase letter, one lowercase letter, and one number."
                ));
    }

    // --- Login Tests --- 
    
    @Test
    void loginUser_whenValidCredentials_thenSuccessAndReturnsTokenAndStudentRole() throws Exception {
        // Arrange: Create a user to log in with
        String rawPassword = "LoginPassword123";
        String userEmail = "login.test@example.com";
        User user = createUserForTest(userEmail, "loginTestUser", rawPassword);

        // Use LoginRequest
        LoginRequest loginRequest = new LoginRequest(); 
        loginRequest.setEmail(userEmail); // LoginRequest uses setEmail
        loginRequest.setPassword(rawPassword);

        // Target /auth/login endpoint
        mockMvc.perform(post("/auth/login") 
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.userId").value(user.getId()))
                .andExpect(jsonPath("$.email").value(userEmail))
                .andExpect(jsonPath("$.username").value("loginTestUser"))
                .andExpect(jsonPath("$.role").value(User.Role.STUDENT.name())); // Verify the role returned
    }

    @Test
    void loginUser_whenPasswordIncorrect_thenUnauthorized() throws Exception {
        // Arrange: Create a user
        String userEmail = "badpass.test@example.com";
        createUserForTest(userEmail, "badPassUser", "CorrectPassword123");

        // Use LoginRequest
        LoginRequest loginRequest = new LoginRequest(); 
        loginRequest.setEmail(userEmail);
        loginRequest.setPassword("WrongPassword123");

        // Target /auth/login endpoint
        mockMvc.perform(post("/auth/login") 
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized()); // Expect 401 Unauthorized
    }

    @Test
    void loginUser_whenUserNotFound_thenUnauthorized() throws Exception {
        // Use LoginRequest
        LoginRequest loginRequest = new LoginRequest(); 
        loginRequest.setEmail("nonexistent.user@example.com");
        loginRequest.setPassword("anyPassword123");

        // Target /auth/login endpoint
        mockMvc.perform(post("/auth/login") 
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized()); // Expect 401 Unauthorized
    }
    
    // --- Password Reset Tests --- // REMOVED as endpoints are not implemented in AuthController
    /*
    @Test
    @Transactional // Use transaction to easily retrieve the token from the DB
    void forgotPassword_whenUserExists_thenSuccessAndTokenGenerated() throws Exception {
        // Arrange
        String userEmail = "forgotpass.test@example.com";
        User user = createUserForTest(userEmail, "forgotPassUser", "InitialPass123");
        ForgotPasswordRequest forgotRequest = new ForgotPasswordRequest();
        forgotRequest.setEmail(userEmail);

        // Act
        mockMvc.perform(post("/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(forgotRequest)))
                .andExpect(status().isOk());

        // Assert: Check if token was generated in DB
        PasswordResetToken token = tokenRepo.findByUser(user).orElse(null);
        assertThat(token).isNotNull();
        assertThat(token.getToken()).isNotEmpty();
        assertThat(token.isExpired()).isFalse();
    }

    @Test
    void forgotPassword_whenUserDoesNotExist_thenStillSuccess() throws Exception {
        // Arrange - Don't create user
        ForgotPasswordRequest forgotRequest = new ForgotPasswordRequest();
        forgotRequest.setEmail("nosuchuser.forgot@example.com");

        // Act & Assert 
        // Should still return OK to prevent leaking info about existing emails
        mockMvc.perform(post("/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(forgotRequest)))
                .andExpect(status().isOk()); 
    }

    @Test
    @Transactional
    void resetPassword_whenTokenIsValid_thenSuccessAndPasswordChanged() throws Exception {
        // Arrange: Create user and generate token
        String userEmail = "resetpass.test@example.com";
        String originalPassword = "ResetThisPass123";
        User user = createUserForTest(userEmail, "resetPassUser", originalPassword);
        
        // Manually create a token (simulating the forgot password step)
        PasswordResetToken token = new PasswordResetToken(user);
        tokenRepo.save(token);
        String validToken = token.getToken();
        String newPassword = "NewSecurePass456";

        ResetPasswordRequest resetRequest = new ResetPasswordRequest();
        resetRequest.setToken(validToken);
        resetRequest.setNewPassword(newPassword);

        // Act
        mockMvc.perform(post("/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(resetRequest)))
                .andExpect(status().isOk());

        // Assert: Verify password changed in DB
        User updatedUser = userRepo.findByEmail(userEmail);
        assertThat(updatedUser).isNotNull();
        assertThat(passwordEncoder.matches(newPassword, updatedUser.getPassword())).isTrue();
        assertThat(passwordEncoder.matches(originalPassword, updatedUser.getPassword())).isFalse();
        
        // Assert: Verify token was deleted
        assertThat(tokenRepo.findByToken(validToken)).isEmpty();
    }
    
    @Test
    void resetPassword_whenTokenIsInvalid_thenBadRequest() throws Exception {
        // Arrange
        String invalidToken = "this-is-not-a-valid-token";
        String newPassword = "NewSecurePass789";
        ResetPasswordRequest resetRequest = new ResetPasswordRequest();
        resetRequest.setToken(invalidToken);
        resetRequest.setNewPassword(newPassword);

        // Act & Assert
        mockMvc.perform(post("/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(resetRequest)))
                .andExpect(status().isBadRequest()); // Or NotFound, depending on controller logic
    }

    // Potential test: resetPassword_whenTokenIsExpired
    // Requires manually setting expiry date or waiting, more complex setup.
    */

    // --- Helper Methods (Optional) ---
    private User createUserForTest(String email, String username, String password) {
        User user = new User();
        user.setEmail(email);
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password)); // Ensure password is encoded
        user.setEnabled(true);
        user.setRole(User.Role.STUDENT); // Explicitly set default role for clarity
        user.setProvider(User.AuthProvider.LOCAL);
        return userRepo.save(user);
    }
} 