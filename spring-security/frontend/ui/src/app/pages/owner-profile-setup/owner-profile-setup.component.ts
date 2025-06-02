import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { UserProfileService } from '../../services/user-profile.service';
import { AuthService } from '../../auth/auth.service';
import { UserProfile } from '../../models/user-profile.model';

@Component({
  selector: 'app-owner-profile-setup',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterModule
  ],
  templateUrl: './owner-profile-setup.component.html',
  styleUrl: './owner-profile-setup.component.css'
})
export class OwnerProfileSetupComponent implements OnInit {
  ownerProfileForm!: FormGroup;
  isLoading: boolean = false;
  errorMessage: string | null = null;
  successMessage: string | null = null;
  userId: number | null = null;
  
  // Options for dropdown selects
  titleOptions: string[] = ['Mr.', 'Mrs.', 'Ms.', 'Dr.', 'Prof.'];
  countryOptions: string[] = ['Tunisia', 'United Kingdom', 'United States', 'France', 'Germany', 'Spain', 'Italy'];
  accommodationTypeOptions: string[] = ['Apartment', 'House', 'Studio', 'Dorm', 'Shared Room', 'Other'];

  constructor(
    private fb: FormBuilder,
    private userProfileService: UserProfileService,
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.initializeForm();
    
    // Get current user ID
    this.userId = this.userProfileService.getCurrentUserId();
    
    if (this.userId) {
      this.loadExistingProfile();
    } else {
      this.errorMessage = 'Not logged in. Please log in to continue.';
      // Redirect to login after a delay
      setTimeout(() => this.router.navigate(['/auth/login']), 2000);
    }
  }

  private initializeForm(): void {
    this.ownerProfileForm = this.fb.group({
      title: ['Mr.', Validators.required],
      fullName: ['', [Validators.required, Validators.minLength(3)]],
      email: ['', [Validators.required, Validators.email]],
      phoneCode: ['+216', Validators.required],
      contactNumber: ['', [Validators.required, Validators.pattern(/^\d{8,}$/)]],
      state: ['', Validators.required],
      accommodationType: ['', Validators.required],
      propertyManagementSystem: [''],
      additionalInformation: [''],
      isAgency: [false]
    });
  }

  private loadExistingProfile(): void {
    if (!this.userId) return;
    
    this.isLoading = true;
    this.userProfileService.getOwnerProfile(this.userId).subscribe({
      next: (profile) => {
        if (profile) {
          // Pre-populate the form with existing data
          this.ownerProfileForm.patchValue({
            fullName: profile.fullName,
            contactNumber: profile.contactNumber,
            state: profile.state,
            accommodationType: profile.accommodationType,
            propertyManagementSystem: profile.propertyManagementSystem,
            additionalInformation: profile.additionalInformation,
            isAgency: profile.isAgency || false
          });
          
          // If we have user information from the token, populate email
          const token = this.authService.getDecodedToken();
          if (token && token.email) {
            this.ownerProfileForm.patchValue({ email: token.email });
          }
        }
        this.isLoading = false;
      },
      error: (err) => {
        // Only show error if it's not a 404 (which is expected for new owners)
        if (err.status !== 404) {
          this.errorMessage = 'Failed to load profile. Please try again.';
        }
        this.isLoading = false;
      }
    });
  }

  onSubmit(): void {
    if (this.ownerProfileForm.invalid) {
      this.markFormGroupTouched(this.ownerProfileForm);
      this.errorMessage = 'Please complete all required fields correctly.';
      return;
    }
    
    if (!this.userId) {
      this.errorMessage = 'User ID not found. Please log in again.';
      return;
    }
    
    this.isLoading = true;
    this.errorMessage = null;
    this.successMessage = null;
    
    // Prepare profile data from form
    const formData = this.ownerProfileForm.value;
    
    // Combine phone code and number
    const phoneWithCode = `${formData.phoneCode}${formData.contactNumber}`;
    
    const profileData = {
      id: 0, // Will be set by backend
      userId: this.userId,
      fullName: formData.fullName,
      contactNumber: phoneWithCode,
      state: formData.state,
      accommodationType: formData.accommodationType,
      propertyManagementSystem: formData.propertyManagementSystem,
      additionalInformation: formData.additionalInformation,
      isAgency: formData.isAgency
    };
    
    this.userProfileService.updateOwnerProfile(this.userId, profileData).subscribe({
      next: (response) => {
        this.successMessage = 'Profile saved successfully!';
        this.isLoading = false;
        
        // Redirect to the main profile page or dashboard after a short delay
        setTimeout(() => {
          this.router.navigate(['/profile'], { replaceUrl: true });
        }, 1500);
      },
      error: (err) => {
        this.errorMessage = 'Failed to save profile. Please try again later.';
        console.error('Error saving profile:', err);
        this.isLoading = false;
      }
    });
  }
  
  // Helper method to mark all form controls as touched for validation display
  private markFormGroupTouched(formGroup: FormGroup): void {
    Object.values(formGroup.controls).forEach(control => {
      control.markAsTouched();
      if ((control as any).controls) {
        this.markFormGroupTouched(control as FormGroup);
      }
    });
  }
}
