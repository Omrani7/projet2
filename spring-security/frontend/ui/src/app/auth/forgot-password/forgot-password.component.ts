import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../auth.service';

@Component({
  selector: 'app-forgot-password',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './forgot-password.component.html',
  styleUrl: './forgot-password.component.css'
})
export class ForgotPasswordComponent {
  email: string = '';
  errorMessage: string = '';
  successMessage: string = '';
  isSubmitting: boolean = false;

  constructor(
    private router: Router,
    private authService: AuthService
  ) {}

  submitForgotPassword() {
    this.errorMessage = '';
    this.successMessage = '';

    // Basic validation
    if (!this.email) {
      this.errorMessage = 'Please enter your email address';
      return;
    }

    this.isSubmitting = true;

    this.authService.forgotPassword(this.email)
      .subscribe({
        next: (response: any) => {
          this.isSubmitting = false;
          this.successMessage = 'If an account with that email exists, we have sent password reset instructions.';
          this.email = ''; // Clear the input
        },
        error: (error) => {
          this.isSubmitting = false;
          console.error('Error requesting password reset:', error);
          // For security reasons, still show success message even on error
          this.successMessage = 'If an account with that email exists, we have sent password reset instructions.';
        }
      });
  }

  goToLogin() {
    this.router.navigate(['/auth/login']);
  }

  closeModal() {
    this.router.navigate(['/']);
  }
}
