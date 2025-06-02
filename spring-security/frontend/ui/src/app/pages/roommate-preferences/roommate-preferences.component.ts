import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, FormArray, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { UserRoommatePreferencesService, UserRoommatePreferencesDTO } from '../../services/user-roommate-preferences.service';

@Component({
  selector: 'app-roommate-preferences',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './roommate-preferences.component.html',
  styleUrls: ['./roommate-preferences.component.css']
})
export class RoommatePreferencesComponent implements OnInit {
  preferencesForm!: FormGroup;
  isLoading = false;
  isSaving = false;
  errorMessage = '';
  successMessage = '';
  
  // Available options
  availableLifestyleTags: string[] = [];
  availableStudyHabits: string[] = [];
  cleanlinessDescriptions: {[key: number]: string} = {};
  socialDescriptions: {[key: number]: string} = {};
  
  // Form state
  hasExistingPreferences = false;
  isPreferencesComplete = false;

  constructor(
    private fb: FormBuilder,
    private preferencesService: UserRoommatePreferencesService,
    private router: Router
  ) {
    this.initializeForm();
  }

  ngOnInit(): void {
    this.loadAvailableOptions();
    this.loadUserPreferences();
  }

  private initializeForm(): void {
    this.preferencesForm = this.fb.group({
      // Lifestyle preferences
      lifestyleTags: this.fb.array([]),
      cleanlinessLevel: [null, [Validators.min(1), Validators.max(5)]],
      socialLevel: [null, [Validators.min(1), Validators.max(5)]],
      
      // Study preferences
      studyHabits: this.fb.array([]),
      
      // Budget preferences
      budgetMin: [null, [Validators.min(0)]],
      budgetMax: [null, [Validators.min(0)]],
      
      // Additional preferences
      additionalPreferences: ['', [Validators.maxLength(1000)]]
    });
  }

  private loadAvailableOptions(): void {
    this.availableLifestyleTags = this.preferencesService.getAvailableLifestyleTags();
    this.availableStudyHabits = this.preferencesService.getAvailableStudyHabits();
    this.cleanlinessDescriptions = this.preferencesService.getCleanlinesLevelDescriptions();
    this.socialDescriptions = this.preferencesService.getSocialLevelDescriptions();
  }

  private loadUserPreferences(): void {
    this.isLoading = true;
    this.errorMessage = '';

    this.preferencesService.getUserPreferences().subscribe({
      next: (preferences) => {
        this.hasExistingPreferences = preferences.isComplete || false;
        this.isPreferencesComplete = preferences.isComplete || false;
        this.populateForm(preferences);
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Error loading preferences:', error);
        this.errorMessage = error.message || 'Failed to load preferences';
        this.isLoading = false;
      }
    });
  }

  private populateForm(preferences: UserRoommatePreferencesDTO): void {
    // Populate basic fields
    this.preferencesForm.patchValue({
      cleanlinessLevel: preferences.cleanlinessLevel,
      socialLevel: preferences.socialLevel,
      budgetMin: preferences.budgetMin,
      budgetMax: preferences.budgetMax,
      additionalPreferences: preferences.additionalPreferences
    });

    // Populate lifestyle tags
    const lifestyleArray = this.preferencesForm.get('lifestyleTags') as FormArray;
    lifestyleArray.clear();
    this.availableLifestyleTags.forEach(tag => {
      const isSelected = preferences.lifestyleTags?.includes(tag) || false;
      lifestyleArray.push(this.fb.control(isSelected));
    });

    // Populate study habits
    const studyArray = this.preferencesForm.get('studyHabits') as FormArray;
    studyArray.clear();
    this.availableStudyHabits.forEach(habit => {
      const isSelected = preferences.studyHabits?.includes(habit) || false;
      studyArray.push(this.fb.control(isSelected));
    });
  }

  // Getter methods for form arrays
  get lifestyleTagsArray(): FormArray {
    return this.preferencesForm.get('lifestyleTags') as FormArray;
  }

  get studyHabitsArray(): FormArray {
    return this.preferencesForm.get('studyHabits') as FormArray;
  }

  // Helper methods for UI
  getSelectedLifestyleTags(): string[] {
    return this.availableLifestyleTags.filter((tag, index) => 
      this.lifestyleTagsArray.at(index).value
    );
  }

  getSelectedStudyHabits(): string[] {
    return this.availableStudyHabits.filter((habit, index) => 
      this.studyHabitsArray.at(index).value
    );
  }

  onSubmit(): void {
    if (this.preferencesForm.invalid) {
      this.markFormGroupTouched();
      this.errorMessage = 'Please fix the errors in the form';
      return;
    }

    const formValue = this.preferencesForm.value;
    
    // Build preferences DTO
    const preferences: UserRoommatePreferencesDTO = {
      lifestyleTags: this.getSelectedLifestyleTags(),
      cleanlinessLevel: formValue.cleanlinessLevel,
      socialLevel: formValue.socialLevel,
      studyHabits: this.getSelectedStudyHabits(),
      budgetMin: formValue.budgetMin,
      budgetMax: formValue.budgetMax,
      additionalPreferences: formValue.additionalPreferences
    };

    // Validate preferences
    const validationErrors = this.preferencesService.validatePreferences(preferences);
    if (validationErrors.length > 0) {
      this.errorMessage = validationErrors.join(', ');
      return;
    }

    this.savePreferences(preferences);
  }

  private savePreferences(preferences: UserRoommatePreferencesDTO): void {
    this.isSaving = true;
    this.errorMessage = '';
    this.successMessage = '';

    this.preferencesService.updateUserPreferences(preferences).subscribe({
      next: (savedPreferences) => {
        this.isSaving = false;
        this.successMessage = 'Preferences saved successfully!';
        this.hasExistingPreferences = true;
        this.isPreferencesComplete = this.preferencesService.isPreferencesComplete(savedPreferences);
        
        // Auto-hide success message after 3 seconds
        setTimeout(() => {
          this.successMessage = '';
        }, 3000);
      },
      error: (error) => {
        console.error('Error saving preferences:', error);
        this.errorMessage = error.message || 'Failed to save preferences';
        this.isSaving = false;
      }
    });
  }

  onReset(): void {
    if (confirm('Are you sure you want to reset all preferences? This will clear all your current settings.')) {
      this.initializeForm();
      this.loadAvailableOptions();
      this.errorMessage = '';
      this.successMessage = '';
    }
  }

  onDelete(): void {
    if (confirm('Are you sure you want to delete all your roommate preferences? This action cannot be undone.')) {
      this.isLoading = true;
      this.errorMessage = '';

      this.preferencesService.deleteUserPreferences().subscribe({
        next: (response) => {
          this.isLoading = false;
          this.successMessage = response.message;
          this.hasExistingPreferences = false;
          this.isPreferencesComplete = false;
          this.initializeForm();
          this.loadAvailableOptions();
        },
        error: (error) => {
          console.error('Error deleting preferences:', error);
          this.errorMessage = error.message || 'Failed to delete preferences';
          this.isLoading = false;
        }
      });
    }
  }

  navigateToRoommates(): void {
    this.router.navigate(['/browse-roommates']);
  }

  private markFormGroupTouched(): void {
    Object.keys(this.preferencesForm.controls).forEach(key => {
      const control = this.preferencesForm.get(key);
      control?.markAsTouched();
      
      if (control instanceof FormArray) {
        control.controls.forEach(c => c.markAsTouched());
      }
    });
  }

  // Helper methods for template
  isFieldInvalid(fieldName: string): boolean {
    const field = this.preferencesForm.get(fieldName);
    return !!(field && field.invalid && field.touched);
  }

  getFieldError(fieldName: string): string {
    const field = this.preferencesForm.get(fieldName);
    if (field && field.errors && field.touched) {
      if (field.errors['required']) return `${fieldName} is required`;
      if (field.errors['min']) return `${fieldName} must be at least ${field.errors['min'].min}`;
      if (field.errors['max']) return `${fieldName} must be at most ${field.errors['max'].max}`;
      if (field.errors['maxlength']) return `${fieldName} is too long`;
    }
    return '';
  }

  formatTagName(tag: string): string {
    return tag.replace(/_/g, ' ').toLowerCase().replace(/\b\w/g, l => l.toUpperCase());
  }

  // Helper methods for UI icons and descriptions
  getLifestyleIcon(tag: string): string {
    const icons: {[key: string]: string} = {
      'QUIET': '🤫',
      'SOCIAL': '🎉',
      'STUDIOUS': '📚',
      'PARTY': '🎊',
      'EARLY_BIRD': '🌅',
      'NIGHT_OWL': '🦉',
      'FITNESS': '💪',
      'COOKING': '👨‍🍳',
      'MUSIC': '🎵',
      'GAMING': '🎮'
    };
    return icons[tag] || '🏷️';
  }

  getStudyIcon(habit: string): string {
    const icons: {[key: string]: string} = {
      'QUIET_STUDY': '🤫',
      'GROUP_STUDY': '👥',
      'LIBRARY': '📚',
      'HOME_STUDY': '🏠',
      'LATE_NIGHT': '🌙',
      'EARLY_MORNING': '🌅',
      'MUSIC_WHILE_STUDYING': '🎧',
      'BREAKS_OFTEN': '☕'
    };
    return icons[habit] || '📖';
  }
} 