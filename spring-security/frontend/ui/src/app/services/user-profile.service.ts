import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, throwError, of } from 'rxjs';
import { catchError, map, tap, switchMap } from 'rxjs/operators';
import { UserProfile } from '../models/user-profile.model';
import { AuthService } from '../auth/auth.service'; // Corrected path

@Injectable({
  providedIn: 'root'
})
export class UserProfileService {
  private apiUrl = 'http://localhost:8080/api/profiles/students'; // Backend API URL for student profiles
  private ownerApiUrl = 'http://localhost:8080/api/profiles/owners'; // Backend API URL for owner profiles
  private currentUserProfile: UserProfile | null = null;

  constructor(private http: HttpClient, private authService: AuthService) {}

  private getAuthHeaders(): HttpHeaders {
    const token = this.authService.getToken();
    if (!token) {
      // Handle case where token is not available, though protected routes should prevent this
      console.error('Authentication token not found.');
      return new HttpHeaders(); // Or throw an error / redirect to login
    }
    return new HttpHeaders().set('Authorization', `Bearer ${token}`);
  }

  getCurrentUserId(): number | null {
    const token = this.authService.getToken();
    if (token) {
      const decodedToken = this.authService.decodeToken(token);
      return decodedToken ? decodedToken.id : null;
    }
    return null;
  }

  getStudentProfile(userId: number): Observable<UserProfile> {
    const headers = this.getAuthHeaders();
    return this.http.get<UserProfile>(`${this.apiUrl}/${userId}`, { headers })
      .pipe(
        tap(profile => this.currentUserProfile = profile),
        catchError(this.handleError)
      );
  }

  updateStudentProfile(userId: number, profileData: UserProfile): Observable<UserProfile> {
    const headers = this.getAuthHeaders();
    
    // Filter out owner-specific fields when updating student profile
    const studentProfileData = {
      id: profileData.id,
      fullName: profileData.fullName,
      dateOfBirth: profileData.dateOfBirth,
      fieldOfStudy: profileData.fieldOfStudy,
      institute: profileData.institute,
      userType: profileData.userType,
      studentYear: profileData.studentYear,
      favoritePropertyIds: profileData.favoritePropertyIds,
      userId: profileData.userId
      // Explicitly exclude owner-specific fields: contactNumber, isAgency, state, etc.
    };
    
    return this.http.put<UserProfile>(`${this.apiUrl}/${userId}`, studentProfileData, { headers })
      .pipe(
        tap(profile => this.currentUserProfile = profile),
        catchError(this.handleError)
      );
  }

  // New methods for Owner Profiles
  getOwnerProfile(ownerId: number): Observable<UserProfile> {
    const headers = this.getAuthHeaders();
    // IMPORTANT: This endpoint URL (/api/profiles/owners/:id) needs to be created on the backend.
    return this.http.get<UserProfile>(`${this.ownerApiUrl}/${ownerId}`, { headers })
      .pipe(
        tap(profile => this.currentUserProfile = profile), // Assuming owner profile can also be cached here
        catchError(this.handleError)
      );
  }

  updateOwnerProfile(ownerId: number, profileData: UserProfile): Observable<UserProfile> {
    const headers = this.getAuthHeaders();
    // IMPORTANT: This endpoint URL (/api/profiles/owners/:id) needs to be created on the backend.
    return this.http.put<UserProfile>(`${this.ownerApiUrl}/${ownerId}`, profileData, { headers })
      .pipe(
        tap(profile => this.currentUserProfile = profile), // Assuming owner profile can also be cached here
        catchError(this.handleError)
      );
  }

  // Add a property to favorites
  addToFavorites(propertyId: number): Observable<UserProfile> {
    const userId = this.getCurrentUserId();
    if (!userId) {
      return throwError(() => 'User not authenticated');
    }

    // First get the current profile if we don't have it cached
    if (!this.currentUserProfile) {
      return this.getStudentProfile(userId).pipe(
        switchMap(profile => {
          return this.updateFavorites(userId, profile, propertyId, 'add');
        }),
        catchError(this.handleError)
      );
    }

    return this.updateFavorites(userId, this.currentUserProfile, propertyId, 'add');
  }

  // Remove a property from favorites
  removeFromFavorites(propertyId: number): Observable<UserProfile> {
    const userId = this.getCurrentUserId();
    if (!userId) {
      return throwError(() => 'User not authenticated');
    }

    // First get the current profile if we don't have it cached
    if (!this.currentUserProfile) {
      return this.getStudentProfile(userId).pipe(
        switchMap(profile => {
          return this.updateFavorites(userId, profile, propertyId, 'remove');
        }),
        catchError(this.handleError)
      );
    }

    return this.updateFavorites(userId, this.currentUserProfile, propertyId, 'remove');
  }

  // Toggle favorite status
  toggleFavorite(propertyId: number): Observable<UserProfile> {
    const userId = this.getCurrentUserId();
    if (!userId) {
      return throwError(() => 'User not authenticated');
    }

    if (!this.currentUserProfile) {
      return this.getStudentProfile(userId).pipe(
        switchMap(profile => {
          const action = this.isPropertyFavorite(propertyId) ? 'remove' : 'add';
          return this.updateFavorites(userId, profile, propertyId, action);
        }),
        catchError(this.handleError)
      );
    }

    const action = this.isPropertyFavorite(propertyId) ? 'remove' : 'add';
    return this.updateFavorites(userId, this.currentUserProfile, propertyId, action);
  }

  // Check if a property is in favorites
  isPropertyFavorite(propertyId: number): boolean {
    if (!this.currentUserProfile || !this.currentUserProfile.favoritePropertyIds) {
      return false;
    }
    return this.currentUserProfile.favoritePropertyIds.includes(propertyId);
  }

  // Get all favorite properties IDs
  getFavoritePropertyIds(): number[] {
    if (!this.currentUserProfile || !this.currentUserProfile.favoritePropertyIds) {
      return [];
    }
    return this.currentUserProfile.favoritePropertyIds;
  }

  // Helper method to update favorites
  private updateFavorites(userId: number, profile: UserProfile, propertyId: number, action: 'add' | 'remove'): Observable<UserProfile> {
    const updatedProfile = { ...profile };
    
    if (!updatedProfile.favoritePropertyIds) {
      updatedProfile.favoritePropertyIds = [];
    }

    if (action === 'add' && !updatedProfile.favoritePropertyIds.includes(propertyId)) {
      updatedProfile.favoritePropertyIds = [...updatedProfile.favoritePropertyIds, propertyId];
    } else if (action === 'remove') {
      updatedProfile.favoritePropertyIds = updatedProfile.favoritePropertyIds.filter(id => id !== propertyId);
    }

    return this.updateStudentProfile(userId, updatedProfile);
  }

  private handleError(error: any) {
    console.error('An error occurred in UserProfileService:', error);
    // Could be more sophisticated error handling (e.g., user-friendly messages)
    return throwError(() => error.error?.message || error.message || 'Server error');
  }
} 