import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AdminService, AdminRoommateAnnouncement, PagedResponse } from '../../services/admin.service';
import { AuthService } from '../../auth/auth.service';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-admin-roommate-announcements',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './admin-roommate-announcements.component.html',
  styleUrl: './admin-roommate-announcements.component.css'
})
export class AdminRoommateAnnouncementsComponent implements OnInit, OnDestroy {
  
  // Data
  announcements: AdminRoommateAnnouncement[] = [];
  selectedAnnouncement: AdminRoommateAnnouncement | null = null;
  announcementStats: any = {};
  
  // Pagination
  currentPage = 0;
  pageSize = 20;
  totalElements = 0;
  totalPages = 0;
  
  // Filters and search
  searchTerm = '';
  statusFilter = '';
  sortBy = 'createdAt';
  sortDir = 'desc';
  
  // UI state
  isLoading = true;
  isDeleting = false;
  isUpdatingStatus = false;
  showDeleteModal = false;
  showDetailsModal = false;
  showStatusModal = false;
  
  // Status options
  statusOptions = [
    { value: '', label: 'All Statuses' },
    { value: 'ACTIVE', label: 'Active' },
    { value: 'PAUSED', label: 'Paused' },
    { value: 'FILLED', label: 'Filled' },
    { value: 'EXPIRED', label: 'Expired' }
  ];
  
  // Utility properties
  Math = Math;
  
  private subscriptions: Subscription[] = [];
  
  constructor(
    private adminService: AdminService,
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit() {
    this.checkAdminAccess();
    this.loadAnnouncements();
    this.loadStats();
  }

  ngOnDestroy() {
    this.subscriptions.forEach(sub => sub.unsubscribe());
  }

  private checkAdminAccess() {
    const userRole = this.authService.getUserRole();
    if (userRole !== 'ADMIN') {
      console.error('Access denied: Admin role required');
      this.router.navigate(['/']);
      return;
    }
  }

  loadAnnouncements() {
    this.isLoading = true;
    
    this.subscriptions.push(
      this.adminService.getAllRoommateAnnouncements(
        this.currentPage,
        this.pageSize,
        this.sortBy,
        this.sortDir,
        this.searchTerm || undefined,
        this.statusFilter || undefined
      ).subscribe({
        next: (response: PagedResponse<AdminRoommateAnnouncement>) => {
          this.announcements = response.content;
          this.totalElements = response.totalElements;
          this.totalPages = response.totalPages;
          this.isLoading = false;
        },
        error: (error) => {
          console.error('Error loading announcements:', error);
          this.isLoading = false;
        }
      })
    );
  }

  loadStats() {
    this.subscriptions.push(
      this.adminService.getRoommateAnnouncementStats().subscribe({
        next: (stats) => {
          this.announcementStats = stats;
        },
        error: (error) => {
          console.error('Error loading announcement stats:', error);
        }
      })
    );
  }

  // Search and filter methods
  onSearch() {
    this.currentPage = 0;
    this.loadAnnouncements();
  }

  onFilterChange() {
    this.currentPage = 0;
    this.loadAnnouncements();
  }

  onSortChange(field: string) {
    if (this.sortBy === field) {
      this.sortDir = this.sortDir === 'asc' ? 'desc' : 'asc';
    } else {
      this.sortBy = field;
      this.sortDir = 'desc';
    }
    this.currentPage = 0;
    this.loadAnnouncements();
  }

  // Pagination methods
  goToPage(page: number) {
    this.currentPage = page;
    this.loadAnnouncements();
  }

  nextPage() {
    if (this.currentPage < this.totalPages - 1) {
      this.currentPage++;
      this.loadAnnouncements();
    }
  }

  previousPage() {
    if (this.currentPage > 0) {
      this.currentPage--;
      this.loadAnnouncements();
    }
  }

  // Modal methods
  viewDetails(announcement: AdminRoommateAnnouncement) {
    this.selectedAnnouncement = announcement;
    this.showDetailsModal = true;
  }

  openStatusModal(announcement: AdminRoommateAnnouncement) {
    this.selectedAnnouncement = announcement;
    this.showStatusModal = true;
  }

  openDeleteModal(announcement: AdminRoommateAnnouncement) {
    this.selectedAnnouncement = announcement;
    this.showDeleteModal = true;
  }

  closeModals() {
    this.showDetailsModal = false;
    this.showStatusModal = false;
    this.showDeleteModal = false;
    this.selectedAnnouncement = null;
  }

  // CRUD operations
  updateStatus(newStatus: string) {
    if (!this.selectedAnnouncement) return;
    
    this.isUpdatingStatus = true;
    
    this.subscriptions.push(
      this.adminService.updateRoommateAnnouncementStatus(this.selectedAnnouncement.id, newStatus).subscribe({
        next: (response) => {
          console.log('Status updated successfully:', response);
          alert('Status updated successfully!');
          this.loadAnnouncements();
          this.loadStats();
          this.closeModals();
          this.isUpdatingStatus = false;
        },
        error: (error) => {
          console.error('Error updating status:', error);
          let errorMessage = 'Error updating status';
          if (error.error && typeof error.error === 'string') {
            errorMessage = error.error;
          } else if (error.message) {
            errorMessage = error.message;
          }
          alert(errorMessage);
          this.isUpdatingStatus = false;
        }
      })
    );
  }

  deleteAnnouncement() {
    if (!this.selectedAnnouncement) return;
    
    this.isDeleting = true;
    
    this.subscriptions.push(
      this.adminService.deleteRoommateAnnouncement(this.selectedAnnouncement.id).subscribe({
        next: (response) => {
          console.log('Announcement deleted successfully:', response);
          alert('Announcement deleted successfully!');
          this.loadAnnouncements();
          this.loadStats();
          this.closeModals();
          this.isDeleting = false;
        },
        error: (error) => {
          console.error('Error deleting announcement:', error);
          let errorMessage = 'Error deleting announcement';
          if (error.error && typeof error.error === 'string') {
            errorMessage = error.error;
          } else if (error.message) {
            errorMessage = error.message;
          }
          alert(errorMessage);
          this.isDeleting = false;
        }
      })
    );
  }

  // Utility methods
  getStatusColor(status: string): string {
    switch (status?.toUpperCase()) {
      case 'ACTIVE':
        return 'text-green-600 bg-green-100';
      case 'PAUSED':
        return 'text-yellow-600 bg-yellow-100';
      case 'FILLED':
        return 'text-blue-600 bg-blue-100';
      case 'EXPIRED':
        return 'text-red-600 bg-red-100';
      default:
        return 'text-gray-600 bg-gray-100';
    }
  }

  getTypeColor(isTypeA: boolean): string {
    return isTypeA ? 'text-purple-600 bg-purple-100' : 'text-orange-600 bg-orange-100';
  }

  getTypeLabel(isTypeA: boolean): string {
    return isTypeA ? 'Property Owner' : 'Looking for Property';
  }

  formatCurrency(amount: number): string {
    return new Intl.NumberFormat('en-TN', {
      style: 'currency',
      currency: 'TND'
    }).format(amount);
  }

  formatDate(dateString: string): string {
    return new Date(dateString).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric'
    });
  }

  formatDateTime(dateString: string): string {
    return new Date(dateString).toLocaleString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  isExpiringSoon(expiresAt: string): boolean {
    const expiryDate = new Date(expiresAt);
    const now = new Date();
    const daysUntilExpiry = (expiryDate.getTime() - now.getTime()) / (1000 * 60 * 60 * 24);
    return daysUntilExpiry <= 7 && daysUntilExpiry > 0;
  }

  isExpired(expiresAt: string): boolean {
    return new Date(expiresAt) < new Date();
  }

  goBack() {
    this.router.navigate(['/admin/dashboard']);
  }
} 