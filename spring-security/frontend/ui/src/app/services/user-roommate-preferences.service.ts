import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError, tap } from 'rxjs/operators';
import { AuthService } from '../auth/auth.service';

export interface UserRoommatePreferencesDTO {
  userId?: number;
  lifestyleTags: string[];
  cleanlinessLevel?: number;
  socialLevel?: number;
  studyHabits: string[];
  budgetMin?: number;
  budgetMax?: number;
  additionalPreferences?: string;
  updatedAt?: string;
  isComplete?: boolean;
}

export interface PreferencesStatus {
  hasPreferences: boolean;
  userId: number;
}

@Injectable({
  providedIn: 'root'
})
export class UserRoommatePreferencesService {
  private apiUrl = 'http://localhost:8080/api/v1/users/roommate-preferences';

  constructor(
    private http: HttpClient,
    private authService: AuthService
  ) {}

  /**
   * Get current user's roommate preferences
   */
  getUserPreferences(): Observable<UserRoommatePreferencesDTO> {
    const headers = this.getAuthHeaders();
    
    return this.http.get<UserRoommatePreferencesDTO>(this.apiUrl, { headers })
      .pipe(
        tap(preferences => console.log('Fetched user preferences:', preferences)),
        catchError(this.handleError)
      );
  }

  /**
   * Update current user's roommate preferences
   */
  updateUserPreferences(preferences: UserRoommatePreferencesDTO): Observable<UserRoommatePreferencesDTO> {
    const headers = this.getAuthHeaders();
    
    return this.http.put<UserRoommatePreferencesDTO>(this.apiUrl, preferences, { headers })
      .pipe(
        tap(updatedPreferences => console.log('Updated user preferences:', updatedPreferences)),
        catchError(this.handleError)
      );
  }

  /**
   * Check if current user has preferences set
   */
  getPreferencesStatus(): Observable<PreferencesStatus> {
    const headers = this.getAuthHeaders();
    
    return this.http.get<PreferencesStatus>(`${this.apiUrl}/status`, { headers })
      .pipe(
        tap(status => console.log('Preferences status:', status)),
        catchError(this.handleError)
      );
  }

  /**
   * Delete current user's roommate preferences
   */
  deleteUserPreferences(): Observable<{status: string, message: string}> {
    const headers = this.getAuthHeaders();
    
    return this.http.delete<{status: string, message: string}>(this.apiUrl, { headers })
      .pipe(
        tap(response => console.log('Deleted user preferences:', response)),
        catchError(this.handleError)
      );
  }

  /**
   * Get available lifestyle tags
   */
  getAvailableLifestyleTags(): string[] {
    return [
      'QUIET',
      'SOCIAL', 
      'STUDIOUS',
      'PARTY',
      'NIGHT_OWL',
      'EARLY_BIRD',
      'ORGANIZED',
      'RELAXED',
      'FITNESS_ORIENTED',
      'COOKING_ENTHUSIAST'
    ];
  }

  /**
   * Get available study habits
   */
  getAvailableStudyHabits(): string[] {
    return [
      'QUIET_STUDY',
      'GROUP_STUDY',
      'LIBRARY_PREFERRED',
      'HOME_STUDY',
      'MUSIC_WHILE_STUDYING',
      'LATE_NIGHT_STUDY',
      'EARLY_MORNING_STUDY',
      'WEEKEND_STUDY'
    ];
  }

  /**
   * Get cleanliness level descriptions
   */
  getCleanlinesLevelDescriptions(): {[key: number]: string} {
    return {
      1: 'Very relaxed - Occasional cleaning is fine',
      2: 'Relaxed - Clean weekly',
      3: 'Moderate - Keep common areas tidy',
      4: 'Strict - Clean and organized daily',
      5: 'Very strict - Everything spotless always'
    };
  }

  /**
   * Get social level descriptions
   */
  getSocialLevelDescriptions(): {[key: number]: string} {
    return {
      1: 'Very introverted - Prefer quiet, minimal interaction',
      2: 'Introverted - Occasional friendly chat',
      3: 'Balanced - Social but respect privacy',
      4: 'Extroverted - Enjoy regular conversations',
      5: 'Very extroverted - Love socializing and activities'
    };
  }

  /**
   * Validate preferences before submission
   */
  validatePreferences(preferences: UserRoommatePreferencesDTO): string[] {
    const errors: string[] = [];

    // Budget validation
    if (preferences.budgetMin && preferences.budgetMax) {
      if (preferences.budgetMin > preferences.budgetMax) {
        errors.push('Budget minimum must be less than or equal to budget maximum');
      }
    }

    // Location validation removed with location preferences

    // Range validations
    if (preferences.cleanlinessLevel && (preferences.cleanlinessLevel < 1 || preferences.cleanlinessLevel > 5)) {
      errors.push('Cleanliness level must be between 1 and 5');
    }

    if (preferences.socialLevel && (preferences.socialLevel < 1 || preferences.socialLevel > 5)) {
      errors.push('Social level must be between 1 and 5');
    }

    // Location radius validation removed with location preferences

    return errors;
  }

  /**
   * Check if preferences are complete enough for ML matching
   */
  isPreferencesComplete(preferences: UserRoommatePreferencesDTO): boolean {
    return !!(
      preferences.cleanlinessLevel &&
      preferences.socialLevel &&
      preferences.budgetMin &&
      preferences.budgetMax &&
      preferences.lifestyleTags &&
      preferences.lifestyleTags.length > 0
    );
  }

  private getAuthHeaders(): HttpHeaders {
    const token = this.authService.getToken();
    return new HttpHeaders({
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    });
  }

  private handleError = (error: any): Observable<never> => {
    console.error('UserRoommatePreferencesService error:', error);
    
    let errorMessage = 'An unexpected error occurred';
    
    if (error.error?.message) {
      errorMessage = error.error.message;
    } else if (error.message) {
      errorMessage = error.message;
    } else if (error.status === 0) {
      errorMessage = 'Unable to connect to server. Please check your connection.';
    } else if (error.status === 401) {
      errorMessage = 'You are not authorized to perform this action.';
    } else if (error.status === 403) {
      errorMessage = 'Access denied. Only students can manage roommate preferences.';
    } else if (error.status === 404) {
      errorMessage = 'Preferences not found.';
    } else if (error.status >= 500) {
      errorMessage = 'Server error. Please try again later.';
    }
    
    return throwError(() => new Error(errorMessage));
  };
} 