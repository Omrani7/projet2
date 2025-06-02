import { Injectable } from '@angular/core';
import { CanActivate, ActivatedRouteSnapshot, RouterStateSnapshot, Router } from '@angular/router';
import { Observable, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { UserProfileService } from '../services/user-profile.service';
import { AuthService } from './auth.service';

@Injectable({
  providedIn: 'root'
})
export class OwnerProfileGuard implements CanActivate {
  
  constructor(
    private userProfileService: UserProfileService,
    private authService: AuthService,
    private router: Router
  ) {}
  
  canActivate(
    route: ActivatedRouteSnapshot,
    state: RouterStateSnapshot
  ): Observable<boolean> | Promise<boolean> | boolean {
    // Get current user's role from token
    const userRole = this.authService.getUserRole();
    
    // If not an owner, allow access (this guard should only be applied to owner routes)
    if (userRole !== 'OWNER') {
      return true;
    }
    
    // Get the current user ID
    const userId = this.userProfileService.getCurrentUserId();
    if (!userId) {
      // If no user ID, redirect to login
      this.router.navigate(['/auth/login']);
      return false;
    }
    
    // Check if the owner has completed their profile
    return this.userProfileService.getOwnerProfile(userId).pipe(
      map(profile => {
        // Check if profile has all required fields
        const isProfileComplete = !!(
          profile &&
          profile.fullName &&
          profile.contactNumber &&
          profile.state &&
          profile.accommodationType
        );
        
        if (!isProfileComplete) {
          // Redirect to profile setup page
          this.router.navigate(['/owner/profile-setup']);
          return false;
        }
        
        return true;
      }),
      catchError(error => {
        // If profile doesn't exist or other error, redirect to profile setup
        if (error.status === 404) {
          this.router.navigate(['/owner/profile-setup']);
        } else {
          console.error('Error checking owner profile:', error);
          // For other errors, allow navigation to continue
        }
        return of(false);
      })
    );
  }
} 