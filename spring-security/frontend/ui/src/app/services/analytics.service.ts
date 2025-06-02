import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, forkJoin, of, delay } from 'rxjs';
import { map, catchError } from 'rxjs/operators';
import { AuthService } from '../auth/auth.service';
import { 
  AnalyticsDashboardData, 
  AnalyticsOverview, 
  RevenueData, 
  PropertyPerformance, 
  MarketInsight,
  TimeSeriesData,
  BarChartData,
  PieChartData
} from '../models/analytics.model';

// Analytics models
export interface StudentAnalytics {
  totalInquiries: number;
  activeInquiries: number;
  favoriteProperties: number;
  closedDeals: number;
  propertiesViewed: number;
  averageResponseTimeHours: number;
  inquiryResponseRate: number;
  profileCompletionStatus: {
    isComplete: boolean;
    completionPercentage: number;
    missingFields: string[];
  };
}

export interface OwnerAnalytics {
  totalProperties: number;
  activeProperties: number;
  totalInquiries: number;
  unreadInquiries: number;
  totalViews: number;
  conversionRate: number;
  averageResponseTimeHours: number;
  monthlyRevenue: number;
  popularPropertyTypes: { type: string; count: number }[];
  inquiryTrends: { month: string; count: number }[];
}

export interface RecentActivity {
  id: number;
  type: 'inquiry' | 'favorite' | 'view' | 'response' | 'deal_closed';
  title: string;
  description: string;
  timestamp: Date;
  propertyTitle?: string;
  propertyId?: number;
  ownerName?: string;
  metadata?: any;
}

@Injectable({
  providedIn: 'root'
})
export class AnalyticsService {
  private readonly apiUrl = 'http://localhost:8080/api/v1/analytics';

  constructor(
    private http: HttpClient,
    private authService: AuthService
  ) {}

  /**
   * Get comprehensive analytics dashboard data
   */
  getAnalyticsDashboard(): Observable<AnalyticsDashboardData> {
    const data: AnalyticsDashboardData = {
      overview: this.generateOverviewData(),
      revenueData: this.generateRevenueData(),
      inquiryTrends: this.generateInquiryTrendsData(),
      propertyPerformance: this.generatePropertyPerformanceData(),
      inquiryStatusBreakdown: this.generateInquiryStatusBreakdown(),
      marketInsights: this.generateMarketInsights(),
      monthlyComparison: this.generateMonthlyComparisonData(),
      responseTimeData: this.generateResponseTimeData()
    };

    // Simulate API delay
    return of(data).pipe(delay(1000));
  }

  /**
   * Generate overview KPI data
   */
  private generateOverviewData(): AnalyticsOverview {
    return {
      totalInquiries: 247,
      conversionRate: 24.3, // percentage
      totalRevenue: 18650,
      averageResponseTime: 4.2, // hours
      activeProperties: 12,
      totalViews: 3420
    };
  }

  /**
   * Generate revenue data for the last 12 months
   */
  private generateRevenueData(): RevenueData[] {
    const months = [
      'Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun',
      'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'
    ];
    
    return months.map((month, index) => ({
      month,
      revenue: Math.floor(Math.random() * 3000) + 1000,
      inquiries: Math.floor(Math.random() * 30) + 15,
      deals: Math.floor(Math.random() * 8) + 2
    }));
  }

  /**
   * Generate inquiry trends data (line chart)
   */
  private generateInquiryTrendsData(): TimeSeriesData {
    const last30Days = Array.from({ length: 30 }, (_, i) => {
      const date = new Date();
      date.setDate(date.getDate() - (29 - i));
      return date.toLocaleDateString('en-US', { month: 'short', day: 'numeric' });
    });

    const inquiryData = Array.from({ length: 30 }, () => Math.floor(Math.random() * 12) + 2);
    const responseData = Array.from({ length: 30 }, () => Math.floor(Math.random() * 10) + 1);

    return {
      labels: last30Days,
      datasets: [
        {
          label: 'New Inquiries',
          data: inquiryData,
          borderColor: '#667eea',
          backgroundColor: 'rgba(102, 126, 234, 0.1)',
          tension: 0.4
        },
        {
          label: 'Responses Sent',
          data: responseData,
          borderColor: '#764ba2',
          backgroundColor: 'rgba(118, 75, 162, 0.1)',
          tension: 0.4
        }
      ]
    };
  }

  /**
   * Generate property performance data
   */
  private generatePropertyPerformanceData(): PropertyPerformance[] {
    const properties = [
      'Modern Apartment in Tunis Center',
      'Cozy Studio near University',
      'Spacious 2BR in Sfax',
      'Student Housing Complex',
      'Luxury Apartment in La Marsa'
    ];

    return properties.map((title, index) => {
      const inquiries = Math.floor(Math.random() * 25) + 10;
      const conversions = Math.floor(inquiries * (Math.random() * 0.4 + 0.1)); // 10-50% conversion
      
      return {
        propertyId: index + 1,
        title,
        inquiries,
        conversions,
        revenue: conversions * (Math.floor(Math.random() * 200) + 600),
        views: Math.floor(Math.random() * 150) + 50,
        conversionRate: Number(((conversions / inquiries) * 100).toFixed(1))
      };
    });
  }

  /**
   * Generate inquiry status breakdown (pie chart)
   */
  private generateInquiryStatusBreakdown(): PieChartData {
    const data = [45, 32, 18, 12]; // PENDING_REPLY, REPLIED, CLOSED, PROPERTY_NO_LONGER_AVAILABLE
    const colors = ['#f093fb', '#4facfe', '#43e97b', '#ff6b6b'];

    return {
      labels: ['Pending Reply', 'Replied', 'Closed Deals', 'No Longer Available'],
      datasets: [{
        data,
        backgroundColor: colors,
        borderColor: colors.map(color => color + '80'),
        borderWidth: 2
      }]
    };
  }

  /**
   * Generate market insights
   */
  private generateMarketInsights(): MarketInsight[] {
    return [
      {
        category: 'Demand',
        label: 'Average Inquiries per Property',
        value: 20.6,
        trend: 'up',
        percentage: 12.5
      },
      {
        category: 'Competition',
        label: 'Market Response Time',
        value: 6.8,
        trend: 'down',
        percentage: 8.3
      },
      {
        category: 'Pricing',
        label: 'Average Rent per m²',
        value: 18.5,
        trend: 'up',
        percentage: 5.2
      },
      {
        category: 'Satisfaction',
        label: 'Owner Rating',
        value: 4.7,
        trend: 'stable',
        percentage: 0.0
      }
    ];
  }

  /**
   * Generate monthly comparison data (bar chart)
   */
  private generateMonthlyComparisonData(): BarChartData {
    const months = ['Oct', 'Nov', 'Dec', 'Jan', 'Feb'];
    const inquiries = [28, 35, 42, 38, 47];
    const deals = [7, 9, 12, 10, 14];

    return {
      labels: months,
      datasets: [
        {
          label: 'Inquiries',
          data: inquiries,
          backgroundColor: ['#667eea', '#764ba2', '#f093fb', '#4facfe', '#43e97b']
        },
        {
          label: 'Closed Deals',
          data: deals,
          backgroundColor: ['#667eea80', '#764ba280', '#f093fb80', '#4facfe80', '#43e97b80']
        }
      ]
    };
  }

  /**
   * Generate response time data (line chart)
   */
  private generateResponseTimeData(): TimeSeriesData {
    const weeks = ['Week 1', 'Week 2', 'Week 3', 'Week 4'];
    const responseTime = [5.2, 4.8, 3.9, 4.2];
    const targetTime = [6, 6, 6, 6]; // Target response time

    return {
      labels: weeks,
      datasets: [
        {
          label: 'Actual Response Time (hours)',
          data: responseTime,
          borderColor: '#ff6b6b',
          backgroundColor: 'rgba(255, 107, 107, 0.1)',
          tension: 0.4
        },
        {
          label: 'Target Response Time (hours)',
          data: targetTime,
          borderColor: '#48bb78',
          backgroundColor: 'rgba(72, 187, 120, 0.1)',
          tension: 0
        }
      ]
    };
  }

  /**
   * Get data for a specific date range
   */
  getAnalyticsForDateRange(startDate: Date, endDate: Date): Observable<AnalyticsDashboardData> {
    // In a real implementation, this would filter data based on date range
    return this.getAnalyticsDashboard();
  }

  /**
   * Get property-specific analytics
   */
  getPropertyAnalytics(propertyId: number): Observable<any> {
    // Mock property-specific data
    return of({
      propertyId,
      inquiries: Math.floor(Math.random() * 25) + 10,
      views: Math.floor(Math.random() * 150) + 50,
      conversions: Math.floor(Math.random() * 8) + 2,
      revenue: Math.floor(Math.random() * 2000) + 1000
    }).pipe(delay(500));
  }

  // ==================== STUDENT ANALYTICS ====================

  /**
   * Get comprehensive analytics for the authenticated student
   */
  getStudentAnalytics(): Observable<StudentAnalytics> {
    return this.http.get<StudentAnalytics>(`${this.apiUrl}/student`).pipe(
      catchError(error => {
        console.warn('Student analytics API not available, generating mock data:', error);
        return of(this.generateMockStudentAnalytics());
      })
    );
  }

  /**
   * Get student's recent activity
   */
  getStudentRecentActivity(limit: number = 10): Observable<RecentActivity[]> {
    const params = new HttpParams().set('limit', limit.toString());
    return this.http.get<RecentActivity[]>(`${this.apiUrl}/student/activity`, { params }).pipe(
      catchError(error => {
        console.warn('Student activity API not available, generating mock data:', error);
        return of(this.generateMockStudentActivity(limit));
      })
    );
  }

  /**
   * Track student property view
   */
  trackPropertyView(propertyId: number): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/student/view`, { propertyId }).pipe(
      catchError(error => {
        console.warn('Property view tracking failed:', error);
        return of(void 0);
      })
    );
  }

  /**
   * Track student property favorite
   */
  trackPropertyFavorite(propertyId: number, action: 'add' | 'remove'): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/student/favorite`, { propertyId, action }).pipe(
      catchError(error => {
        console.warn('Property favorite tracking failed:', error);
        return of(void 0);
      })
    );
  }

  // ==================== OWNER ANALYTICS ====================

  /**
   * Get comprehensive analytics for the authenticated owner
   */
  getOwnerAnalytics(): Observable<OwnerAnalytics> {
    return this.http.get<OwnerAnalytics>(`${this.apiUrl}/owner`).pipe(
      catchError(error => {
        console.warn('Owner analytics API not available, generating mock data:', error);
        return of(this.generateMockOwnerAnalytics());
      })
    );
  }

  /**
   * Get property performance metrics for owner
   */
  getOwnerPropertyPerformance(): Observable<PropertyPerformance[]> {
    return this.http.get<PropertyPerformance[]>(`${this.apiUrl}/owner/property-performance`).pipe(
      catchError(error => {
        console.warn('Property performance API not available, generating mock data:', error);
        return of(this.generateMockPropertyPerformance());
      })
    );
  }

  /**
   * Get owner's revenue analytics
   */
  getOwnerRevenueAnalytics(period: 'weekly' | 'monthly' | 'yearly' = 'monthly'): Observable<any> {
    const params = new HttpParams().set('period', period);
    return this.http.get<any>(`${this.apiUrl}/owner/revenue`, { params }).pipe(
      catchError(error => {
        console.warn('Revenue analytics API not available, generating mock data:', error);
        return of(this.generateMockRevenueData(period));
      })
    );
  }

  // ==================== MOCK DATA GENERATORS ====================

  private generateMockStudentAnalytics(): StudentAnalytics {
    const baseInquiries = Math.floor(Math.random() * 15) + 5; // 5-20 inquiries
    const activePercentage = 0.3; // 30% are still active
    const favoriteCount = Math.floor(Math.random() * 25) + 8; // 8-33 favorites
    const closedDeals = Math.floor(Math.random() * 3) + 1; // 1-4 closed deals
    const viewsMultiplier = 3.5; // Students view ~3.5x more properties than they favorite

    return {
      totalInquiries: baseInquiries,
      activeInquiries: Math.floor(baseInquiries * activePercentage),
      favoriteProperties: favoriteCount,
      closedDeals: closedDeals,
      propertiesViewed: Math.floor(favoriteCount * viewsMultiplier),
      averageResponseTimeHours: Math.floor(Math.random() * 24) + 2, // 2-26 hours
      inquiryResponseRate: Math.floor(Math.random() * 30) + 70, // 70-100%
      profileCompletionStatus: {
        isComplete: Math.random() > 0.3,
        completionPercentage: Math.floor(Math.random() * 30) + 70, // 70-100%
        missingFields: ['phone', 'preferences'].filter(() => Math.random() > 0.7)
      }
    };
  }

  private generateMockStudentActivity(limit: number): RecentActivity[] {
    const activities: RecentActivity[] = [
      {
        id: 1,
        type: 'response',
        title: 'Owner Response Received',
        description: 'Mohamed Ben Ali responded to your inquiry about S+2 Apartment near FSM',
        timestamp: new Date(Date.now() - 2 * 60 * 60 * 1000), // 2 hours ago
        propertyTitle: 'S+2 Apartment near FSM',
        propertyId: 123,
        ownerName: 'Mohamed Ben Ali'
      },
      {
        id: 2,
        type: 'inquiry',
        title: 'Inquiry Sent',
        description: 'You sent an inquiry about Modern Studio in Manar',
        timestamp: new Date(Date.now() - 5 * 60 * 60 * 1000), // 5 hours ago
        propertyTitle: 'Modern Studio in Manar',
        propertyId: 124
      },
      {
        id: 3,
        type: 'favorite',
        title: 'Property Saved',
        description: 'Added S+1 Furnished Apartment to your favorites',
        timestamp: new Date(Date.now() - 1 * 24 * 60 * 60 * 1000), // 1 day ago
        propertyTitle: 'S+1 Furnished Apartment',
        propertyId: 125
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
        type: 'deal_closed',
        title: 'Deal Finalized',
        description: 'Rental agreement completed for Spacious S+3 Villa',
        timestamp: new Date(Date.now() - 3 * 24 * 60 * 60 * 1000), // 3 days ago
        propertyTitle: 'Spacious S+3 Villa',
        propertyId: 126
      }
    ];

    return activities.slice(0, limit);
  }

  private generateMockOwnerAnalytics(): OwnerAnalytics {
    return {
      totalProperties: Math.floor(Math.random() * 10) + 5, // 5-15 properties
      activeProperties: Math.floor(Math.random() * 8) + 3, // 3-11 active
      totalInquiries: Math.floor(Math.random() * 200) + 50, // 50-250 inquiries
      unreadInquiries: Math.floor(Math.random() * 10) + 1, // 1-11 unread
      totalViews: Math.floor(Math.random() * 2000) + 500, // 500-2500 views
      conversionRate: Math.floor(Math.random() * 20) + 15, // 15-35%
      averageResponseTimeHours: Math.floor(Math.random() * 12) + 2, // 2-14 hours
      monthlyRevenue: Math.floor(Math.random() * 15000) + 5000, // 5000-20000 TND
      popularPropertyTypes: [
        { type: 'S+1', count: Math.floor(Math.random() * 20) + 10 },
        { type: 'S+2', count: Math.floor(Math.random() * 15) + 8 },
        { type: 'Studio', count: Math.floor(Math.random() * 12) + 5 }
      ],
      inquiryTrends: [
        { month: 'Jan', count: Math.floor(Math.random() * 30) + 10 },
        { month: 'Feb', count: Math.floor(Math.random() * 35) + 15 },
        { month: 'Mar', count: Math.floor(Math.random() * 40) + 20 }
      ]
    };
  }

    private generateMockPropertyPerformance(): PropertyPerformance[] {    const properties = [      'Modern S+2 near FSM',      'Furnished Studio in Manar',      'Spacious S+3 Villa',      'Cozy S+1 Apartment',      'Student Housing Complex'    ];    return properties.map((title, index) => {      const inquiries = Math.floor(Math.random() * 20) + 5;      const conversions = Math.floor(inquiries * (Math.random() * 0.4 + 0.1));            return {        propertyId: index + 1,        title,        views: Math.floor(Math.random() * 200) + 50,        inquiries,        conversions,        revenue: conversions * (Math.floor(Math.random() * 200) + 600),        conversionRate: Number(((conversions / inquiries) * 100).toFixed(1))      };    });
  }

  private generateMockRevenueData(period: string): any {
    const data = [];
    const months = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
    
    for (let i = 0; i < (period === 'yearly' ? 12 : 6); i++) {
      data.push({
        period: months[i] || `Week ${i + 1}`,
        revenue: Math.floor(Math.random() * 5000) + 2000,
        properties: Math.floor(Math.random() * 5) + 2
      });
    }

    return { data, total: data.reduce((sum, item) => sum + item.revenue, 0) };
  }
} 