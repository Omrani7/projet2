import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { FormBuilder, FormGroup, FormArray, Validators, ReactiveFormsModule, FormsModule } from '@angular/forms';
import { RoommateService, RoommateAnnouncementCreateDTO } from '../../services/roommate.service';
import { AuthService } from '../../auth/auth.service';

interface ClosedDeal {
  id: number;
  property: {
    id: number;
    title: string;
    address: string;
    price: number;
    location: string;
    rooms: number;
    type: string;
  };
  agreedPrice: number;
  timestamp: string;
  status: string;
}

interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

@Component({
  selector: 'app-post-roommate-announcement',
  standalone: true,
  imports: [CommonModule, RouterModule, ReactiveFormsModule, FormsModule],
  templateUrl: './post-roommate-announcement.component.html',
  styleUrls: ['./post-roommate-announcement.component.css']
})
export class PostRoommateAnnouncementComponent implements OnInit {
  
  // Form and UI state
  announcementForm: FormGroup;
  announcementType: 'TYPE_A' | 'TYPE_B' = 'TYPE_A'; // Default to closed deals
  
  // Data
  closedDeals: ClosedDeal[] = [];
  selectedDeal?: ClosedDeal;
  
  // UI state
  isLoadingDeals = true;
  isSubmitting = false;
  error: string | null = null;
  successMessage: string | null = null;
  showPreview = false;

  constructor(
    private fb: FormBuilder,
    private roommateService: RoommateService,
    private authService: AuthService,
    private router: Router
  ) {
    this.announcementForm = this.createForm();
  }

  ngOnInit(): void {
    this.loadClosedDeals();
    this.setupFormWatchers();
  }

  /**
   * Create the reactive form
   */
  private createForm(): FormGroup {
    return this.fb.group({
      // Property details (required for Type B, auto-filled for Type A)
      propertyTitle: ['', [Validators.required, Validators.maxLength(200)]],
      propertyAddress: ['', [Validators.required, Validators.maxLength(500)]],
      propertyLatitude: [null],
      propertyLongitude: [null],
      totalRent: [null, [Validators.required, Validators.min(1)]],
      totalRooms: [null, [Validators.required, Validators.min(1)]],
      availableRooms: [null, [Validators.required, Validators.min(1)]],
      propertyType: ['APARTMENT', Validators.required],
      
      // Roommate preferences
      maxRoommates: [1, [Validators.required, Validators.min(1), Validators.max(10)]],
      genderPreference: ['NO_PREFERENCE', Validators.required],
      ageMin: [18, [Validators.required, Validators.min(18), Validators.max(65)]],
      ageMax: [35, [Validators.required, Validators.min(18), Validators.max(65)]],
      lifestyleTags: this.fb.array([]),
      smokingAllowed: [false],
      petsAllowed: [false],
      cleanlinessLevel: [3, [Validators.required, Validators.min(1), Validators.max(5)]],
      
      // Financial details
      rentPerPerson: [null, [Validators.required, Validators.min(1)]],
      securityDeposit: [0, [Validators.min(0)]],
      utilitiesSplit: ['EQUAL', Validators.required],
      additionalCosts: [''],
      
      // Posting details
      description: ['', [Validators.required, Validators.maxLength(1000)]],
      moveInDate: ['', Validators.required],
      leaseDurationMonths: [12, [Validators.required, Validators.min(1), Validators.max(60)]]
    });
  }

  /**
   * Load closed deals for Type A announcements
   */
  private loadClosedDeals(): void {
    this.isLoadingDeals = true;
    this.error = null;

    this.roommateService.getClosedDealsForStudent().subscribe({
      next: (response: Page<ClosedDeal>) => {
        this.closedDeals = response.content;
        this.isLoadingDeals = false;
        
        console.log(`Loaded ${this.closedDeals.length} closed deals`);
        
        // If no closed deals, switch to Type B
        if (this.closedDeals.length === 0) {
          this.announcementType = 'TYPE_B';
        }
      },
      error: (error) => {
        console.error('Error loading closed deals:', error);
        this.error = 'Failed to load your closed deals. You can still create an announcement manually.';
        this.isLoadingDeals = false;
        this.announcementType = 'TYPE_B';
      }
    });
  }

  /**
   * Setup form value change watchers
   */
  private setupFormWatchers(): void {
    // Watch for total rent changes to auto-calculate rent per person
    this.announcementForm.get('totalRent')?.valueChanges.subscribe(totalRent => {
      const maxRoommates = this.announcementForm.get('maxRoommates')?.value || 1;
      if (totalRent && maxRoommates) {
        const rentPerPerson = totalRent / (maxRoommates + 1); // +1 for poster
        this.announcementForm.get('rentPerPerson')?.setValue(Math.round(rentPerPerson));
      }
    });

    // Watch for max roommates changes
    this.announcementForm.get('maxRoommates')?.valueChanges.subscribe(maxRoommates => {
      const totalRent = this.announcementForm.get('totalRent')?.value;
      if (totalRent && maxRoommates) {
        const rentPerPerson = totalRent / (maxRoommates + 1);
        this.announcementForm.get('rentPerPerson')?.setValue(Math.round(rentPerPerson));
      }
    });

    // Watch for age min/max validation
    this.announcementForm.get('ageMin')?.valueChanges.subscribe(ageMin => {
      const ageMax = this.announcementForm.get('ageMax')?.value;
      if (ageMin && ageMax && ageMin > ageMax) {
        this.announcementForm.get('ageMax')?.setValue(ageMin);
      }
    });
  }

  // ========== TYPE SWITCHING ==========

  /**
   * Switch between Type A and Type B announcements
   */
  switchAnnouncementType(type: 'TYPE_A' | 'TYPE_B'): void {
    this.announcementType = type;
    this.selectedDeal = undefined;
    this.resetForm();
  }

  /**
   * Reset form to default values
   */
  private resetForm(): void {
    this.announcementForm.reset({
      propertyType: 'APARTMENT',
      maxRoommates: 1,
      genderPreference: 'NO_PREFERENCE',
      ageMin: 18,
      ageMax: 35,
      cleanlinessLevel: 3,
      utilitiesSplit: 'EQUAL',
      leaseDurationMonths: 12,
      smokingAllowed: false,
      petsAllowed: false,
      securityDeposit: 0
    });
    this.clearLifestyleTags();
  }

  // ========== CLOSED DEAL HANDLING ==========

  /**
   * Select a closed deal for Type A announcement
   */
  selectClosedDeal(deal: ClosedDeal): void {
    this.selectedDeal = deal;
    this.populateFormFromDeal(deal);
  }

  /**
   * Populate form from selected closed deal
   */
  private populateFormFromDeal(deal: ClosedDeal): void {
    this.announcementForm.patchValue({
      propertyTitle: deal.property.title,
      propertyAddress: deal.property.address,
      totalRent: deal.agreedPrice,
      totalRooms: deal.property.rooms,
      availableRooms: Math.max(1, deal.property.rooms - 1), // Assume poster takes 1 room
      propertyType: deal.property.type?.toUpperCase() || 'APARTMENT'
    });

    // Auto-calculate rent per person
    const maxRoommates = this.announcementForm.get('maxRoommates')?.value || 1;
    const rentPerPerson = deal.agreedPrice / (maxRoommates + 1);
    this.announcementForm.get('rentPerPerson')?.setValue(Math.round(rentPerPerson));

    console.log('Form populated from closed deal:', deal);
  }

  // ========== LIFESTYLE TAGS MANAGEMENT ==========

  get lifestyleTags(): FormArray {
    return this.announcementForm.get('lifestyleTags') as FormArray;
  }

  /**
   * Add lifestyle tag
   */
  addLifestyleTag(tag: string): void {
    if (!this.hasLifestyleTag(tag)) {
      this.lifestyleTags.push(this.fb.control(tag));
    }
  }

  /**
   * Remove lifestyle tag
   */
  removeLifestyleTag(index: number): void {
    this.lifestyleTags.removeAt(index);
  }

  /**
   * Check if lifestyle tag exists
   */
  hasLifestyleTag(tag: string): boolean {
    return this.lifestyleTags.value.includes(tag);
  }

  /**
   * Clear all lifestyle tags
   */
  clearLifestyleTags(): void {
    while (this.lifestyleTags.length !== 0) {
      this.lifestyleTags.removeAt(0);
    }
  }

  /**
   * Toggle lifestyle tag
   */
  toggleLifestyleTag(tag: string): void {
    if (this.hasLifestyleTag(tag)) {
      const index = this.lifestyleTags.value.indexOf(tag);
      this.removeLifestyleTag(index);
    } else {
      this.addLifestyleTag(tag);
    }
  }

  // ========== FORM SUBMISSION ==========

  /**
   * Submit the announcement
   */
  onSubmit(): void {
    if (this.announcementForm.invalid) {
      this.markFormGroupTouched();
      this.error = 'Please fill in all required fields correctly.';
      return;
    }

    this.isSubmitting = true;
    this.error = null;

    const formValue = this.announcementForm.value;
    
    const createDTO: RoommateAnnouncementCreateDTO = {
      // Add propertyListingId if Type A
      ...(this.announcementType === 'TYPE_A' && this.selectedDeal ? 
        { propertyListingId: this.selectedDeal.property.id } : {}),
      
      // Property details
      propertyTitle: formValue.propertyTitle,
      propertyAddress: formValue.propertyAddress,
      totalRent: formValue.totalRent,
      totalRooms: formValue.totalRooms,
      availableRooms: formValue.availableRooms,
      propertyType: formValue.propertyType,
      
      // Roommate preferences
      maxRoommates: formValue.maxRoommates,
      genderPreference: formValue.genderPreference,
      ageMin: formValue.ageMin,
      ageMax: formValue.ageMax,
      lifestyleTags: formValue.lifestyleTags,
      smokingAllowed: formValue.smokingAllowed,
      petsAllowed: formValue.petsAllowed,
      cleanlinessLevel: formValue.cleanlinessLevel,
      
      // Financial details
      rentPerPerson: formValue.rentPerPerson,
      securityDeposit: formValue.securityDeposit,
      utilitiesSplit: formValue.utilitiesSplit,
      additionalCosts: formValue.additionalCosts,
      
      // Posting details
      description: formValue.description,
      moveInDate: formValue.moveInDate,
      leaseDurationMonths: formValue.leaseDurationMonths
    };

    this.roommateService.createAnnouncement(createDTO).subscribe({
      next: (createdAnnouncement) => {
        this.isSubmitting = false;
        this.successMessage = 'Roommate announcement posted successfully!';
        
        console.log('Announcement created:', createdAnnouncement);
        
        // Redirect to the announcement details or my announcements
        setTimeout(() => {
          this.router.navigate(['/roommates/announcement', createdAnnouncement.id]);
        }, 2000);
      },
      error: (error) => {
        console.error('Error creating announcement:', error);
        this.error = error || 'Failed to create announcement. Please try again.';
        this.isSubmitting = false;
      }
    });
  }

  /**
   * Mark all form fields as touched to show validation errors
   */
  private markFormGroupTouched(): void {
    Object.keys(this.announcementForm.controls).forEach(key => {
      const control = this.announcementForm.get(key);
      control?.markAsTouched();
    });
  }

  // ========== UTILITY METHODS ==========

  /**
   * Check if field has error
   */
  hasFieldError(fieldName: string): boolean {
    const field = this.announcementForm.get(fieldName);
    return !!(field && field.invalid && field.touched);
  }

  /**
   * Get field error message
   */
  getFieldError(fieldName: string): string {
    const field = this.announcementForm.get(fieldName);
    if (field && field.errors && field.touched) {
      if (field.errors['required']) return `${fieldName} is required`;
      if (field.errors['min']) return `${fieldName} must be at least ${field.errors['min'].min}`;
      if (field.errors['max']) return `${fieldName} must be at most ${field.errors['max'].max}`;
      if (field.errors['maxlength']) return `${fieldName} is too long`;
    }
    return '';
  }

  /**
   * Get form preview data
   */
  getFormPreview(): any {
    return {
      ...this.announcementForm.value,
      type: this.announcementType,
      selectedDeal: this.selectedDeal
    };
  }

  /**
   * Cancel and go back
   */
  cancel(): void {
    this.router.navigate(['/roommates/browse']);
  }
} 