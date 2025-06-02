import { Sort } from './pageable.model'; // We'll create Sort in pageable.model.ts

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
} 