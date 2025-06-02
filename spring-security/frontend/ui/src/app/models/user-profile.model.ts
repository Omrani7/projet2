export interface UserProfile {
    id: number;
    fullName?: string;
    dateOfBirth?: string; // Represent as string, can be converted to Date object if needed
    fieldOfStudy?: string;
    institute?: string;
    userType?: 'OWNER' | 'STUDENT';
    studentYear?: string;
    educationLevel?: 'BACHELOR' | 'MASTERS' | 'PHD';
    favoritePropertyIds?: number[]; // Assuming property IDs are numbers (Long maps to number)
    userId: number;
    
    // Owner-specific fields
    contactNumber?: string;
    isAgency?: boolean;
    state?: string;
    accommodationType?: string;
    propertyManagementSystem?: string;
    additionalInformation?: string;
} 