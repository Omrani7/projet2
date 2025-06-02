# 🏠 **Roommate Feature API Endpoints Documentation**

## **Base URL: `/api/v1/roommates`**

### **Authentication Required**: All endpoints require STUDENT role authentication

---

## 📋 **1. ROOMMATE ANNOUNCEMENT ENDPOINTS**

### **1.1 Create Roommate Announcement**
```http
POST /api/v1/roommates/announcements
Authorization: Bearer {jwt_token}
Content-Type: application/json
```

**Request Body (Type A - Based on Closed Deal):**
```json
{
  "propertyListingId": 123,
  "maxRoommates": 2,
  "genderPreference": "MIXED",
  "ageMin": 20,
  "ageMax": 30,
  "lifestyleTags": ["QUIET", "STUDIOUS"],
  "smokingAllowed": false,
  "petsAllowed": true,
  "cleanlinessLevel": 4,
  "rentPerPerson": 350.00,
  "securityDeposit": 500.00,
  "utilitiesSplit": "EQUAL",
  "additionalCosts": "Internet included",
  "description": "Looking for a responsible roommate for this great apartment!",
  "moveInDate": "2024-09-01",
  "leaseDurationMonths": 12
}
```

**Request Body (Type B - Manual Property Entry):**
```json
{
  "propertyTitle": "Modern 3-Bedroom Apartment",
  "propertyAddress": "123 University Street, Tunis",
  "propertyLatitude": 36.8065,
  "propertyLongitude": 10.1815,
  "totalRent": 1200.00,
  "totalRooms": 3,
  "availableRooms": 2,
  "propertyType": "APARTMENT",
  "amenities": ["WIFI", "PARKING", "BALCONY"],
  "imageUrls": ["url1.jpg", "url2.jpg"],
  "maxRoommates": 2,
  "genderPreference": "FEMALE",
  "ageMin": 18,
  "ageMax": 25,
  "lifestyleTags": ["SOCIAL", "OUTGOING"],
  "smokingAllowed": false,
  "petsAllowed": false,
  "cleanlinessLevel": 5,
  "rentPerPerson": 400.00,
  "securityDeposit": 600.00,
  "utilitiesSplit": "SHARED",
  "description": "Seeking female roommates for a modern apartment near campus!",
  "moveInDate": "2024-08-15",
  "leaseDurationMonths": 10
}
```

**Response (201 Created):**
```json
{
  "id": 456,
  "poster": {
    "id": 1,
    "username": "john_doe",
    "email": "john@example.com",
    "role": "STUDENT"
  },
  "propertyListing": {
    "id": 123,
    "title": "Modern Apartment",
    "price": 1200.00,
    "location": "University District"
  },
  "propertyTitle": "Modern 3-Bedroom Apartment",
  "propertyAddress": "123 University Street, Tunis",
  "totalRent": 1200.00,
  "totalRooms": 3,
  "availableRooms": 2,
  "propertyType": "APARTMENT",
  "maxRoommates": 2,
  "genderPreference": "MIXED",
  "ageMin": 20,
  "ageMax": 30,
  "lifestyleTags": ["QUIET", "STUDIOUS"],
  "smokingAllowed": false,
  "petsAllowed": true,
  "cleanlinessLevel": 4,
  "rentPerPerson": 350.00,
  "securityDeposit": 500.00,
  "utilitiesSplit": "EQUAL",
  "description": "Looking for a responsible roommate for this great apartment!",
  "moveInDate": "2024-09-01",
  "leaseDurationMonths": 12,
  "status": "ACTIVE",
  "createdAt": "2024-01-15T10:30:00",
  "expiresAt": "2024-04-15T10:30:00",
  "remainingSpots": 2,
  "isTypeA": true,
  "isTypeB": false,
  "applicationCount": 0,
  "currentUserApplied": false
}
```

### **1.2 Browse Available Announcements**
```http
GET /api/v1/roommates/announcements?page=0&size=10&sort=createdAt,desc
Authorization: Bearer {jwt_token}
```

**Response (200 OK):**
```json
{
  "content": [
    {
      "id": 456,
      "poster": {
        "id": 2,
        "username": "jane_smith",
        "email": "jane@example.com",
        "role": "STUDENT"
      },
      "propertyTitle": "Cozy Studio Apartment",
      "propertyAddress": "456 Student Avenue, Tunis",
      "rentPerPerson": 300.00,
      "moveInDate": "2024-09-01",
      "genderPreference": "FEMALE",
      "availableRooms": 1,
      "remainingSpots": 1,
      "applicationCount": 3,
      "currentUserApplied": false,
      "status": "ACTIVE",
      "createdAt": "2024-01-15T09:00:00"
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 10,
    "sort": {
      "sorted": true,
      "orderBy": "createdAt",
      "direction": "DESC"
    }
  },
  "totalElements": 15,
  "totalPages": 2,
  "first": true,
  "last": false
}
```

### **1.3 Get My Announcements**
```http
GET /api/v1/roommates/announcements/my?page=0&size=10
Authorization: Bearer {jwt_token}
```

### **1.4 Get Announcement Details**
```http
GET /api/v1/roommates/announcements/456
Authorization: Bearer {jwt_token}
```

### **1.5 Get Application Count**
```http
GET /api/v1/roommates/announcements/456/application-count
Authorization: Bearer {jwt_token}
```

**Response (200 OK):**
```json
{
  "applicationCount": 5,
  "remainingSpots": 1
}
```

---

## 📝 **2. ROOMMATE APPLICATION ENDPOINTS**

### **2.1 Apply to Announcement**
```http
POST /api/v1/roommates/applications
Authorization: Bearer {jwt_token}
Content-Type: application/json
```

**Request Body:**
```json
{
  "announcementId": 456,
  "message": "Hi! I'm a 22-year-old engineering student. I'm clean, quiet, and responsible. I'd love to be your roommate!"
}
```

**Response (201 Created):**
```json
{
  "id": 789,
  "announcement": {
    "id": 456,
    "propertyTitle": "Modern 3-Bedroom Apartment",
    "poster": {
      "id": 2,
      "username": "jane_smith",
      "email": "jane@example.com",
      "role": "STUDENT"
    }
  },
  "applicant": {
    "id": 1,
    "username": "john_doe",
    "email": "john@example.com",
    "role": "STUDENT"
  },
  "message": "Hi! I'm a 22-year-old engineering student. I'm clean, quiet, and responsible. I'd love to be your roommate!",
  "compatibilityScore": 0.85,
  "status": "PENDING",
  "appliedAt": "2024-01-15T14:30:00",
  "respondedAt": null,
  "responseMessage": null,
  "compatibilityPercentage": 85,
  "canBeWithdrawn": true,
  "qualityCategory": "HIGH"
}
```

### **2.2 Get Received Applications**
```http
GET /api/v1/roommates/applications/received?announcementId=456&page=0&size=10
Authorization: Bearer {jwt_token}
```

**Response (200 OK):**
```json
{
  "content": [
    {
      "id": 789,
      "applicant": {
        "id": 3,
        "username": "mike_wilson",
        "email": "mike@example.com",
        "role": "STUDENT"
      },
      "message": "I'm a quiet computer science student looking for a peaceful place to study.",
      "compatibilityScore": 0.92,
      "status": "PENDING",
      "appliedAt": "2024-01-15T12:00:00",
      "compatibilityPercentage": 92,
      "qualityCategory": "HIGH"
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 10
  },
  "totalElements": 8,
  "totalPages": 1
}
```

### **2.3 Get Sent Applications**
```http
GET /api/v1/roommates/applications/sent?page=0&size=10
Authorization: Bearer {jwt_token}
```

### **2.4 Respond to Application**
```http
PUT /api/v1/roommates/applications/789/respond
Authorization: Bearer {jwt_token}
Content-Type: application/json
```

**Request Body (Accept):**
```json
{
  "status": "ACCEPTED",
  "responseMessage": "Welcome! I'd love to have you as my roommate. Let's discuss the details!"
}
```

**Request Body (Reject):**
```json
{
  "status": "REJECTED",
  "responseMessage": "Thank you for your interest, but I've decided to go with another applicant."
}
```

**Response (200 OK):**
```json
{
  "id": 789,
  "applicant": {
    "id": 3,
    "username": "mike_wilson",
    "email": "mike@example.com",
    "role": "STUDENT"
  },
  "message": "I'm a quiet computer science student looking for a peaceful place to study.",
  "compatibilityScore": 0.92,
  "status": "ACCEPTED",
  "appliedAt": "2024-01-15T12:00:00",
  "respondedAt": "2024-01-15T16:45:00",
  "responseMessage": "Welcome! I'd love to have you as my roommate. Let's discuss the details!",
  "compatibilityPercentage": 92,
  "canBeWithdrawn": false
}
```

---

## 🏢 **3. CLOSED DEALS ENDPOINT**

### **3.1 Get Closed Deals for Type A Announcements**
```http
GET /api/v1/roommates/closed-deals?page=0&size=10
Authorization: Bearer {jwt_token}
```

**Response (200 OK):**
```json
{
  "content": [
    {
      "id": 123,
      "student": {
        "id": 1,
        "username": "john_doe",
        "email": "john@example.com",
        "role": "STUDENT"
      },
      "owner": {
        "id": 5,
        "username": "property_owner",
        "email": "owner@example.com",
        "role": "OWNER"
      },
      "property": {
        "id": 456,
        "title": "Modern Apartment Near Campus",
        "price": 800.00,
        "location": "University District",
        "city": "Tunis"
      },
      "message": "I'm interested in renting this property",
      "timestamp": "2024-01-10T09:00:00",
      "reply": "Congratulations! The property is yours.",
      "replyTimestamp": "2024-01-10T14:30:00",
      "status": "CLOSED"
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 10
  },
  "totalElements": 3,
  "totalPages": 1
}
```

---

## ⚙️ **4. BUSINESS LOGIC FEATURES**

### **4.1 Type A vs Type B Announcements**
- **Type A**: Created from closed property deals (propertyListingId provided)
  - Property details auto-populated from existing PropertyListing
  - Validates that student has a closed deal for the property
- **Type B**: Manual property entry (no propertyListingId)
  - All property details manually entered by student

### **4.2 Application Workflow**
1. Student applies to announcement
2. Compatibility score calculated (ML integration ready)
3. Poster receives email + WebSocket notification
4. Poster can accept/reject with optional message
5. Applicant receives email + WebSocket notification

### **4.3 Security & Validation**
- All endpoints require STUDENT role
- Users cannot apply to their own announcements
- Users cannot apply twice to the same announcement
- Only poster can view/respond to applications
- Applications cannot be modified after response

### **4.4 Integration Points**
- **Email Service**: Notifications for applications and responses
- **WebSocket Service**: Real-time notifications
- **Inquiry System**: Closed deals verification for Type A
- **Property Listings**: Auto-population for Type A announcements

---

## 🔒 **5. ERROR RESPONSES**

### **403 Forbidden - Access Denied**
```json
{
  "timestamp": "2024-01-15T10:30:00",
  "status": 403,
  "error": "Forbidden",
  "message": "You can only respond to applications for your own announcements",
  "path": "/api/v1/roommates/applications/789/respond"
}
```

### **400 Bad Request - Validation Error**
```json
{
  "timestamp": "2024-01-15T10:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "You have already applied to this announcement",
  "path": "/api/v1/roommates/applications"
}
```

### **404 Not Found**
```json
{
  "timestamp": "2024-01-15T10:30:00",
  "status": 404,
  "error": "Not Found",
  "message": "Announcement not found",
  "path": "/api/v1/roommates/announcements/999"
}
```

---

## 📊 **6. PAGINATION & FILTERING**

All paginated endpoints support:
- `page`: Page number (0-based)
- `size`: Page size (default: 20)
- `sort`: Sort criteria (e.g., `createdAt,desc`, `rentPerPerson,asc`)

**Example:**
```http
GET /api/v1/roommates/announcements?page=0&size=5&sort=rentPerPerson,asc
```

---

## 🎯 **7. FRONTEND INTEGRATION NOTES**

### **Dashboard Components**
- **Browse Announcements**: Use pagination with search/filter
- **My Announcements**: Track applications and responses
- **Application Management**: Accept/reject with messaging
- **Type A Creation**: Select from closed deals
- **Type B Creation**: Full property entry form

### **Real-time Features**
- WebSocket integration for instant notifications
- Application count updates
- Status change notifications

### **Mobile Responsive**
- All endpoints return consistent JSON structure
- Optimized for mobile app consumption
- Offline-friendly data structure

---

## ✅ **8. IMPLEMENTATION STATUS**

### **✅ COMPLETED**
- [x] Database schema (6 tables)
- [x] Entity models (6 entities)
- [x] Repository layer (6 repositories + 50+ queries)
- [x] Service layer (RoommateService - 600+ lines)
- [x] DTO layer (7 DTOs with validation)
- [x] Controller layer (RoommateController - 9 endpoints)
- [x] Integration with existing systems
- [x] Email notifications
- [x] WebSocket notifications

### **🚀 READY FOR FRONTEND**
All backend components are production-ready and fully integrated with the existing UniNest architecture! 