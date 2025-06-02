import { Component, Input, Output, EventEmitter, OnChanges, SimpleChanges, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { PropertyListingDTO } from '../../models/property-listing.dto';
import { Page } from '../../models/page.model';
import { HttpClient } from '@angular/common/http';
import { DomSanitizer, SafeUrl } from '@angular/platform-browser';
import { UserProfileService } from '../../services/user-profile.service';
import { AuthService } from '../../auth/auth.service';
import { Router } from '@angular/router';
import { InquiryFormComponent } from '../inquiry-form/inquiry-form.component';

@Component({
  selector: 'app-property-list',
  standalone: true,
  imports: [CommonModule, InquiryFormComponent],
  templateUrl: './property-list.component.html',
  styleUrls: ['./property-list.component.css']
})
export class PropertyListComponent implements OnChanges, OnInit, OnDestroy {
  @Input() propertiesPage: Page<PropertyListingDTO> | null = null;
  @Input() isLoading: boolean = false;
  @Input() errorMessage: string | null = null;
  @Input() selectedPropertyId: number | null = null;

  @Output() pageChanged = new EventEmitter<number>();
  @Output() propertySelected = new EventEmitter<number>();

  // Visible property ids for current slide
  visibleImageIndex: { [propertyId: number]: number } = {};
  
  // Authentication status
  isStudent = false;

  // NO_IMAGE fallback
  readonly NO_IMAGE_FALLBACK = 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIzMDAiIGhlaWdodD0iMjAwIiB2aWV3Qm94PSIwIDAgMzAwIDIwMCI+PHJlY3Qgd2lkdGg9IjMwMCIgaGVpZ2h0PSIyMDAiIGZpbGw9IiNlZWUiLz48dGV4dCB4PSI1MCUiIHk9IjUwJSIgZm9udC1mYW1pbHk9IkFyaWFsLCBzYW5zLXNlcmlmIiBmb250LXNpemU9IjI0IiB0ZXh0LWFuY2hvcj0ibWlkZGxlIiBmaWxsPSIjOTk5Ij5ObyBJbWFnZTwvdGV4dD48L3N2Zz4=';

  // Small image cache - limit to just visible images
  private imageCache = new Map<string, SafeUrl>();
  private activeHttpRequests = new Set<string>();
  private imagePreloadQueue: string[] = [];
  private preloadingActive = false;

  constructor(
    private http: HttpClient,
    private sanitizer: DomSanitizer,
    private userProfileService: UserProfileService,
    private authService: AuthService,
    private router: Router
  ) { }

  ngOnInit(): void {
    // Initialize with all properties showing first image
    if (this.propertiesPage?.content) {
      this.propertiesPage.content.forEach(prop => {
        if (prop.id) {
          this.visibleImageIndex[prop.id] = 0;
        }
      });
    }
    
    // Check authentication status
    this.checkAuth();
  }
  
  // Check if user is authenticated and is a student
  private checkAuth(): void {
    const isLoggedIn = this.authService.isLoggedIn();
    
    if (isLoggedIn) {
      const token = this.authService.getToken();
      
      if (token) {
        try {
          const decodedToken = this.authService.decodeToken(token);
          this.isStudent = decodedToken?.role === 'STUDENT';
        } catch (err) {
          console.error('Error decoding token:', err);
          this.isStudent = false;
        }
      } else {
        this.isStudent = false;
      }
    } else {
      this.isStudent = false;
    }
  }

  // Toggle favorite status for a property
  toggleFavorite(event: Event, property: PropertyListingDTO): void {
    event.preventDefault();
    event.stopPropagation();
    
    if (!property || !property.id) {
      console.error('Invalid property or missing ID');
      return;
    }

    // Redirect to login if not authenticated
    if (!this.authService.isLoggedIn()) {
      this.router.navigate(['/auth/login']);
      return;
    }
    
    const propertyId = Number(property.id);
    this.userProfileService.toggleFavorite(propertyId).subscribe({
      next: (profile) => {
        // Success is handled by the service caching the updated profile
      },
      error: (err) => {
        console.error('Error toggling favorite:', err);
      }
    });
  }

  // Check if a property is in favorites
  isFavorite(property: PropertyListingDTO): boolean {
    if (!property || !property.id) {
      return false;
    }
    
    // For testing - always return false to show outline heart
    if (!this.authService.isLoggedIn() || !this.isStudent) {
      return false;
    }
    
    const propertyId = Number(property.id);
    return this.userProfileService.isPropertyFavorite(propertyId);
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['propertiesPage']) {
      // Reset image indexes when properties change
      this.visibleImageIndex = {};
      
      // Initialize new properties with first image
      if (this.propertiesPage?.content) {
        this.propertiesPage.content.forEach(prop => {
          if (prop.id) {
            this.visibleImageIndex[prop.id] = 0;
            
            // Preload main images and additional images
          if (prop.mainImageUrl) {
              this.queueImageForPreload(prop.mainImageUrl);
            }
            if (prop.imageUrls && prop.imageUrls.length > 0) {
              // Queue first 2 additional images for preloading
              prop.imageUrls.slice(0, 2).forEach(url => {
                this.queueImageForPreload(url);
              });
            }
          }
        });
      }
      
      // Start preloading process
      this.processPreloadQueue();
      
      // Clear the cache when property page changes to free memory
      this.imageCache.clear();
    }
  }

  ngOnDestroy(): void {
    // Clear cache on destroy
    this.imageCache.clear();
  }

  // Check if a property is selected
  isPropertySelected(property: PropertyListingDTO): boolean {
    return property.id === this.selectedPropertyId;
  }

  // Handle property card click
  onPropertyCardClicked(property: PropertyListingDTO): void {
    if (property.id) {
      this.propertySelected.emit(property.id);
    }
  }

  // Helper method to clean image URLs (remove leading @ symbol)
  cleanImageUrl(url: string | undefined | null): string {
    if (!url) {
      return this.NO_IMAGE_FALLBACK;
    }

    // Handle @https:// format
    if (url.startsWith('@')) {
      return url.substring(1);
    }

    return url;
  }

  // Get the current visible image for a property
  getCurrentImage(property: PropertyListingDTO): string | undefined {
    if (!property || !property.id) {
      return undefined;
    }
    
    // Get the current index for this property
    const index = this.visibleImageIndex[property.id] || 0;
    
    // Check if property has multiple images
    if (property.imageUrls && property.imageUrls.length > 0) {
      return property.imageUrls[index];
    }
    
    // Fall back to main image
    return property.mainImageUrl;
  }
  
  // Move to next image for a property
  nextImage(event: Event, property: PropertyListingDTO): void {
    event.stopPropagation(); // Prevent property card click
    
    if (!property || !property.id || !property.imageUrls || property.imageUrls.length <= 1) {
      return;
    }
    
    const currentIndex = this.visibleImageIndex[property.id] || 0;
    const nextIndex = (currentIndex + 1) % property.imageUrls.length;
    this.visibleImageIndex[property.id] = nextIndex;
    
    // Preload the next image in sequence if it exists
    const upcomingIndex = (nextIndex + 1) % property.imageUrls.length;
    if (property.imageUrls[upcomingIndex]) {
      this.queueImageForPreload(property.imageUrls[upcomingIndex]);
    }
  }
  
  // Move to previous image for a property
  prevImage(event: Event, property: PropertyListingDTO): void {
    event.stopPropagation(); // Prevent property card click
    
    if (!property || !property.id || !property.imageUrls || property.imageUrls.length <= 1) {
      return;
    }
    
    const currentIndex = this.visibleImageIndex[property.id] || 0;
    const prevIndex = (currentIndex - 1 + property.imageUrls.length) % property.imageUrls.length;
    this.visibleImageIndex[property.id] = prevIndex;
    
    // Preload the previous image in sequence
    const upcomingIndex = (prevIndex - 1 + property.imageUrls.length) % property.imageUrls.length;
    if (property.imageUrls[upcomingIndex]) {
      this.queueImageForPreload(property.imageUrls[upcomingIndex]);
    }
  }

  // Get image - optimized version that doesn't load all images at once
  getImageAsDataUrl(url: string | undefined | null): SafeUrl {
    if (!url) {
      return this.NO_IMAGE_FALLBACK;
    }

    // Clean the URL if needed
    const cleanedUrl = this.cleanImageUrl(url);

    // Check if we've already converted this image
    if (this.imageCache.has(cleanedUrl)) {
      return this.imageCache.get(cleanedUrl)!;
    }

    // Avoid duplicate requests for the same image
    if (this.activeHttpRequests.has(cleanedUrl)) {
      return this.NO_IMAGE_FALLBACK;
    }

    // Queue for loading
    this.queueImageForPreload(cleanedUrl);
    
    // Return fallback until image is loaded
    return this.NO_IMAGE_FALLBACK;
  }

  // Add an image to the preload queue
  private queueImageForPreload(url: string): void {
    const cleanedUrl = this.cleanImageUrl(url);
    
    // Skip if already cached or being loaded
    if (this.imageCache.has(cleanedUrl) || 
        this.activeHttpRequests.has(cleanedUrl) ||
        this.imagePreloadQueue.includes(cleanedUrl)) {
      return;
    }
    
    // Add to queue
    this.imagePreloadQueue.push(cleanedUrl);
    
    // Start processing if not already running
    if (!this.preloadingActive) {
      this.processPreloadQueue();
    }
  }
  
  // Process the image preload queue
  private processPreloadQueue(): void {
    if (this.imagePreloadQueue.length === 0) {
      this.preloadingActive = false;
      return;
    }
    
    this.preloadingActive = true;
    
    // Get next URL to preload
    const url = this.imagePreloadQueue.shift()!;
    
    // Load through proxy for tayara.tn to avoid CORS
    if (url.includes('tayara.tn') || url.includes('mubawab.tn')) {
      this.loadImageViaProxy(url).finally(() => {
        // Continue with next image in queue
        setTimeout(() => this.processPreloadQueue(), 100);
      });
    } else {
      this.loadImageDirectly(url).finally(() => {
        // Continue with next image in queue
        setTimeout(() => this.processPreloadQueue(), 100);
      });
    }
  }

  private async loadImageDirectly(url: string): Promise<void> {
    // Track this request
    this.activeHttpRequests.add(url);
    
    try {
      const response = await fetch(url);
      if (!response.ok) {
        throw new Error(`Direct fetch failed with status ${response.status}`);
      }
      
      const blob = await response.blob();
      return new Promise<void>((resolve) => {
        const reader = new FileReader();
        reader.onload = () => {
          const rawDataUrl = reader.result as string;
          const safeDataUrl = this.sanitizer.bypassSecurityTrustUrl(rawDataUrl);
          this.imageCache.set(url, safeDataUrl);
          this.activeHttpRequests.delete(url);
          resolve();
        };
        reader.readAsDataURL(blob);
      });
    } catch (err) {
      console.error(`Direct fetch failed for ${url}, falling back to proxy`, err);
      return this.loadImageViaProxy(url);
    }
  }

  private async loadImageViaProxy(url: string): Promise<void> {
    // Track this request
    this.activeHttpRequests.add(url);
    
    const proxyUrl = `/api/v1/image-proxy?url=${encodeURIComponent(url)}`;
    
    return new Promise<void>((resolve, reject) => {
      this.http.get(proxyUrl, { responseType: 'blob' }).subscribe({
        next: (blob) => {
          const reader = new FileReader();
          reader.onload = () => {
            const rawDataUrl = reader.result as string;
            const safeDataUrl = this.sanitizer.bypassSecurityTrustUrl(rawDataUrl);
            this.imageCache.set(url, safeDataUrl);
            this.activeHttpRequests.delete(url);
            resolve();
          };
          reader.onerror = () => {
            this.imageCache.set(url, this.NO_IMAGE_FALLBACK);
            this.activeHttpRequests.delete(url);
            reject(reader.error);
        };
        reader.readAsDataURL(blob);
      },
      error: (err) => {
          console.error(`Failed to load image through proxy for ${url}`, err);
          this.imageCache.set(url, this.NO_IMAGE_FALLBACK);
          this.activeHttpRequests.delete(url);
          reject(err);
      }
      });
    });
  }

  handleImageError(event: Event, property: PropertyListingDTO): void {
    const target = event.target as HTMLImageElement;
    if (target) {
      target.src = this.NO_IMAGE_FALLBACK;
      
      // If this was a property's main image, try using the first image from imageUrls
      if (property.imageUrls && property.imageUrls.length > 0 && 
          property.id && this.visibleImageIndex[property.id] === 0) {
        // Try loading the first alternative image
        this.visibleImageIndex[property.id] = 1;
      }
    }
  }

  // Check if property has multiple images
  hasMultipleImages(property: PropertyListingDTO): boolean {
    return !!(property.imageUrls && property.imageUrls.length > 1);
  }

  // Get number of images for a property (including main image)
  getImageCount(property: PropertyListingDTO): number {
    return (property.imageUrls?.length || 0) + (property.mainImageUrl ? 1 : 0);
  }

  onPageChange(pageNumber: number): void {
    if (this.propertiesPage && pageNumber >= 0 && pageNumber < this.propertiesPage.totalPages) {
      this.pageChanged.emit(pageNumber);
    }
  }

  // Helper for pagination: an array of page numbers to display
  get pageNumbers(): number[] {
    if (!this.propertiesPage || this.propertiesPage.totalPages <= 1) {
      return [];
    }
    // Simple range for now, can be made more sophisticated (e.g., with ellipses)
    const maxPagesToShow = 5;
    const currentPage = this.propertiesPage.number;
    const totalPages = this.propertiesPage.totalPages;
    let startPage: number, endPage: number;

    if (totalPages <= maxPagesToShow) {
      startPage = 0;
      endPage = totalPages - 1;
    } else {
      if (currentPage <= Math.floor(maxPagesToShow / 2)) {
        startPage = 0;
        endPage = maxPagesToShow - 1;
      } else if (currentPage + Math.floor(maxPagesToShow / 2) >= totalPages) {
        startPage = totalPages - maxPagesToShow;
        endPage = totalPages - 1;
      } else {
        startPage = currentPage - Math.floor(maxPagesToShow / 2);
        endPage = currentPage + Math.floor(maxPagesToShow / 2);
      }
    }
    return Array.from(Array((endPage - startPage) + 1).keys()).map(i => startPage + i);
  }

  /**
   * Handle inquiry sent event
   */
  onInquirySent(property: PropertyListingDTO): void {
    console.log('Inquiry sent for property:', property.title);
    // You can add additional logic here, such as showing a toast notification
    // or updating the property card to indicate an inquiry was sent
  }

  /**
   * Handle inquiry form closed event
   */
  onInquiryFormClosed(): void {
    // Optional: Handle when the inquiry form is closed
    console.log('Inquiry form closed');
  }
}
