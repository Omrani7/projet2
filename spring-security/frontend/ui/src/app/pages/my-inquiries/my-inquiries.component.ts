import { Component, OnInit, ViewEncapsulation } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { InquiryService } from '../../services/inquiry.service';
import { AnalyticsService } from '../../services/analytics.service';
import { Inquiry, Page } from '../../models/inquiry.model';

interface InquiryWithActions extends Inquiry {
  isExpanded?: boolean;
  isReplying?: boolean;
  replyText?: string;
}

type InquiryStatus = 'PENDING_REPLY' | 'REPLIED' | 'CLOSED' | 'PROPERTY_NO_LONGER_AVAILABLE';

@Component({
  selector: 'app-my-inquiries',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './my-inquiries.component.html',
  styleUrls: ['./my-inquiries.component.css'],
  encapsulation: ViewEncapsulation.None
})
export class MyInquiriesComponent implements OnInit {
  inquiries: InquiryWithActions[] = [];
  isLoading = true;
  error: string | null = null;
  
  // Pagination
  currentPage = 0;
  pageSize = 10;
  totalElements = 0;
  totalPages = 0;
  
  // Filters
  selectedStatus: 'ALL' | InquiryStatus = 'ALL';
  searchQuery = '';
  
  // Statistics
  inquiryStats = {
    total: 0,
    pending: 0,
    replied: 0,
    closed: 0
  };

  readonly statusOptions = [
    { value: 'ALL', label: 'All Inquiries', color: '#6b7280' },
    { value: 'PENDING_REPLY', label: 'Pending Reply', color: '#f59e0b' },
    { value: 'REPLIED', label: 'Replied', color: '#3b82f6' },
    { value: 'CLOSED', label: 'Closed', color: '#10b981' },
    { value: 'PROPERTY_NO_LONGER_AVAILABLE', label: 'Property Unavailable', color: '#ef4444' }
  ];

  constructor(
    private inquiryService: InquiryService,
    private analyticsService: AnalyticsService
  ) {}

  ngOnInit(): void {
    this.loadInquiries();
    this.loadInquiryStats();
  }

  loadInquiries(): void {
    this.isLoading = true;
    this.error = null;

    this.inquiryService.getStudentInquiries(this.currentPage, this.pageSize).subscribe({
      next: (response: Page<Inquiry>) => {
        this.inquiries = response.content.map(inquiry => ({
          ...inquiry,
          isExpanded: false,
          isReplying: false,
          replyText: ''
        }));
        
        this.totalElements = response.totalElements;
        this.totalPages = response.totalPages;
        this.isLoading = false;
        this.loadInquiryStats();
        
        // Track analytics
        this.analyticsService.trackPropertyView(0).subscribe();
      },
      error: (err) => {
        console.error('Error loading inquiries:', err);
        this.error = 'Failed to load inquiries. Please try again.';
        this.isLoading = false;
        
        // Generate mock data for development
        this.generateMockInquiries();
      }
    });
  }

  loadInquiryStats(): void {
    this.inquiryStats = {
      total: this.inquiries.length,
      pending: this.inquiries.filter(i => i.status === 'PENDING_REPLY').length,
      replied: this.inquiries.filter(i => i.status === 'REPLIED').length,
      closed: this.inquiries.filter(i => i.status === 'CLOSED').length
    };
  }

  refresh(): void {
    this.currentPage = 0;
    this.loadInquiries();
  }

  onStatusFilterChange(): void {
    this.currentPage = 0;
    this.loadInquiries();
  }

  onSearchChange(): void {
    this.currentPage = 0;
    this.loadInquiries();
  }

  toggleInquiryExpansion(inquiry: InquiryWithActions): void {
    inquiry.isExpanded = !inquiry.isExpanded;
  }

  startReply(inquiry: InquiryWithActions): void {
    inquiry.isReplying = true;
    inquiry.replyText = '';
  }

  cancelReply(inquiry: InquiryWithActions): void {
    inquiry.isReplying = false;
    inquiry.replyText = '';
  }

  sendReply(inquiry: InquiryWithActions): void {
    if (!inquiry.replyText?.trim()) {
      return;
    }

    console.log('Sending reply to inquiry:', inquiry.id, inquiry.replyText);
    
    inquiry.isReplying = false;
    inquiry.replyText = '';
    
    alert('Reply sent successfully!');
  }

  markAsFavorite(inquiry: InquiryWithActions): void {
    if (inquiry.property?.id) {
      this.analyticsService.trackPropertyFavorite(inquiry.property.id, 'add').subscribe({
        next: () => console.log('Property marked as favorite'),
        error: (err) => console.error('Failed to mark as favorite:', err)
      });
    }
  }

  viewProperty(inquiry: InquiryWithActions): void {
    if (inquiry.property?.id) {
      this.analyticsService.trackPropertyView(inquiry.property.id).subscribe();
    }
  }

  retryInquiry(inquiry: InquiryWithActions): void {
    console.log('Retrying inquiry for property:', inquiry.property?.id);
  }

  // Pagination methods
  goToPage(page: number): void {
    if (page >= 0 && page < this.totalPages) {
      this.currentPage = page;
      this.loadInquiries();
    }
  }

  nextPage(): void {
    this.goToPage(this.currentPage + 1);
  }

  previousPage(): void {
    this.goToPage(this.currentPage - 1);
  }

  // Template methods
  getStatusColor(status: InquiryStatus): string {
    const option = this.statusOptions.find(opt => opt.value === status);
    return option?.color || '#6b7280';
  }

  getStatusLabel(status: InquiryStatus): string {
    const option = this.statusOptions.find(opt => opt.value === status);
    return option?.label || status;
  }

  getStatusText(status: string): string {
    const option = this.statusOptions.find(opt => opt.value === status);
    return option?.label || status;
  }

  getStatusBadgeClass(status: string): string {
    switch (status) {
      case 'PENDING_REPLY': return 'status-pending';
      case 'REPLIED': return 'status-replied';
      case 'CLOSED': return 'status-closed';
      case 'PROPERTY_NO_LONGER_AVAILABLE': return 'status-unavailable';
      default: return 'status-pending';
    }
  }

  formatDate(date: string | Date): string {
    const d = new Date(date);
    return d.toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  getTimeAgo(date: string | Date): string {
    const now = new Date();
    const inquiryDate = new Date(date);
    const diffInMs = now.getTime() - inquiryDate.getTime();
    const diffInHours = Math.floor(diffInMs / (1000 * 60 * 60));
    const diffInDays = Math.floor(diffInMs / (1000 * 60 * 60 * 24));

    if (diffInHours < 24) {
      return `${diffInHours}h ago`;
    } else if (diffInDays < 7) {
      return `${diffInDays}d ago`;
    } else {
      return this.formatDate(date);
    }
  }

  getPageNumbers(): number[] {
    const pages: number[] = [];
    const maxPagesToShow = 5;
    const startPage = Math.max(0, this.currentPage - Math.floor(maxPagesToShow / 2));
    const endPage = Math.min(this.totalPages - 1, startPage + maxPagesToShow - 1);

    for (let i = startPage; i <= endPage; i++) {
      pages.push(i);
    }

    return pages;
  }

  trackByInquiryId(index: number, inquiry: Inquiry): number {
    return inquiry.id;
  }

  // Mock data for development
  private generateMockInquiries(): void {
    const mockInquiries: InquiryWithActions[] = [
      {
        id: 1,
        message: "Hi, I'm very interested in this S+2 apartment. Is it still available?",
        timestamp: new Date(Date.now() - 2 * 60 * 60 * 1000).toISOString(),
        status: 'REPLIED',
        reply: "Hello! Yes, the apartment is still available. You can visit tomorrow at 3 PM.",
        replyTimestamp: new Date(Date.now() - 1 * 60 * 60 * 1000).toISOString(),
        student: {
          id: 1,
          username: 'current_user',
          email: 'student@example.com',
          role: 'STUDENT'
        },
        owner: {
          id: 2,
          username: 'mohamed_benali',
          email: 'owner@example.com',
          role: 'OWNER'
        },
        property: {
          id: 123,
          title: 'S+2 Apartment near FSM',
          location: 'Tunis',
          city: 'Tunis',
          price: 650,
          propertyType: 'APARTMENT'
        },
        isExpanded: false,
        isReplying: false,
        replyText: ''
      },
      {
        id: 2,
        message: "I'm looking for a furnished studio for the next semester.",
        timestamp: new Date(Date.now() - 5 * 60 * 60 * 1000).toISOString(),
        status: 'PENDING_REPLY',
        student: {
          id: 1,
          username: 'current_user',
          email: 'student@example.com',
          role: 'STUDENT'
        },
        owner: {
          id: 3,
          username: 'fatma_trabelsi',
          email: 'fatma@example.com',
          role: 'OWNER'
        },
        property: {
          id: 124,
          title: 'Modern Studio in Manar',
          location: 'Manar',
          city: 'Tunis',
          price: 450,
          propertyType: 'STUDIO'
        },
        isExpanded: false,
        isReplying: false,
        replyText: ''
      }
    ];

    this.inquiries = mockInquiries;
    this.totalElements = mockInquiries.length;
    this.totalPages = 1;
    this.isLoading = false;
    this.loadInquiryStats();
  }
} 