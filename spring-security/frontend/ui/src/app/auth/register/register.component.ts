import { Component, EventEmitter, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../auth.service';
import { HttpErrorResponse } from '@angular/common/http';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './register.component.html',
  styleUrl: './register.component.css'
})
export class RegisterComponent {
  @Output() closeModalEvent = new EventEmitter<void>();

  email: string = '';
  username: string = '';
  password: string = '';
  phoneNumber: string = '';
  errorMessage: string = '';
  selectedRole: string = 'STUDENT'; // Default role
  isRegistering: boolean = false; // Added for loading state
  isGoogleLoginInProgress: boolean = false; // Added for Google Sign-up loading state
  isPasswordVisible: boolean = false; // To toggle password visibility

  // Regex for basic email format validation
  private emailPattern = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/;
  // Regex for password complexity (mirroring backend/HTML)
  private passwordPattern = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).{8,}$/;

  constructor(
    private router: Router,
    private authService: AuthService
  ) {}

  register() {
    this.errorMessage = '';
    this.isRegistering = true; // Start loading

    // --- Enhanced Client-Side Validation ---

    // 1. Basic required field check
    if (!this.email || !this.username || !this.password) {
      this.errorMessage = 'Please fill in all required fields (Email, Username, Password).';
      this.isRegistering = false; // Stop loading
      return; // Stop execution
    }

    // 2. Email format check
    if (!this.emailPattern.test(this.email)) {
      this.errorMessage = 'Please enter a valid email address.';
      this.isRegistering = false; // Stop loading
      return; // Stop execution
    }

    // 3. Password complexity check
    if (!this.passwordPattern.test(this.password)) {
      this.errorMessage = 'Password must be at least 8 characters long and contain at least one uppercase letter, one lowercase letter, and one number.';
      this.isRegistering = false; // Stop loading
      return; // Stop execution
    }

    // --- Validation Passed - Proceed with API Call ---

    const userData = {
      email: this.email,
      username: this.username,
      password: this.password,
      phoneNumber: this.phoneNumber,
      role: this.selectedRole,
      profileDetails: {
        fullName: this.username, // Using username as fullName for now
        fieldOfStudy: '',
        institute: '',
        userType: this.selectedRole
      }
    };

    this.authService.register(userData).subscribe({
      next: (response) => {
        console.log('Registration successful', response);
        this.isRegistering = false; // Stop loading
        this.router.navigate(['/']);
      },
      error: (error: HttpErrorResponse) => {
        console.error('Registration failed', error);
        if (error.error && typeof error.error === 'object' && error.error.message) {
            this.errorMessage = error.error.message;
        } else if (error.error && typeof error.error === 'string') {
            try {
              const parsedError = JSON.parse(error.error);
              this.errorMessage = parsedError.message || 'Registration failed. Please check your input.';
            } catch (e) {
              this.errorMessage = error.error;
            }
        } else {
            this.errorMessage = 'There is an account with this email address';
        }
        this.isRegistering = false; // Stop loading
      }
    });
  }

  togglePasswordVisibility() {
    this.isPasswordVisible = !this.isPasswordVisible;
  }

  selectRole(role: string) {
    if (role === 'PROPERTY_OWNER') {
      this.selectedRole = 'OWNER';
    } else {
    this.selectedRole = role;
    }
  }

  registerWithGoogle() {
    this.isGoogleLoginInProgress = true; // Start Google loading
    this.errorMessage = ''; // Clear previous errors
    // Assuming oauthLogin might handle errors or navigate away.
    // If it has callbacks/promises, set isGoogleLoginInProgress = false there.
    // For now, we'll assume navigation or another component handles the end state.
    // A more robust implementation might use finalize() on the observable if AuthService returns one.
    this.authService.oauthLogin('google');
    // Note: If oauthLogin fails immediately or doesn't navigate,
    // isGoogleLoginInProgress might remain true. Consider adding error handling
    // to oauthLogin or its caller if necessary to reset the flag.
  }

  closeModal() {
    // Check if this component is being used as a modal (has observers) or as a routed page
    if (this.closeModalEvent.observers.length > 0) {
      // Used as modal - emit event to parent component to close the modal
      this.closeModalEvent.emit();
    } else {
      // Used as routed page - navigate back or to home
      const originalPage = localStorage.getItem('original_page');
      
      if (originalPage && originalPage !== '/auth/register') {
        // Navigate back to the original page
        localStorage.removeItem('original_page');
        this.router.navigate([originalPage]);
      } else if (window.history.length > 1) {
        // Go back in browser history
        window.history.back();
      } else {
        // Fallback to home page
        this.router.navigate(['/']);
      }
    }
  }

  goToLogin() {
    this.router.navigate(['/auth/login']);
  }
}
