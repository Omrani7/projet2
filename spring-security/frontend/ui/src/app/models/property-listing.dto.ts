export interface PropertyListingDTO {
  id: number;
  title: string;
  description: string;
  price: number;
  location: string;
  area: number;
  propertyType: string;
  contactInfo: string;
  city: string;
  district: string;
  fullAddress: string;
  latitude: number;
  longitude: number;
  formattedAddress: string;
  rooms: number;
  bedrooms: number;
  bathrooms: number;
  imageUrls: string[];
  sourceUrl: string;
  sourceWebsite: string;
  createdAt: string;
  updatedAt: string;
  listingDate: string;
  active: boolean;
  mainImageUrl?: string;
  
  // Owner-specific fields
  securityDeposit: number;
  availableTo: string;
  paymentFrequency: string;
  minimumStayMonths: number;
  hasBalcony: boolean;
  floor: number;
  amenities: string[];
  
  // Source type and owner information
  sourceType: string; // "OWNER" or "SCRAPED"
  ownerId: number;
} 