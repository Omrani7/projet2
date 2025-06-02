import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { Property } from '../../models/property.model';
import { UserProfileService } from '../../services/user-profile.service';
import { AuthService } from '../../auth/auth.service';

@Component({
  selector: 'app-property-card',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './property-card.component.html',
  styleUrl: './property-card.component.css'
})
export class PropertyCardComponent {
  @Input() property: Property | undefined;
  isStudent = false;
  private isHandlingError = false; // Flag to prevent infinite loops
  private readonly NO_IMAGE_FALLBACK = 'data:image/svg+xml;charset=UTF-8,%3Csvg%20width%3D%22286%22%20height%3D%22180%22%20xmlns%3D%22http%3A%2F%2Fwww.w3.org%2F2000%2Fsvg%22%20viewBox%3D%220%200%20286%20180%22%20preserveAspectRatio%3D%22none%22%3E%3Cdefs%3E%3Cstyle%20type%3D%22text%2Fcss%22%3E%23holder_189b3e01124%20text%20%7B%20fill%3A%23999%3Bfont-weight%3Anormal%3Bfont-family%3AArial%2C%20Helvetica%2C%20Open%20Sans%2C%20sans-serif%2C%20monospace%3Bfont-size%3A14pt%20%7D%20%3C%2Fstyle%3E%3C%2Fdefs%3E%3Cg%20id%3D%22holder_189b3e01124%22%3E%3Crect%20width%3D%22286%22%20height%3D%22180%22%20fill%3D%22%23373940%22%3E%3C%2Frect%3E%3Cg%3E%3Ctext%20x%3D%2298.92499923706055%22%20y%3D%2296.3%22%3ENo%20Image%3C%2Ftext%3E%3C%2Fg%3E%3C%2Fg%3E%3C%2Fsvg%3E';

  constructor(
    private router: Router,
    private userProfileService: UserProfileService,
    private authService: AuthService
  ) {
    this.checkAuth();
  }

  private checkAuth(): void {
    const token = this.authService.getToken();
    if (token) {
      const decodedToken = this.authService.decodeToken(token);
      this.isStudent = decodedToken?.role === 'STUDENT';
    }
  }
  
  navigateToDetails(event: Event): void {
    if (this.property) {
      event.preventDefault();
      event.stopPropagation();
      this.router.navigate(['/properties', this.property.id]);
    }
  }

  // Handler for image loading errors
  handleImageError(event: Event): void {
    // Prevent infinite loops
    if (this.isHandlingError) {
      return;
    }
    
    this.isHandlingError = true;
    const imgElement = event.target as HTMLImageElement;
    imgElement.src = this.NO_IMAGE_FALLBACK;
    this.isHandlingError = false;
  }

  toggleFavorite(event: Event): void {
    event.preventDefault();
    event.stopPropagation();
    
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

  // Get the property image URL - handles both imageUrl and imageUrls fields
  getPropertyImageUrl(): string {
    if (!this.property) {
      return this.NO_IMAGE_FALLBACK;
    }

    // First try imageUrl (for Property model)
    if ((this.property as any).imageUrl) {
      return (this.property as any).imageUrl;
    }

    // Then try imageUrls array (for PropertyListingDTO model)
    if ((this.property as any).imageUrls && (this.property as any).imageUrls.length > 0) {
      return (this.property as any).imageUrls[0];
    }

    // Then try mainImageUrl (backup field)
    if ((this.property as any).mainImageUrl) {
      return (this.property as any).mainImageUrl;
    }

    // Fall back to no image placeholder
    return this.NO_IMAGE_FALLBACK;
  }

  // Get property address - handles different address fields
  getPropertyAddress(): string {
    if (!this.property) return '';
    
    const prop = this.property as any;
    return prop.address || prop.fullAddress || prop.location || prop.city || 'Location not specified';
  }

  // Get property type
  getPropertyType(): string {
    if (!this.property) return 'Property';
    
    const prop = this.property as any;
    return prop.propertyType || 'Property';
  }

  // Get bedrooms count - handles both beds and bedrooms fields
  getBedrooms(): number | null {
    if (!this.property) return null;
    
    const prop = this.property as any;
    return prop.bedrooms || prop.beds || null;
  }

  // Get bathrooms count - handles both baths and bathrooms fields  
  getBathrooms(): number | null {
    if (!this.property) return null;
    
    const prop = this.property as any;
    return prop.bathrooms || prop.baths || null;
  }

  // Get area
  getArea(): number | null {
    if (!this.property) return null;
    
    const prop = this.property as any;
    return prop.area || null;
  }

  // Get listing date
  getListingDate(): string | null {
    if (!this.property) return null;
    
    const prop = this.property as any;
    return prop.listingDate || prop.createdAt || null;
  }
}
