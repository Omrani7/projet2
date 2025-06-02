import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { PropertyListingDTO } from '../../models/property-listing.dto';
import { DomSanitizer, SafeUrl } from '@angular/platform-browser';
import { HttpClient } from '@angular/common/http';

@Component({
  selector: 'app-owner-property-card',
  standalone: true,
  imports: [CommonModule, RouterModule],
  template: `
    <div class="property-card">
      <div class="property-image">
        <img [src]="safeImageUrl || 'assets/images/property-placeholder.jpg'" 
             alt="{{ property.title }}" 
             (error)="handleImageError($event)" />
        <div class="property-status" [ngClass]="{'active': property.active, 'inactive': !property.active}">
          {{ property.active ? 'Active' : 'Inactive' }}
        </div>
        <div class="source-badge" *ngIf="property.sourceType">
          {{ property.sourceType === 'OWNER' ? 'Owner Listed' : 'Scraped' }}
        </div>
      </div>
      <div class="property-info">
        <h3>{{ property.title }}</h3>
        <p class="price">{{ property.price | currency:'TND ' }}</p>
        <p class="location">{{ property.city }}, {{ property.district }}</p>
        <div class="property-details">
          <span *ngIf="property.area">{{ property.area }} m²</span>
          <span *ngIf="property.bedrooms">{{ property.bedrooms }} bd</span>
          <span *ngIf="property.bathrooms">{{ property.bathrooms }} ba</span>
        </div>
        <div class="property-actions">
          <a [routerLink]="['/properties', property.id]" class="view-btn">View</a>
          <a [routerLink]="['/owner/property/edit', property.id]" class="edit-btn">Edit</a>
          <button class="delete-btn" (click)="onDeleteClick()">Delete</button>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .property-card {
      border-radius: 8px;
      overflow: hidden;
      box-shadow: 0 2px 8px rgba(0,0,0,0.1);
      transition: transform 0.2s, box-shadow 0.2s;
      background-color: white;
      height: 100%;
      display: flex;
      flex-direction: column;
    }
    .property-card:hover {
      transform: translateY(-5px);
      box-shadow: 0 4px 12px rgba(0,0,0,0.15);
    }
    .property-image {
      position: relative;
      height: 180px;
      overflow: hidden;
    }
    .property-image img {
      width: 100%;
      height: 100%;
      object-fit: cover;
      transition: transform 0.3s;
    }
    .property-card:hover .property-image img {
      transform: scale(1.05);
    }
    .property-status {
      position: absolute;
      top: 10px;
      right: 10px;
      padding: 5px 10px;
      border-radius: 4px;
      font-size: 0.8rem;
      font-weight: 600;
      color: white;
    }
    .source-badge {
      position: absolute;
      top: 10px;
      left: 10px;
      padding: 5px 10px;
      border-radius: 4px;
      font-size: 0.8rem;
      font-weight: 600;
      color: white;
      background-color: #007bff;
    }
    .active {
      background-color: #28a745;
    }
    .inactive {
      background-color: #dc3545;
    }
    .property-info {
      padding: 15px;
      flex-grow: 1;
      display: flex;
      flex-direction: column;
    }
    h3 {
      margin: 0 0 10px;
      font-size: 1.2rem;
      color: #333;
      line-height: 1.4;
      display: -webkit-box;
      -webkit-line-clamp: 2;
      -webkit-box-orient: vertical;
      overflow: hidden;
    }
    .price {
      font-size: 1.3rem;
      font-weight: 600;
      color: var(--primary-color, #007bff);
      margin: 0 0 5px;
    }
    .location {
      color: #666;
      margin: 0 0 10px;
      font-size: 0.9rem;
    }
    .property-details {
      display: flex;
      gap: 10px;
      margin-bottom: 15px;
      color: #555;
      font-size: 0.9rem;
    }
    .property-actions {
      display: flex;
      gap: 8px;
      margin-top: auto;
    }
    .view-btn, .edit-btn, .delete-btn {
      flex: 1;
      padding: 8px 0;
      text-align: center;
      border-radius: 4px;
      font-weight: 500;
      text-decoration: none;
      font-size: 0.9rem;
      cursor: pointer;
      transition: background-color 0.2s;
      border: none;
    }
    .view-btn {
      background-color: #f8f9fa;
      color: #333;
      border: 1px solid #ddd;
    }
    .view-btn:hover {
      background-color: #e9ecef;
    }
    .edit-btn {
      background-color: var(--primary-color, #007bff);
      color: white;
    }
    .edit-btn:hover {
      background-color: var(--primary-color-dark, #0056b3);
    }
    .delete-btn {
      background-color: #dc3545;
      color: white;
    }
    .delete-btn:hover {
      background-color: #c82333;
    }
  `]
})
export class OwnerPropertyCardComponent {
  @Input() property!: PropertyListingDTO;
  @Output() deleteClicked = new EventEmitter<number>();

  safeImageUrl: SafeUrl | null = null;
  readonly NO_IMAGE_FALLBACK = 'data:image/svg+xml;charset=UTF-8,%3Csvg%20width%3D%22286%22%20height%3D%22180%22%20xmlns%3D%22http%3A%2F%2Fwww.w3.org%2F2000%2Fsvg%22%20viewBox%3D%220%200%20286%20180%22%20preserveAspectRatio%3D%22none%22%3E%3Cdefs%3E%3Cstyle%20type%3D%22text%2Fcss%22%3E%23holder_189b3e01124%20text%20%7B%20fill%3A%23999%3Bfont-weight%3Anormal%3Bfont-family%3AArial%2C%20Helvetica%2C%20Open%20Sans%2C%20sans-serif%2C%20monospace%3Bfont-size%3A14pt%20%7D%20%3C%2Fstyle%3E%3C%2Fdefs%3E%3Cg%20id%3D%22holder_189b3e01124%22%3E%3Crect%20width%3D%22286%22%20height%3D%22180%22%20fill%3D%22%23373940%22%3E%3C%2Frect%3E%3Cg%3E%3Ctext%20x%3D%2298.92499923706055%22%20y%3D%2296.3%22%3ENo%20Image%3C%2Ftext%3E%3C%2Fg%3E%3C%2Fg%3E%3C%2Fsvg%3E';
  private isHandlingError = false; // Flag to prevent infinite loops

  constructor(
    private sanitizer: DomSanitizer,
    private http: HttpClient
  ) {}

  ngOnInit(): void {
    this.loadPropertyImage();
  }

  private loadPropertyImage(): void {
    if (!this.property.imageUrls || this.property.imageUrls.length === 0) {
      this.safeImageUrl = this.sanitizer.bypassSecurityTrustUrl(this.NO_IMAGE_FALLBACK);
      return;
    }

    const imageUrl = this.property.imageUrls[0];
    
    if (imageUrl.startsWith('/api/images/')) {
      // Direct image loading for owner images
      this.safeImageUrl = this.sanitizer.bypassSecurityTrustUrl(imageUrl);
    } else if (imageUrl.includes('tayara.tn') || imageUrl.includes('mubawab.tn')) {
      // Use proxy for external images
      const proxyUrl = `/api/v1/image-proxy?url=${encodeURIComponent(imageUrl)}`;
      this.safeImageUrl = this.sanitizer.bypassSecurityTrustUrl(proxyUrl);
    } else {
      // Other images load directly
      this.safeImageUrl = this.sanitizer.bypassSecurityTrustUrl(imageUrl);
    }
  }

  handleImageError(event: Event): void {
    // Prevent infinite loops by checking if we're already handling an error
    if (this.isHandlingError) {
      return;
    }
    
    this.isHandlingError = true;
    
    const imgElement = event.target as HTMLImageElement;
    
    // Try next image in array if available
    if (this.property.imageUrls && this.property.imageUrls.length > 1) {
      const currentIndex = this.property.imageUrls.indexOf(imgElement.src);
      if (currentIndex >= 0 && currentIndex < this.property.imageUrls.length - 1) {
        const nextImageUrl = this.property.imageUrls[currentIndex + 1];
        imgElement.src = nextImageUrl;
        this.isHandlingError = false;
        return;
      }
    }
    
    // If no next image available or current image not found in array, use fallback
    imgElement.src = this.NO_IMAGE_FALLBACK;
    this.isHandlingError = false;
  }

  onDeleteClick(): void {
      this.deleteClicked.emit(this.property.id);
  }
} 