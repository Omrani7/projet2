export interface UserBasic {
  id: number;
  username: string;
  email: string;
  phoneNumber?: string;
  role: string;
}

export interface PropertyBasic {
  id: number;
  title: string;
  price: number;
  location: string;
  city: string;
  propertyType: string;
  bedrooms?: number;
  bathrooms?: number;
  imageUrls?: string[];
}

export interface Inquiry {
  id: number;
  student: UserBasic;
  owner: UserBasic;
  property: PropertyBasic;
  message: string;
  timestamp: string; // ISO date string
  reply?: string;
  replyTimestamp?: string; // ISO date string
  status: 'PENDING_REPLY' | 'REPLIED' | 'CLOSED' | 'PROPERTY_NO_LONGER_AVAILABLE';
  studentPhoneNumber?: string;
  ownerPhoneNumber?: string;
}

export interface InquiryCreate {
  propertyId: number;
  message: string;
  phoneNumber: string;
}

export interface InquiryReply {
  reply: string;
}

export interface Page<T> {
  content: T[];
  pageable: {
    sort: {
      empty: boolean;
      sorted: boolean;
      unsorted: boolean;
    };
    offset: number;
    pageSize: number;
    pageNumber: number;
    paged: boolean;
    unpaged: boolean;
  };
  last: boolean;
  totalPages: number;
  totalElements: number;
  size: number;
  number: number;
  sort: {
    empty: boolean;
    sorted: boolean;
    unsorted: boolean;
  };
  first: boolean;
  numberOfElements: number;
  empty: boolean;
}

export interface WebSocketNotification {
  type: 'NEW_INQUIRY' | 'INQUIRY_REPLY' | 'PROPERTY_NO_LONGER_AVAILABLE' | 
        'NEW_ROOMMATE_APPLICATION' | 'ROOMMATE_APPLICATION_RESPONSE' | 'ROOMMATE_MATCH_FOUND';
  inquiry?: Inquiry;
  roommateApplication?: any; // Will be properly typed when roommate models are created
  message: string;
  timestamp?: number;
  compatibilityScore?: number; // For ML-enhanced notifications
} 