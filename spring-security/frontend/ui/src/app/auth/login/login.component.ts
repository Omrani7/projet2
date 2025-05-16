import { Component, EventEmitter, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './login.component.html',
  styleUrl: './login.component.css'
})
export class LoginComponent {
  @Output() closeModalEvent = new EventEmitter<void>();

  email: string = '';
  password: string = '';
  isPasswordVisible: boolean = false;
  errorMessage: string = '';
  isLoggingIn: boolean = false;
  isGoogleLoginInProgress: boolean = false;

  constructor(
    private router: Router,
    private authService: AuthService
  ) {}

  continueWithEmail() {
    this.errorMessage = '';

    if (this.isPasswordVisible) {
      // --- Client-Side Validation for Login Attempt ---
      if (!this.email || !this.password) {
        this.errorMessage = 'Please enter both email and password.';
        return; // Stop if fields are empty
      }
      // --- End Validation ---

      if (this.isLoggingIn) return; // Prevent multiple submissions

      this.isLoggingIn = true;
      this.authService.login(this.email, this.password).subscribe({
        next: (response) => {
          console.log('Login successful', response);
          this.isLoggingIn = false;
          this.closeModalEvent.emit();
          this.router.navigate(['/']);
        },
        error: (error) => {
          console.error('Login failed', error);
          this.errorMessage = error.message || 'Login failed. Please try again.';
          this.isLoggingIn = false;
        }
      });
    } else {
      this.isPasswordVisible = true;
    }
  }

  loginWithGoogle() {
    if (this.isGoogleLoginInProgress) return; // Prevent multiple clicks

    this.isGoogleLoginInProgress = true;

    // Show loading state for a short time before redirect
    setTimeout(() => {
      // Save intended destination before redirect
      localStorage.setItem('oauth_redirect', window.location.href);
      // Store the login modal state to know if we should close it after successful login
      localStorage.setItem('login_modal_open', 'true');
      this.authService.oauthLogin('google');
    }, 500); // Short delay to show loading indicator
  }

  closeModal() {
    // Emit event to parent component to close the modal
    this.closeModalEvent.emit();
    // No need to navigate, just letting the parent component know to hide the modal
  }
}
