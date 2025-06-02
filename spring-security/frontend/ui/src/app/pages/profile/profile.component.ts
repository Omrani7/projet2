import { Component, OnInit, ViewEncapsulation } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { RouterModule, ActivatedRoute } from '@angular/router';
import { UserProfileService } from '../../services/user-profile.service';
import { AuthService } from '../../auth/auth.service'; // For basic user info like email
import { UserProfile } from '../../models/user-profile.model';
import { PropertyListingService } from '../../services/property-listing.service';
import { Property } from '../../models/property.model';
import { PropertyCardComponent } from '../../components/property-card/property-card.component';
import { OwnerPropertyCardComponent } from '../../components/owner-property-card/owner-property-card.component';
import { PropertyListingDTO } from '../../models/property-listing.dto';
import { InquiryService } from '../../services/inquiry.service';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterModule,
    PropertyCardComponent,
    OwnerPropertyCardComponent
  ],
  templateUrl: './profile.component.html',
  styleUrl: './profile.component.css',
  encapsulation: ViewEncapsulation.None
})
export class ProfileComponent implements OnInit {
  profileForm!: FormGroup;
  currentUserProfile: UserProfile | null = null;
  currentUserId: number | null = null;
  isLoading = true;
  isEditing = false;
  errorMessage: string | null = null;
  successMessage: string | null = null;
  favoriteProperties: Property[] = [];
  showFavorites = false;
  loadingFavorites = false;

  // For displaying basic user info (name, email, phone)
  userName: string | null = null;
  userEmail: string | null = null;
  userPhoneNumber: string | null = null; // Assuming phone number is part of basic user info

  // Added properties for role detection
  userRole: string | null = null;
  isStudent: boolean = false;
  isOwner: boolean = false;

  // For owner properties - changed from Property to PropertyListingDTO
  ownerProperties: PropertyListingDTO[] = [];
  showOwnerProperties = false;
  loadingOwnerProperties = false;

  // For closed deals (students only)
  closedDeals: any[] = [];
  showClosedDeals = false;
  loadingClosedDeals = false;
  closedDealsCount = 0;

  constructor(
    private fb: FormBuilder,
    private userProfileService: UserProfileService,
    private authService: AuthService,
    private propertyService: PropertyListingService,
    private route: ActivatedRoute,
    private inquiryService: InquiryService
  ) {}

  ngOnInit(): void {
    // Check if we should show owner properties based on route data
    this.route.data.subscribe(data => {
      if (data && data['showOwnerProperties']) {
        // Set flag to show owner properties immediately
        this.showOwnerProperties = true;
      }
    });
    
    this.currentUserId = this.userProfileService.getCurrentUserId();
    console.log('[ProfileComponent] ngOnInit - Current User ID from service:', this.currentUserId);
    const token = this.authService.getToken();
    if (token) {
        const decodedToken = this.authService.decodeToken(token);
        if (decodedToken) {
            this.userName = decodedToken.name || decodedToken.username;
            this.userEmail = decodedToken.email;
            this.userPhoneNumber = decodedToken.phoneNumber;
            
            // Extract role from token and set flags
            this.userRole = decodedToken.role;
            this.isStudent = this.userRole === 'STUDENT';
            this.isOwner = this.userRole === 'OWNER';
        }
    }

    // Initialize appropriate form based on role
    this.initializeForm();

    if (this.currentUserId) {
      console.log('[ProfileComponent] ngOnInit - Calling loadUserProfile with ID:', this.currentUserId);
      this.loadUserProfile(this.currentUserId);
      
      // If user is an owner, load their properties
      if (this.isOwner) {
        this.loadOwnerProperties();
      }
    } else {
      this.isLoading = false;
      this.errorMessage = 'Could not identify user.';
    }
  }

  // New method to initialize different forms based on role
  initializeForm(): void {
    if (this.isOwner) {
      this.profileForm = this.fb.group({
        // Owner-specific fields
        contactNumber: ['', [Validators.required]],
        isAgency: [false],
        // Common fields
        fullName: [''],
        // Any other owner-specific fields
      });
    } else {
      // Default to student form
      this.profileForm = this.fb.group({
        institute: ['', [Validators.required, Validators.minLength(3)]],
        fieldOfStudy: ['', [Validators.required, Validators.minLength(2)]],
        studentYear: ['', [Validators.required]],
        educationLevel: ['', [Validators.required]],
        dateOfBirth: ['']
      });
    }
  }

  // Modify loadUserProfile to handle both student and owner
  loadUserProfile(userId: number): void {
    console.log('[ProfileComponent] loadUserProfile - Received userId:', userId, 'isOwner:', this.isOwner);
    this.isLoading = true;
    this.errorMessage = null;
    
    if (this.isOwner) {
      this.userProfileService.getOwnerProfile(userId).subscribe({
        next: (profile) => {
          this.currentUserProfile = profile;
          // Assuming owner profile might have different fields to patch to the form
          // For now, using existing fields as an example.
          // This will need adjustment when owner-specific form is fully defined.
          this.profileForm.patchValue({
            contactNumber: profile.contactNumber, // Example owner field
            isAgency: profile.isAgency,           // Example owner field
            fullName: profile.fullName            // Common field
          });
          if (!this.userName && profile.fullName) this.userName = profile.fullName;
          this.isLoading = false;
          this.isEditing = false;
        },
        error: (err) => {
          this.errorMessage = typeof err === 'string' ? err : 'Failed to load owner profile. ' + (err.message || '');
          this.isLoading = false;
        }
      });
    } else { // Student profile loading
      this.userProfileService.getStudentProfile(userId).subscribe({
        next: (profile) => {
          this.currentUserProfile = profile;
          this.profileForm.patchValue({
            institute: profile.institute,
            fieldOfStudy: profile.fieldOfStudy,
            studentYear: profile.studentYear,
            educationLevel: profile.educationLevel,
            dateOfBirth: profile.dateOfBirth
          });
          if (!this.userName && profile.fullName) this.userName = profile.fullName;
          this.isLoading = false;
          this.isEditing = false;
        },
        error: (err) => {
          this.errorMessage = typeof err === 'string' ? err : 'Failed to load student profile. ' + (err.message || '');
          this.isLoading = false;
        }
      });
    }
  }

  // Also update onSubmit to handle different roles
  onSubmit(): void {
    if (this.profileForm.invalid || !this.currentUserId) {
      this.errorMessage = 'Please fill all required fields correctly.';
      return;
    }
    console.log('[ProfileComponent] onSubmit - currentUserId:', this.currentUserId, 'isOwner:', this.isOwner);
    this.isLoading = true;
    this.errorMessage = null;
    this.successMessage = null;

    const formValues = this.profileForm.value;
    
    // Start with a base from currentUserProfile if it exists, or an empty object
    // Then, add only the fields relevant to the current role and form
    let profileToSend: Partial<UserProfile> = { 
      ...(this.currentUserProfile || {}), // Spread existing profile fields
      id: this.currentUserId // Ensure ID is present
    };

    if (this.isOwner) {
      profileToSend = {
        ...profileToSend,
        contactNumber: formValues.contactNumber,
        isAgency: formValues.isAgency,
        fullName: formValues.fullName || this.currentUserProfile?.fullName || this.userName,
        userType: 'OWNER'
      };
      // Remove student-specific fields that might have been spread from currentUserProfile
      delete profileToSend.institute;
      delete profileToSend.fieldOfStudy;
      delete profileToSend.studentYear;
    } else { // Student
      profileToSend = {
        ...profileToSend,
        institute: formValues.institute,
        fieldOfStudy: formValues.fieldOfStudy,
        studentYear: formValues.studentYear,
        educationLevel: formValues.educationLevel,
        dateOfBirth: formValues.dateOfBirth,
        fullName: formValues.fullName || this.currentUserProfile?.fullName || this.userName,
        userType: 'STUDENT'
      };
      // Remove owner-specific fields
      delete profileToSend.contactNumber;
      delete profileToSend.isAgency;
    }
    
    // The service expects UserProfile, so we cast. 
    // This assumes UserProfile model fields are mostly optional or correctly handled by backend if not all are sent.
    // Or, ensure all *required* fields for UserProfile are present in profileToSend.
    const apiCall = this.isOwner 
      ? this.userProfileService.updateOwnerProfile(this.currentUserId, profileToSend as UserProfile)
      : this.userProfileService.updateStudentProfile(this.currentUserId, profileToSend as UserProfile);

    apiCall.subscribe({
      next: (updatedProfile) => {
        this.currentUserProfile = updatedProfile;
        if (this.isOwner) {
          this.profileForm.patchValue({
            contactNumber: updatedProfile.contactNumber,
            isAgency: updatedProfile.isAgency,
            fullName: updatedProfile.fullName
          });
        } else {
          this.profileForm.patchValue({
            institute: updatedProfile.institute,
            fieldOfStudy: updatedProfile.fieldOfStudy,
            studentYear: updatedProfile.studentYear,
            educationLevel: updatedProfile.educationLevel,
            dateOfBirth: updatedProfile.dateOfBirth,
            fullName: updatedProfile.fullName
          });
        }
        if (!this.userName && updatedProfile.fullName) this.userName = updatedProfile.fullName;
        this.isLoading = false;
        this.isEditing = false;
        this.successMessage = 'Profile updated successfully!';
      },
      error: (err) => {
        this.errorMessage = typeof err === 'string' ? err : 'Failed to update profile. ' + (err.message || '');
        this.isLoading = false;
      }
    });
  }

  enableEdit(): void {
    this.isEditing = true;
    this.successMessage = null;
    this.errorMessage = null;
  }

  cancelEdit(): void {
    this.isEditing = false;
    // Reset form to original values if currentUserProfile is available
    if (this.currentUserProfile) {
      if (this.isStudent) {
        this.profileForm.patchValue({
          institute: this.currentUserProfile.institute,
          fieldOfStudy: this.currentUserProfile.fieldOfStudy,
          studentYear: this.currentUserProfile.studentYear,
          educationLevel: this.currentUserProfile.educationLevel,
          dateOfBirth: this.currentUserProfile.dateOfBirth
        });
      } else {
        this.profileForm.patchValue({
          contactNumber: this.currentUserProfile.contactNumber,
          isAgency: this.currentUserProfile.isAgency,
          fullName: this.currentUserProfile.fullName
        });
      }
    }
  }

  toggleFavoritesView(): void {
    this.showFavorites = !this.showFavorites;
    
    if (this.showFavorites && this.favoriteProperties.length === 0 && 
        this.currentUserProfile?.favoritePropertyIds?.length) {
      this.loadFavoriteProperties();
    }
  }

  loadFavoriteProperties(): void {
    if (!this.currentUserProfile?.favoritePropertyIds?.length) {
      return;
    }

    this.loadingFavorites = true;
    const favoriteIds = this.currentUserProfile.favoritePropertyIds;
    
    // Load properties one by one based on their IDs
    const loadedProperties: Property[] = [];
    let loadCount = 0;
    
    favoriteIds.forEach(id => {
      this.propertyService.getPropertyById(Number(id)).subscribe({
        next: (property) => {
          loadedProperties.push(property as unknown as Property);
          loadCount++;
          
          // When all properties are loaded, update the list
          if (loadCount === favoriteIds.length) {
            this.favoriteProperties = loadedProperties;
            this.loadingFavorites = false;
          }
        },
        error: (err) => {
          console.error(`Error loading property ${id}:`, err);
          loadCount++;
          
          // Even if there's an error, check if all properties have been attempted
          if (loadCount === favoriteIds.length) {
            this.favoriteProperties = loadedProperties;
            this.loadingFavorites = false;
          }
        }
      });
    });
  }

  removeFavorite(propertyId: number): void {
    this.userProfileService.removeFromFavorites(propertyId).subscribe({
      next: (profile) => {
        this.currentUserProfile = profile;
        this.favoriteProperties = this.favoriteProperties.filter(p => p.id !== propertyId);
      },
      error: (err) => {
        console.error('Error removing favorite:', err);
      }
    });
  }

  // Helper method to convert string to number in template
  Number(value: string | number): number {
    return Number(value);
  }

  // Helper method to calculate age from date of birth
  calculateAge(dateOfBirth: string | undefined): number | null {
    if (!dateOfBirth) return null;
    
    const today = new Date();
    const birthDate = new Date(dateOfBirth);
    let age = today.getFullYear() - birthDate.getFullYear();
    const monthDiff = today.getMonth() - birthDate.getMonth();
    
    if (monthDiff < 0 || (monthDiff === 0 && today.getDate() < birthDate.getDate())) {
      age--;
    }
    
    return age;
  }

  // Load properties that belong to the owner
  loadOwnerProperties(): void {
    this.loadingOwnerProperties = true;
    
    this.propertyService.getOwnerProperties().subscribe({
      next: (properties) => {
        this.ownerProperties = properties as unknown as PropertyListingDTO[];
        this.loadingOwnerProperties = false;
      },
      error: (error) => {
        console.error('Error loading owner properties:', error);
        this.loadingOwnerProperties = false;
      }
    });
  }

  // Toggle owner properties view
  toggleOwnerPropertiesView(): void {
    this.showOwnerProperties = !this.showOwnerProperties;
    
    // Load properties if not loaded yet and view is being shown
    if (this.showOwnerProperties && this.ownerProperties.length === 0 && !this.loadingOwnerProperties) {
      this.loadOwnerProperties();
    }
  }

  // Handle property deletion
  handleDeleteProperty(propertyId: number): void {
    if (confirm('Are you sure you want to delete this property? This action cannot be undone.')) {
      // Use the owner-specific endpoint for deletion
      this.propertyService.deleteOwnerProperty(propertyId).subscribe({
        next: () => {
          this.ownerProperties = this.ownerProperties.filter(p => p.id !== propertyId);
          this.successMessage = 'Property deleted successfully.';
          setTimeout(() => this.successMessage = null, 3000);
        },
        error: (err) => {
          console.error('Error deleting property:', err);
          this.errorMessage = 'Failed to delete property. Please try again.';
          setTimeout(() => this.errorMessage = null, 5000);
        }
      });
    }
  }

  // Toggle closed deals view
  toggleClosedDealsView(): void {
    this.showClosedDeals = !this.showClosedDeals;
    
    // Load closed deals if not loaded yet and view is being shown
    if (this.showClosedDeals && this.closedDeals.length === 0 && !this.loadingClosedDeals) {
      this.loadClosedDeals();
    }
  }

  // Load closed deals for student
  loadClosedDeals(): void {
    if (!this.isStudent) {
      return;
    }

    this.loadingClosedDeals = true;
    
    this.inquiryService.getStudentClosedDeals(0, 20).subscribe({
      next: (response) => {
        this.closedDeals = response.content || [];
        this.closedDealsCount = response.totalElements || 0;
        this.loadingClosedDeals = false;
      },
      error: (error) => {
        console.error('Error loading closed deals:', error);
        this.loadingClosedDeals = false;
      }
    });
  }
}