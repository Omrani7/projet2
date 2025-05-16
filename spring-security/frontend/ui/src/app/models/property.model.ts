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
}
