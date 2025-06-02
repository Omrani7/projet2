export interface Sort {
    property: string;
    direction: 'ASC' | 'DESC';
    ignoreCase?: boolean;
    nullHandling?: 'NATIVE' | 'NULLS_FIRST' | 'NULLS_LAST';
}

export interface Pageable {
    page?: number;    // 0-indexed page number
    size?: number;    // Number of items per page
    sort?: string;    // Comma-separated sort orders, e.g., "property1,asc", "property2,desc"
                      // Alternatively, can be structured if preferred: sort?: Sort[];
} 