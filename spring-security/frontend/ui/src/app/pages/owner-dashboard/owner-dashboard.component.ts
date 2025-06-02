import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { InquiryService } from '../../services/inquiry.service';
import { AuthService } from '../../auth/auth.service';
import { WebSocketService } from '../../services/websocket.service';
import { Inquiry, WebSocketNotification } from '../../models/inquiry.model';
import { Subscription } from 'rxjs';

interface DashboardStats {
  totalInquiries: number;
  pendingReplies: number;
  repliedInquiries: number;
  closedDeals: number;
  totalProperties: number;
  activeProperties: number;
  totalRevenue: number;
  monthlyRevenue: number;
}

interface RecentActivity {
  type: 'inquiry' | 'reply' | 'deal_closed';
  title: string;
  description: string;
  timestamp: Date;
  status: 'success' | 'info' | 'warning';
}

@Component({
  selector: 'app-owner-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './owner-dashboard.component.html',
  styleUrls: ['./owner-dashboard.component.css']
})
export class OwnerDashboardComponent implements OnInit, OnDestroy {
  
  // Dashboard data
  stats: DashboardStats = {
    totalInquiries: 0,
    pendingReplies: 0,
    repliedInquiries: 0,
    closedDeals: 0,
    totalProperties: 0,
    activeProperties: 0,
    totalRevenue: 0,
    monthlyRevenue: 0
  };

  recentActivities: RecentActivity[] = [];
  recentInquiries: Inquiry[] = [];
  
  // Loading states
  isLoading = true;
  error: string | null = null;
  
  // Subscriptions
  private notificationSubscription?: Subscription;

  constructor(
    private inquiryService: InquiryService,
    private authService: AuthService,
    private webSocketService: WebSocketService
  ) {}

  ngOnInit(): void {
    this.loadDashboardData();
    this.setupWebSocketNotifications();
  }

  ngOnDestroy(): void {
    if (this.notificationSubscription) {
      this.notificationSubscription.unsubscribe();
    }
  }

  /**
   * Load all dashboard data
   */
  private loadDashboardData(): void {
    this.isLoading = true;
    this.error = null;

    // Load inquiry statistics
    this.loadInquiryStats();
    
    // Load recent inquiries
    this.loadRecentInquiries();
    
    // Load recent activities
    this.loadRecentActivities();
    
    this.isLoading = false;
  }

  /**
   * Load inquiry statistics
   */
  private loadInquiryStats(): void {
    this.inquiryService.getOwnerInquiries(0, 100).subscribe({
      next: (response) => {
        const inquiries = response.content;
        
        this.stats.totalInquiries = inquiries.length;
        this.stats.pendingReplies = inquiries.filter(i => i.status === 'PENDING_REPLY').length;
        this.stats.repliedInquiries = inquiries.filter(i => i.status === 'REPLIED').length;
        this.stats.closedDeals = inquiries.filter(i => i.status === 'CLOSED').length;
        
        // Calculate estimated revenue (this is mock data - you'd get this from property service)
        this.stats.totalRevenue = this.stats.closedDeals * 800; // Average rent
        this.stats.monthlyRevenue = this.calculateMonthlyRevenue(inquiries);
        
        // Mock property data (you'd get this from property service)
        this.stats.totalProperties = 12;
        this.stats.activeProperties = 8;
      },
      error: (error) => {
        console.error('Error loading inquiry stats:', error);
        this.error = 'Failed to load dashboard statistics';
      }
    });
  }

  /**
   * Load recent inquiries for quick view
   */
  private loadRecentInquiries(): void {
    this.inquiryService.getOwnerInquiries(0, 5).subscribe({
      next: (response) => {
        this.recentInquiries = response.content;
      },
      error: (error) => {
        console.error('Error loading recent inquiries:', error);
      }
    });
  }

  /**
   * Load recent activities
   */
  private loadRecentActivities(): void {
    // This would typically come from a dedicated activity service
    // For now, we'll generate some mock data based on recent inquiries
    this.recentActivities = [
      {
        type: 'inquiry',
        title: 'New Inquiry Received',
        description: 'Ahmed Ben Ali inquired about your property in Tunis',
        timestamp: new Date(Date.now() - 2 * 60 * 60 * 1000), // 2 hours ago
        status: 'info'
      },
      {
        type: 'deal_closed',
        title: 'Deal Closed Successfully',
        description: 'Rental agreement finalized for Apartment in Sfax',
        timestamp: new Date(Date.now() - 4 * 60 * 60 * 1000), // 4 hours ago
        status: 'success'
      },
      {
        type: 'reply',
        title: 'Reply Sent',
        description: 'You replied to Sarah Mohamed\'s inquiry',
        timestamp: new Date(Date.now() - 6 * 60 * 60 * 1000), // 6 hours ago
        status: 'info'
      }
    ];
  }

  /**
   * Calculate monthly revenue from closed deals
   */
  private calculateMonthlyRevenue(inquiries: Inquiry[]): number {
    const currentMonth = new Date().getMonth();
    const currentYear = new Date().getFullYear();
    
    const monthlyDeals = inquiries.filter(inquiry => {
      if (inquiry.status !== 'CLOSED' || !inquiry.replyTimestamp) return false;
      
      const dealDate = new Date(inquiry.replyTimestamp);
      return dealDate.getMonth() === currentMonth && dealDate.getFullYear() === currentYear;
    });
    
    return monthlyDeals.length * 800; // Average rent
  }

  /**
   * Setup WebSocket notifications
   */
  private setupWebSocketNotifications(): void {
    if (!this.webSocketService.isConnected()) {
      this.webSocketService.connect();
    }

    this.notificationSubscription = this.webSocketService.notifications$.subscribe({
      next: (notification: WebSocketNotification) => {
        if (notification.type === 'NEW_INQUIRY' && notification.inquiry) {
          // Update stats
          this.stats.totalInquiries++;
          this.stats.pendingReplies++;
          
          // Add to recent inquiries
          this.recentInquiries.unshift(notification.inquiry);
          if (this.recentInquiries.length > 5) {
            this.recentInquiries.pop();
          }
          
          // Add to recent activities
          this.recentActivities.unshift({
            type: 'inquiry',
            title: 'New Inquiry Received',
            description: `${notification.inquiry.student.username} inquired about ${notification.inquiry.property.title}`,
            timestamp: new Date(),
            status: 'info'
          });
          
          if (this.recentActivities.length > 10) {
            this.recentActivities.pop();
          }
        } else if (notification.type === 'NEW_ROOMMATE_APPLICATION') {
          // Handle roommate application notifications
          console.log('New roommate application notification received', notification);
          // Could add roommate-specific stats or activities here
        }
      },
      error: (error) => {
        console.error('WebSocket notification error:', error);
      }
    });
  }

  /**
   * Get status badge class for inquiries
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
   * Format date for display
   */
  formatDate(dateString: string): string {
    const date = new Date(dateString);
    return date.toLocaleDateString() + ' ' + date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
  }

  /**
   * Format time ago
   */
  formatTimeAgo(date: Date): string {
    const now = new Date();
    const diffInMinutes = Math.floor((now.getTime() - date.getTime()) / (1000 * 60));
    
    if (diffInMinutes < 60) {
      return `${diffInMinutes}m ago`;
    } else if (diffInMinutes < 1440) {
      return `${Math.floor(diffInMinutes / 60)}h ago`;
    } else {
      return `${Math.floor(diffInMinutes / 1440)}d ago`;
    }
  }

  /**
   * Refresh dashboard data
   */
  refresh(): void {
    this.loadDashboardData();
  }
} 