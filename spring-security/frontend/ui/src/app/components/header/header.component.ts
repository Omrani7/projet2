import { Component, inject, HostListener, ElementRef, Renderer2, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { AuthService } from '../../auth/auth.service';
import { ConnectionRequestService } from '../../services/connection-request.service';
import { MessagingService } from '../../services/messaging.service';
import { Observable, Subscription } from 'rxjs';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule
  ],
  templateUrl: './header.component.html',
  styleUrls: ['./header.component.css']
})
export class HeaderComponent implements OnDestroy {
  private authService = inject(AuthService);
  private connectionRequestService = inject(ConnectionRequestService);
  private messagingService = inject(MessagingService);
  private renderer = inject(Renderer2);
  private el = inject(ElementRef);
  private router = inject(Router);

  isAuthenticated$: Observable<boolean>;
  isDropdownOpen = false;
  isScrolled = false;
  private clickListener!: () => void;
  private authSubscription: Subscription;
  private pendingCountSubscription?: Subscription;
  private unreadMessageSubscription?: Subscription;
  currentUserRole: string | null = null;
  pendingConnectionRequests = 0;
  unreadMessageCount = 0;

  constructor() {
    this.isAuthenticated$ = this.authService.authState$;
    this.authSubscription = this.isAuthenticated$.subscribe(isAuth => {
      if (isAuth) {
        this.currentUserRole = this.authService.getUserRole();
        // Load pending connection requests count for students
        if (this.currentUserRole === 'STUDENT') {
          this.loadPendingConnectionRequests();
          this.loadUnreadMessageCount();
        }
      } else {
        this.isDropdownOpen = false;
        this.currentUserRole = null;
        this.pendingConnectionRequests = 0;
        this.unreadMessageCount = 0;
      }
    });
  }

  @HostListener('window:scroll', [])
  onWindowScroll(): void {
    const scrollOffset = window.pageYOffset || document.documentElement.scrollTop || document.body.scrollTop || 0;
    this.isScrolled = scrollOffset > 50;
  }

  ngOnDestroy(): void {
    if (this.authSubscription) {
      this.authSubscription.unsubscribe();
    }
    if (this.pendingCountSubscription) {
      this.pendingCountSubscription.unsubscribe();
    }
    if (this.unreadMessageSubscription) {
      this.unreadMessageSubscription.unsubscribe();
    }
    this.removeClickListener();
  }

  /**
   * Load pending connection requests count for students
   */
  private loadPendingConnectionRequests(): void {
    this.pendingCountSubscription = this.connectionRequestService.getPendingRequestsCount().subscribe({
      next: (response) => {
        this.pendingConnectionRequests = response.pendingCount;
      },
      error: (error) => {
        console.error('Error loading pending connection requests count:', error);
        this.pendingConnectionRequests = 0;
      }
    });
  }

  /**
   * Load unread message count for students
   */
  private loadUnreadMessageCount(): void {
    this.unreadMessageSubscription = this.messagingService.getUnreadMessageCount().subscribe({
      next: (response) => {
        this.unreadMessageCount = response.unreadCount;
      },
      error: (error) => {
        console.error('Error loading unread message count:', error);
        this.unreadMessageCount = 0;
      }
    });
  }

  toggleDropdown(): void {
    this.isDropdownOpen = !this.isDropdownOpen;
    if (this.isDropdownOpen) {
      this.addClickListener();
    } else {
      this.removeClickListener();
    }
  }

  private addClickListener(): void {
    this.removeClickListener();
    
    setTimeout(() => {
      this.clickListener = this.renderer.listen('document', 'click', (event) => {
        if (event.defaultPrevented) {
          return;
        }
        
        if (!this.el.nativeElement.contains(event.target)) {
          this.isDropdownOpen = false;
          this.removeClickListener();
        }
      });
    }, 10);
  }

  private removeClickListener(): void {
    if (this.clickListener) {
      this.clickListener();
    }
  }

  logout(): void {
    this.isDropdownOpen = false;
    this.removeClickListener();
    this.authService.logout().subscribe({
      next: () => console.log('Logout successful'),
      error: (err) => console.error('Logout failed:', err)
    });
  }

  navigateToLogin(): void {
    // Store the current URL
    const currentUrl = this.router.url;
    console.log('Header: Storing original page:', currentUrl);
    
    // Save original page for redirect after login
    localStorage.setItem('original_page', currentUrl);
    
    // Set flag indicating this is a user-initiated login (not browser back button)
    localStorage.setItem('user_initiated_login', 'true');
    
    // Navigate to login page
    this.router.navigate(['/auth/login']);
  }

  navigateToRoommateLogin(): void {
    // Store that user wants to access roommates feature after login
    console.log('Header: User clicked Find Roommates - redirecting to login');
    
    // Save the roommates page as destination after login
    localStorage.setItem('original_page', '/roommates/browse');
    
    // Set flag indicating this is a user-initiated login for roommate feature
    localStorage.setItem('user_initiated_login', 'true');
    localStorage.setItem('roommate_feature_requested', 'true');
    
    // Navigate to login page
    this.router.navigate(['/auth/login']);
  }
}
