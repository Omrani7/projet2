import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule, Router } from '@angular/router';
import { OwnerPropertyListingDto } from '../../models/owner-property-listing.dto';
import { OwnerPropertyService } from '../../services/owner-property.service';
import { PropertyCardComponent } from '../../components/property-card/property-card.component';
import { FilterBarComponent } from '../../components/filter-bar/filter-bar.component';
import { HeaderComponent } from '../../components/header/header.component';
import { MapDisplayComponent } from '../../components/map-display/map-display.component';
import { AuthService } from '../../auth/auth.service';
import { HttpErrorResponse } from '@angular/common/http';

@Component({
  selector: 'app-owner-property-details',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    PropertyCardComponent,
    FilterBarComponent,
    HeaderComponent,
    MapDisplayComponent
  ],
  template: `
    <app-header></app-header>
    
    <div class="property-details-container">
      <div *ngIf="loading" class="loading">
        <p>Loading property details...</p>
      </div>
      
      <div *ngIf="error" class="error-message">
        <p>Sorry, there was an error loading this property. Please try again later.</p>
        <p *ngIf="errorDetails">{{ errorDetails }}</p>
        <button (click)="goBack()" class="btn-back">Go Back</button>
      </div>
      
      <div *ngIf="property && !loading && !error" class="property-content">
        <div class="breadcrumbs">
          <a [routerLink]="['/']">Home</a> &gt;
          <a [routerLink]="['/owner/my-properties']">My Properties</a> &gt;
          <span>{{ property.title }}</span>
        </div>
        
        <div class="property-header">
          <h1>{{ property.title }}</h1>
          <div class="property-location">
            <i class="pi pi-map-marker"></i> {{ property.fullAddress || property.city }}
          </div>
          <div class="property-price">
            {{ property.price | currency:'TND':'symbol':'1.0-0' }} / month
          </div>
          
          <div class="status-badge" [ngClass]="{'active': property.active, 'inactive': !property.active}">
            {{ property.active ? 'Active' : 'Inactive' }}
          </div>
        </div>
        
        <div class="image-gallery">
          <div class="main-image-container">
            <img [src]="currentImage" alt="{{ property.title }}" class="main-image">
            <button class="gallery-nav prev" (click)="prevImage()" *ngIf="property.imageUrls && property.imageUrls.length > 1">
              <i class="pi pi-chevron-left"></i>
            </button>
            <button class="gallery-nav next" (click)="nextImage()" *ngIf="property.imageUrls && property.imageUrls.length > 1">
              <i class="pi pi-chevron-right"></i>
            </button>
          </div>
          
          <div class="thumbnail-container" *ngIf="property.imageUrls && property.imageUrls.length > 1">
            <div 
              *ngFor="let img of property.imageUrls; let i = index" 
              class="thumbnail" 
              [class.active]="i === activeImageIndex"
              (click)="setActiveImage(i)">
              <img [src]="img" alt="Thumbnail {{ i + 1 }}">
            </div>
          </div>
        </div>
        
        <div class="property-info-grid">
          <div class="info-card">
            <i class="pi pi-home"></i>
            <div class="info-label">Property Type</div>
            <div class="info-value">{{ property.propertyType || 'Not specified' }}</div>
          </div>
          <div class="info-card">
            <i class="pi pi-th-large"></i>
            <div class="info-label">Area</div>
            <div class="info-value">{{ property.area || '-' }} m²</div>
          </div>
          <div class="info-card">
            <i class="pi pi-inbox"></i>
            <div class="info-label">Rooms</div>
            <div class="info-value">{{ property.rooms || '-' }} rooms</div>
          </div>
          <div class="info-card">
            <i class="pi pi-calendar"></i>
            <div class="info-label">Available From</div>
            <div class="info-value">{{ property.availableFrom ? (property.availableFrom | date:'mediumDate') : 'Not specified' }}</div>
          </div>
        </div>
        
        <div class="property-section">
          <h2>Description</h2>
          <div class="description">{{ property.description || 'No description provided.' }}</div>
        </div>
        
        <div class="property-section" *ngIf="property.amenities && property.amenities.length > 0">
          <h2>Amenities</h2>
          <div class="amenities-list">
            <div class="amenity" *ngFor="let amenity of property.amenities">
              <i class="pi pi-check"></i> {{ amenity }}
            </div>
          </div>
        </div>
        
        <div class="property-section" *ngIf="property.latitude && property.longitude">
          <h2>Location</h2>
          <app-map-display
            [properties]="[mapPropertyData]"
            [selectedPropertyId]="property.id">
          </app-map-display>
        </div>
        
        <div class="property-actions">
          <a [routerLink]="['/owner/property/edit', property.id]" class="btn-primary">Edit Property</a>
          <button (click)="togglePropertyStatus()" class="btn-secondary">
            {{ property.active ? 'Set Inactive' : 'Set Active' }}
          </button>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .property-details-container {
      max-width: 1200px;
      margin: 20px auto;
      padding: 0 20px;
    }
    
    .loading, .error-message {
      text-align: center;
      padding: 40px 0;
    }
    
    .error-message {
      color: #721c24;
      background-color: #f8d7da;
      border-radius: 8px;
      padding: 20px;
    }
    
    .btn-back {
      background-color: #6c757d;
      color: white;
      border: none;
      padding: 10px 20px;
      border-radius: 4px;
      cursor: pointer;
      margin-top: 15px;
    }
    
    .property-content {
      background-color: white;
      border-radius: 8px;
      box-shadow: 0 2px 10px rgba(0,0,0,0.05);
      overflow: hidden;
    }
    
    .breadcrumbs {
      padding: 15px 20px;
      font-size: 0.9rem;
      background-color: #f8f9fa;
      border-bottom: 1px solid #e9ecef;
    }
    
    .breadcrumbs a {
      color: #007bff;
      text-decoration: none;
    }
    
    .breadcrumbs a:hover {
      text-decoration: underline;
    }
    
    .property-header {
      padding: 20px;
      border-bottom: 1px solid #e9ecef;
      position: relative;
    }
    
    .property-header h1 {
      font-size: 1.8rem;
      margin: 0 0 10px 0;
      color: #343a40;
    }
    
    .property-location {
      color: #6c757d;
      margin-bottom: 10px;
      display: flex;
      align-items: center;
    }
    
    .property-location i {
      margin-right: 8px;
    }
    
    .property-price {
      font-size: 1.4rem;
      font-weight: bold;
      color: #007bff;
    }
    
    .status-badge {
      position: absolute;
      top: 20px;
      right: 20px;
      padding: 5px 10px;
      border-radius: 4px;
      font-size: 0.8rem;
      font-weight: bold;
    }
    
    .status-badge.active {
      background-color: #d4edda;
      color: #155724;
    }
    
    .status-badge.inactive {
      background-color: #f8d7da;
      color: #721c24;
    }
    
    .image-gallery {
      margin: 20px;
    }
    
    .main-image-container {
      position: relative;
      width: 100%;
      height: 400px;
      overflow: hidden;
      border-radius: 8px;
    }
    
    .main-image {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }
    
    .gallery-nav {
      position: absolute;
      top: 50%;
      transform: translateY(-50%);
      background-color: rgba(255, 255, 255, 0.7);
      border: none;
      width: 40px;
      height: 40px;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      cursor: pointer;
      transition: background-color 0.3s;
    }
    
    .gallery-nav:hover {
      background-color: rgba(255, 255, 255, 0.9);
    }
    
    .gallery-nav.prev {
      left: 15px;
    }
    
    .gallery-nav.next {
      right: 15px;
    }
    
    .thumbnail-container {
      display: flex;
      gap: 10px;
      margin-top: 10px;
      overflow-x: auto;
      padding-bottom: 10px;
    }
    
    .thumbnail {
      width: 80px;
      height: 60px;
      border-radius: 4px;
      overflow: hidden;
      cursor: pointer;
      opacity: 0.7;
      transition: opacity 0.3s;
    }
    
    .thumbnail:hover, .thumbnail.active {
      opacity: 1;
    }
    
    .thumbnail img {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }
    
    .property-info-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
      gap: 15px;
      padding: 20px;
      background-color: #f8f9fa;
      margin: 0 20px;
      border-radius: 8px;
    }
    
    .info-card {
      background-color: white;
      padding: 15px;
      border-radius: 8px;
      box-shadow: 0 1px 5px rgba(0,0,0,0.05);
      text-align: center;
    }
    
    .info-card i {
      font-size: 1.5rem;
      color: #007bff;
      margin-bottom: 10px;
    }
    
    .info-label {
      color: #6c757d;
      font-size: 0.85rem;
      margin-bottom: 5px;
    }
    
    .info-value {
      font-weight: bold;
    }
    
    .property-section {
      margin: 30px 20px;
    }
    
    .property-section h2 {
      margin-bottom: 15px;
      font-size: 1.4rem;
      color: #343a40;
    }
    
    .description {
      line-height: 1.6;
      color: #495057;
    }
    
    .amenities-list {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
      gap: 10px;
    }
    
    .amenity {
      display: flex;
      align-items: center;
      background-color: #f8f9fa;
      padding: 8px 12px;
      border-radius: 4px;
    }
    
    .amenity i {
      color: #28a745;
      margin-right: 8px;
    }
    
    .property-actions {
      display: flex;
      gap: 15px;
      margin: 30px 20px;
      padding-top: 20px;
      border-top: 1px solid #e9ecef;
    }
    
    .btn-primary, .btn-secondary {
      padding: 10px 20px;
      border-radius: 4px;
      font-weight: 500;
      cursor: pointer;
      text-decoration: none;
      display: inline-block;
      text-align: center;
    }
    
    .btn-primary {
      background-color: #007bff;
      color: white;
      border: none;
    }
    
    .btn-primary:hover {
      background-color: #0069d9;
    }
    
    .btn-secondary {
      background-color: #6c757d;
      color: white;
      border: none;
    }
    
    .btn-secondary:hover {
      background-color: #5a6268;
    }
    
    @media (max-width: 768px) {
      .main-image-container {
        height: 300px;
      }
      
      .property-info-grid {
        grid-template-columns: repeat(2, 1fr);
      }
      
      .amenities-list {
        grid-template-columns: repeat(2, 1fr);
      }
    }
    
    @media (max-width: 576px) {
      .property-info-grid {
        grid-template-columns: 1fr;
      }
      
      .amenities-list {
        grid-template-columns: 1fr;
      }
      
      .property-actions {
        flex-direction: column;
      }
    }
  `]
})
export class OwnerPropertyDetailsComponent implements OnInit {
  property: OwnerPropertyListingDto | null = null;
  loading = true;
  error = false;
  errorDetails: string | null = null;
  activeImageIndex = 0;
  
  // Derived property for the map component
  get mapPropertyData(): any {
    if (!this.property) return null;
    
    return {
      id: this.property.id,
      title: this.property.title,
      propertyType: this.property.propertyType || 'Property',
      address: this.property.fullAddress || this.property.city || 'Address not specified',
      price: this.property.price,
      currency: 'TND',
      latitude: this.property.latitude,
      longitude: this.property.longitude,
      location: (this.property.latitude && this.property.longitude) ? 
        { lat: this.property.latitude, lng: this.property.longitude } : undefined,
      imageUrls: this.property.imageUrls,
      active: this.property.active,
      sourceType: 'owner' // Explicitly set sourceType for map component
    };
  }

  get currentImage(): string {
    if (this.property?.imageUrls && this.property.imageUrls.length > 0) {
      return this.property.imageUrls[this.activeImageIndex];
    }
    return 'assets/images/default-property.png';
  }

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private ownerPropertyService: OwnerPropertyService,
    private authService: AuthService
  ) {
    console.log('OwnerPropertyDetailsComponent CONSTRUCTOR - Route:', this.router.url);
  }

  ngOnInit(): void {
    if (!this.authService.isLoggedIn()) {
      this.router.navigate(['/auth/login']);
      return;
    }

    // Log the current route for debugging
    console.log('Current route:', this.router.url);

    this.route.paramMap.subscribe(params => {
      const propertyId = params.get('id');
      console.log('Property ID from route:', propertyId);
      
      if (propertyId) {
        this.loadPropertyDetails(Number(propertyId));
      } else {
        this.error = true;
        this.loading = false;
        this.errorDetails = 'No property ID provided';
      }
    });
  }

  loadPropertyDetails(id: number): void {
    this.loading = true;
    console.log(`Loading owner property details for ID: ${id} using OwnerPropertyService`);
    
    this.ownerPropertyService.getPropertyById(id).subscribe({
      next: (property) => {
        console.log('Successfully loaded owner property:', property);
        this.property = property;
        this.loading = false;
      },
      error: (err: HttpErrorResponse) => {
        console.error('Error loading owner property:', err);
        this.error = true;
        this.loading = false;
        
        if (err.status === 404) {
          this.errorDetails = 'Property not found. It may have been deleted or you do not have permission to view it.';
        } else if (err.status === 401 || err.status === 403) {
          this.errorDetails = 'You do not have permission to view this property.';
        } else {
          this.errorDetails = `Error: ${err.message || 'Unknown error occurred'}`;
        }
      }
    });
  }

  setActiveImage(index: number): void {
    if (this.property?.imageUrls && index >= 0 && index < this.property.imageUrls.length) {
      this.activeImageIndex = index;
    }
  }

  nextImage(): void {
    if (this.property?.imageUrls) {
      this.activeImageIndex = (this.activeImageIndex + 1) % this.property.imageUrls.length;
    }
  }

  prevImage(): void {
    if (this.property?.imageUrls) {
      this.activeImageIndex = (this.activeImageIndex - 1 + this.property.imageUrls.length) % this.property.imageUrls.length;
    }
  }

  togglePropertyStatus(): void {
    if (!this.property) return;
    
    const updatedStatus = !this.property.active;
    console.log(`Toggling property status to ${updatedStatus ? 'active' : 'inactive'}`);
    
    this.ownerPropertyService.updateProperty(this.property.id, { active: updatedStatus }).subscribe({
      next: (updatedProperty) => {
        console.log('Property status updated successfully:', updatedProperty);
        if (this.property) {
          this.property.active = updatedProperty.active;
        }
      },
      error: (err) => {
        console.error('Error updating property status:', err);
        alert('Failed to update property status. Please try again.');
      }
    });
  }

  goBack(): void {
    this.router.navigate(['/owner/my-properties']);
  }
} 