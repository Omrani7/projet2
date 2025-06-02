package com.example.spring_security.model;

import jakarta.persistence.*;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDate;
import java.util.Set;
import java.util.HashSet;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(exclude = {"user"}) // Exclude user to prevent circular reference
@Entity
@Table(name = "user_profiles")
public class UserProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "full_name")
    private String fullName;
    
    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;
    
    @Column(name = "field_of_study")
    private String fieldOfStudy;
    
    private String institute;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "user_type")
    private UserType userType;
    
    private String studentYear;

    // Education level for students (Bachelor, Masters, PhD)
    @Enumerated(EnumType.STRING)
    @Column(name = "education_level")
    private EducationLevel educationLevel;

    // Owner-specific fields
    @Column(name = "contact_number")
    private String contactNumber;
    
    private String state;
    
    @Column(name = "accommodation_type")
    private String accommodationType;
    
    @Column(name = "property_management_system")
    private String propertyManagementSystem;
    
    @Column(name = "additional_information")
    private String additionalInformation;
    
    @Column(name = "is_agency")
    private Boolean isAgency;

    @JsonIgnore // Prevent serialization to avoid potential issues
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "user_favorite_properties", joinColumns = @JoinColumn(name = "user_profile_id"))
    @Column(name = "property_id")
    private Set<Long> favoritePropertyIds = new HashSet<>();
    
    @JsonIgnore // Break circular reference: User -> UserProfile -> User
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    public enum UserType {
        OWNER, STUDENT
    }
    
    public enum EducationLevel {
        BACHELOR, MASTERS, PHD
    }
} 