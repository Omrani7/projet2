import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { InquiryService } from '../../services/inquiry.service';
import { WebSocketService } from '../../services/websocket.service';
import { AuthService } from '../../auth/auth.service';
import { Inquiry, Page, WebSocketNotification, InquiryReply } from '../../models/inquiry.model';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-owner-inquiries',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './owner-inquiries.component.html',
  styleUrls: ['./owner-inquiries.component.css']
})
export class OwnerInquiriesComponent implements OnInit, OnDestroy {
  inquiries: Inquiry[] = [];
  currentPage = 0;
  pageSize = 10;
  totalElements = 0;
  totalPages = 0;
  isLoading = false;
  error: string | null = null;
  unreadCount = 0;

  // Reply form state
  replyForms: { [inquiryId: number]: FormGroup } = {};
  replyingTo: number | null = null;
  isSubmittingReply = false;

  private notificationSubscription?: Subscription;

  constructor(
    private inquiryService: InquiryService,
    private webSocketService: WebSocketService,
    private authService: AuthService,
    private fb: FormBuilder
  ) {}

  ngOnInit(): void {
    this.loadInquiries();
    this.loadUnreadCount();
    this.setupWebSocketNotifications();
  }

  ngOnDestroy(): void {
    if (this.notificationSubscription) {
      this.notificationSubscription.unsubscribe();
    }
  }

  /**
   * Load inquiries for the current owner
   */
  loadInquiries(): void {
    if (!this.authService.isLoggedIn()) {
      this.error = 'You must be logged in to view inquiries';
      return;
    }

    this.isLoading = true;
    this.error = null;

    this.inquiryService.getOwnerInquiries(this.currentPage, this.pageSize).subscribe({
      next: (response: Page<Inquiry>) => {
        this.inquiries = response.content;
        this.totalElements = response.totalElements;
        this.totalPages = response.totalPages;
        this.isLoading = false;
        
        // Initialize reply forms for inquiries that don't have replies
        this.inquiries.forEach(inquiry => {
          if (!inquiry.reply && !this.replyForms[inquiry.id]) {
            this.replyForms[inquiry.id] = this.fb.group({
              reply: ['', [Validators.required, Validators.minLength(10), Validators.maxLength(1000)]]
            });
          }
        });
      },
      error: (error) => {
        console.error('Error loading inquiries:', error);
        this.isLoading = false;
        
        if (error.status === 401 || error.status === 403) {
          this.error = 'You do not have permission to view inquiries';
        } else {
          this.error = 'Failed to load inquiries. Please try again.';
        }
      }
    });
  }

  /**
   * Load unread inquiry count
   */
  loadUnreadCount(): void {
    this.inquiryService.getUnreadInquiryCount().subscribe({
      next: (response) => {
        this.unreadCount = response.unreadCount;
      },
      error: (error) => {
        console.error('Error loading unread count:', error);
      }
    });
  }

  /**
   * Setup WebSocket notifications for real-time updates
   */
  private setupWebSocketNotifications(): void {
    // Connect to WebSocket if not already connected
    if (!this.webSocketService.isConnected()) {
      this.webSocketService.connect();
    }

    // Subscribe to notifications    this.notificationSubscription = this.webSocketService.notifications$.subscribe({      next: (notification: WebSocketNotification) => {        if (notification.type === 'NEW_INQUIRY' && notification.inquiry) {          // Add new inquiry to the list          this.inquiries.unshift(notification.inquiry);          this.totalElements++;          this.unreadCount++;                    // Initialize reply form for new inquiry          this.replyForms[notification.inquiry.id] = this.fb.group({            reply: ['', [Validators.required, Validators.minLength(10), Validators.maxLength(1000)]]          });                    // Show notification          this.showNotification('New inquiry received!');        } else if (notification.type === 'NEW_ROOMMATE_APPLICATION') {          // Handle roommate application notifications for owners who also post roommate announcements          console.log('New roommate application notification received', notification);          this.showNotification('New roommate application received!');        }      },      error: (error) => {        console.error('WebSocket notification error:', error);      }    });
  }

  /**
   * Show a notification to the user
   */
  private showNotification(message: string): void {
    console.log('Notification:', message);
    // You can integrate with a toast service here
  }

  /**
   * Start replying to an inquiry
   */
  startReply(inquiryId: number): void {
    this.replyingTo = inquiryId;
  }

  /**
   * Cancel reply
   */
  cancelReply(): void {
    this.replyingTo = null;
  }

  /**
   * Submit reply to inquiry
   */
  submitReply(inquiry: Inquiry): void {
    const form = this.replyForms[inquiry.id];
    if (!form || form.invalid || this.isSubmittingReply) {
      return;
    }

    this.isSubmittingReply = true;
    const replyData: InquiryReply = {
      reply: form.get('reply')?.value
    };

    this.inquiryService.replyToInquiry(inquiry.id, replyData).subscribe({
      next: (updatedInquiry) => {
        // Update the inquiry in the list
        const index = this.inquiries.findIndex(inq => inq.id === inquiry.id);
        if (index !== -1) {
          this.inquiries[index] = updatedInquiry;
        }
        
        this.isSubmittingReply = false;
        this.replyingTo = null;
        
        // Update unread count
        if (this.unreadCount > 0) {
          this.unreadCount--;
        }
        
        console.log('Reply sent successfully');
      },
      error: (error) => {
        console.error('Error sending reply:', error);
        this.isSubmittingReply = false;
      }
    });
  }

  /**
   * Update inquiry status
   */
  updateStatus(inquiryId: number, status: string): void {
    if (!confirm(`Are you sure you want to mark this inquiry as ${status.toLowerCase()}?`)) {
      return;
    }

    this.inquiryService.updateInquiryStatus(inquiryId, status).subscribe({
      next: (updatedInquiry) => {
        // Update the inquiry in the list
        const index = this.inquiries.findIndex(i => i.id === inquiryId);
        if (index !== -1) {
          this.inquiries[index] = updatedInquiry;
        }
        this.showNotification(`Inquiry marked as ${status.toLowerCase()}`);
      },
      error: (error) => {
        console.error('Error updating inquiry status:', error);
        this.error = 'Failed to update inquiry status. Please try again.';
      }
    });
  }

  /**
   * Close deal with a specific student (NEW ENHANCED FEATURE)
   * This will automatically notify all other students that the property is no longer available
   */
  closeDealWithStudent(inquiryId: number): void {
    const inquiry = this.inquiries.find(i => i.id === inquiryId);
    if (!inquiry) {
      this.error = 'Inquiry not found';
      return;
    }

    const confirmMessage = `Are you sure you want to close the deal with ${inquiry.student.username} for "${inquiry.property.title}"?\n\n` +
                          `This will:\n` +
                          `• Mark this inquiry as CLOSED (successful deal)\n` +
                          `• Automatically notify all other students that the property is no longer available\n` +
                          `• Remove the property from search results\n\n` +
                          `This action cannot be undone.`;
    
    if (!confirm(confirmMessage)) {
      return;
    }

    this.inquiryService.closeDealWithStudent(inquiryId).subscribe({
      next: (updatedInquiry) => {
        // Update the inquiry in the list
        const index = this.inquiries.findIndex(i => i.id === inquiryId);
        if (index !== -1) {
          this.inquiries[index] = updatedInquiry;
        }
        
        this.showNotification(`🎉 Deal closed successfully with ${inquiry.student.username}! Other students have been notified.`);
        
        // Refresh the list to see updated statuses for other inquiries
        this.refresh();
      },
      error: (error) => {
        console.error('Error closing deal:', error);
        this.error = 'Failed to close deal. Please try again.';
      }
    });
  }

  /**
   * Go to next page
   */
  nextPage(): void {
    if (this.currentPage < this.totalPages - 1) {
      this.currentPage++;
      this.loadInquiries();
    }
  }

  /**
   * Go to previous page
   */
  previousPage(): void {
    if (this.currentPage > 0) {
      this.currentPage--;
      this.loadInquiries();
    }
  }

  /**
   * Go to specific page
   */
  goToPage(page: number): void {
    if (page >= 0 && page < this.totalPages) {
      this.currentPage = page;
      this.loadInquiries();
    }
  }

  /**
   * Refresh inquiries
   */
  refresh(): void {
    this.currentPage = 0;
    this.loadInquiries();
    this.loadUnreadCount();
  }

  /**
   * Format date for display
   */
  formatDate(dateString: string): string {
    const date = new Date(dateString);
    return date.toLocaleDateString() + ' ' + date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
  }

  /**
   * Get display text for inquiry status
   */
  getStatusText(status: string): string {
    switch (status) {
      case 'PENDING_REPLY': return 'Pending Reply';
      case 'REPLIED': return 'Replied';
      case 'CLOSED': return 'Closed';
      case 'PROPERTY_NO_LONGER_AVAILABLE': return 'Property Unavailable';
      default: return status;
    }
  }

  /**
   * Get CSS class for status badge
   */
  getStatusBadgeClass(status: string): string {
    switch (status) {
      case 'PENDING_REPLY': return 'status-pending';
      case 'REPLIED': return 'status-replied';
      case 'CLOSED': return 'status-closed';
      case 'PROPERTY_NO_LONGER_AVAILABLE': return 'status-unavailable';
      default: return 'status-pending';
    }
  }

  /**
   * Get page numbers for pagination
   */
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

  /**
   * TrackBy function for inquiry list optimization
   */
  trackByInquiryId(index: number, inquiry: Inquiry): number {
    return inquiry.id;
  }

  /**
   * Get reply form control
   */
  getReplyControl(inquiryId: number) {
    return this.replyForms[inquiryId]?.get('reply');
  }
} 