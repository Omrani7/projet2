export interface OwnerPropertyListingDto {
    id: number;
    title: string;
    description?: string;
    price: number;
    propertyType: string;
    fullAddress: string;
    city?: string;
    district?: string;
    latitude?: number;
    longitude?: number;
    formattedAddress?: string;
    rooms?: number;
    bedrooms?: number;
    bathrooms?: number;
    area?: number;
    floor?: number;
    hasBalcony?: boolean;
    securityDeposit?: number;
    availableFrom: string; // Assuming string date format from backend (e.g., "YYYY-MM-DD")
    availableTo?: string; // Assuming string date format
    paymentFrequency?: string;
    minimumStayMonths?: number;
    amenities?: string[];
    contactInfo?: string;
    imageUrls?: string[];
    ownerId: number;
    ownerUsername?: string;
    createdAt: string; // Assuming string date-time format
    updatedAt?: string; // Assuming string date-time format
    listingDate?: string; // Assuming string date-time format
    active: boolean;
} 