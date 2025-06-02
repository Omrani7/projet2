import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { AuthService } from '../../auth/auth.service';
import { Observable } from 'rxjs';

@Component({
  selector: 'app-hero-section',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './hero-section.component.html',
  styleUrls: ['./hero-section.component.css']
})
export class HeroSectionComponent {
  isAuthenticated$: Observable<boolean>;
  currentUserRole: string | null = null;

  constructor(
    private router: Router,
    private authService: AuthService
  ) {
    this.isAuthenticated$ = this.authService.authState$;
    
    // Subscribe to auth state to get user role
    this.authService.authState$.subscribe(isAuth => {
      if (isAuth) {
        this.currentUserRole = this.authService.getUserRole();
      } else {
        this.currentUserRole = null;
      }
    });
  }

  navigateToDiscovery(): void {
    // Check if user is authenticated
    if (!this.authService.isLoggedIn()) {
      // Store discovery as the intended destination
      localStorage.setItem('original_page', '/discovery');
      localStorage.setItem('user_initiated_login', 'true');
      this.router.navigate(['/auth/login']);
      return;
    }

    // Check user role
    const userRole = this.authService.getUserRole();
    
    if (userRole === 'STUDENT') {
      // Students can access discovery
      this.router.navigate(['/discovery']);
    } else if (userRole === 'OWNER') {
      // Redirect owners to their dashboard
      this.router.navigate(['/owner/dashboard']);
    } else if (userRole === 'ADMIN') {
      // Redirect admins to their dashboard
      this.router.navigate(['/admin/dashboard']);
    } else {
      // Fallback: redirect to login
      this.router.navigate(['/auth/login']);
    }
  }

  /**
   * Check if the discovery button should be visible and enabled
   */
  shouldShowDiscoveryButton(): boolean {
    // Show the button if not authenticated or if user is a student
    return !this.authService.isLoggedIn() || this.currentUserRole === 'STUDENT';
  }

  /**
   * Get the appropriate button text based on authentication and role
   */
  getDiscoveryButtonText(): string {
    if (!this.authService.isLoggedIn()) {
      return 'Discover Properties';
    }
    
    switch (this.currentUserRole) {
      case 'STUDENT':
        return 'Discover Properties';
      case 'OWNER':
        return 'Go to Dashboard';
      case 'ADMIN':
        return 'Go to Admin Dashboard';
      default:
        return 'Discover Properties';
    }
  }
}
