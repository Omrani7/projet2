export interface PropertyListingDTO {
    id: number;
    title: string;
    description?: string; // Optional for list view, present for detail
    price: number; // Assuming price from backend is numeric; BigDecimal becomes number
    city?: string;
    district?: string;
    location?: string; 
    area?: number;
    propertyType: string;
    latitude: number;
    longitude: number;
    rooms?: number;
    bedrooms?: number;
    bathrooms?: number;
    imageUrls?: string[];
    listingDate: string; // ISO date string, can be converted to Date object
    active: boolean;
    ownerUsername?: string;
    mainImageUrl?: string;
    // distanceToInstitute?: number; // If we decide to return this from backend later
} 