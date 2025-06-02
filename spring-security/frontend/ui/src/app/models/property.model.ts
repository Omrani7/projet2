export interface Property {
  id: string | number; // Or number, depending on your backend
  imageUrl: string;
  title: string;
  address: string; // Or more specific address fields like city, street
  price: number;
  currency: string; // e.g., 'TND', 'USD'
  beds?: number;
  baths?: number;
  area?: number; // e.g., in sqm
  propertyType?: string; // e.g., 'Apartment', 'Studio'
  images?: string[]; // Additional property images
  description?: string; // Detailed property description
  amenities?: string[]; // List of amenities (e.g., 'WiFi', 'Gym', 'Pool')
  
  // Required fields for map functionality
  latitude?: number;
  longitude?: number;
  listingDate?: string; // ISO date format
  active?: boolean;
  sourceType?: 'scraped' | 'owner'; // To distinguish property source
  
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
    from?: string; // ISO date format
    to?: string; // ISO date format
  };
  nearbyPlaces?: {
    name: string;
    distance: string; // e.g., '500m' or '1.2km'
    type: string; // e.g., 'University', 'Supermarket', 'Bus Stop'
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
