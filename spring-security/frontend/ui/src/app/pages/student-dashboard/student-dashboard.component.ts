import { Component, OnInit, ViewEncapsulation } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { AuthService } from '../../auth/auth.service';
import { UserProfileService } from '../../services/user-profile.service';
import { PropertyListingService } from '../../services/property-listing.service';
import { InquiryService } from '../../services/inquiry.service';
import { AnalyticsService, StudentAnalytics, RecentActivity } from '../../services/analytics.service';
import { UserProfile } from '../../models/user-profile.model';
import { Property } from '../../models/property.model';
import { PropertyCardComponent } from '../../components/property-card/property-card.component';

interface StudentStats {
  totalInquiries: number;
  activeInquiries: number;
  favoriteProperties: number;
  closedDeals: number;
  propertiesViewed: number;
  averageResponseTime: string;
}

// RecentActivity interface is now imported from AnalyticsService

interface QuickAction {
  id: string;
  title: string;
  description: string;
  icon: string;
  route: string;
  gradient: string;
  count?: number;
}

@Component({
  selector: 'app-student-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    PropertyCardComponent
  ],
  templateUrl: './student-dashboard.component.html',
  styleUrl: './student-dashboard.component.css',
  encapsulation: ViewEncapsulation.None
})
export class StudentDashboardComponent implements OnInit {
  isLoading = true;
  userName: string | null = null;
  userEmail: string | null = null;
  currentUserProfile: UserProfile | null = null;
  currentDate = new Date();
  
  // Dashboard data
  studentStats: StudentStats = {
    totalInquiries: 0,
    activeInquiries: 0,
    favoriteProperties: 0,
    closedDeals: 0,
    propertiesViewed: 0,
    averageResponseTime: '0h'
  };
  
  recentActivities: RecentActivity[] = [];
  recommendedProperties: Property[] = [];
  loadingRecommendations = false;
  
  quickActions: QuickAction[] = [
    {
      id: 'browse',
      title: 'Browse Properties',
      description: 'Discover new properties that match your preferences',
      icon: 'M21 21l-5.197-5.197m0 0A7.5 7.5 0 105.196 5.196a7.5 7.5 0 0010.607 10.607z',
      route: '/discovery',
      gradient: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)'
    },
    {
      id: 'compatible-students',
      title: 'Discover Compatible Students',
      description: 'Find potential roommates with AI-powered matching',
      icon: 'M15 19.128a9.38 9.38 0 002.625.372 9.337 9.337 0 004.121-.952 4.125 4.125 0 00-7.533-2.493M15 19.128v-.003c0-1.113-.285-2.16-.786-3.07M15 19.128v.106A12.318 12.318 0 018.624 21c-2.331 0-4.512-.645-6.374-1.766l-.001-.109a6.375 6.375 0 0111.964-3.07M12 6.375a3.375 3.375 0 11-6.75 0 3.375 3.375 0 016.75 0zm8.25 2.25a2.625 2.625 0 11-5.25 0 2.625 2.625 0 015.25 0z',
      route: '/roommates/compatible-students',
      gradient: 'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)'
    },
    {
      id: 'post-roommate',
      title: 'Post Roommate Announcement',
      description: 'Find compatible roommates with AI matching',
      icon: 'M12 4.5v15m7.5-7.5h-15',
      route: '/roommates/post',
      gradient: 'linear-gradient(135deg, #ff6b6b 0%, #ee5a24 100%)'
    },
    {
      id: 'my-announcements',
      title: 'My Announcements',
      description: 'View and manage your roommate announcements',
      icon: 'M8.25 6.75h12M8.25 12h12m-12 5.25h12M3.75 6.75h.007v.008H3.75V6.75zm.375 0a.375.375 0 11-.75 0 .375.375 0 01.75 0zM3.75 12h.007v.008H3.75V12zm.375 0a.375.375 0 11-.75 0 .375.375 0 01.75 0zm-.375 5.25h.007v.008H3.75v-.008zm.375 0a.375.375 0 11-.75 0 .375.375 0 01.75 0z',
      route: '/roommates/my-announcements',
      gradient: 'linear-gradient(135deg, #20bf6b 0%, #01a3a4 100%)'
    },
    {
      id: 'browse-roommates',
      title: 'Browse Roommates',
      description: 'Find perfect roommates for your property',
      icon: 'M15 19.128a9.38 9.38 0 002.625.372 9.337 9.337 0 004.121-.952 4.125 4.125 0 00-7.533-2.493M15 19.128v-.003c0-1.113-.285-2.16-.786-3.07M15 19.128v.106A12.318 12.318 0 018.624 21c-2.331 0-4.512-.645-6.374-1.766l-.001-.109a6.375 6.375 0 0111.964-3.07M12 6.375a3.375 3.375 0 11-6.75 0 3.375 3.375 0 016.75 0zm8.25 2.25a2.625 2.625 0 11-5.25 0 2.625 2.625 0 015.25 0z',
      route: '/roommates/browse',
      gradient: 'linear-gradient(135deg, #a29bfe 0%, #6c5ce7 100%)'
    },
    {
      id: 'favorites',
      title: 'My Favorites',
      description: 'View and manage your saved properties',
      icon: 'M21 8.25c0-2.485-2.099-4.5-4.688-4.5-1.935 0-3.597 1.126-4.312 2.733-.715-1.607-2.377-2.733-4.313-2.733C5.1 3.75 3 5.765 3 8.25c0 7.22 9 12 9 12s9-4.78 9-12z',
      route: '/profile',
      gradient: 'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)'
    },
    {
      id: 'inquiries',
      title: 'My Inquiries',
      description: 'Track your property inquiries and responses',
      icon: 'M21.75 6.75v10.5a2.25 2.25 0 01-2.25 2.25h-15a2.25 2.25 0 01-2.25-2.25V6.75m19.5 0A2.25 2.25 0 0019.5 4.5h-15a2.25 2.25 0 00-2.25 2.25m19.5 0v.243a2.25 2.25 0 01-1.07 1.916l-7.5 4.615a2.25 2.25 0 01-2.36 0L3.32 8.91a2.25 2.25 0 01-1.07-1.916V6.75',
      route: '/my-inquiries',
      gradient: 'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)'
    },
    {
      id: 'profile',
      title: 'My Profile',
      description: 'Update your personal and academic information',
      icon: 'M15.75 6a3.75 3.75 0 11-7.5 0 3.75 3.75 0 017.5 0zM4.501 20.118a7.5 7.5 0 0114.998 0A17.933 17.933 0 0112 21.75c-2.676 0-5.216-.584-7.499-1.632z',
      route: '/profile',
      gradient: 'linear-gradient(135deg, #fa709a 0%, #fee140 100%)'
    }
  ];

  constructor(
    private authService: AuthService,
    private userProfileService: UserProfileService,
    private propertyService: PropertyListingService,
    private inquiryService: InquiryService,
    private analyticsService: AnalyticsService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadUserInfo();
    this.loadDashboardData();
  }

  private loadUserInfo(): void {
    const token = this.authService.getToken();
    if (token) {
      const decodedToken = this.authService.decodeToken(token);
      if (decodedToken) {
        this.userName = decodedToken.name || decodedToken.username;
        this.userEmail = decodedToken.email;
      }
    }

    // Load user profile
    const userId = this.userProfileService.getCurrentUserId();
    if (userId) {
      this.userProfileService.getStudentProfile(userId).subscribe({
        next: (profile) => {
          this.currentUserProfile = profile;
          this.loadRecommendedProperties();
        },
        error: (err) => {
          console.error('Error loading student profile:', err);
        }
      });
    }
  }

  private loadDashboardData(): void {
    // Load real analytics data with fallback to mock data
    this.analyticsService.getStudentAnalytics().subscribe({
      next: (analytics) => {
        this.updateStatsFromAnalytics(analytics);
        this.isLoading = false;
      },
      error: (err) => {
        console.warn('Failed to load analytics, using mock data:', err);
        this.generateMockStats();
        this.isLoading = false;
      }
    });

    // Load real activity data
    this.analyticsService.getStudentRecentActivity(5).subscribe({
      next: (activities) => {
        this.recentActivities = activities;
      },
      error: (err) => {
        console.warn('Failed to load activities, using mock data:', err);
        this.generateMockActivities();
      }
    });
  }

  private updateStatsFromAnalytics(analytics: StudentAnalytics): void {
    this.studentStats = {
      totalInquiries: analytics.totalInquiries,
      activeInquiries: analytics.activeInquiries,
      favoriteProperties: analytics.favoriteProperties,
      closedDeals: analytics.closedDeals,
      propertiesViewed: analytics.propertiesViewed,
      averageResponseTime: analytics.averageResponseTimeHours < 24 
        ? `${analytics.averageResponseTimeHours}h`
        : `${Math.floor(analytics.averageResponseTimeHours / 24)}d ${analytics.averageResponseTimeHours % 24}h`
    };

    // Update quick actions with counts
    this.quickActions[1].count = this.studentStats.favoriteProperties;
    this.quickActions[2].count = this.studentStats.activeInquiries;
  }

  private generateMockStats(): void {
    // Generate realistic mock data - in real app, this would come from API
    const baseInquiries = Math.floor(Math.random() * 15) + 5; // 5-20 inquiries
    const activePercentage = 0.3; // 30% are still active
    const favoriteCount = Math.floor(Math.random() * 25) + 8; // 8-33 favorites
    const closedDeals = Math.floor(Math.random() * 3) + 1; // 1-4 closed deals
    const viewsMultiplier = 3.5; // Students view ~3.5x more properties than they favorite

    this.studentStats = {
      totalInquiries: baseInquiries,
      activeInquiries: Math.floor(baseInquiries * activePercentage),
      favoriteProperties: favoriteCount,
      closedDeals: closedDeals,
      propertiesViewed: Math.floor(favoriteCount * viewsMultiplier),
      averageResponseTime: this.generateResponseTime()
    };

    // Update quick actions with counts
    this.quickActions[1].count = this.studentStats.favoriteProperties;
    this.quickActions[2].count = this.studentStats.activeInquiries;
  }

  private generateResponseTime(): string {
    const hours = Math.floor(Math.random() * 24) + 2; // 2-26 hours
    if (hours < 24) {
      return `${hours}h`;
    } else {
      const days = Math.floor(hours / 24);
      const remainingHours = hours % 24;
      return remainingHours > 0 ? `${days}d ${remainingHours}h` : `${days}d`;
    }
  }

  private generateMockActivities(): void {
    const activities: RecentActivity[] = [
      {
        id: 1,
        type: 'response',
        title: 'Owner Response Received',
        description: 'Mohamed Ben Ali responded to your inquiry about S+2 Apartment near FSM',
        timestamp: new Date(Date.now() - 2 * 60 * 60 * 1000), // 2 hours ago
        propertyTitle: 'S+2 Apartment near FSM',
        ownerName: 'Mohamed Ben Ali'
      },
      {
        id: 2,
        type: 'inquiry',
        title: 'Inquiry Sent',
        description: 'You sent an inquiry about Modern Studio in Manar',
        timestamp: new Date(Date.now() - 5 * 60 * 60 * 1000), // 5 hours ago
        propertyTitle: 'Modern Studio in Manar'
      },
      {
        id: 3,
        type: 'favorite',
        title: 'Property Saved',
        description: 'Added S+1 Furnished Apartment to your favorites',
        timestamp: new Date(Date.now() - 1 * 24 * 60 * 60 * 1000), // 1 day ago
        propertyTitle: 'S+1 Furnished Apartment'
      },
      {
        id: 4,
        type: 'view',
        title: 'Property Viewed',
        description: 'You viewed 3 new properties in Ariana',
        timestamp: new Date(Date.now() - 2 * 24 * 60 * 60 * 1000), // 2 days ago
      },
      {
        id: 5,
        type: 'response',
        title: 'Visit Scheduled',
        description: 'Scheduled a visit for tomorrow at 3 PM for Spacious S+3 Villa',
        timestamp: new Date(Date.now() - 3 * 24 * 60 * 60 * 1000), // 3 days ago
        propertyTitle: 'Spacious S+3 Villa'
      }
    ];

    this.recentActivities = activities;
  }

  private loadRecommendedProperties(): void {
    this.loadingRecommendations = true;
    
    // In a real app, this would be based on user preferences, location, budget, etc.
    // For now, use getAllProperties as the method exists
    this.propertyService.getAllProperties(0, 6).subscribe({
      next: (response: any) => {
        // Convert PropertyListingDTO to Property format
        const properties = (response.content || []).map((prop: any) => ({
          id: prop.id,
          title: prop.title,
          description: prop.description,
          price: prop.price,
          location: prop.location,
          imageUrl: prop.imageUrl || '/assets/images/default-property.jpg',
          address: prop.address || prop.location,
          currency: prop.currency || 'TND',
          propertyType: prop.propertyType,
          bedrooms: prop.bedrooms,
          bathrooms: prop.bathrooms,
          area: prop.area,
          furnished: prop.furnished,
          available: prop.available,
          createdAt: prop.createdAt,
          updatedAt: prop.updatedAt,
          ownerId: prop.ownerId
        }));
        this.recommendedProperties = properties.slice(0, 6);
        this.loadingRecommendations = false;
      },
      error: (err: any) => {
        console.error('Error loading recommended properties:', err);
        this.loadingRecommendations = false;
        this.recommendedProperties = [];
      }
    });
  }

  getActivityIcon(type: string): string {
    switch (type) {
      case 'inquiry':
        return 'M21.75 6.75v10.5a2.25 2.25 0 01-2.25 2.25h-15a2.25 2.25 0 01-2.25-2.25V6.75m19.5 0A2.25 2.25 0 0019.5 4.5h-15a2.25 2.25 0 00-2.25 2.25m19.5 0v.243a2.25 2.25 0 01-1.07 1.916l-7.5 4.615a2.25 2.25 0 01-2.36 0L3.32 8.91a2.25 2.25 0 01-1.07-1.916V6.75';
      case 'favorite':
        return 'M21 8.25c0-2.485-2.099-4.5-4.688-4.5-1.935 0-3.597 1.126-4.312 2.733-.715-1.607-2.377-2.733-4.313-2.733C5.1 3.75 3 5.765 3 8.25c0 7.22 9 12 9 12s9-4.78 9-12z';
      case 'view':
        return 'M2.036 12.322a1.012 1.012 0 010-.639C3.423 7.51 7.36 4.5 12 4.5c4.638 0 8.573 3.007 9.963 7.178.07.207.07.431 0 .639C20.577 16.49 16.64 19.5 12 19.5c-4.638 0-8.573-3.007-9.963-7.178z M15 12a3 3 0 11-6 0 3 3 0 016 0z';
      case 'response':
        return 'M8.625 12a.375.375 0 11-.75 0 .375.375 0 01.75 0zm0 0H8.25m4.125 0a.375.375 0 11-.75 0 .375.375 0 01.75 0zm0 0H12m4.125 0a.375.375 0 11-.75 0 .375.375 0 01.75 0zm0 0h-.375M21 12c0 4.556-4.03 8.25-9 8.25a9.764 9.764 0 01-2.555-.337A5.972 5.972 0 015.41 20.97a5.969 5.969 0 01-.474-.065 4.48 4.48 0 00.978-2.025c.09-.457-.133-.901-.467-1.226C3.93 16.178 3 14.189 3 12c0-4.556 4.03-8.25 9-8.25s9 3.694 9 8.25z';
      default:
        return 'M13.5 4.5L21 12m0 0l-7.5 7.5M21 12H3';
    }
  }

  getActivityColor(type: string): string {
    switch (type) {
      case 'inquiry':
        return '#4facfe';
      case 'favorite':
        return '#f093fb';
      case 'view':
        return '#667eea';
      case 'response':
        return '#4ade80';
      default:
        return '#94a3b8';
    }
  }

  formatTimeAgo(timestamp: Date): string {
    const now = new Date();
    const diffInMs = now.getTime() - timestamp.getTime();
    const diffInMinutes = Math.floor(diffInMs / (1000 * 60));
    const diffInHours = Math.floor(diffInMs / (1000 * 60 * 60));
    const diffInDays = Math.floor(diffInMs / (1000 * 60 * 60 * 24));

    if (diffInMinutes < 60) {
      return `${diffInMinutes}m ago`;
    } else if (diffInHours < 24) {
      return `${diffInHours}h ago`;
    } else {
      return `${diffInDays}d ago`;
    }
  }

  /**
   * Test navigation to post roommate announcement
   */
  navigateToPostAnnouncement(): void {
    console.log('🚀 Dashboard: Navigating to post announcement page...');
    this.router.navigate(['/roommates/post']).then(
      (success: boolean) => {
        console.log('Dashboard Navigation success:', success);
      }
    ).catch((error: any) => {
      console.error('Dashboard Navigation error:', error);
    });
  }
} 