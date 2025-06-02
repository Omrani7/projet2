export interface PropertySearchCriteria {
  instituteId?: number;
  propertyType?: string;
  minPrice?: number;
  maxPrice?: number;
  radiusKm?: number;
  bedrooms?: number;
  minArea?: number;
  maxArea?: number;
  city?: string;
  district?: string;
  genericQuery?: string;
} 