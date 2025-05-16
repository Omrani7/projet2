import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { PropertyListingDTO} from "../../../../src/app/models/property-listing.dto";
import { Page} from "../../../../src/app/models/page.model";
import { Pageable} from "../../../../src/app/models/pageable.model";
import { PropertySearchCriteria} from "../../../../src/app/models/property-search-criteria.model";
import { environment} from "../../../../src/environments/environment";

@Injectable({
  providedIn: 'root'
})
export class PropertyListingService {

  private apiUrl = `${environment.apiBaseUrl}/properties`; // Corrected: Removed redundant /api/v1

  constructor(private http: HttpClient) { }

  searchProperties(criteria: PropertySearchCriteria, pageable: Pageable): Observable<Page<PropertyListingDTO>> {
    let params = new HttpParams();

    // Criteria - only add if defined
    Object.entries(criteria).forEach(([key, value]) => {
      if (value !== undefined && value !== null) {
        params = params.set(key, String(value));
      }
    });

    // Pageable
    if (pageable.page !== undefined) params = params.set('page', pageable.page.toString());
    if (pageable.size !== undefined) params = params.set('size', pageable.size.toString());
    if (pageable.sort) params = params.set('sort', pageable.sort); // e.g., "price,asc" or "distance,asc"

    return this.http.get<Page<PropertyListingDTO>>(`${this.apiUrl}/search`, { params });
  }

  getPropertyById(id: number): Observable<PropertyListingDTO> {
    return this.http.get<PropertyListingDTO>(`${this.apiUrl}/${id}`);
  }

  // TODO: Later for CRUD operations
}
