import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AdminService, AdminProperty, PagedResponse } from '../../services/admin.service';
import { Subscription } from 'rxjs';
import { Observable } from 'rxjs';

@Component({
  selector: 'app-admin-properties',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './admin-properties.component.html',
  styleUrl: './admin-properties.component.css'
})
export class AdminPropertiesComponent implements OnInit, OnDestroy {
  
  properties: AdminProperty[] = [];
  filteredProperties: AdminProperty[] = [];
  isLoading = true;
  
  // Filters and search
  searchTerm = '';
  selectedStatus = '';
  selectedType = '';
  
  // Pagination
  currentPage = 0;
  pageSize = 10;
  totalElements = 0;
  totalPages = 0;
  
  // Edit modal
  showEditModal = false;
  editingProperty: AdminProperty | null = null;
  newStatus = true;
  
  // Make Math available in template
  Math = Math;
  
  private subscriptions: Subscription[] = [];
  
  constructor(
    private adminService: AdminService,
    private router: Router
  ) {}

  ngOnInit() {
    this.loadProperties();
  }

  ngOnDestroy() {
    this.subscriptions.forEach(sub => sub.unsubscribe());
  }

  loadProperties() {
    this.isLoading = true;
    
    this.subscriptions.push(
      this.adminService.getAllProperties(
        this.currentPage, 
        this.pageSize, 
        'createdAt', 
        'desc',
        this.searchTerm || undefined,
        this.selectedStatus || undefined
      ).subscribe({
        next: (response) => {
          this.properties = response.content;
          this.filteredProperties = response.content;
          this.totalElements = response.totalElements;
          this.totalPages = response.totalPages;
          this.isLoading = false;
        },
        error: (error) => {
          console.error('Error loading properties:', error);
          this.isLoading = false;
        }
      })
    );
  }

  onSearch() {
    this.currentPage = 0;
    this.loadProperties();
  }

  onStatusFilter() {
    this.currentPage = 0;
    this.loadProperties();
  }

  onTypeFilter() {
    if (this.selectedType === '') {
      this.filteredProperties = this.properties;
    } else {
      this.filteredProperties = this.properties.filter(property => 
        property.propertyType === this.selectedType
      );
    }
  }

  nextPage() {
    if (this.currentPage < this.totalPages - 1) {
      this.currentPage++;
      this.loadProperties();
    }
  }

  previousPage() {
    if (this.currentPage > 0) {
      this.currentPage--;
      this.loadProperties();
    }
  }

  goToPage(page: number) {
    this.currentPage = page;
    this.loadProperties();
  }

  editProperty(property: AdminProperty) {
    this.editingProperty = { ...property };
    this.newStatus = property.active;
    this.showEditModal = true;
  }

  savePropertyChanges() {
    if (!this.editingProperty) return;

    // Update status if changed
    if (this.newStatus !== this.editingProperty.active) {
      const status = this.newStatus ? 'ACTIVE' : 'INACTIVE';
      
      this.subscriptions.push(
        this.adminService.updatePropertyStatus(this.editingProperty.id, status).subscribe({
          next: () => {
            this.showEditModal = false;
            this.editingProperty = null;
            this.loadProperties();
            alert('Property updated successfully!');
          },
          error: (error) => {
            console.error('Error updating property:', error);
            alert('Error updating property: ' + error.message);
          }
        })
      );
    } else {
      this.showEditModal = false;
      this.editingProperty = null;
    }
  }

  deleteProperty(property: AdminProperty) {
    if (confirm(`Are you sure you want to delete property "${property.title}"? This action cannot be undone.`)) {
      this.subscriptions.push(
        this.adminService.deleteProperty(property.id).subscribe({
          next: () => {
            this.loadProperties();
            alert('Property deleted successfully!');
          },
          error: (error) => {
            console.error('Error deleting property:', error);
            alert('Error deleting property: ' + error.message);
          }
        })
      );
    }
  }

  closeModal() {
    this.showEditModal = false;
    this.editingProperty = null;
  }

  goBack() {
    this.router.navigate(['/admin/dashboard']);
  }

  viewProperty(property: AdminProperty) {
    this.router.navigate(['/property', property.id]);
  }

  getStatusColor(active: boolean): string {
    return active ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800';
  }

  getTypeColor(type: string): string {
    switch (type?.toUpperCase()) {
      case 'APARTMENT': return 'bg-blue-100 text-blue-800';
      case 'HOUSE': return 'bg-green-100 text-green-800';
      case 'STUDIO': return 'bg-purple-100 text-purple-800';
      case 'VILLA': return 'bg-yellow-100 text-yellow-800';
      default: return 'bg-gray-100 text-gray-800';
    }
  }

  formatPrice(price: number): string {
    return new Intl.NumberFormat('en-TN', {
      style: 'currency',
      currency: 'TND',
      minimumFractionDigits: 0
    }).format(price);
  }

  formatDate(date: string): string {
    return new Date(date).toLocaleDateString();
  }

  truncateText(text: string, maxLength: number = 50): string {
    if (!text) return '';
    return text.length > maxLength ? text.substring(0, maxLength) + '...' : text;
  }
} 