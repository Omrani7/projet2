export interface PropertySearchCriteria {
    genericQuery?: string; // For general text search from landing page
    instituteId?: number;
    propertyType?: string;
    minPrice?: number;
    maxPrice?: number;
    radiusKm?: number;
    bedrooms?: number;
    minArea?: number;
    maxArea?: number;
    // Add other potential filter fields based on your FilterModalComponent
    // e.g., locality?: string;
    // moveInMonth?: string; // Or Date
    // stayDuration?: string; // Or number of months
    // housingType?: string; // This might be same as propertyType or more granular
} 