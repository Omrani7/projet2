import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError, tap } from 'rxjs/operators';
import { AuthService } from '../auth/auth.service';

// Enhanced interfaces with ML compatibility scores
export interface AnnouncementWithScore {
  announcement: {
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
  };
  compatibilityScore: number; // 0.0 to 1.0 (ML-calculated)
  compatibilityLevel: string; // 'Excellent', 'Very Good', 'Good', 'Fair', 'Poor'
  compatibilityPercentage: number; // 0 to 100 (for display)
  compatibilityFactors?: {
    university: number;
    fieldOfStudy: number;
    educationLevel: number;
    age: number;
  };
}

export interface UserWithScore {
  user: {
    id: number;
    username: string;
    email: string;
    role: string;
    institute?: string;
    fieldOfStudy?: string;
    educationLevel?: string;
    age?: number;
  };
  compatibilityScore: number;
  compatibilityLevel: string;
  compatibilityPercentage: number;
  recommendationReason?: string;
  rank?: number;
  application?: {
    id: number;
    message: string;
    appliedAt: string;
    status: string;
  };
}

export interface RecommendationStats {
  totalRecommendations: number;
  highQualityMatches: number; // 70%+ compatibility
  averageCompatibility: number; // 0.0 to 1.0
  topCompatibilityScore: number;
  recommendationTypes: {
    [key: string]: number;
  };
  // Additional analytics data
  unviewedRecommendations: number;
  successfulMatches: number;
  successRate: number;
}

export interface AlgorithmInfo {
  algorithm_type: string;
  description: string;
  factors: {
    university_weight: number;
    study_field_weight: number;
    education_level_weight: number;
    age_weight: number;
  };
  priority_order: string[];
  min_compatibility_threshold: number;
  high_quality_threshold: number;
  version: string;
}

@Injectable({
  providedIn: 'root'
})
export class RecommendationService {
  private apiUrl = 'http://localhost:8080/api/v1/recommendations';

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

  // ========== PERSONALIZED RECOMMENDATIONS ==========

  /**
   * Get personalized roommate recommendations based on academic profile
   * Prioritizes: University (40%) → Field of Study (25%) → Education Level (20%) → Age (15%)
   * GET /api/v1/recommendations/roommates
   */
  getPersonalizedRecommendations(limit = 10): Observable<AnnouncementWithScore[]> {
    const headers = this.getAuthHeaders();
    const params = new HttpParams().set('limit', limit.toString());
    
    return this.http.get<AnnouncementWithScore[]>(`${this.apiUrl}/roommates`, { headers, params })
      .pipe(
        tap(recommendations => {
          console.log(`Loaded ${recommendations.length} personalized recommendations`);
          recommendations.forEach(rec => {
            console.log(`${rec.announcement.propertyTitle}: ${rec.compatibilityPercentage}% (${rec.compatibilityLevel})`);
          });
        }),
        catchError(this.handleError)
      );
  }

  /**
   * Get high-quality matches (70%+ compatibility) - Premium recommendations
   * GET /api/v1/recommendations/high-quality
   */
  getHighQualityMatches(limit = 5): Observable<AnnouncementWithScore[]> {
    const headers = this.getAuthHeaders();
    const params = new HttpParams().set('limit', limit.toString());
    
    return this.http.get<AnnouncementWithScore[]>(`${this.apiUrl}/high-quality`, { headers, params })
      .pipe(
        tap(matches => {
          console.log(`Found ${matches.length} high-quality matches (70%+ compatibility)`);
          matches.forEach(match => {
            console.log(`🎯 ${match.announcement.propertyTitle}: ${match.compatibilityPercentage}% compatibility`);
          });
        }),
        catchError(this.handleError)
      );
  }

  // ========== ANNOUNCEMENT-SPECIFIC RECOMMENDATIONS ==========

  /**
   * Get compatible applicants for an announcement (for announcement posters)
   * GET /api/v1/recommendations/announcements/{announcementId}/compatible-applicants
   */
  getCompatibleApplicants(announcementId: number): Observable<UserWithScore[]> {
    const headers = this.getAuthHeaders();
    
    return this.http.get<UserWithScore[]>(`${this.apiUrl}/announcements/${announcementId}/compatible-applicants`, { headers })
      .pipe(
        tap(applicants => {
          console.log(`Found ${applicants.length} compatible applicants for announcement ${announcementId}`);
          applicants.forEach(applicant => {
            console.log(`${applicant.user.username}: ${applicant.compatibilityPercentage}% compatibility`);
          });
        }),
        catchError(this.handleError)
      );
  }

  /**
   * Get similar users for an announcement (potential matches)
   * GET /api/v1/recommendations/announcements/{announcementId}/similar-users
   */
  getSimilarUsers(announcementId: number, limit = 10): Observable<UserWithScore[]> {
    const headers = this.getAuthHeaders();
    const params = new HttpParams().set('limit', limit.toString());
    
    return this.http.get<UserWithScore[]>(`${this.apiUrl}/announcements/${announcementId}/similar-users`, { headers, params })
      .pipe(
        tap(users => console.log(`Found ${users.length} similar users for announcement ${announcementId}`)),
        catchError(this.handleError)
      );
  }

  // ========== RECOMMENDATION STATISTICS ==========

  /**
   * NEW: Get compatible students based on user profile (general recommendations)
   * Not tied to specific announcements - discover potential roommates
   * GET /api/v1/recommendations/compatible-students
   */
  getCompatibleStudents(limit = 15): Observable<UserWithScore[]> {
    const headers = this.getAuthHeaders();
    const params = new HttpParams().set('limit', limit.toString());
    
    return this.http.get<UserWithScore[]>(`${this.apiUrl}/compatible-students`, { headers, params })
      .pipe(
        tap(students => {
          console.log(`Found ${students.length} compatible students`);
          students.forEach(student => {
            console.log(`${student.user.username} (${student.user.institute}): ${student.compatibilityPercentage}% - ${student.recommendationReason}`);
          });
        }),
        catchError(this.handleError)
      );
  }

  // ========== ANALYTICS & TRACKING ==========

  /**
   * Get recommendation statistics for current user
   * GET /api/v1/recommendations/stats
   */
  getRecommendationStats(): Observable<RecommendationStats> {
    const headers = this.getAuthHeaders();
    
    return this.http.get<RecommendationStats>(`${this.apiUrl}/stats`, { headers })
      .pipe(
        tap(stats => {
          console.log('Recommendation stats:', stats);
          console.log(`Average compatibility: ${(stats.averageCompatibility * 100).toFixed(1)}%`);
          console.log(`High-quality matches: ${stats.highQualityMatches}`);
        }),
        catchError(this.handleError)
      );
  }

  /**
   * Track recommendation view (for analytics)
   * POST /api/v1/recommendations/{matchId}/view
   */
  trackRecommendationView(matchId: string): Observable<void> {
    const headers = this.getAuthHeaders();
    
    return this.http.post<void>(`${this.apiUrl}/${matchId}/view`, {}, { headers })
      .pipe(
        tap(() => console.log(`Tracked view for recommendation ${matchId}`)),
        catchError(this.handleError)
      );
  }

  /**
   * Track recommendation click (for analytics)
   * POST /api/v1/recommendations/{matchId}/click
   */
  trackRecommendationClick(matchId: string): Observable<void> {
    const headers = this.getAuthHeaders();
    
    return this.http.post<void>(`${this.apiUrl}/${matchId}/click`, {}, { headers })
      .pipe(
        tap(() => console.log(`Tracked click for recommendation ${matchId}`)),
        catchError(this.handleError)
      );
  }

  /**
   * Get algorithm information (for debugging/admin)
   * GET /api/v1/recommendations/algorithm-info
   */
  getAlgorithmInfo(): Observable<AlgorithmInfo> {
    const headers = this.getAuthHeaders();
    
    return this.http.get<AlgorithmInfo>(`${this.apiUrl}/algorithm-info`, { headers })
      .pipe(
        tap(info => {
          console.log('ML Algorithm Info:', info);
          console.log(`Type: ${info.algorithm_type}`);
          console.log(`Priority: ${info.priority_order.join(' → ')}`);
        }),
        catchError(this.handleError)
      );
  }

  // ========== UTILITY METHODS FOR ML DISPLAY ==========

  /**
   * Get compatibility color for UI display
   */
  getCompatibilityColor(score: number): string {
    if (score >= 0.90) return '#22c55e'; // Green
    if (score >= 0.75) return '#84cc16'; // Light green
    if (score >= 0.60) return '#eab308'; // Yellow
    if (score >= 0.40) return '#f97316'; // Orange
    return '#ef4444'; // Red
  }

  /**
   * Get compatibility emoji
   */
  getCompatibilityEmoji(score: number): string {
    if (score >= 0.90) return '🎯';
    if (score >= 0.75) return '⭐';
    if (score >= 0.60) return '👍';
    if (score >= 0.40) return '👌';
    return '⚠️';
  }

  /**
   * Format compatibility factors for display
   */
  formatCompatibilityFactors(factors: any): string[] {
    if (!factors) return [];
    
    return [
      `University Match: ${(factors.university * 100).toFixed(0)}%`,
      `Field of Study: ${(factors.fieldOfStudy * 100).toFixed(0)}%`,
      `Education Level: ${(factors.educationLevel * 100).toFixed(0)}%`,
      `Age Similarity: ${(factors.age * 100).toFixed(0)}%`
    ];
  }

  /**
   * Get recommendation explanation based on ML factors
   */
  getRecommendationExplanation(score: number, factors?: any): string {
    const percentage = Math.round(score * 100);
    
    if (!factors) {
      return `${percentage}% compatibility based on your academic profile`;
    }
    
    const topFactor = this.getTopCompatibilityFactor(factors);
    return `${percentage}% compatibility - Strong match in ${topFactor}`;
  }

  /**
   * Get the strongest compatibility factor
   */
  private getTopCompatibilityFactor(factors: any): string {
    if (!factors) return 'overall profile';
    
    const factorNames = {
      university: 'university',
      fieldOfStudy: 'field of study', 
      educationLevel: 'education level',
      age: 'age group'
    };
    
    let topFactor = 'university';
    let topScore = factors.university || 0;
    
    Object.keys(factors).forEach(key => {
      if (factors[key] > topScore) {
        topScore = factors[key];
        topFactor = key;
      }
    });
    
    return factorNames[topFactor as keyof typeof factorNames] || topFactor;
  }

  /**
   * Check if user should see premium recommendations
   */
  shouldShowPremiumRecommendations(stats: RecommendationStats): boolean {
    return stats.highQualityMatches > 0 || stats.averageCompatibility >= 0.6;
  }

  /**
   * Error handler
   */
  private handleError(error: any) {
    console.error('RecommendationService error:', error);
    
    let errorMessage = 'Failed to load recommendations';
    
    if (error.status === 401) {
      errorMessage = 'You must be logged in to view recommendations';
    } else if (error.status === 403) {
      errorMessage = 'You do not have permission to view recommendations';
    } else if (error.status === 404) {
      errorMessage = 'No recommendations found';
    } else if (error.error?.message) {
      errorMessage = error.error.message;
    } else if (error.message) {
      errorMessage = error.message;
    }
    
    return throwError(() => errorMessage);
  }
} 