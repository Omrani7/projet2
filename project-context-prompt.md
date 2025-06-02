# 🎯 COMPREHENSIVE PROJECT CONTEXT PROMPT

## 📋 **Project Overview**
I'm working on **UniNest** - a property management platform connecting students and property owners. The project consists of a **Spring Boot backend** and **Angular frontend** with **PostgreSQL database**. We've completed 3 sprints and are about to start Sprint 4 (Roommate Connecting Features).

---

## 🏗️ **Technology Stack & Architecture**

### **Backend:**
- **Framework**: Spring Boot 3.x (Java)
- **Database**: PostgreSQL with JPA/Hibernate
- **Security**: Spring Security with JWT authentication
- **Architecture**: Enhanced Monolithic (NOT microservices)
- **Package**: `com.example.spring_security`

### **Frontend:**
- **Framework**: Angular 17+ (TypeScript)
- **Styling**: Custom CSS with glassmorphism design system
- **Charts**: Chart.js for analytics
- **Real-time**: WebSocket integration planned

### **Project Structure:**
```
pfe/
├── spring-security/ (Main backend)
│   ├── src/main/java/com/example/spring_security/
│   │   ├── controller/ (REST controllers)
│   │   ├── service/ (Business logic)
│   │   ├── repository/ (JPA repositories)
│   │   ├── model/ (Entity classes)
│   │   ├── dto/ (Data transfer objects)
│   │   ├── config/ (Configuration classes)
│   │   └── exception/ (Custom exceptions)
│   └── frontend/ui/src/app/ (Angular frontend)
│       ├── pages/ (Main page components)
│       ├── components/ (Reusable components)
│       ├── services/ (Angular services)
│       ├── models/ (TypeScript interfaces)
│       └── assets/
└── scraping_module/ (Separate module for web scraping)
```

---

## 🗄️ **Database Schema (Key Tables)**

### **Users & Authentication:**
- `users` (id, email, name, role: STUDENT/OWNER, created_at, etc.)
- `user_profiles` (user_id, phone, address, preferences, etc.)

### **Properties & Inquiries:**
- `properties` (id, owner_id, title, description, price, location, etc.)
- `inquiries` (id, student_id, property_id, message, status: PENDING/REPLIED/CLOSED, etc.)

### **Upcoming (Sprint 3):**
- `roommate_announcements` (id, poster_id, property_id, preferences, financials, etc.)
- `roommate_applications` (id, announcement_id, applicant_id, compatibility_score, etc.)
- `messages` (id, conversation_id, sender_id, content, timestamp, etc.)

---

## 🎨 **Frontend Architecture**

### **Key Components:**
- **Pages**: `student-dashboard`, `my-inquiries`, `property-details`, `owner-dashboard`
- **Components**: `header`, `filter-bar`, `property-card`, `inquiry-list-item`
- **Services**: `analytics.service.ts`, `inquiry.service.ts`, `auth.service.ts`

### **Design System:**
- **Theme**: Glassmorphism with purple-blue gradients
- **Colors**: Primary purple (#8B5CF6), gradient backgrounds
- **Layout**: Responsive with modern UI patterns

### **Routing Structure:**
```typescript
Routes = [
  { path: 'student/dashboard', component: StudentDashboardComponent },
  { path: 'my-inquiries', component: MyInquiriesComponent },
  { path: 'discovery', component: DiscoveryComponent },
  { path: 'owner/dashboard', component: OwnerDashboardComponent },
  // More routes...
];
```

---

## 🔧 **Backend Services & Controllers**

### **Existing Controllers:**
- `InquiryController` - Handles student inquiries to owners
- `PropertyController` - Property management and listing
- `UserController` - User management and profiles
- `AnalyticsController` - Dashboard analytics and metrics

### **Key Services:**
- `InquiryService` - Business logic for inquiry management
- `PropertyListingService` - Property operations
- `AnalyticsService` - Real + mock data for dashboards
- `UserProfileService` - User profile management

### **Authentication:**
- JWT-based authentication
- Role-based access (STUDENT/OWNER)
- Spring Security configuration

---

## 🚀 **Features Completed (Sprints 1-2)**

### **Sprint 1: Core Property & Inquiry System**
- Student property search with advanced filters
- Inquiry creation and management
- Owner inquiry handling and responses
- Basic analytics dashboards

### **Sprint 2: Enhanced Analytics & UI**
- Student dashboard with real-time analytics
- Owner analytics with Chart.js integration
- Advanced filter system (price ranges, location, bedrooms)
- My Inquiries page with status tracking
- Glassmorphism design system implementation

### **Key Features Working:**
- ✅ User authentication and authorization
- ✅ Property search and filtering
- ✅ Inquiry creation and management
- ✅ Real-time dashboard analytics (with fallback to mock data)
- ✅ Responsive UI with modern design
- ✅ Owner and student role separation

---

## 📊 **Data Models & Relationships**

### **Core Entities:**
```java
@Entity User {
    Long id;
    String email, name;
    UserRole role; // STUDENT, OWNER
    LocalDateTime createdAt;
    // Relationships with UserProfile, Properties, Inquiries
}

@Entity Inquiry {
    Long id;
    Long studentId, propertyId;
    String message, ownerResponse;
    InquiryStatus status; // PENDING_REPLY, REPLIED, CLOSED
    LocalDateTime createdAt, respondedAt;
}

@Entity Property {
    Long id;
    Long ownerId;
    String title, description, address;
    BigDecimal price;
    Integer rooms, area;
    PropertyType type; // APARTMENT, HOUSE, STUDIO
}
```

### **Analytics Models:**
- Student analytics: inquiries sent, favorites, response times
- Owner analytics: property performance, revenue metrics, inquiry trends

---

## 🔄 **Current Sprint 3: Roommate Connecting Features**

### **Objectives:**
1. **Roommate Announcements**: Students can post roommate requests (from closed deals OR manual)
2. **Application System**: Students apply to roommate announcements
3. **Real-time Messaging**: WebSocket-based chat between students
4. **Compatibility Scoring**: Java-based algorithm (NOT TensorFlow)
5. **Recommendations**: Personalized roommate matching

### **New Architecture Additions:**
- WebSocket integration within existing Spring Boot app
- New controllers: `RoommateController`, `MessagingController`
- New services: `RoommateService`, `CompatibilityService`, `MessagingService`
- New Angular pages: `browse-roommates`, `post-announcement`, `conversations`

---

## 🔗 **Integration Points & Dependencies**

### **Backend Dependencies (pom.xml):**
```xml
<!-- Existing: Spring Boot, Spring Security, JPA, PostgreSQL driver -->
<!-- New for Sprint 3: -->
<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-websocket</artifactId>
</dependency>
```

### **Frontend Dependencies (package.json):**
```json
{
  "dependencies": {
    "@angular/core": "^17.0.0",
    "@angular/common": "^17.0.0",
    "chart.js": "^4.0.0",
    "rxjs": "^7.8.0"
    // New: "socket.io-client" for WebSocket
  }
}
```

### **API Patterns:**
- REST endpoints: `/api/v1/{resource}`
- Authentication: Bearer JWT tokens
- Error handling: Consistent exception handling
- Pagination: Page-based with size/sort parameters

---

## 🎯 **Development Guidelines**

### **Code Conventions:**
- **Java**: CamelCase, service layer pattern, @Transactional for data operations
- **TypeScript**: Reactive programming with RxJS, OnPush change detection
- **Database**: UUID primary keys, snake_case column names, proper indexing

### **Testing Strategy:**
- Unit tests for services and components
- Integration tests for API endpoints
- Mock data fallbacks for external dependencies

### **Security Considerations:**
- Input validation with @Valid annotations
- XSS protection in Angular templates
- CORS configuration for frontend-backend communication
- Rate limiting for API endpoints

---

## 📝 **Current State & Known Issues**

### **What's Working:**
- Complete authentication flow
- Property search and inquiry system
- Analytics dashboards with real-time updates
- Responsive UI across all devices

### **Recent Fixes:**
- Resolved TypeScript compilation errors in filter components
- Fixed duplicate method issues in Angular services
- Corrected template binding errors in student dashboard

### **Next Steps:**
- Implement roommate announcement system
- Add WebSocket real-time messaging
- Create compatibility scoring algorithm
- Build recommendation engine

---

## 🚨 **IMPORTANT CONSTRAINTS**

1. **Architecture**: MUST work within existing Spring Boot monolith - NO microservices
2. **ML**: Use Java-based algorithms - NO TensorFlow/Python integration
3. **Database**: Extend existing PostgreSQL schema - NO new databases
4. **Frontend**: Add to existing Angular app - NO separate frontend applications
5. **Authentication**: Use existing Spring Security setup - NO new auth systems

---

## 💡 **Context for AI Assistant**

I need you to:
- Understand this is a REAL working project with existing codebase
- Be precise and cautious with changes to avoid breaking existing functionality
- Follow existing patterns and conventions in the codebase
- Suggest practical, implementable solutions compatible with current stack
- Provide complete, working code that integrates seamlessly
- Consider performance, security, and maintainability in all suggestions

When I ask for implementations, provide:
1. **Complete code examples** that work with existing structure
2. **Database migration scripts** when needed
3. **Integration steps** for connecting new features
4. **Testing considerations** for new functionality
5. **Deployment notes** if configuration changes are needed

**Current Focus**: Sprint 3 roommate connecting features following the comprehensive plan in `roommate-connecting-sprint-plan.md`. 