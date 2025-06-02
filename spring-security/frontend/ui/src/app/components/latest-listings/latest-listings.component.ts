import { Component, ElementRef, OnInit, ViewChild, AfterViewInit, OnDestroy, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { PropertyCardComponent } from '../property-card/property-card.component';
import { Property } from '../../models/property.model';
import { PropertyServiceService } from '../../services/property.service.service';
import { HttpClientModule, HttpErrorResponse } from '@angular/common/http';
import { catchError, finalize } from 'rxjs/operators';
import { of } from 'rxjs';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-latest-listings',
  standalone: true,
  imports: [CommonModule, PropertyCardComponent, HttpClientModule, RouterModule],
  templateUrl: './latest-listings.component.html',
  styleUrl: './latest-listings.component.css'
})
export class LatestListingsComponent implements OnInit, AfterViewInit, OnDestroy {
  @ViewChild('scrollContainer') scrollContainer!: ElementRef;
  latestProperties: Property[] = [];
  isLoading: boolean = true;
  error: string | null = null;
  showLeftArrow: boolean = false;
  showRightArrow: boolean = true;
  currentMobileScrollIndex: number = 0;
  scrollObserver: IntersectionObserver | null = null;
  scrollListener: any;

  constructor(private propertyService: PropertyServiceService) {}

  ngOnInit(): void {
    this.fetchLatestProperties();
  }

  ngAfterViewInit(): void {
    this.updateScrollArrows();
    
    // Set up intersection observer for mobile scroll indicators
    if (this.latestProperties.length > 0 && window.innerWidth < 768) {
      this.setupScrollObserver();
    }
    
    // Set up scroll listener for mobile
    this.setupScrollListener();
  }
  
  ngOnDestroy(): void {
    // Clean up observers and listeners
    if (this.scrollObserver) {
      this.scrollObserver.disconnect();
    }
    
    if (this.scrollListener) {
      this.scrollContainer.nativeElement.removeEventListener('scroll', this.scrollListener);
    }
  }
  
  @HostListener('window:resize')
  onResize() {
    this.updateScrollArrows();
    
    // Re-setup scroll observer on window resize
    if (this.scrollObserver) {
      this.scrollObserver.disconnect();
    }
    
    if (this.latestProperties.length > 0 && window.innerWidth < 768) {
      this.setupScrollObserver();
    }
  }

  fetchLatestProperties(): void {
    this.isLoading = true;
    
    // Use the PropertyService to get the latest properties from all sources
    this.propertyService.getLatestOwnerProperties(6)
      .pipe(
        catchError(error => {
          console.error('Error fetching latest properties:', error);
          this.error = 'Failed to load latest properties. Please try again later.';
          return of([]);
        }),
        finalize(() => {
          this.isLoading = false;
        })
      )
      .subscribe(properties => {
        this.latestProperties = properties;
        // Initialize scroll arrows based on content
        this.updateScrollArrows();
        
        // Setup intersection observer after properties are loaded
        setTimeout(() => {
          if (this.latestProperties.length > 0 && window.innerWidth < 768) {
            this.setupScrollObserver();
          }
        }, 500);
      });
  }

  updateScrollArrows(): void {
    if (!this.scrollContainer) return;
    
    const element = this.scrollContainer.nativeElement;
    
    // Check if scroll is possible
    const canScroll = element.scrollWidth > element.clientWidth;
    
    // Show right arrow only if there's more content to scroll to
    this.showRightArrow = canScroll && element.scrollLeft < (element.scrollWidth - element.clientWidth - 10);
    
    // Show left arrow only if we've scrolled to the right
    this.showLeftArrow = canScroll && element.scrollLeft > 10;
  }

  scrollLeft(): void {
    if (!this.scrollContainer) return;
    
    const element = this.scrollContainer.nativeElement;
    const scrollAmount = element.clientWidth * 0.8; // Scroll 80% of the visible width
    
    element.scrollBy({
      left: -scrollAmount,
      behavior: 'smooth'
    });
    
    // Update arrows after scroll animation completes
    setTimeout(() => this.updateScrollArrows(), 500);
  }

  scrollRight(): void {
    if (!this.scrollContainer) return;
    
    const element = this.scrollContainer.nativeElement;
    const scrollAmount = element.clientWidth * 0.8; // Scroll 80% of the visible width
    
    element.scrollBy({
      left: scrollAmount,
      behavior: 'smooth'
    });
    
    // Update arrows after scroll animation completes
    setTimeout(() => this.updateScrollArrows(), 500);
  }
  
  // Set up intersection observer for mobile scroll indicators
  setupScrollObserver(): void {
    if (!this.scrollContainer) return;
    
    const options = {
      root: this.scrollContainer.nativeElement,
      threshold: 0.6 // Consider an element "visible" when 60% is in view
    };
    
    this.scrollObserver = new IntersectionObserver((entries) => {
      entries.forEach(entry => {
        if (entry.isIntersecting) {
          // Find the index of the property card that's currently in view
          const element = entry.target as HTMLElement;
          const index = Array.from(this.scrollContainer.nativeElement.children).indexOf(element);
          if (index >= 0) {
            this.currentMobileScrollIndex = index;
          }
        }
      });
    }, options);
    
    // Observe all property cards in the scroll container
    setTimeout(() => {
      if (this.scrollContainer && this.scrollContainer.nativeElement) {
        const cards = this.scrollContainer.nativeElement.children;
        for (let i = 0; i < cards.length; i++) {
          this.scrollObserver?.observe(cards[i]);
        }
      }
    }, 100);
  }
  
  // Set up scroll listener for scroll arrows update
  setupScrollListener(): void {
    if (!this.scrollContainer) return;
    
    this.scrollListener = () => {
      this.updateScrollArrows();
    };
    
    this.scrollContainer.nativeElement.addEventListener('scroll', this.scrollListener);
  }
}
