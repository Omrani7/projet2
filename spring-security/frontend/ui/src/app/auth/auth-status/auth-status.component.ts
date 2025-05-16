import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AuthService } from '../auth.service';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-auth-status',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="auth-status text-sm">
      <div *ngIf="isAuthenticated" class="inline-flex items-center text-green-600">
        <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4 mr-1" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
        </svg>
        <span>Authenticated</span>
      </div>
      <div *ngIf="!isAuthenticated" class="inline-flex items-center text-gray-500">
        <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4 mr-1" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M18.364 18.364A9 9 0 005.636 5.636m12.728 12.728A9 9 0 015.636 5.636m12.728 12.728L5.636 5.636" />
        </svg>
        <span>Not authenticated</span>
      </div>
    </div>
  `,
  styles: [`
    .auth-status {
      padding: 0.25rem 0.5rem;
      border-radius: 0.25rem;
    }
  `]
})
export class AuthStatusComponent implements OnInit, OnDestroy {
  isAuthenticated = false;
  private authSubscription: Subscription | null = null;

  constructor(private authService: AuthService) {}

  ngOnInit(): void {
    // Initialize state
    this.isAuthenticated = this.authService.isLoggedIn();

    // Subscribe to auth state changes
    this.authSubscription = this.authService.authState$.subscribe(
      state => {
        this.isAuthenticated = state;
      }
    );
  }

  ngOnDestroy(): void {
    if (this.authSubscription) {
      this.authSubscription.unsubscribe();
    }
  }
}
