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
    // Check if user is authenticated - use both AuthService state and direct token check
    const isLoggedInViaService = this.authService.isLoggedIn();
    const hasTokenInStorage = !!this.authService.getToken();
    
    console.log('StudentGuard: Checking authentication for route:', state.url);
    console.log('StudentGuard: isLoggedIn() result:', isLoggedInViaService);
    console.log('StudentGuard: hasTokenInStorage:', hasTokenInStorage);
    
    // If not logged in via either method, redirect to login
    if (!isLoggedInViaService && !hasTokenInStorage) {
      console.log('StudentGuard: User not authenticated, redirecting to login');
      // Store the attempted URL for redirect after login
      localStorage.setItem('original_page', state.url);
      this.router.navigate(['/auth/login']);
      return false;
    }
    
    // If we have token but AuthService state is inconsistent, fix it
    if (!isLoggedInViaService && hasTokenInStorage) {
      console.log('StudentGuard: Fixing inconsistent auth state - token exists but service says not logged in');
      // Force update the auth service state
      this.authService.saveToken(this.authService.getToken()!);
    }
    
    // Get current user's role from token
    const userRole = this.authService.getUserRole();
    console.log('StudentGuard: User role:', userRole);
    
    // Only allow STUDENT role
    if (userRole === 'STUDENT') {
      console.log('StudentGuard: Student access granted');
      return true;
    }
    
    // For OWNER and ADMIN, redirect to appropriate dashboard
    if (userRole === 'OWNER') {
      console.log('StudentGuard: Owner detected, redirecting to owner dashboard');
      this.router.navigate(['/owner/dashboard']);
      return false;
    } else if (userRole === 'ADMIN') {
      console.log('StudentGuard: Admin detected, redirecting to admin dashboard');
      this.router.navigate(['/admin/dashboard']);
      return false;
    }
    
    // If we have a token but no valid role, there might be a token issue
    if (hasTokenInStorage) {
      console.log('StudentGuard: Token exists but no valid role found, clearing invalid token');
      this.authService.clearToken();
      localStorage.setItem('original_page', state.url);
      this.router.navigate(['/auth/login']);
      return false;
    }
    
    // Fallback: redirect to login instead of home to preserve user intent
    console.log('StudentGuard: Fallback case, redirecting to login');
    localStorage.setItem('original_page', state.url);
    this.router.navigate(['/auth/login']);
    return false;
  }
} 