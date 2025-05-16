import { Component, inject, HostListener, ElementRef, Renderer2, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { AuthService } from '../../auth/auth.service';
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
  private renderer = inject(Renderer2);
  private el = inject(ElementRef);

  isAuthenticated$: Observable<boolean>;
  isDropdownOpen = false;
  isScrolled = false;
  private clickListener!: () => void;
  private authSubscription: Subscription;

  constructor() {
    this.isAuthenticated$ = this.authService.authState$;
    this.authSubscription = this.isAuthenticated$.subscribe(isAuth => {
      if (!isAuth) {
        this.isDropdownOpen = false;
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
    this.removeClickListener();
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
    setTimeout(() => {
      this.clickListener = this.renderer.listen('document', 'click', (event) => {
        if (!this.el.nativeElement.contains(event.target)) {
          this.isDropdownOpen = false;
          this.removeClickListener();
        }
      });
    }, 0);
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
}
