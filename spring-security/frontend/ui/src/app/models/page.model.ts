import { Sort } from './pageable.model';

export interface Page<T> {
  content: T[];
  totalPages: number;
  totalElements: number;
  number: number; // Current page number (0-indexed)
  size: number;   // Page size
  sort: Sort[];   // Array of sort objects
  first: boolean;
  last: boolean;
  numberOfElements: number;
  empty: boolean;
  
  // Add these for backwards compatibility with older code
  pageable?: {
    sort: {
      empty: boolean;
      sorted: boolean;
      unsorted: boolean;
    };
    offset: number;
    pageNumber: number;
    pageSize: number;
    paged: boolean;
    unpaged: boolean;
  };
} 