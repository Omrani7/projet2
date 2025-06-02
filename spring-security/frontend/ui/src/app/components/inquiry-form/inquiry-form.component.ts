import { Component, Input, Output, EventEmitter, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { InquiryService } from '../../services/inquiry.service';
import { AuthService } from '../../auth/auth.service';
import { InquiryCreate } from '../../models/inquiry.model';

@Component({
  selector: 'app-inquiry-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './inquiry-form.component.html',
  styleUrls: ['./inquiry-form.component.css']
})
export class InquiryFormComponent implements OnInit {
  @Input() propertyId!: number;
  @Input() propertyTitle!: string;
  @Input() sourceType!: string; // To check if it's OWNER property
  @Output() inquirySent = new EventEmitter<void>();
  @Output() closeForm = new EventEmitter<void>();

  inquiryForm: FormGroup;
  isSubmitting = false;
  showForm = false;
  submitError: string | null = null;
  submitSuccess = false;

  constructor(
    private fb: FormBuilder,
    private inquiryService: InquiryService,
    private authService: AuthService
  ) {
    this.inquiryForm = this.fb.group({
      message: ['', [Validators.required, Validators.minLength(10), Validators.maxLength(500)]],
      phoneNumber: ['', [Validators.required, Validators.pattern(/^[0-9+\-\s()]{8,15}$/)]]
    });
  }

  ngOnInit(): void {
    // Component is ready
  }

  /**
   * Check if current user is a student and authenticated
   */
  canMakeInquiry(): boolean {
    if (!this.authService.isLoggedIn()) {
      return false;
    }

    const userRole = this.authService.getUserRole();
    const isStudent = userRole === 'STUDENT';
    const isOwnerProperty = this.sourceType === 'OWNER';
    
    return isStudent && isOwnerProperty;
  }

  /**
   * Get the message to display for the inquiry button
   */
  getInquiryButtonMessage(): string {
    if (!this.authService.isLoggedIn()) {
      return 'Login to Inquire';
    }

    const userRole = this.authService.getUserRole();
    if (userRole !== 'STUDENT') {
      return 'Only Students Can Inquire';
    }

    if (this.sourceType !== 'OWNER') {
      return 'Inquiries Not Available';
    }

    return 'Enquire';
  }

  /**
   * Show the inquiry form
   */
  openInquiryForm(): void {
    if (!this.canMakeInquiry()) {
      if (!this.authService.isLoggedIn()) {
        // Redirect to login - user will handle navigation
        console.log('User needs to login');
        return;
      }
      return;
    }

    this.showForm = true;
    this.resetForm();
  }

  /**
   * Hide the inquiry form
   */
  closeInquiryForm(): void {
    this.showForm = false;
    this.resetForm();
    this.closeForm.emit();
  }

  /**
   * Submit the inquiry
   */
  onSubmit(): void {
    if (this.inquiryForm.invalid || this.isSubmitting) {
      return;
    }

    // DEBUGGING: Check auth state before making request
    console.log('[InquiryForm] Starting inquiry submission...');
    console.log('[InquiryForm] Is logged in:', this.authService.isLoggedIn());
    console.log('[InquiryForm] Token from AuthService:', this.authService.getToken());
    console.log('[InquiryForm] User role:', this.authService.getUserRole());
    console.log('[InquiryForm] localStorage auth_token:', localStorage.getItem('auth_token'));

    this.isSubmitting = true;
    this.submitError = null;

    const inquiryData: InquiryCreate = {
      propertyId: this.propertyId,
      message: this.inquiryForm.get('message')?.value,
      phoneNumber: this.inquiryForm.get('phoneNumber')?.value
    };

    console.log('[InquiryForm] About to call inquiryService.createInquiry...');

    this.inquiryService.createInquiry(inquiryData).subscribe({
      next: (inquiry) => {
        console.log('Inquiry sent successfully:', inquiry);
        this.submitSuccess = true;
        this.isSubmitting = false;
        
        // Show success message briefly then close form
        setTimeout(() => {
          this.closeInquiryForm();
          this.inquirySent.emit();
        }, 2000);
      },
      error: (error) => {
        console.error('Error sending inquiry:', error);
        this.isSubmitting = false;
        
        if (error.status === 400 && error.error?.message) {
          this.submitError = error.error.message;
        } else if (error.status === 403) {
          this.submitError = 'You do not have permission to make inquiries about this property.';
        } else if (error.status === 404) {
          this.submitError = 'Property not found.';
        } else {
          this.submitError = 'Failed to send inquiry. Please try again.';
        }
      }
    });
  }

  /**
   * Reset the form
   */
  private resetForm(): void {
    this.inquiryForm.reset();
    this.submitError = null;
    this.submitSuccess = false;
    this.isSubmitting = false;
  }

  /**
   * Get form control for template
   */
  get messageControl() {
    return this.inquiryForm.get('message');
  }

  /**
   * Get phone number form control for template
   */
  get phoneNumberControl() {
    return this.inquiryForm.get('phoneNumber');
  }
} 