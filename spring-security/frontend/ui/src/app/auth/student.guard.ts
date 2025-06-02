import { Injectable } from '@angular/core';
import { CanActivate, ActivatedRouteSnapshot, RouterStateSnapshot, Router } from '@angular/router';
import { AuthService } from './auth.service';

@Injectable({
  providedIn: 'root'
})
export class StudentGuard implements CanActivate {
  
  constructor(
    private authService: AuthService,
    private router: Router
  ) {}
  
  canActivate(
    route: ActivatedRouteSnapshot,
    state: RouterStateSnapshot
  ): boolean {
    // Check if user is authenticated
    if (!this.authService.isLoggedIn()) {
      // Not logged in, redirect to login
      this.router.navigate(['/auth/login']);
      return false;
    }
    
    // Get current user's role from token
    const userRole = this.authService.getUserRole();
    
    // Only allow STUDENT role
    if (userRole === 'STUDENT') {
      return true;
    }
    
    // For OWNER and ADMIN, redirect to appropriate dashboard
    if (userRole === 'OWNER') {
      this.router.navigate(['/owner/dashboard']);
      return false;
    } else if (userRole === 'ADMIN') {
      this.router.navigate(['/admin/dashboard']);
      return false;
    }
    
    // Fallback: redirect to home
    this.router.navigate(['/']);
    return false;
  }
} 