# UniNest - Comprehensive Project Context Documentation

## 🏢 **Project Overview**

**Project Name**: UniNest  
**Description**: University-focused property management and roommate matching platform  
**Architecture**: Monolithic Spring Boot backend + Angular 17+ frontend + PostgreSQL database  
**Purpose**: Connect university students with property owners and facilitate roommate matching with ML-powered compatibility scoring

---

## 🏗️ **System Architecture**

### **Technology Stack**
- **Backend**: Spring Boot 3.x, Java 21
- **Frontend**: Angular 17+ (Standalone components)
- **Database**: PostgreSQL 15+
- **Authentication**: JWT + OAuth2 (Google)
- **Real-time**: WebSocket (Native, not SockJS)
- **ORM**: JPA/Hibernate
- **Build Tools**: Maven (Backend), npm/Angular CLI (Frontend)

### **Project Structure**
```
pfe/
├── spring-security/ (Main Spring Boot Application)
│   ├── src/main/java/com/example/spring_security/
│   │   ├── config/ (Security, WebSocket, CORS)
│   │   ├── controller/ (REST endpoints)
│   │   ├── dao/ (Repository interfaces)
│   │   ├── dto/ (Data Transfer Objects)
│   │   ├── exception/ (Custom exceptions)
│   │   ├── model/ (JPA Entities)
│   │   └── service/ (Business logic)
│   ├── frontend/ui/ (Angular 17+ Application)
│   │   ├── src/app/
│   │   │   ├── auth/ (Authentication components)
│   │   │   ├── components/ (Reusable UI components)
│   │   │   ├── models/ (TypeScript interfaces)
│   │   │   ├── pages/ (Route components)
│   │   │   └── services/ (HTTP services)
│   │   └── package.json
│   └── pom.xml
├── scraping_module/ (Property scraping microservice)
└── uploads/ (File storage)
```

---

## 🚀 **Current Implementation Status**

### **✅ Completed Features**

#### **Authentication System**
- JWT-based authentication
- OAuth2 Google integration
- Password reset functionality
- Role-based access control (STUDENT, OWNER, ADMIN)
- Session management

#### **Property Management**
- Property listing CRUD operations
- Image upload and management
- Property discovery with filtering
- Map integration (OpenLayers)
- Property analytics for owners

#### **Inquiry System**
- Student-to-owner inquiry workflow
- Real-time WebSocket notifications
- Email notifications
- Deal closing functionality
- Inquiry status management

#### **User Management**
- User registration and profiles
- Profile completion tracking
- Institute data seeding (124 universities)
- Phone number verification

#### **Dashboard Systems**
- Owner dashboard with analytics
- Student dashboard
- Recent activities tracking
- Statistics and charts

### **🔄 In Progress: Roommate Features (Sprint 4)**

#### **Roommate Announcements**
- ✅ Backend entities and repositories
- ✅ DTOs and validation
- ✅ Service layer with ML compatibility scoring
- ✅ REST API endpoints
- 🔄 Frontend components (partially implemented)

#### **ML Compatibility Scoring**
- ✅ Academic-focused compatibility algorithm
- ✅ University matching (40% weight)
- ✅ Field of study compatibility (25% weight)
- ✅ Education level matching (20% weight)
- ✅ Age compatibility (15% weight)
- ✅ BigDecimal precision scoring (0.00-1.00)

#### **Real-time Notifications**
- ✅ WebSocket configuration
- ✅ Native WebSocket service (replaced SockJS)
- ✅ Notification service integration
- ✅ Real-time application notifications

---

## 🔧 **Recent Critical Fixes Applied**

### **Bean Conflict Resolution (Latest)**
**Issue**: Multiple Spring beans with same names causing startup failures
**Solution**: Removed duplicate service classes:
- Deleted: `service/compatibility/CompatibilityService.java` (simple version)
- Kept: `service/CompatibilityService.java` (comprehensive ML algorithm)
- Deleted: `service/email/EmailService.java` (basic version)
- Kept: `service/EmailService.java` (full-featured with async support)
- Deleted: `service/websocket/WebSocketNotificationService.java` (stub version)
- Kept: `service/WebSocketNotificationService.java` (complete implementation)

### **SockJS Global Variable Fix (Latest)**
**Issue**: "global is not defined" error causing white page
**Solution**: 
1. Added global polyfill to `index.html`:
```html
<script>
  if (typeof global === 'undefined') {
    var global = globalThis || window || self;
  }
</script>
```
2. Replaced SockJS with native WebSocket API
3. Removed `sockjs-client` and `@types/sockjs-client` dependencies

### **Compilation Issues Resolution**
**Issue**: Java syntax errors in RoommateService.java
**Solution**: 
- Fixed malformed method formatting
- Corrected DTO type mismatches (PropertyListingDTO → PropertyListingBasicDTO)
- Fixed entity field mappings (address → fullAddress, type → propertyType)
- Removed problematic validation annotations

---

## 💾 **Database Schema**

### **Core Entities**
- `users` (id, username, email, password, role, phone_number, etc.)
- `user_profiles` (education details, institute, field of study)
- `properties` (property listings with geocoding)
- `inquiries` (student-owner communication)
- `password_reset_tokens` (password reset workflow)

### **Roommate Entities (New)**
- `roommate_announcements` (roommate posts with ML scoring)
- `roommate_applications` (applications with compatibility scores)
- `roommate_matches` (ML recommendation tracking)

### **Key Relationships**
- User (1) → (N) Properties (owner relationship)
- User (1) → (N) Inquiries (student/owner relationship)
- User (1) → (1) UserProfile (extended profile data)
- RoommateAnnouncement (1) → (N) RoommateApplications

---

## 🌐 **API Architecture**

### **Authentication Endpoints**
- `POST /api/auth/register` - User registration
- `POST /api/auth/login` - JWT authentication
- `POST /api/auth/refresh` - Token refresh
- `GET /oauth2/authorization/google` - Google OAuth2

### **Property Management**
- `GET /api/properties` - List properties with filtering
- `POST /api/properties` - Create property (owners only)
- `GET /api/properties/{id}` - Property details
- `PUT /api/properties/{id}` - Update property
- `DELETE /api/properties/{id}` - Delete property

### **Inquiry System**
- `POST /api/inquiries` - Create inquiry
- `GET /api/inquiries/owner` - Owner's received inquiries
- `GET /api/inquiries/student` - Student's sent inquiries
- `POST /api/inquiries/{id}/reply` - Owner reply
- `POST /api/inquiries/{id}/close` - Close deal

### **Roommate Features (New)**
- `POST /api/v1/roommates/announcements` - Create announcement
- `GET /api/v1/roommates/announcements` - Browse announcements
- `POST /api/v1/roommates/applications` - Apply to announcement
- `GET /api/v1/roommates/applications` - View applications
- `PUT /api/v1/roommates/applications/{id}/respond` - Respond to application

---

## 🎨 **Frontend Architecture**

### **Angular 17+ Features**
- Standalone components (no NgModules)
- Reactive forms with validation
- Route guards for authentication
- HTTP interceptors for JWT
- Service-based state management

### **Key Services**
- `AuthService` - Authentication and token management
- `PropertyListingService` - Property CRUD operations
- `InquiryService` - Inquiry workflow management
- `RoommateService` - Roommate announcements and applications
- `WebSocketService` - Real-time notifications (native WebSocket)
- `RecommendationService` - ML-powered roommate recommendations

### **Component Structure**
- `LandingPageComponent` - Public homepage
- `StudentDashboardComponent` - Student dashboard
- `OwnerDashboardComponent` - Owner dashboard with analytics
- `BrowseRoommatesComponent` - Roommate discovery
- `PropertyDetailsComponent` - Property information
- `InquiryFormComponent` - Inquiry submission

### **Routing System**
- Public routes (landing, auth)
- Protected routes with guards
- Role-based route access
- Lazy loading for performance

---

## 🔐 **Security Implementation**

### **Backend Security**
- JWT token authentication
- BCrypt password hashing
- CORS configuration for localhost:4200
- Role-based method security (@PreAuthorize)
- Input validation with Jakarta Validation
- XSS and CSRF protection

### **Authentication Flow**
1. User login → JWT token issued
2. Token stored in localStorage (frontend)
3. HTTP interceptor adds Authorization header
4. Backend validates JWT on protected endpoints
5. WebSocket authentication via token parameter

---

## 🧠 **ML Compatibility Algorithm**

### **Academic-Focused Scoring (Java Implementation)**
```java
// Weights (must sum to 1.0)
UNIVERSITY_WEIGHT = 0.40      // Same university students
STUDY_FIELD_WEIGHT = 0.25     // Same/related field of study  
EDUCATION_LEVEL_WEIGHT = 0.20 // Bachelor/Master/PhD level
AGE_WEIGHT = 0.15             // Similar age groups
```

### **Scoring Logic**
- **University Match**: Perfect (1.0) if same, penalty (0.2) if different
- **Field Similarity**: Category-based matching (Engineering, Sciences, etc.)
- **Education Proximity**: Adjacent levels score 0.7, same level 1.0
- **Age Compatibility**: ≤1 year = 1.0, ≤2 years = 0.9, progressive decline

### **Output**: BigDecimal score (0.00-1.00) with compatibility level description

---

## 📡 **WebSocket Implementation**

### **Native WebSocket (No SockJS)**
```typescript
// Connection with JWT authentication
const wsUrl = `ws://localhost:8080/ws?token=${token}`;
const ws = new WebSocket(wsUrl);

// Automatic reconnection with exponential backoff
// Real-time notifications for inquiries and roommate applications
```

### **Notification Types**
- `NEW_INQUIRY` - Property inquiry received
- `INQUIRY_REPLY` - Owner replied to inquiry
- `NEW_ROOMMATE_APPLICATION` - Roommate application with ML score
- `ROOMMATE_APPLICATION_RESPONSE` - Application accepted/rejected
- `ROOMMATE_MATCH_FOUND` - High compatibility match discovered

---

## 🚨 **Known Issues & Solutions**

### **Resolved Issues**
1. **Bean Conflicts**: Fixed by removing duplicate service classes
2. **SockJS Global Error**: Fixed with native WebSocket implementation
3. **Compilation Errors**: Fixed DTO type mismatches and formatting
4. **Circular Dependencies**: Simplified DTOs to remove validation cycles

### **Current Considerations**
1. **Chart.js Version Conflict**: ng2-charts requires Angular 19+, current project uses 18+
2. **Package Security**: 5 moderate vulnerabilities in npm packages
3. **WebSocket Backend**: Need to implement server-side native WebSocket handler

### **Development Notes**
- Use `--force` or `--legacy-peer-deps` for npm operations due to version conflicts
- Backend runs on port 8080, frontend on port 4200
- Database connection configured for localhost PostgreSQL
- File uploads stored in `./uploads/owner-property-images/`

---

## 🔄 **Development Workflow**

### **Starting the Application**
1. **Backend**: 
   ```bash
   cd spring-security
   ./mvnw spring-boot:run
   ```

2. **Frontend**:
   ```bash
   cd spring-security/frontend/ui
   npm start  # or ng serve
   ```

3. **Database**: Ensure PostgreSQL is running on localhost:5432

### **Build Commands**
- Backend: `./mvnw clean package`
- Frontend: `ng build --configuration production`

### **Testing**
- Backend: `./mvnw test`
- Frontend: `ng test`

---

## 📊 **Current Sprint Status**

### **Sprint 4: Roommate Connecting Features**
- ✅ Backend implementation (90% complete)
- ✅ ML compatibility algorithm
- ✅ Database schema
- ✅ API endpoints
- 🔄 Frontend components (60% complete)
- 🔄 WebSocket integration testing
- ❌ End-to-end testing

### **Next Priorities**
1. Complete roommate frontend components
2. Implement server-side WebSocket handler
3. Add comprehensive error handling
4. Performance optimization
5. Security audit

---

## 🔗 **Integration Points**

### **External Dependencies**
- PostgreSQL database
- Email service (configured for Gmail SMTP)
- Google OAuth2 (for social login)
- OpenLayers for mapping
- Chart.js for analytics

### **File Structure Dependencies**
- Property images: `./uploads/owner-property-images/`
- Application logs: Default Spring Boot logging
- Database migrations: Hibernate auto-DDL

---

## 📝 **Environment Configuration**

### **Application Properties**
- `application-dev.properties` - Development configuration
- Database URL, credentials
- Email SMTP settings
- JWT secret key
- CORS allowed origins

### **Required Environment Variables**
- Database connection parameters
- Email credentials
- JWT secret
- OAuth2 client credentials

---

## 🎯 **Business Logic Summary**

**Core Workflow**: Student discovers properties → Submits inquiry → Owner responds → Deal closes → Student can recruit roommates → ML matches compatible students → Real-time notifications throughout

**Value Proposition**: Academic-focused compatibility matching with university, field of study, and education level prioritization for better roommate matches in university settings.

---

*Last Updated: May 24, 2025*  
*Version: Sprint 4 - Roommate Features Implementation*  
*Status: Backend stable, Frontend in development, WebSocket integration completed* 