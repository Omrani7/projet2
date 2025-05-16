import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../auth.service';
import { HttpClient } from '@angular/common/http';

@Component({
  selector: 'app-oauth2-callback',
  template: `
    <div class="oauth-callback">
      <div *ngIf="loading && !showRoleSelection" class="spinner-border" role="status">
        <span class="visually-hidden">Loading...</span>
      </div>
      <div *ngIf="error" class="error-message">
        <p>Authentication failed: {{errorMessage}}</p>
        <button (click)="retry()" class="retry-button">Try Again</button>
      </div>
      <p *ngIf="loading && !showRoleSelection">Processing authentication, please wait...</p>
      <p *ngIf="!loading && !error && !showRoleSelection">Authentication successful! Redirecting...</p>

      <!-- Role selection dialog for new users -->
      <div *ngIf="showRoleSelection" class="role-selection-container">
        <h2>Welcome! Please select your role:</h2>
        <p>Choose the role that best describes how you'll use our platform</p>

        <div class="role-options">
          <div class="role-option" [class.selected]="selectedRole === 'STUDENT'" (click)="selectRole('STUDENT')">
            <h3>Student</h3>
            <p>Looking for rental opportunities</p>
          </div>

          <div class="role-option" [class.selected]="selectedRole === 'OWNER'" (click)="selectRole('OWNER')">
            <h3>Owner</h3>
            <p>Publishing rental listings</p>
          </div>
        </div>

        <button [disabled]="!selectedRole || updatingRole"
                (click)="confirmRole()"
                class="confirm-button">
          <span *ngIf="updatingRole" class="spinner-border spinner-border-sm me-2" role="status"></span>
          Confirm Selection
        </button>
      </div>

      <!-- Debug info -->
      <div *ngIf="debug" class="debug-info">
        <h3>Debug Information</h3>
        <p>Token: {{token || 'No token found'}}</p>
        <p>Error: {{errorMessage || 'No error'}}</p>
        <p>New User: {{isNewUser}}</p>
        <p>Selected Role: {{selectedRole}}</p>
        <button (click)="testDirectRedirect()">Test Direct Redirect</button>
      </div>
    </div>
  `,
  styles: [`
    .oauth-callback {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      height: 100vh;
      text-align: center;
    }
    .spinner-border {
      width: 3rem;
      height: 3rem;
      margin-bottom: 1rem;
    }
    .error-message {
      color: red;
      margin-bottom: 1rem;
    }
    .retry-button, .confirm-button {
      padding: 0.5rem 1rem;
      background-color: #007bff;
      color: white;
      border: none;
      border-radius: 4px;
      cursor: pointer;
    }
    .confirm-button:disabled {
      background-color: #6c757d;
      cursor: not-allowed;
    }
    .debug-info {
      margin-top: 2rem;
      padding: 1rem;
      border: 1px solid #ddd;
      border-radius: 4px;
      text-align: left;
    }
    .role-selection-container {
      max-width: 600px;
      padding: 2rem;
      border-radius: 8px;
      box-shadow: 0 4px 6px rgba(0,0,0,0.1);
      background-color: white;
    }
    .role-options {
      display: flex;
      gap: 1rem;
      margin: 2rem 0;
      justify-content: center;
    }
    .role-option {
      flex: 1;
      padding: 1.5rem;
      border: 2px solid #eee;
      border-radius: 8px;
      cursor: pointer;
      transition: all 0.2s ease;
    }
    .role-option:hover {
      border-color: #007bff;
      transform: translateY(-5px);
    }
    .role-option.selected {
      border-color: #007bff;
      background-color: #f0f9ff;
    }
  `]
})
export class OAuth2CallbackComponent implements OnInit {
  loading = true;
  error = false;
  errorMessage = '';
  token = '';
  debug = false; // Set to true for debugging

  // Role selection fields
  isNewUser = false;
  showRoleSelection = false;
  selectedRole: string | null = null;
  userEmail: string | null = null;
  userId: number | null = null;
  updatingRole = false;

  constructor(
    private authService: AuthService,
    private router: Router,
    private route: ActivatedRoute,
    private http: HttpClient
  ) { }

  ngOnInit(): void {
    // Extract token and other parameters from URL
    this.route.queryParams.subscribe(params => {
      console.log('OAuth2 callback params:', params);
      const token = params['token'];
      const error = params['error'];
      const message = params['message'];
      const isNewUser = params['newUser'] === 'true';

      this.isNewUser = isNewUser;

      if (token) {
        // Save token temporarily (we might need to update it after role selection)
        this.token = token;

        // For new users, show role selection before proceeding
        if (isNewUser) {
          console.log('New OAuth2 user, showing role selection');
          this.loading = false;
          this.showRoleSelection = true;

          // Get the user's email from the token
          this.getUserInfoFromToken();

        } else {
          // For existing users, proceed normally
          this.completeAuthentication(token);
        }
      } else if (error) {
        console.error('OAuth2 callback error:', message);
        this.error = true;
        this.errorMessage = message || 'Authentication failed';
        this.loading = false;

        // Remove login modal flag if present
        localStorage.removeItem('login_modal_open');
      } else {
        // Check if we're coming from the OAuth2 flow but without params
        console.log('No token or error parameters found');
        this.error = true;
        this.errorMessage = 'No authentication data received';
        this.loading = false;

        // Remove login modal flag if present
        localStorage.removeItem('login_modal_open');
      }
    });
  }

  getUserInfoFromToken(): void {
    try {
      const tokenData = this.authService.decodeToken(this.token);
      this.userEmail = tokenData.sub;
      this.userId = tokenData.id;
      console.log('User email:', this.userEmail, 'User ID:', this.userId);
      if (!this.userId) {
        console.error('User ID not found in token!');
      }
    } catch(e) {
      console.error('Error decoding token:', e);
    }
  }

  selectRole(role: string): void {
    this.selectedRole = role;
  }

  confirmRole(): void {
    if (!this.selectedRole || !this.userId) {
      console.error('Role or User ID is missing for update.');
      return;
    }

    this.updatingRole = true;

    // Call the backend to update the user's role using userId
    this.http.post('/oauth2/update-role', {
      userId: this.userId,
      role: this.selectedRole
    }).subscribe({
      next: (response: any) => {
        console.log('Role updated successfully:', response);
        this.updatingRole = false;

        // Update the token with the new one that includes the updated role
        if (response.token) {
          this.completeAuthentication(response.token);
        } else {
          // Fallback? Or handle error? Should always get a token.
          console.error('No new token received after role update!');
          this.completeAuthentication(this.token); // Use old token as fallback (might have wrong role)
        }
      },
      error: (error) => {
        console.error('Error updating role:', error);
        this.updatingRole = false;
        this.error = true;
        // Use the actual error message from the backend if available
        this.errorMessage = error.error?.message || error.error || 'Failed to update role';
      }
    });
  }

  completeAuthentication(token: string): void {
    // Save the token
    this.authService.saveToken(token);
    console.log('OAuth2 login successful');

    // Check if we should close a login modal
    const loginModalOpen = localStorage.getItem('login_modal_open');
    if (loginModalOpen === 'true') {
      // Remove the flag
      localStorage.removeItem('login_modal_open');
    }

    this.loading = false;
    this.showRoleSelection = false;

    // Redirect to home or saved redirect URL
    setTimeout(() => {
      const redirectUrl = localStorage.getItem('oauth_redirect') || '/';
      localStorage.removeItem('oauth_redirect');
      // Remove hash and query parameters if present in redirect URL
      const cleanRedirectUrl = redirectUrl.split('#')[0].split('?')[0];
      this.router.navigateByUrl(cleanRedirectUrl || '/');
    }, 500); // Small delay to show success message
  }

  retry(): void {
    this.router.navigate(['/auth/login']);
  }

  testDirectRedirect(): void {
    // This function tests if redirects work correctly from backend to frontend
    window.location.href = 'http://localhost:8080/test/oauth2-redirect-test';
  }
}
