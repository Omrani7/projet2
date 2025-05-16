import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { LoginComponent } from '../auth/login/login.component';
import { AuthService } from '../auth/auth.service';
import { Subscription } from 'rxjs';
import { AuthStatusComponent } from '../auth/auth-status/auth-status.component';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, LoginComponent, AuthStatusComponent],
  templateUrl: './home.component.html',
  styleUrl: './home.component.css'
})
export class HomeComponent implements OnInit, OnDestroy {
  isLoginModalOpen: boolean = false;
  isUserLoggedIn: boolean = false;
  private authSubscription: Subscription | null = null;
  logoutInProgress: boolean = false;

  constructor(
    private router: Router,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    // Subscribe to auth state changes
    this.authSubscription = this.authService.authState$.subscribe(
      isLoggedIn => {
        console.log('Auth state changed:', isLoggedIn);
        this.isUserLoggedIn = isLoggedIn;

        // If user just logged in and login modal is open, close it
        if (isLoggedIn && this.isLoginModalOpen) {
          this.closeLoginModal();
        }
      }
    );

    // Set initial state
    this.isUserLoggedIn = this.authService.isLoggedIn();
  }

  ngOnDestroy(): void {
    // Clean up subscription
    if (this.authSubscription) {
      this.authSubscription.unsubscribe();
    }
  }

  login() {
    this.isLoginModalOpen = true;
  }

  logout() {
    if (this.logoutInProgress) return;

    this.logoutInProgress = true;
    this.authService.logout().subscribe({
      next: () => {
        console.log('Logout successful');
        this.logoutInProgress = false;
      },
      error: (err) => {
        console.error('Logout error:', err);
        this.logoutInProgress = false;
      }
    });
  }

  isLoggedIn(): boolean {
    return this.isUserLoggedIn;
  }

  closeLoginModal() {
    this.isLoginModalOpen = false;
  }
}
