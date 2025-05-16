package com.example.spring_security.model;

import jakarta.persistence.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "users")
public class User {

    // Define the new Role enum (can be moved to a separate file if preferred)
    public enum Role {
        STUDENT, // Can view/apply to announcements
        OWNER,   // Can manage announcements
        ADMIN    // Can manage users and system settings
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    
    @Column(unique = true, nullable = false)
    private String email;
    
    @Column(nullable = false)
    private String username;
    
    @Column(nullable = false)
    private String password;
    
    private String phoneNumber;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "auth_provider", nullable = false)
    private AuthProvider provider = AuthProvider.LOCAL;
    
    @Column(name = "provider_id")
    private String providerId;
    
    @Column(nullable = false)
    private boolean enabled = true;
    
    // Use the new Role enum, default to STUDENT
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private Role role = Role.STUDENT;
    
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private UserProfile userProfile;
    
    // Transient field to indicate if this is a new OAuth2 user (not stored in database)
    @Transient
    private boolean newOAuth2User = false;
    
    public enum AuthProvider {
        LOCAL, GOOGLE, GITHUB
    }

    public AuthProvider getProvider() {
        return provider;
    }

    public void setProvider(AuthProvider provider) {
        this.provider = provider;
    }

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }
}
