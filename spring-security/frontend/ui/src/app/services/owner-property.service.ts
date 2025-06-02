import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { OwnerPropertyListingDto } from '../models/owner-property-listing.dto';
import { AuthService } from '../auth/auth.service'; // For token

// Assuming an update DTO, if it's different from the main DTO
// For now, we can use Partial<OwnerPropertyListingDto> or create a specific one
// For simplicity, let's use OwnerPropertyListingDto for update payload type as well, 
// assuming backend handles partial updates gracefully or a specific UpdateDTO mirrors fields.
export interface OwnerPropertyListingUpdateDto extends Partial<OwnerPropertyListingDto> {
  // Define specific fields for update if different from OwnerPropertyListingDto
  // For example, image handling might be just URLs if not uploading new files during edit text.
}

@Injectable({
  providedIn: 'root'
})
export class OwnerPropertyService {
  private apiUrl = '/api/owner-properties';

  constructor(private http: HttpClient, private authService: AuthService) { }

  private getAuthHeaders(): HttpHeaders {
    const token = this.authService.getToken();
    if (!token) {
      console.error('Authentication token not found. User might be logged out.');
      // Redirect to login or throw a specific error that components can catch
      this.authService.handleAuthError('Your session has expired. Please log in again.');
      return new HttpHeaders(); // Return empty headers, error handling will redirect
    }
    return new HttpHeaders().set('Authorization', `Bearer ${token}`);
  }

  // Create property (if submitting FormData from service)
  createPropertyWithFormData(formData: FormData): Observable<OwnerPropertyListingDto> {
    return this.http.post<OwnerPropertyListingDto>(this.apiUrl, formData, { 
      headers: this.getAuthHeaders() // FormData sets its own Content-Type, but Auth header is needed
    })
    .pipe(catchError(this.handleError));
  }

  getMyProperties(): Observable<OwnerPropertyListingDto[]> {
    return this.http.get<OwnerPropertyListingDto[]>(`${this.apiUrl}/my-listings`, { headers: this.getAuthHeaders() })
      .pipe(catchError(this.handleError));
  }

  getPropertyById(id: number): Observable<OwnerPropertyListingDto> {
    return this.http.get<OwnerPropertyListingDto>(`${this.apiUrl}/${id}`, { headers: this.getAuthHeaders() })
      .pipe(catchError(this.handleError));
  }
  
  updateProperty(id: number, propertyData: OwnerPropertyListingUpdateDto): Observable<OwnerPropertyListingDto> {
    return this.http.put<OwnerPropertyListingDto>(`${this.apiUrl}/${id}`, propertyData, { headers: this.getAuthHeaders() })
      .pipe(catchError(this.handleError));
  }

  deleteProperty(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`, { headers: this.getAuthHeaders() })
      .pipe(catchError(this.handleError));
  }

  // Image upload during edit would be a separate, more complex flow if files are changing.
  // The current PUT on the backend likely only updates textual data and image URLs list.

  private handleError(error: any) {
    console.error('An error occurred in OwnerPropertyService:', error);
    // Forward the error so components can handle it if needed
    return throwError(() => error); 
  }
} 