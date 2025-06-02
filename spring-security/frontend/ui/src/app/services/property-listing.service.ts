import { Injectable } from '@angular/core';
import { HttpClient, HttpParams, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { PropertyListingDTO } from '../models/property-listing.dto';
import { Page } from '../models/page.model';
import { Pageable } from '../models/pageable.model';
import {PropertySearchCriteria} from '../models/property-search-criteria.model';
import { AuthService } from '../auth/auth.service';
import { map } from 'rxjs/operators';


@Injectable({
  providedIn: 'root'
})
export class PropertyListingService {
  private apiUrl = 'http://localhost:8080/api/v1/properties';

  constructor(private http: HttpClient, private authService: AuthService) { }

  getAllProperties(page: number = 0, size: number = 10): Observable<Page<PropertyListingDTO>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http.get<Page<PropertyListingDTO>>(this.apiUrl, { params });
  }

  getPropertyById(id: number): Observable<PropertyListingDTO> {
    return this.http.get<PropertyListingDTO>(`${this.apiUrl}/${id}`);
  }

  searchProperties(criteria: PropertySearchCriteria, pageable: Pageable | number = 0): Observable<Page<PropertyListingDTO>> {
    let params = new HttpParams();

    // Add criteria parameters
    if (criteria.genericQuery) params = params.set('genericQuery', criteria.genericQuery);
    if (criteria.propertyType) params = params.set('propertyType', criteria.propertyType);
    if (criteria.minPrice !== undefined) params = params.set('minPrice', criteria.minPrice.toString());
    if (criteria.maxPrice !== undefined) params = params.set('maxPrice', criteria.maxPrice.toString());
    if (criteria.bedrooms !== undefined) params = params.set('bedrooms', criteria.bedrooms.toString());
    if (criteria.minArea !== undefined) params = params.set('minArea', criteria.minArea.toString());
    if (criteria.maxArea !== undefined) params = params.set('maxArea', criteria.maxArea.toString());
    if (criteria.instituteId !== undefined) params = params.set('instituteId', criteria.instituteId.toString());
    if (criteria.radiusKm !== undefined) params = params.set('radiusKm', criteria.radiusKm.toString());

    // Handle pagination
    if (typeof pageable === 'number') {
      // Handle case where pageable is just a page number
      params = params.set('page', pageable.toString());
      params = params.set('size', '10'); // Default size
    } else {
      // Handle case where pageable is a Pageable object
      if (pageable.page !== undefined) {
        params = params.set('page', pageable.page.toString());
      } else {
        params = params.set('page', '0'); // Default to first page
      }

      if (pageable.size !== undefined) {
        params = params.set('size', pageable.size.toString());
      } else {
        params = params.set('size', '10'); // Default size
      }

      if (pageable.sort) {
        params = params.set('sort', pageable.sort);
      }
    }

    return this.http.get<Page<PropertyListingDTO>>(`${this.apiUrl}/search`, { params });
  }

  getLatestProperties(limit: number = 8): Observable<PropertyListingDTO[]> {
    return this.http.get<PropertyListingDTO[]>(`${this.apiUrl}/latest?limit=${limit}`);
  }

  // Owner property management methods

  createProperty(property: any): Observable<PropertyListingDTO> {
    return this.http.post<PropertyListingDTO>(this.apiUrl, property);
  }

  updateProperty(id: number, property: any): Observable<PropertyListingDTO> {
    return this.http.put<PropertyListingDTO>(`${this.apiUrl}/${id}`, property);
  }

  deleteProperty(id: number): Observable<void> {
    console.log('[PropertyListingService] Deleting property with ID:', id);
    const tokenForCall = this.authService.getToken();
    
    // Create HttpHeaders with the token
    const headers = new HttpHeaders({
      'Authorization': `Bearer ${tokenForCall}`
    });
    
    // Pass the headers explicitly in the options
    return this.http.delete<void>(`${this.apiUrl}/${id}`, { headers });
  }

  uploadPropertyImages(propertyId: number, formData: FormData): Observable<PropertyListingDTO> {
    return this.http.post<PropertyListingDTO>(`${this.apiUrl}/${propertyId}/images`, formData);
  }

  // Get owner's properties
  getOwnerProperties(): Observable<PropertyListingDTO[]> {
    const ownerApiUrl = `${this.apiUrl}/owner`;
    console.log('[PropertyListingService] Fetching owner properties from URL:', ownerApiUrl);
    const tokenForCall = this.authService.getToken();
    console.log('[PropertyListingService] Token value just before getOwnerProperties call:', tokenForCall);
    
    // Create HttpHeaders with the token
    const headers = new HttpHeaders({
      'Authorization': `Bearer ${tokenForCall}`
    });
    
    // Pass the headers explicitly in the options
    return this.http.get<PropertyListingDTO[]>(ownerApiUrl, { headers })
      .pipe(
        map(properties => {
          // Filter to only include properties with sourceType = 'OWNER'
          const ownerProperties = properties.filter(prop => prop.sourceType === 'OWNER');
          console.log('[PropertyListingService] Filtered owner properties:', 
                      ownerProperties.length, 'out of', properties.length);
          return ownerProperties;
        })
      );
  }

  // Delete property using owner-specific endpoint
  deleteOwnerProperty(id: number): Observable<void> {
    console.log('[PropertyListingService] Deleting owner property with ID:', id);
    const tokenForCall = this.authService.getToken();
    
    // Create HttpHeaders with the token
    const headers = new HttpHeaders({
      'Authorization': `Bearer ${tokenForCall}`
    });
    
    // Use the owner-specific endpoint
    return this.http.delete<void>(`${this.apiUrl}/owner/${id}`, { headers });
  }
}
