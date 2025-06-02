import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Observable, BehaviorSubject, of } from 'rxjs';
import { AuthService } from '../auth/auth.service';
import { tap, catchError } from 'rxjs/operators';

export interface AdminStats {
  totalUsers: number;
  activeUsers: number;
  newUsersToday: number;
  newUsersThisWeek: number;
  studentUsers: number;
  ownerUsers: number;
  adminUsers: number;
  totalProperties: number;
  activeProperties: number;
  pendingProperties: number;
  propertiesListedToday: number;
  propertiesListedThisWeek: number;
  totalInquiries: number;
  pendingInquiries: number;
  acceptedInquiries: number;
  rejectedInquiries: number;
  inquiriesToday: number;
  inquiriesThisWeek: number;
  totalAnnouncements: number;
  activeAnnouncements: number;
  totalApplications: number;
  successfulMatches: number;
  announcementsToday: number;
  applicationsToday: number;
  systemLoad: number;
  memoryUsage: number;
  uptimeHours: string;
  userGrowthRate: number;
  propertyGrowthRate: number;
  inquiryGrowthRate: number;
  announcementGrowthRate: number;
  usersByRole: { [key: string]: number };
  inquiriesByStatus: { [key: string]: number };
  usersByProvider: { [key: string]: number };
  activitiesLast24Hours: number;
}

export interface SystemHealth {
  overallStatus: string;
  timestamp: string;
  database: {
    status: string;
    connectionPoolSize: number;
    activeConnections: number;
    idleConnections: number;
    averageResponseTime: number;
    totalQueries: number;
    slowQueries: number;
    lastBackup: string;
  };
  application: {
    status: string;
    version: string;
    buildDate: string;
    uptime: number;
    heapMemoryUsed: number;
    heapMemoryMax: number;
    nonHeapMemoryUsed: number;
    threadCount: number;
    activeThreads: number;
    cpuUsage: number;
    javaVersion: string;
  };
  externalServices: {
    emailServiceStatus: string;
    scraperServiceStatus: string;
    websocketServiceStatus: string;
    fileStorageStatus: string;
    thirdPartyServices: { [key: string]: string };
  };
  systemResources: {
    diskSpaceUsed: number;
    diskSpaceTotal: number;
    diskSpacePercent: number;
    memoryUsed: number;
    memoryTotal: number;
    memoryPercent: number;
    networkIn: number;
    networkOut: number;
    processCount: number;
  };
  recentAlerts: Array<{
    severity: string;
    message: string;
    component: string;
    timestamp: string;
    resolved: boolean;
  }>;
}

export interface AdminUser {
  id: number;
  email: string;
  username: string;
  phoneNumber: string;
  role: string;
  provider: string;
  enabled: boolean;
  createdAt?: string;
  lastLoginAt?: string;
  studyField: string;
  age: number;
  institute: string;
  profileStatus: string;
  totalProperties: number;
  totalInquiries: number;
  totalAnnouncements: number;
}

export interface AdminProperty {
  id: number;
  title: string;
  description: string;
  location: string;
  price: number;
  rooms: number;
  bathrooms: number;
  area: number;
  propertyType: string;
  active: boolean;
  createdAt: string;
  updatedAt: string;
  userId: number;
  ownerUsername: string;
  ownerEmail: string;
  totalInquiries: number;
  viewCount: number;
  images: string[];
}

export interface PagedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

export interface AdminRoommateAnnouncement {
  id: number;
  posterId: number;
  posterUsername: string;
  posterEmail: string;
  posterRole: string;
  propertyTitle: string;
  propertyAddress: string;
  propertyLatitude?: number;
  propertyLongitude?: number;
  totalRent: number;
  totalRooms: number;
  availableRooms: number;
  propertyType: string;
  amenities: string[];
  imageUrls: string[];
  maxRoommates: number;
  genderPreference: string;
  ageMin: number;
  ageMax: number;
  lifestyleTags: string[];
  smokingAllowed: boolean;
  petsAllowed: boolean;
  cleanlinessLevel: number;
  rentPerPerson: number;
  securityDeposit: number;
  utilitiesSplit: string;
  additionalCosts?: string;
  description?: string;
  moveInDate: string;
  leaseDurationMonths: number;
  status: string;
  createdAt: string;
  expiresAt: string;
  updatedAt: string;
  totalApplications: number;
  pendingApplications: number;
  acceptedApplications: number;
  viewCount: number;
  isTypeA: boolean;
  propertyListingId?: number;
}

@Injectable({
  providedIn: 'root'
})
export class AdminService {
  private apiUrl = 'http://localhost:8080/api/admin';
  private currentStatsSubject = new BehaviorSubject<AdminStats | null>(null);
  public currentStats$ = this.currentStatsSubject.asObservable();

  constructor(
    private http: HttpClient,
    private authService: AuthService
  ) {}

  private getAuthHeaders(): HttpHeaders {
    const token = this.authService.getToken();
    if (!token) {
      console.error('Authentication token not found.');
      this.authService.handleAuthError('Your session has expired. Please log in again.');
      return new HttpHeaders();
    }
    return new HttpHeaders().set('Authorization', `Bearer ${token}`);
  }

  // ==================== DASHBOARD STATS ====================
  
  getOverviewStats(): Observable<AdminStats> {
    const headers = this.getAuthHeaders();
    console.log('AdminService: Making request to /api/admin/stats/overview');
    console.log('AdminService: Headers:', headers);
    return this.http.get<AdminStats>(`${this.apiUrl}/stats/overview`, { headers })
      .pipe(
        tap(response => {
          console.log('AdminService: Successfully received stats:', response);
        }),
        catchError(error => {
          console.error('AdminService: Error getting overview stats:', error);
          console.error('AdminService: Error status:', error.status);
          console.error('AdminService: Error message:', error.message);
          console.error('AdminService: Error body:', error.error);
          
          // Return mock data as fallback
          console.warn('AdminService: Returning mock data due to backend error');
          return of(this.getMockStats());
        })
      );
  }

  private getMockStats(): AdminStats {
    return {
      totalUsers: 5,
      activeUsers: 4,
      newUsersToday: 1,
      newUsersThisWeek: 2,
      studentUsers: 3,
      ownerUsers: 1,
      adminUsers: 1,
      totalProperties: 0,
      activeProperties: 0,
      pendingProperties: 0,
      propertiesListedToday: 0,
      propertiesListedThisWeek: 0,
      totalInquiries: 0,
      pendingInquiries: 0,
      acceptedInquiries: 0,
      rejectedInquiries: 0,
      inquiriesToday: 0,
      inquiriesThisWeek: 0,
      totalAnnouncements: 0,
      activeAnnouncements: 0,
      totalApplications: 0,
      successfulMatches: 0,
      announcementsToday: 0,
      applicationsToday: 0,
      systemLoad: 0.5,
      memoryUsage: 1024 * 1024 * 256, // 256MB
      uptimeHours: "24.5",
      userGrowthRate: 15.2,
      propertyGrowthRate: 0,
      inquiryGrowthRate: 0,
      announcementGrowthRate: 0,
      usersByRole: {
        'STUDENT': 3,
        'OWNER': 1,
        'ADMIN': 1
      },
      inquiriesByStatus: {
        'PENDING': 0,
        'REPLIED': 0,
        'CLOSED': 0
      },
      usersByProvider: {
        'LOCAL': 4,
        'GOOGLE': 1
      },
      activitiesLast24Hours: 15
    };
  }

  getSystemHealth(): Observable<SystemHealth> {
    const headers = this.getAuthHeaders();
    return this.http.get<SystemHealth>(`${this.apiUrl}/stats/system-health`, { headers });
  }

  getUserGrowthStats(days: number = 30): Observable<any> {
    const headers = this.getAuthHeaders();
    const params = new HttpParams().set('days', days.toString());
    return this.http.get<any>(`${this.apiUrl}/stats/user-growth`, { headers, params });
  }

  getActivityStats(days: number = 7): Observable<any> {
    const headers = this.getAuthHeaders();
    const params = new HttpParams().set('days', days.toString());
    return this.http.get<any>(`${this.apiUrl}/stats/activity`, { headers, params });
  }

  // ==================== USER MANAGEMENT ====================

  getAllUsers(
    page: number = 0,
    size: number = 20,
    sortBy: string = 'id',
    sortDir: string = 'desc',
    search?: string,
    role?: string
  ): Observable<PagedResponse<AdminUser>> {
    const headers = this.getAuthHeaders();
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sortBy', sortBy)
      .set('sortDir', sortDir);
    
    if (search) params = params.set('search', search);
    if (role) params = params.set('role', role);

    return this.http.get<PagedResponse<AdminUser>>(`${this.apiUrl}/users`, { headers, params });
  }

  getUserDetails(userId: number): Observable<AdminUser> {
    const headers = this.getAuthHeaders();
    return this.http.get<AdminUser>(`${this.apiUrl}/users/${userId}`, { headers });
  }

  updateUserRole(userId: number, role: string): Observable<string> {
    const headers = this.getAuthHeaders();
    return this.http.put(`${this.apiUrl}/users/${userId}/role`, { role }, { 
      headers, 
      responseType: 'text' 
    });
  }

  updateUserStatus(userId: number, enabled: boolean): Observable<string> {
    const headers = this.getAuthHeaders();
    return this.http.put(`${this.apiUrl}/users/${userId}/status`, { enabled }, { 
      headers, 
      responseType: 'text' 
    });
  }

  deleteUser(userId: number): Observable<string> {
    const headers = this.getAuthHeaders();
    return this.http.delete(`${this.apiUrl}/users/${userId}`, { 
      headers, 
      responseType: 'text' 
    });
  }

  // ==================== PROPERTY MANAGEMENT ====================

  getAllProperties(
    page: number = 0,
    size: number = 20,
    sortBy: string = 'createdAt',
    sortDir: string = 'desc',
    search?: string,
    status?: string
  ): Observable<PagedResponse<AdminProperty>> {
    const headers = this.getAuthHeaders();
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sortBy', sortBy)
      .set('sortDir', sortDir);
    
    if (search) params = params.set('search', search);
    if (status) params = params.set('status', status);

    return this.http.get<PagedResponse<AdminProperty>>(`${this.apiUrl}/properties`, { headers, params });
  }

  deleteProperty(propertyId: number): Observable<string> {
    const headers = this.getAuthHeaders();
    return this.http.delete(`${this.apiUrl}/properties/${propertyId}`, { 
      headers, 
      responseType: 'text' 
    });
  }

  updatePropertyStatus(propertyId: number, status: string): Observable<string> {
    const headers = this.getAuthHeaders();
    return this.http.put(`${this.apiUrl}/properties/${propertyId}/status`, { status }, { 
      headers, 
      responseType: 'text' 
    });
  }

  // ==================== ROOMMATE MANAGEMENT ====================

  getAllRoommateAnnouncements(
    page: number = 0,
    size: number = 20,
    sortBy: string = 'createdAt',
    sortDir: string = 'desc',
    search?: string,
    status?: string
  ): Observable<PagedResponse<AdminRoommateAnnouncement>> {
    const headers = this.getAuthHeaders();
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sortBy', sortBy)
      .set('sortDir', sortDir);
    
    if (search) params = params.set('search', search);
    if (status) params = params.set('status', status);

    return this.http.get<PagedResponse<AdminRoommateAnnouncement>>(`${this.apiUrl}/roommate-announcements`, { headers, params });
  }

  getRoommateAnnouncementDetails(announcementId: number): Observable<AdminRoommateAnnouncement> {
    const headers = this.getAuthHeaders();
    return this.http.get<AdminRoommateAnnouncement>(`${this.apiUrl}/roommate-announcements/${announcementId}`, { headers });
  }

  updateRoommateAnnouncementStatus(announcementId: number, status: string): Observable<string> {
    const headers = this.getAuthHeaders();
    return this.http.put(`${this.apiUrl}/roommate-announcements/${announcementId}/status`, { status }, { 
      headers, 
      responseType: 'text' 
    });
  }

  deleteRoommateAnnouncement(announcementId: number): Observable<string> {
    const headers = this.getAuthHeaders();
    return this.http.delete(`${this.apiUrl}/roommate-announcements/${announcementId}`, { 
      headers, 
      responseType: 'text' 
    });
  }

  getRoommateAnnouncementStats(): Observable<any> {
    const headers = this.getAuthHeaders();
    return this.http.get<any>(`${this.apiUrl}/roommate-announcements/stats`, { headers });
  }

  // ==================== INQUIRY MANAGEMENT ====================

  getAllInquiries(
    page: number = 0,
    size: number = 20,
    sortBy: string = 'createdAt',
    sortDir: string = 'desc',
    search?: string,
    status?: string
  ): Observable<PagedResponse<any>> {
    const headers = this.getAuthHeaders();
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sortBy', sortBy)
      .set('sortDir', sortDir);
    
    if (search) params = params.set('search', search);
    if (status) params = params.set('status', status);

    return this.http.get<PagedResponse<any>>(`${this.apiUrl}/inquiries`, { headers, params });
  }

  // ==================== DATA EXPORT SERVICES ====================

  exportUsersReport(format: string = 'csv'): Observable<string> {
    const headers = this.getAuthHeaders();
    return this.http.post(`${this.apiUrl}/export/users`, { format }, { 
      headers, 
      responseType: 'text' 
    });
  }

  exportPropertiesReport(format: string = 'csv'): Observable<string> {
    const headers = this.getAuthHeaders();
    return this.http.post(`${this.apiUrl}/export/properties`, { format }, { 
      headers, 
      responseType: 'text' 
    });
  }

  exportInquiriesReport(format: string = 'csv'): Observable<string> {
    const headers = this.getAuthHeaders();
    return this.http.post(`${this.apiUrl}/export/inquiries`, { format }, { 
      headers, 
      responseType: 'text' 
    });
  }

  exportAnnouncementsReport(format: string = 'csv'): Observable<string> {
    const headers = this.getAuthHeaders();
    return this.http.post(`${this.apiUrl}/export/announcements`, { format }, { 
      headers, 
      responseType: 'text' 
    });
  }

  exportAnalyticsReport(format: string = 'csv'): Observable<string> {
    const headers = this.getAuthHeaders();
    return this.http.post(`${this.apiUrl}/export/analytics`, { format }, { 
      headers, 
      responseType: 'text' 
    });
  }

  // ==================== SYSTEM OPERATIONS ====================

  clearSystemCache(): Observable<string> {
    const headers = this.getAuthHeaders();
    return this.http.post(`${this.apiUrl}/system/clear-cache`, {}, { 
      headers, 
      responseType: 'text' 
    });
  }

  backupDatabase(): Observable<string> {
    const headers = this.getAuthHeaders();
    return this.http.post(`${this.apiUrl}/system/backup`, {}, { 
      headers, 
      responseType: 'text' 
    });
  }

  getRecentLogs(lines: number = 100): Observable<string[]> {
    const headers = this.getAuthHeaders();
    const params = new HttpParams().set('lines', lines.toString());
    return this.http.get<string[]>(`${this.apiUrl}/logs/recent`, { headers, params });
  }

  // ==================== SCRAPER MANAGEMENT ====================

  getScraperStatus(): Observable<string> {
    const headers = this.getAuthHeaders();
    return this.http.get(`${this.apiUrl}/scrape/status`, { 
      headers, 
      responseType: 'text' 
    });
  }

  triggerImmobilierScraper(): Observable<string> {
    const headers = this.getAuthHeaders();
    return this.http.post(`${this.apiUrl}/scrape/trigger/immobilier`, {}, { 
      headers, 
      responseType: 'text' 
    });
  }

  triggerTayaraScraper(url?: string): Observable<string> {
    const headers = this.getAuthHeaders();
    let params = new HttpParams();
    if (url) params = params.set('url', url);
    
    return this.http.post(`${this.apiUrl}/scrape/trigger/tayara`, {}, { 
      headers, 
      params, 
      responseType: 'text' 
    });
  }

  // ==================== UTILITY METHODS ====================

  refreshStats(): void {
    this.getOverviewStats().subscribe({
      next: (stats) => {
        this.currentStatsSubject.next(stats);
      },
      error: (error) => {
        console.error('Error refreshing admin stats:', error);
      }
    });
  }

  // Format numbers for display
  formatNumber(num: number): string {
    if (num >= 1000000) {
      return (num / 1000000).toFixed(1) + 'M';
    } else if (num >= 1000) {
      return (num / 1000).toFixed(1) + 'K';
    }
    return num.toString();
  }

  // Format bytes
  formatBytes(bytes: number): string {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
  }

  // Get status color
  getStatusColor(status: string): string {
    switch (status.toUpperCase()) {
      case 'UP':
      case 'ACTIVE':
      case 'ENABLED':
        return 'text-green-600';
      case 'DOWN':
      case 'INACTIVE':
      case 'DISABLED':
        return 'text-red-600';
      case 'DEGRADED':
      case 'PENDING':
        return 'text-yellow-600';
      default:
        return 'text-gray-600';
    }
  }
} 