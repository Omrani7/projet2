import { Injectable} from "@angular/core";
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { PropertyListingDTO } from '../models/property-listing.dto';
import { Page } from '../models/page.model';
import { Pageable } from '../models/pageable.model';
import { PropertySearchCriteria } from '../models/property-search-criteria.model';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class PropertyListingService {

  private apiUrl = `${environment.apiBaseUrl}/properties`; // Base for property listings

  constructor(private http: HttpClient) { }

  /**
   * Searches for property listings based on various criteria with pagination and sorting.
   * @param criteria The search criteria.
   * @param pageable Pagination and sorting information.
   * @returns An Observable Page of matching PropertyListingDTOs.
   */
  searchProperties(criteria: PropertySearchCriteria, pageable: Pageable): Observable<Page<PropertyListingDTO>> {
    // let params = new HttpParams();
    // // Criteria
    // if (criteria.instituteId) params = params.set('instituteId', criteria.instituteId.toString());
    // if (criteria.propertyType) params = params.set('propertyType', criteria.propertyType);
    // if (criteria.minPrice) params = params.set('minPrice', criteria.minPrice.toString());
    // if (criteria.maxPrice) params = params.set('maxPrice', criteria.maxPrice.toString());
    // if (criteria.radiusKm) params = params.set('radiusKm', criteria.radiusKm.toString());
    // if (criteria.bedrooms) params = params.set('bedrooms', criteria.bedrooms.toString());
    // if (criteria.minArea) params = params.set('minArea', criteria.minArea.toString());
    // if (criteria.maxArea) params = params.set('maxArea', criteria.maxArea.toString());

    // // Pageable
    // if (pageable.page) params = params.set('page', pageable.page.toString());
    // if (pageable.size) params = params.set('size', pageable.size.toString());
    // if (pageable.sort) params = params.set('sort', pageable.sort); // e.g., "price,asc"

    // return this.http.get<Page<PropertyListingDTO>>(`${this.apiUrl}/search`, { params });

    // STUBBED for now
    console.log(`[PropertyListingService STUB] Searching properties with criteria:`, criteria, ` Pageable:`, pageable);
    const mockPage: Page<PropertyListingDTO> = {
      content: [
        { id: 1, title: 'Mock Property 1 near Institute ' + criteria.instituteId, price: 500, latitude: 36.81, longitude: 10.11, propertyType: 'APARTMENT', listingDate: new Date().toISOString(), active: true, mainImageUrl: 'https://via.placeholder.com/300x200?text=Property+1' },
        { id: 2, title: 'Mock Property 2', price: criteria.minPrice || 700, latitude: 36.82, longitude: 10.12, propertyType: 'STUDIO', listingDate: new Date().toISOString(), active: true, mainImageUrl: 'https://via.placeholder.com/300x200?text=Property+2' }
      ],
      totalPages: 1,
      totalElements: 2,
      number: pageable.page || 0,
      size: pageable.size || 10,
      sort: [{ property: 'price', direction: 'ASC' }],
      first: true,
      last: true,
      numberOfElements: 2,
      empty: false
    };
    return of(mockPage);
  }

  /**
   * Gets a specific property listing by its ID.
   * @param id The ID of the property listing.
   * @returns An Observable PropertyListingDTO if found.
   */
  getPropertyById(id: number): Observable<PropertyListingDTO> {
    // return this.http.get<PropertyListingDTO>(`${this.apiUrl}/${id}`);
    
    // STUBBED for now
    console.log(`[PropertyListingService STUB] Getting property by ID: ${id}`);
    const mockProperty: PropertyListingDTO = { 
        id: id, title: `Mock Property Detail ${id}`, description: 'Detailed description here.', 
        price: 650, latitude: 36.815, longitude: 10.115, propertyType: 'APARTMENT', 
        listingDate: new Date().toISOString(), active: true, ownerUsername: 'testuser', 
        mainImageUrl: `https://via.placeholder.com/600x400?text=Property+${id}`,
        imageUrls: [`https://via.placeholder.com/600x400?text=Property+${id}+Img1`, `https://via.placeholder.com/600x400?text=Property+${id}+Img2`],
        city: 'Mock City', district: 'Mock District', location: 'Central Mock Location', area: 75,
        rooms: 3, bedrooms: 2, bathrooms: 1
    };
    return of(mockProperty);
  }
  
  // TODO: Later for CRUD operations
  // createProperty(propertyData: /* PropertyListingCreateDTO equivalent */ any): Observable<PropertyListingDTO> { ... }
  // updateProperty(id: number, propertyData: /* PropertyListingUpdateDTO equivalent */ any): Observable<PropertyListingDTO> { ... }
  // deleteProperty(id: number): Observable<void> { ... }
} 