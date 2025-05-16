import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../auth.service';

@Component({
  selector: 'app-reset-password',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './reset-password.component.html',
  styleUrl: './reset-password.component.css'
})
export class ResetPasswordComponent implements OnInit {
  token: string = '';
  password: string = '';
  confirmPassword: string = '';
  errorMessage: string = '';
  successMessage: string = '';
  isSubmitting: boolean = false;
  isValidToken: boolean = false;
  isLoading: boolean = true;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private authService: AuthService
  ) {}

  ngOnInit() {
    // Get token from URL
    this.route.queryParams.subscribe(params => {
      this.token = params['token'] || '';
      if (this.token) {
        this.validateToken();
      } else {
        this.errorMessage = 'Missing password reset token';
        this.isLoading = false;
      }
    });
  }

  validateToken() {
    this.authService.validateResetToken(this.token)
      .subscribe({
        next: (response: any) => {
          this.isValidToken = response.valid;
          this.isLoading = false;
          if (!this.isValidToken) {
            this.errorMessage = 'Invalid or expired password reset token';
          }
        },
        error: (error) => {
          this.isLoading = false;
          this.isValidToken = false;
          this.errorMessage = 'Error validating token. Please try again.';
          console.error('Token validation error:', error);
        }
      });
  }

  resetPassword() {
    this.errorMessage = '';
    this.successMessage = '';

    // Basic validation
    if (!this.password || !this.confirmPassword) {
      this.errorMessage = 'Please fill in all fields';
      return;
    }

    if (this.password !== this.confirmPassword) {
      this.errorMessage = 'Passwords do not match';
      return;
    }

    if (this.password.length < 8) {
      this.errorMessage = 'Password must be at least 8 characters long';
      return;
    }

    this.isSubmitting = true;

    this.authService.resetPassword(this.token, this.password, this.confirmPassword)
      .subscribe({
        next: (response: any) => {
          this.isSubmitting = false;
          this.successMessage = 'Your password has been reset successfully';

          // Clear the fields
          this.password = '';
          this.confirmPassword = '';

          // Redirect to login after 3 seconds
          setTimeout(() => {
            this.router.navigate(['/auth/login']);
          }, 3000);
        },
        error: (error) => {
          this.isSubmitting = false;
          console.error('Password reset error:', error);
          if (error.error && error.error.error) {
            this.errorMessage = error.error.error;
          } else {
            this.errorMessage = 'Failed to reset password. Please try again.';
          }
        }
      });
  }

  goToLogin() {
    this.router.navigate(['/auth/login']);
  }
}
