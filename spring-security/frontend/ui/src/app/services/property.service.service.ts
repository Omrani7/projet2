import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse, HttpHeaders } from '@angular/common/http';
import { Observable, of, throwError } from 'rxjs';
import { Property } from '../models/property.model';
import { catchError, map } from 'rxjs/operators';
import { AuthService } from '../auth/auth.service';
import { OwnerPropertyListingDto } from '../models/owner-property-listing.dto';

// PropertyListingDTO interface (for scraped properties primarily, can be refined or merged)
interface ScrapedPropertyListingDTO {
  id: string | number;
  title: string;
  mainImageUrl?: string;
  imageUrls?: string[];
  address: string;
  price: number;
  currency: string;
  beds?: number;
  baths?: number;
  area?: number;
  propertyType?: string;
  description?: string;
  amenities?: string[];
  latitude?: number;
  longitude?: number;
  listingDate?: string;
  active?: boolean;
  location?: {
    lat: number;
    lng: number;
  };
  contactInfo?: {
    phone?: string;
    email?: string;
    name?: string;
  };
  availability?: {
    from?: string;
    to?: string;
  };
  nearbyPlaces?: {
    name: string;
    distance: string;
    type: string;
  }[];
  rating?: {
    average: number;
    count: number;
    reviews?: {
      userName: string;
      rating: number;
      comment: string;
      date: string;
    }[];
  };
}

@Injectable({
  providedIn: 'root'
})
export class PropertyServiceService {
  private readonly scrapedPropertiesApiUrl = '/api/v1/properties';
  private readonly ownerPropertiesApiUrl = '/api/owner-properties';

  // Mock data for development
  private mockProperties: Property[] = [
    {
      id: 1,
      imageUrl: 'https://images.unsplash.com/photo-1522708323590-d24dbb6b0267',
      images: [
        'https://images.unsplash.com/photo-1522708323590-d24dbb6b0267',
        'https://images.unsplash.com/photo-1560448204-603b3fc33ddc',
        'https://images.unsplash.com/photo-1560185007-c5ca9d2c0259'
      ],
      title: 'Modern Studio Apartment',
      address: '123 City Center, Tunis',
      price: 450,
      currency: 'TND',
      beds: 1,
      baths: 1,
      area: 45,
      propertyType: 'Studio',
      description: 'A beautiful modern studio apartment in the heart of Tunis. Perfect for students and young professionals. The apartment features high-speed internet, a fully equipped kitchen, and a comfortable living space.',
      amenities: ['WiFi', 'Air Conditioning', 'Washing Machine', 'Fully Equipped Kitchen', 'TV', 'Study Desk', 'Security'],
      location: {
        lat: 36.8065,
        lng: 10.1815
      },
      contactInfo: {
        phone: '+216 123 456 789',
        email: 'contact@example.com',
        name: 'Rental Agency'
      },
      availability: {
        from: '2023-09-01',
        to: '2024-08-31'
      },
      nearbyPlaces: [
        { name: 'University of Tunis', distance: '500m', type: 'University' },
        { name: 'City Market', distance: '200m', type: 'Supermarket' },
        { name: 'Central Bus Station', distance: '1km', type: 'Transportation' }
      ],
      rating: {
        average: 4.7,
        count: 23,
        reviews: [
          { userName: 'Ahmed M.', rating: 5, comment: 'Great place to stay! Very close to the university.', date: '2023-06-15' },
          { userName: 'Sarra B.', rating: 4, comment: 'Clean and comfortable. Good value for money.', date: '2023-05-20' }
        ]
      }
    },
    {
      id: 2,
      imageUrl: 'https://images.unsplash.com/photo-1560185008-a33f5c7b1594',
      images: [
        'https://images.unsplash.com/photo-1560185008-a33f5c7b1594',
        'https://images.unsplash.com/photo-1560185127-6ed189bf02f4',
        'https://images.unsplash.com/photo-1598928506311-c55ded91a20c'
      ],
      title: 'Spacious 2-Bedroom Apartment',
      address: '45 Seaside Avenue, La Marsa',
      price: 750,
      currency: 'TND',
      beds: 2,
      baths: 1,
      area: 75,
      propertyType: 'Apartment',
      description: 'A spacious 2-bedroom apartment in the beautiful La Marsa area. This property offers a comfortable living space with a balcony overlooking the Mediterranean Sea. Perfect for students who want to share accommodation.',
      amenities: ['WiFi', 'Air Conditioning', 'Balcony', 'Sea View', 'Washing Machine', 'Fully Equipped Kitchen', 'TV', 'Study Area', '24/7 Security'],
      location: {
        lat: 36.8789,
        lng: 10.3230
      },
      contactInfo: {
        phone: '+216 987 654 321',
        email: 'info@example.com',
        name: 'Seaside Properties'
      },
      availability: {
        from: '2023-09-15',
        to: '2024-06-30'
      },
      nearbyPlaces: [
        { name: 'Mediterranean School of Business', distance: '1.2km', type: 'University' },
        { name: 'La Marsa Beach', distance: '300m', type: 'Beach' },
        { name: 'Marsa Market', distance: '500m', type: 'Supermarket' }
      ],
      rating: {
        average: 4.5,
        count: 18,
        reviews: [
          { userName: 'Youssef K.', rating: 5, comment: 'Amazing location and very spacious. Perfect for sharing with a roommate.', date: '2023-07-10' },
          { userName: 'Leila T.', rating: 4, comment: 'Great view and comfortable rooms. Kitchen is well equipped.', date: '2023-04-25' }
        ]
      }
    }
  ];

  constructor(
    private http: HttpClient,
    private authService: AuthService
  ) { }

  getPropertyById(id: string | number): Observable<Property> {
    // First, check if we're dealing with an owner property
    if (id.toString().includes('owner-')) {
      // If it's an owner property, extract the real ID and use the owner property endpoint
      const realId = id.toString().replace('owner-', '');
      return this.getOwnerPropertyById(realId);
    }
    
    // Otherwise, treat it as a regular scraped property
    return this.http.get<ScrapedPropertyListingDTO>(`${this.scrapedPropertiesApiUrl}/${id}`).pipe(
      map((dto: ScrapedPropertyListingDTO): Property => {
        return {
          id: dto.id,
          title: dto.title,
          imageUrl: dto.mainImageUrl || (dto.imageUrls && dto.imageUrls.length > 0 ? dto.imageUrls[0] : ''),
          images: dto.imageUrls || [],
          address: dto.address,
          price: dto.price,
          currency: dto.currency,
          beds: dto.beds,
          baths: dto.baths,
          area: dto.area,
          propertyType: dto.propertyType,
          description: dto.description,
          amenities: dto.amenities,
          latitude: dto.latitude,
          longitude: dto.longitude,
          listingDate: dto.listingDate,
          active: dto.active,
          location: dto.location,
          contactInfo: dto.contactInfo,
          availability: dto.availability,
          nearbyPlaces: dto.nearbyPlaces,
          rating: dto.rating,
          sourceType: 'scraped'
        };
      }),
      catchError((error: HttpErrorResponse) => {
        console.error(`API error fetching (scraped) property ${id}:`, error);
        return throwError(() => error); 
      })
    );
  }
  
  /**
   * Gets a property created by an owner using the owner API endpoint.
   * This method should be used for owner-created properties.
   */
  getOwnerPropertyById(id: string | number): Observable<Property> {
    const token = this.authService.getToken();
    if (!token) {
      return throwError(() => new Error('Authentication required. Please log in.'));
    }
    
    const headers = new HttpHeaders().set('Authorization', `Bearer ${token}`);
    
    return this.http.get<OwnerPropertyListingDto>(`${this.ownerPropertiesApiUrl}/${id}`, { 
      headers: headers
    }).pipe(
      map((dto: OwnerPropertyListingDto): Property => {
        return {
          id: dto.id,
          title: dto.title,
          imageUrl: dto.imageUrls && dto.imageUrls.length > 0 ? dto.imageUrls[0] : 'assets/images/default-property.png',
          images: dto.imageUrls || [],
          address: dto.fullAddress || dto.city || 'Address not specified',
          price: dto.price,
          currency: 'TND',
          beds: dto.bedrooms,
          baths: dto.bathrooms,
          area: dto.area,
          propertyType: dto.propertyType,
          description: dto.description,
          amenities: dto.amenities || [],
          latitude: dto.latitude,
          longitude: dto.longitude,
          listingDate: dto.listingDate ? new Date(dto.listingDate).toISOString() : undefined,
          active: dto.active,
          location: (dto.latitude && dto.longitude) ? { lat: dto.latitude, lng: dto.longitude } : undefined,
          contactInfo: dto.contactInfo ? { name: dto.contactInfo } : undefined,
          availability: { from: dto.availableFrom ? new Date(dto.availableFrom).toISOString() : undefined, to: dto.availableTo ? new Date(dto.availableTo).toISOString() : undefined },
          sourceType: 'owner'
        };
      }),
      catchError((error: HttpErrorResponse) => {
        console.error(`API error fetching owner property ${id}:`, error);
        return throwError(() => error); 
      })
    );
  }
  
  getRecommendedProperties(count: number = 4): Observable<Property[]> {
    if (true) { 
      return of(this.mockProperties.slice(0, count).map(p => ({...p, sourceType: 'scraped'} as Property)));
    }
    return this.http.get<Property[]>(`${this.scrapedPropertiesApiUrl}/recommended?count=${count}`);
  }
  
  addReview(propertyId: string | number, review: any): Observable<any> {
    return this.http.post(`${this.scrapedPropertiesApiUrl}/${propertyId}/reviews`, review);
  }

  /**
   * Gets all properties created by the currently authenticated owner.
   * Calls the correct new endpoint for owner-specific properties.
   */
  getOwnerProperties(): Observable<Property[]> {
    const token = this.authService.getToken();
    if (!token) {
      return throwError(() => new Error('Authentication required. Please log in.'));
    }
    
    const headers = new HttpHeaders().set('Authorization', `Bearer ${token}`);
    
    return this.http.get<OwnerPropertyListingDto[]>(`${this.ownerPropertiesApiUrl}/my-listings`, { 
      headers: headers
    }).pipe(
      map((dtos: OwnerPropertyListingDto[]) => {
        if (!Array.isArray(dtos)) {
          console.error('Expected an array of OwnerPropertyListingDto from /my-listings but received:', dtos);
          return [];
        }
        return dtos.map((dto: OwnerPropertyListingDto): Property => {
          return {
            id: dto.id,
            title: dto.title,
            imageUrl: dto.imageUrls && dto.imageUrls.length > 0 ? dto.imageUrls[0] : 'assets/images/default-property.png',
            images: dto.imageUrls || [],
            address: dto.fullAddress || dto.city || 'Address not specified',
            price: dto.price,
            currency: 'TND',
            beds: dto.bedrooms,
            baths: dto.bathrooms,
            area: dto.area,
            propertyType: dto.propertyType,
            description: dto.description,
            amenities: dto.amenities || [],
            latitude: dto.latitude,
            longitude: dto.longitude,
            listingDate: dto.listingDate ? new Date(dto.listingDate).toISOString() : undefined,
            active: dto.active,
            location: (dto.latitude && dto.longitude) ? { lat: dto.latitude, lng: dto.longitude } : undefined,
            contactInfo: dto.contactInfo ? { name: dto.contactInfo } : undefined,
            availability: { from: dto.availableFrom ? new Date(dto.availableFrom).toISOString() : undefined, to: dto.availableTo ? new Date(dto.availableTo).toISOString() : undefined },
            sourceType: 'owner'
          };
        });
      }),
      catchError((error: HttpErrorResponse) => {
        console.error(`API error fetching owner properties from ${this.ownerPropertiesApiUrl}/my-listings:`, error);
        return throwError(() => error); 
      })
    );
  }
  
  /**
   * Gets the latest properties posted by owners, sorted by date
   * This does not require authentication
   */
  getLatestOwnerProperties(limit: number = 6): Observable<Property[]> {
    return this.http.get<any[]>(`${this.scrapedPropertiesApiUrl}/latest-owner?limit=${limit}`).pipe(
      map(dtos => this.mapPropertiesToModel(dtos, 'owner')),
      catchError(error => {
        console.error('Error fetching latest owner properties:', error);
        return throwError(() => error);
      })
    );
  }
  
  /**
   * Gets properties by city name
   * @param city The name of the city to filter by
   * @param limit Maximum number of properties to return (default: 10)
   * @returns Observable of Property array
   */
  getPropertiesByCity(city: string, limit: number = 10): Observable<Property[]> {
    return this.http.get<any[]>(`${this.scrapedPropertiesApiUrl}/by-city?city=${encodeURIComponent(city)}&limit=${limit}`).pipe(
      map(dtos => this.mapPropertiesToModel(dtos)),
      catchError(error => {
        console.error(`Error fetching properties for city ${city}:`, error);
        return throwError(() => error);
      })
    );
  }

  /**
   * Maps property DTOs from backend to the frontend Property model
   * @param dtos Array of property DTOs from backend
   * @param sourceType Optional source type to set for all properties
   * @returns Array of Property models
   */
  private mapPropertiesToModel(dtos: any[], sourceType?: string): Property[] {
    return dtos.map(dto => {
      const property: Property = {
        id: dto.id,
        title: dto.title,
        imageUrl: dto.mainImageUrl || (dto.imageUrls && dto.imageUrls.length > 0 ? dto.imageUrls[0] : 'assets/images/property-placeholder.jpg'),
        images: dto.imageUrls || [],
        address: dto.address || dto.location || dto.city || 'Location not specified',
        price: dto.price,
        currency: dto.currency || 'TND',
        beds: dto.bedrooms || dto.beds || 0,
        baths: dto.bathrooms || dto.baths || 0,
        area: dto.area || 0,
        propertyType: dto.propertyType,
        description: dto.description,
        amenities: dto.amenities,
        sourceType: sourceType || dto.sourceType || 'unknown'
      };
      
      return property;
    });
  }
}
