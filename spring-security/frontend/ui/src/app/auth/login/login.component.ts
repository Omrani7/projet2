import { Component, EventEmitter, Output, OnInit } from '@angular/core';
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
export class LoginComponent implements OnInit {
  @Output() closeModalEvent = new EventEmitter<void>();

  email: string = '';
  password: string = '';
  isPasswordVisible: boolean = false;
  passwordStep: boolean = false; // To control the password step visibility
  errorMessage: string = '';
  isLoggingIn: boolean = false;
  isGoogleLoginInProgress: boolean = false;

  constructor(
    private router: Router,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    // Check if user is already logged in - this could be happening if
    // user lands on login page while already authenticated
    if (this.authService.isLoggedIn()) {
      console.log('LoginComponent: User is already logged in, determining redirect based on role');
      
      const userRole = this.authService.getUserRole();
      const originalPage = localStorage.getItem('original_page');
      
      // If there's an original page stored, navigate there
      if (originalPage && originalPage !== '/') {
        localStorage.removeItem('original_page');
        this.router.navigate([originalPage]);
      } else {
        // Otherwise, redirect based on role
        switch (userRole) {
          case 'STUDENT':
            this.router.navigate(['/student/dashboard']);
            break;
          case 'OWNER':
            this.router.navigate(['/owner/dashboard']);
            break;
          case 'ADMIN':
            this.router.navigate(['/admin/dashboard']);
            break;
          default:
            // Go back in history if possible
            if (window.history.length > 1) {
              window.history.back();
            } else {
              this.router.navigate(['/']);
            }
            break;
        }
      }
    }
  }

  continueWithEmail() {
    this.errorMessage = '';

    if (this.passwordStep) {
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
          
          // Get user role and redirect accordingly
          const userRole = this.authService.getUserRole();
          const originalPage = localStorage.getItem('original_page');
          
          // If there's an original page stored, navigate there
          if (originalPage && originalPage !== '/') {
            localStorage.removeItem('original_page');
            this.router.navigate([originalPage]);
          } else {
            // Otherwise, redirect based on role
            switch (userRole) {
              case 'STUDENT':
                this.router.navigate(['/student/dashboard']);
                break;
              case 'OWNER':
                this.router.navigate(['/owner/dashboard']);
                break;
              case 'ADMIN':
                this.router.navigate(['/admin/dashboard']);
                break;
              default:
                this.router.navigate(['/']);
                break;
            }
          }
        },
        error: (error) => {
          console.error('Login failed', error);
          this.errorMessage = error.message || 'Login failed. Please try again.';
          this.isLoggingIn = false;
        }
      });
    } else {
      // First step - move to password entry
      if (!this.email) {
        this.errorMessage = 'Please enter your email address.';
        return;
      }
      this.passwordStep = true;
    }
  }

  togglePasswordVisibility() {
    this.isPasswordVisible = !this.isPasswordVisible;
    }

  signInWithGoogle() {
    if (this.isGoogleLoginInProgress) return; // Prevent multiple clicks

    this.isGoogleLoginInProgress = true;

    // Get the original page if set by FilterBarComponent
    const originalPage = localStorage.getItem('original_page');
    console.log('LoginComponent: Original page before Google login:', originalPage);

    // Show loading state for a short time before redirect
    setTimeout(() => {
      // For OAuth flow tracking, set the original page or fallback to home
      localStorage.setItem('oauth_redirect', originalPage || '/');
      
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

  goToRegister() {
    this.router.navigate(['/auth/register']);
    this.closeModalEvent.emit();
  }
}
