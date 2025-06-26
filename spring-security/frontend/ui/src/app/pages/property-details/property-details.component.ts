import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule, Router } from '@angular/router';
import { Property } from '../../models/property.model';
import { PropertyServiceService } from '../../services/property.service.service';
import { PropertyCardComponent } from '../../components/property-card/property-card.component';
import { FilterBarComponent } from '../../components/filter-bar/filter-bar.component';
import { HeaderComponent } from '../../components/header/header.component';
import { MapDisplayComponent } from '../../components/map-display/map-display.component';
import { UserProfileService } from '../../services/user-profile.service';
import { AuthService } from '../../auth/auth.service';

@Component({
  selector: 'app-property-details',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    PropertyCardComponent,
    FilterBarComponent,
    HeaderComponent,
    MapDisplayComponent
  ],
  templateUrl: './property-details.component.html',
  styleUrl: './property-details.component.css'
})
export class PropertyDetailsComponent implements OnInit {
  property: Property | null = null;
  loading = true;
  error = false;
  activeImageIndex = 0;
  similarProperties: Property[] = [];
  isStudent = false;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private propertyService: PropertyServiceService,
    private userProfileService: UserProfileService,
    private authService: AuthService
  ) {
    console.log('PropertyDetailsComponent CONSTRUCTOR - Route:', this.router.url);
    this.checkAuth();
  }

  private checkAuth(): void {
    if (this.authService.isLoggedIn()) {
      const token = this.authService.getToken();
      if (token) {
        const decodedToken = this.authService.decodeToken(token);
        this.isStudent = decodedToken?.role === 'STUDENT';
      }
    }
  }

  ngOnInit(): void {
    this.route.paramMap.subscribe(params => {
      const propertyId = params.get('id');
      if (propertyId) {
        this.loadPropertyDetails(propertyId);
        this.loadSimilarProperties();
      } else {
        this.error = true;
        this.loading = false;
      }
    });
  }

  loadPropertyDetails(id: string): void {
    this.loading = true;
    this.propertyService.getPropertyById(id).subscribe({
      next: (property) => {
        this.property = property;
        
        // Set coordinates from location if needed for map display
        if (this.property && this.property.location) {
          this.property.latitude = this.property.location.lat;
          this.property.longitude = this.property.location.lng;
        }
        
        this.loading = false;
      },
      error: (err) => {
        console.error('Error loading property:', err);
        this.error = true;
        this.loading = false;
      }
    });
  }

  // Safe accessor method for availability date to avoid template errors
  getAvailabilityDate(): string | null {
    if (this.property?.availability?.from) {
      return this.property.availability.from;
    }
    return null;
  }

  // Safe accessor method for review dates
  getReviewDate(date: string | undefined): string | null {
    return date || null;
  }

  // Safe accessor method for contact phone
  getContactPhone(): string | null {
    // Handle both string contactInfo (phone) and object structure
    if (typeof this.property?.contactInfo === 'string') {
      return this.property.contactInfo;
    }
    return this.property?.contactInfo?.phone || null;
  }

  // Safe accessor method for contact name
  getContactName(): string | null {
    // For owner properties, we might not have a separate name field
    // Check if contactInfo is an object with name property
    if (typeof this.property?.contactInfo === 'object' && this.property?.contactInfo?.name) {
      return this.property.contactInfo.name;
    }
    return null;
  }

  // Safe accessor method for contact email
  getContactEmail(): string | null {
    // For owner properties, we might not have email in contactInfo
    // Check if contactInfo is an object with email property
    if (typeof this.property?.contactInfo === 'object' && this.property?.contactInfo?.email) {
      return this.property.contactInfo.email;
    }
    return null;
  }

  loadSimilarProperties(): void {
    this.propertyService.getRecommendedProperties(3).subscribe({
      next: (properties) => {
        this.similarProperties = properties;
      },
      error: (err) => {
        console.error('Error loading similar properties:', err);
      }
    });
  }

  setActiveImage(index: number): void {
    if (this.property?.images && index >= 0 && index < this.property.images.length) {
      this.activeImageIndex = index;
    }
  }

  nextImage(): void {
    if (this.property?.images) {
      this.activeImageIndex = (this.activeImageIndex + 1) % this.property.images.length;
    }
  }

  prevImage(): void {
    if (this.property?.images) {
      this.activeImageIndex = (this.activeImageIndex - 1 + this.property.images.length) % this.property.images.length;
    }
  }

  contactProperty(): void {
    // Implement contact functionality (e.g., open modal, scroll to contact form)
    console.log('Contact property:', this.property?.title);
    // Could trigger a modal or scroll to a contact form
  }

  bookViewingOrApply(): void {
    // Implement booking/application functionality
    console.log('Book viewing or apply for:', this.property?.title);
    // Could redirect to a booking form or application page
  }

  toggleFavorite(): void {
    if (!this.property || !this.authService.isLoggedIn() || !this.isStudent) {
      // Either not logged in or not a student
      if (!this.authService.isLoggedIn()) {
        this.router.navigate(['/auth/login']);
      }
      return;
    }

    const propertyId = Number(this.property.id);
    this.userProfileService.toggleFavorite(propertyId).subscribe({
      next: (profile) => {
        console.log('Favorite toggled successfully');
      },
      error: (err) => {
        console.error('Error toggling favorite:', err);
      }
    });
  }

  isFavorite(): boolean {
    if (!this.property || !this.authService.isLoggedIn() || !this.isStudent) {
      return false;
    }
    const propertyId = Number(this.property.id);
    return this.userProfileService.isPropertyFavorite(propertyId);
  }
}
