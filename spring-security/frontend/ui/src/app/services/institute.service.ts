import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { catchError, tap } from 'rxjs/operators';
import { Institute } from '../models/institute.model';

// Define environment variables here if the external file is not accessible
const environment = {
  apiBaseUrl: 'http://localhost:8080/api/v1'
};

@Injectable({
  providedIn: 'root'
})
export class InstituteService {

  private apiUrl = `${environment.apiBaseUrl}/institutes`;

  constructor(private http: HttpClient) { }

  /**
   * Searches for institutes by a name fragment.
   * @param nameFragment The fragment of the institute name to search for.
   * @returns An Observable array of matching Institutes.
   */
  searchInstitutes(nameFragment: string): Observable<Institute[]> {
    console.log('Searching for institutes with query:', nameFragment);
    
    if (!nameFragment || nameFragment.trim() === '') {
      return of([]); // Return empty array if search term is empty
    }
    
    const params = new HttpParams().set('name', nameFragment);
    
    // Use pipe and catchError for proper error handling with Observables
    return this.http.get<Institute[]>(`${this.apiUrl}/search`, { params }).pipe(
      tap(results => console.log('Fetched institutes:', results)),
      catchError(error => {
        console.error('Error fetching institutes:', error);
        // Return mock data for testing
        return of([
          { id: 1, name: 'University of Tunis', latitude: 36.806389, longitude: 10.181667, address: 'Sample Address 1', city: 'Tunis', district: 'Tunis', type: 'Public', website: 'utm.tn' },
          { id: 2, name: 'University of Carthage', latitude: 36.860833, longitude: 10.323333, address: 'Sample Address 2', city: 'Carthage', district: 'Tunis', type: 'Public', website: 'ucar.rnu.tn' },
          { id: 3, name: 'University of Manouba', latitude: 36.809722, longitude: 10.067778, address: 'Sample Address 3', city: 'Manouba', district: 'Manouba', type: 'Public', website: 'uma.rnu.tn' }
        ].filter(inst => inst.name.toLowerCase().includes(nameFragment.toLowerCase())));
      })
    );
  }

  getAllInstitutes(): Observable<Institute[]> {
    return this.http.get<Institute[]>(this.apiUrl);
  }

  getInstituteById(id: number): Observable<Institute> {
    return this.http.get<Institute>(`${this.apiUrl}/${id}`);
  }

  searchInstitutesByName(name: string): Observable<Institute[]> {
    return this.http.get<Institute[]>(`${this.apiUrl}/search?name=${name}`);
  }
}
