import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { PropertyListingService } from '../../services/property-listing.service';
import { PropertyListingDTO } from '../../models/property-listing.dto';
import { OwnerPropertyCardComponent } from '../../components/owner-property-card/owner-property-card.component';
import { RouterModule, Router } from '@angular/router'; // For Add New Property button and navigation
import { AuthService } from '../../auth/auth.service';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-my-listings',
  standalone: true,
  imports: [CommonModule, OwnerPropertyCardComponent, RouterModule, FormsModule],
  template: `
    <div class="my-listings-container">
      <div class="header">
        <h1>My Property Listings</h1>
        <a routerLink="/owner/property/new" class="btn btn-primary">Add New Property</a>
      </div>

      <div *ngIf="isLoading" class="loading-indicator">
        <p>Loading your properties...</p>
        <div class="spinner"></div>
      </div>

      <div *ngIf="!isLoading && feedbackMessage" 
           [ngClass]="{'feedback-success': !isErrorFeedback, 'feedback-error': isErrorFeedback}"
           class="feedback-message">
        <p>{{ feedbackMessage }}</p>
      </div>

      <div *ngIf="!isLoading && !feedbackMessage && properties.length === 0" class="empty-state">
        <p>You haven't listed any properties yet.</p>
        <img src="assets/images/empty-folder.svg" alt="No properties listed" class="empty-state-image">
        <a routerLink="/owner/property/new" class="btn btn-cta">List Your First Property</a>
      </div>

      <div *ngIf="!isLoading && properties.length > 0" class="listings-container">
        <div class="filters-section">
          <div class="filters">
            <div class="filter-item">
              <label for="sort-by">Sort by:</label>
              <select id="sort-by" [(ngModel)]="sortBy" (change)="sortProperties()">
                <option value="date-desc">Newest first</option>
                <option value="date-asc">Oldest first</option>
                <option value="price-desc">Price: High to Low</option>
                <option value="price-asc">Price: Low to High</option>
              </select>
            </div>
            <div class="filter-item">
              <label for="status-filter">Status:</label>
              <select id="status-filter" [(ngModel)]="statusFilter" (change)="filterProperties()">
                <option value="all">All</option>
                <option value="active">Active</option>
                <option value="inactive">Inactive</option>
              </select>
            </div>
          </div>
          <div class="count-badge">
            Showing {{ filteredProperties.length }} of {{ properties.length }} properties
          </div>
        </div>

        <div class="listings-grid">
          <app-owner-property-card 
            *ngFor="let prop of filteredProperties" 
            [property]="prop"
            (deleteClicked)="handleDeleteProperty($event)">
          </app-owner-property-card>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .my-listings-container {
      padding: 20px;
      max-width: 1200px;
      margin: auto;
    }
    .header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 20px;
      border-bottom: 1px solid #eee;
      padding-bottom: 15px;
    }
    .header h1 {
      font-size: 2rem;
      color: #333;
      margin: 0;
    }
    .loading-indicator {
      text-align: center;
      margin-top: 40px;
      padding: 20px;
    }
    .spinner {
      width: 40px;
      height: 40px;
      margin: 20px auto;
      border: 4px solid rgba(0, 0, 0, 0.1);
      border-left-color: #007bff;
      border-radius: 50%;
      animation: spin 1s linear infinite;
    }
    @keyframes spin {
      to { transform: rotate(360deg); }
    }
    .feedback-message {
      padding: 15px;
      margin-bottom: 20px;
      border-radius: 5px;
      text-align: center;
      animation: fadeIn 0.3s ease-in-out;
    }
    @keyframes fadeIn {
      from { opacity: 0; transform: translateY(-10px); }
      to { opacity: 1; transform: translateY(0); }
    }
    .feedback-success {
      background-color: #d4edda;
      color: #155724;
      border: 1px solid #c3e6cb;
    }
    .feedback-error {
      background-color: #f8d7da;
      color: #721c24;
      border: 1px solid #f5c6cb;
    }
    .empty-state {
      text-align: center;
      margin-top: 40px;
      padding: 30px;
      background-color: #f9f9f9;
      border-radius: 8px;
    }
    .empty-state p {
      font-size: 1.2rem;
      margin-bottom: 20px;
      color: #555;
    }
    .empty-state-image {
      width: 150px; 
      height: auto;
      margin-bottom: 20px;
      opacity: 0.7;
    }
    .listings-container {
      margin-top: 20px;
    }
    .filters-section {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 15px;
      padding: 10px 0;
    }
    .filters {
      display: flex;
      gap: 15px;
    }
    .filter-item {
      display: flex;
      align-items: center;
      gap: 8px;
    }
    .filter-item label {
      font-weight: 500;
      color: #555;
    }
    .filter-item select {
      padding: 8px 10px;
      border-radius: 4px;
      border: 1px solid #ddd;
      background-color: white;
      font-size: 0.9rem;
    }
    .count-badge {
      background-color: #e9ecef;
      padding: 6px 12px;
      border-radius: 20px;
      font-size: 0.85rem;
      color: #495057;
    }
    .listings-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
      gap: 20px;
    }
    .btn {
      padding: 10px 20px;
      border-radius: 5px;
      text-decoration: none;
      font-weight: 500;
      transition: background-color 0.3s ease;
    }
    .btn-primary {
      background-color: var(--primary-color, #007bff);
      color: white;
    }
    .btn-primary:hover {
      background-color: var(--primary-color-dark, #0056b3);
    }
    .btn-cta {
      background-color: var(--accent-color, #28a745);
      color: white;
      font-size: 1.1rem;
      padding: 12px 24px;
    }
    .btn-cta:hover {
      background-color: var(--accent-color-dark, #1e7e34);
    }
    
    @media (max-width: 768px) {
      .header {
        flex-direction: column;
        align-items: flex-start;
        gap: 10px;
      }
      .header h1 {
        font-size: 1.8rem;
      }
      .filters-section {
        flex-direction: column;
        align-items: flex-start;
        gap: 15px;
      }
      .filters {
        flex-direction: column;
        width: 100%;
      }
      .filter-item {
        width: 100%;
      }
      .filter-item select {
        flex-grow: 1;
      }
      .count-badge {
        align-self: flex-end;
      }
    }
  `]
})
export class MyListingsComponent implements OnInit {
  properties: PropertyListingDTO[] = [];
  filteredProperties: PropertyListingDTO[] = [];
  isLoading = true;
  feedbackMessage: string | null = null;
  isErrorFeedback = false;
  
  // Sorting and filtering options
  sortBy: string = 'date-desc';
  statusFilter: string = 'all';

  constructor(
    private propertyService: PropertyListingService, 
    private authService: AuthService,
    private router: Router
  ) { }

  ngOnInit(): void {
    if (!this.authService.isLoggedIn()) {
        this.setFeedback("You must be logged in to view your properties.", true);
        this.isLoading = false;
        setTimeout(() => this.router.navigate(['/auth/login']), 2000);
        return;
    }
    this.loadProperties();
  }

  loadProperties(): void {
    this.isLoading = true;
    this.feedbackMessage = null;
    this.propertyService.getOwnerProperties().subscribe({
      next: (data) => {
        this.properties = data;
        this.applyFiltersAndSort();
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Error fetching owner properties:', err);
        this.setFeedback('Failed to load your properties. Please try again later.', true);
        this.isLoading = false;
      }
    });
  }

  handleDeleteProperty(propertyId: number): void {
    if (confirm('Are you sure you want to delete this property? This action cannot be undone.')) {
      this.setFeedback('Deleting property...', false);
      this.propertyService.deleteProperty(propertyId).subscribe({
        next: () => {
          this.properties = this.properties.filter(p => p.id !== propertyId);
          this.applyFiltersAndSort();
          this.setFeedback('Property deleted successfully.', false);
        },
        error: (err) => {
          console.error('Error deleting property:', err);
          this.setFeedback('Failed to delete property. Please try again.', true);
        }
      });
    }
  }

  sortProperties(): void {
    this.applyFiltersAndSort();
  }

  filterProperties(): void {
    this.applyFiltersAndSort();
  }

  private applyFiltersAndSort(): void {
    // First apply status filter
    let result = [...this.properties];
    
    if (this.statusFilter !== 'all') {
      const isActive = this.statusFilter === 'active';
      result = result.filter(p => p.active === isActive);
    }
    
    // Then sort the filtered results
    switch (this.sortBy) {
      case 'date-desc':
        result.sort((a, b) => new Date(b.listingDate).getTime() - new Date(a.listingDate).getTime());
        break;
      case 'date-asc':
        result.sort((a, b) => new Date(a.listingDate).getTime() - new Date(b.listingDate).getTime());
        break;
      case 'price-desc':
        result.sort((a, b) => b.price - a.price);
        break;
      case 'price-asc':
        result.sort((a, b) => a.price - b.price);
        break;
    }
    
    this.filteredProperties = result;
  }

  private setFeedback(message: string, isError: boolean): void {
    this.feedbackMessage = message;
    this.isErrorFeedback = isError;
    
    if (!isError) {
      // Clear success feedback after some time
      setTimeout(() => {
        if (this.feedbackMessage === message) {
          this.feedbackMessage = null;
        }
      }, 5000); 
    }
  }
} 