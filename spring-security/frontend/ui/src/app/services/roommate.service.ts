import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError, tap } from 'rxjs/operators';
import { AuthService } from '../auth/auth.service';

// Temporary interfaces - will be moved to separate model files later
export interface RoommateAnnouncementCreateDTO {
  // Optional - for Type A announcements (based on closed deals)
  propertyListingId?: number;
  
  // Property Details (required for Type B, auto-filled for Type A)
  propertyTitle: string;
  propertyAddress: string;
  propertyLatitude?: number;
  propertyLongitude?: number;
  totalRent: number;
  totalRooms: number;
  availableRooms: number;
  propertyType: string;
  
  // Roommate Preferences
  maxRoommates: number;
  genderPreference: string;
  ageMin: number;
  ageMax: number;
  lifestyleTags: string[];
  smokingAllowed: boolean;
  petsAllowed: boolean;
  cleanlinessLevel: number;
  
  // Financial Details
  rentPerPerson: number;
  securityDeposit: number;
  utilitiesSplit: string;
  additionalCosts?: string;
  
  // Posting Details
  description: string;
  moveInDate: string; // ISO date string
  leaseDurationMonths: number;
}

export interface RoommateAnnouncementDTO {
  id: number;
  poster: {
    id: number;
    username: string;
    email: string;
    role: string;
  };
  propertyTitle: string;
  propertyAddress: string;
  totalRent: number;
  totalRooms: number;
  availableRooms: number;
  maxRoommates: number;
  rentPerPerson: number;
  securityDeposit: number;
  description: string;
  moveInDate: string;
  leaseDurationMonths: number;
  status: string;
  createdAt: string;
  expiresAt: string;
  remainingSpots: number;
  applicationCount: number;
}

export interface RoommateApplicationCreateDTO {
  announcementId: number;
  message: string;
}

export interface RoommateApplicationDTO {
  id: number;
  applicant: {
    id: number;
    username: string;
    email: string;
    role: string;
  };
  poster: {
    id: number;
    username: string;
    email: string;
    role: string;
  };
  message: string;
  compatibilityScore: number; // ML-calculated score (0.0 to 1.0)
  status: string;
  appliedAt: string;
  respondedAt?: string;
  responseMessage?: string;
}

export interface RoommateApplicationResponseDTO {
  status: 'ACCEPTED' | 'REJECTED';
  responseMessage: string;
}

export interface Page<T> {
  content: T[];
  pageable: {
    sort: {
      empty: boolean;
      sorted: boolean;
      unsorted: boolean;
    };
    offset: number;
    pageSize: number;
    pageNumber: number;
    paged: boolean;
    unpaged: boolean;
  };
  last: boolean;
  totalPages: number;
  totalElements: number;
  size: number;
  number: number;
  sort: {
    empty: boolean;
    sorted: boolean;
    unsorted: boolean;
  };
  first: boolean;
  numberOfElements: number;
  empty: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class RoommateService {
  private apiUrl = 'http://localhost:8080/api/v1/roommates';

  constructor(
    private http: HttpClient,
    private authService: AuthService
  ) {}

  private getAuthHeaders(): HttpHeaders {
    const token = this.authService.getToken();
    if (!token) {
      console.error('Authentication token not found.');
      return new HttpHeaders();
    }
    return new HttpHeaders().set('Authorization', `Bearer ${token}`);
  }

  // ========== ANNOUNCEMENT OPERATIONS ==========

  /**
   * Create a new roommate announcement
   * POST /api/v1/roommates/announcements
   */
  createAnnouncement(announcement: RoommateAnnouncementCreateDTO): Observable<RoommateAnnouncementDTO> {
    const headers = this.getAuthHeaders();
    return this.http.post<RoommateAnnouncementDTO>(`${this.apiUrl}/announcements`, announcement, { headers })
      .pipe(
        tap(result => console.log('Created roommate announcement:', result)),
        catchError(this.handleError)
      );
  }

  /**
   * Get announcements for browsing (excludes own announcements)
   * GET /api/v1/roommates/announcements
   */
  getAnnouncementsForBrowsing(page = 0, size = 10): Observable<Page<RoommateAnnouncementDTO>> {
    const headers = this.getAuthHeaders();
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    
    return this.http.get<Page<RoommateAnnouncementDTO>>(`${this.apiUrl}/announcements`, { headers, params })
      .pipe(
        tap(result => console.log(`Loaded ${result.content.length} announcements for browsing`)),
        catchError(this.handleError)
      );
  }

  /**
   * Get my announcements
   * GET /api/v1/roommates/announcements/my
   */
  getMyAnnouncements(page = 0, size = 10): Observable<Page<RoommateAnnouncementDTO>> {
    const headers = this.getAuthHeaders();
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    
    return this.http.get<Page<RoommateAnnouncementDTO>>(`${this.apiUrl}/announcements/my`, { headers, params })
      .pipe(
        tap(result => console.log(`Loaded ${result.content.length} of my announcements`)),
        catchError(this.handleError)
      );
  }

  /**
   * Get announcement by ID
   * GET /api/v1/roommates/announcements/{id}
   */
  getAnnouncementById(announcementId: number): Observable<RoommateAnnouncementDTO> {
    const headers = this.getAuthHeaders();
    return this.http.get<RoommateAnnouncementDTO>(`${this.apiUrl}/announcements/${announcementId}`, { headers })
      .pipe(
        tap(result => console.log('Loaded announcement:', result)),
        catchError(this.handleError)
      );
  }

  // ========== APPLICATION OPERATIONS ==========

  /**
   * Apply to roommate announcement with ML compatibility scoring
   * POST /api/v1/roommates/applications
   */
  applyToAnnouncement(application: RoommateApplicationCreateDTO): Observable<RoommateApplicationDTO> {
    const headers = this.getAuthHeaders();
    return this.http.post<RoommateApplicationDTO>(`${this.apiUrl}/applications`, application, { headers })
      .pipe(
        tap(result => console.log('Submitted roommate application with compatibility score:', result.compatibilityScore)),
        catchError(this.handleError)
      );
  }

  /**
   * Get applications for a specific announcement (for announcement poster)
   * GET /api/v1/roommates/applications/received
   */
  getApplicationsForAnnouncement(announcementId: number, page = 0, size = 10): Observable<Page<RoommateApplicationDTO>> {
    const headers = this.getAuthHeaders();
    const params = new HttpParams()
      .set('announcementId', announcementId.toString())
      .set('page', page.toString())
      .set('size', size.toString());
    
    return this.http.get<Page<RoommateApplicationDTO>>(`${this.apiUrl}/applications/received`, { headers, params })
      .pipe(
        tap(result => console.log(`Loaded ${result.content.length} applications for announcement`)),
        catchError(this.handleError)
      );
  }

  /**
   * Get my applications (applications I submitted)
   * GET /api/v1/roommates/applications/sent
   */
  getMyApplications(page = 0, size = 10): Observable<Page<RoommateApplicationDTO>> {
    const headers = this.getAuthHeaders();
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    
    return this.http.get<Page<RoommateApplicationDTO>>(`${this.apiUrl}/applications/sent`, { headers, params })
      .pipe(
        tap(result => console.log(`Loaded ${result.content.length} of my applications`)),
        catchError(this.handleError)
      );
  }

  /**
   * Respond to roommate application (accept/reject)
   * PUT /api/v1/roommates/applications/{applicationId}/respond
   */
  respondToApplication(applicationId: number, response: RoommateApplicationResponseDTO): Observable<RoommateApplicationDTO> {
    const headers = this.getAuthHeaders();
    return this.http.put<RoommateApplicationDTO>(`${this.apiUrl}/applications/${applicationId}/respond`, response, { headers })
      .pipe(
        tap(result => console.log(`Application ${response.status.toLowerCase()}:`, result)),
        catchError(this.handleError)
      );
  }

  /**
   * Get closed deals for student (from existing inquiry system)
   * GET /api/v1/roommates/closed-deals
   */
  getClosedDealsForStudent(page = 0, size = 10): Observable<Page<any>> {
    const headers = this.getAuthHeaders();
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    
    return this.http.get<Page<any>>(`${this.apiUrl}/closed-deals`, { headers, params })
      .pipe(
        tap(result => console.log(`Loaded ${result.content.length} closed deals`)),
        catchError(this.handleError)
      );
  }

  // ========== UTILITY METHODS ==========

  /**
   * Get compatibility level description from score
   */
  getCompatibilityLevel(score: number): string {
    if (score >= 0.90) return 'Excellent';
    if (score >= 0.75) return 'Very Good';
    if (score >= 0.60) return 'Good';
    if (score >= 0.40) return 'Fair';
    return 'Poor';
  }

  /**
   * Get compatibility percentage for display
   */
  getCompatibilityPercentage(score: number): number {
    return Math.round(score * 100);
  }

  /**
   * Get CSS class for compatibility level
   */
  getCompatibilityClass(score: number): string {
    if (score >= 0.90) return 'compatibility-excellent';
    if (score >= 0.75) return 'compatibility-very-good';
    if (score >= 0.60) return 'compatibility-good';
    if (score >= 0.40) return 'compatibility-fair';
    return 'compatibility-poor';
  }

  /**
   * Error handler
   */
  private handleError(error: any) {
    console.error('RoommateService error:', error);
    
    let errorMessage = 'An error occurred';
    
    if (error.status === 401) {
      errorMessage = 'You must be logged in to perform this action';
    } else if (error.status === 403) {
      errorMessage = 'You do not have permission to perform this action';
    } else if (error.status === 404) {
      errorMessage = 'The requested resource was not found';
    } else if (error.status === 409) {
      errorMessage = 'This action conflicts with existing data';
    } else if (error.error?.message) {
      errorMessage = error.error.message;
    } else if (error.message) {
      errorMessage = error.message;
    }
    
    return throwError(() => errorMessage);
  }
} 