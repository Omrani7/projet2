# Roommate Connecting Features - Sprint 3 Implementation Plan

## 🎯 **Sprint Objective**
Implement a comprehensive roommate matching platform that allows students to post, browse, and apply for roommate opportunities with ML-powered recommendations and real-time messaging.

---

## 🧠 **Chain of Thoughts Analysis**### **Problem Statement**Students need a way to find compatible roommates after securing properties, or to find shared accommodation opportunities. Current system handles property-owner relationships but lacks peer-to-peer student connections.### **Architectural Decisions**✅ **Enhanced Monolithic Architecture** (instead of microservices)- Maintains compatibility with existing Spring Boot + Angular stack- Reduces complexity and deployment overhead- Easier development and testing workflow- Can be refactored to microservices later if needed✅ **Java-based Compatibility Algorithm** (instead of TensorFlow/Python)- Full integration with Spring Boot ecosystem- No additional infrastructure required- Maintainable by existing Java development team- Practical scoring algorithm with proven effectiveness✅ **WebSocket Integration** (within same Spring Boot app)- Real-time messaging without external services- Leverages existing authentication and security- Simpler deployment and monitoring### **Core Business Logic**1. **Two-Path Posting System**: Leveraging existing closed deals OR manual property entry2. **Application Workflow**: Browse → Apply → Message → Match3. **Intelligent Matching**: Java-based compatibility scoring4. **Communication Layer**: WebSocket real-time messaging system

---

## 📋 **Feature Specifications**

### **1. Roommate Announcement System**

#### **Announcement Types:**
- **Type A**: Based on closed property deals (auto-populated)
- **Type B**: Manual property details entry
- **Enhanced Info**: Preferences, lifestyle, budget splits, etc.

#### **Core Announcement Fields:**
```typescript
interface RoommateAnnouncement {
  id: string;
  posterId: string;
  propertyId?: string; // For Type A (from closed deals)
  
  // Property Details (auto-filled for Type A, manual for Type B)
  propertyDetails: {
    title: string;
    address: string;
    coordinates: [number, number];
    totalRent: number;
    totalRooms: number;
    availableRooms: number;
    propertyType: 'APARTMENT' | 'HOUSE' | 'STUDIO';
    amenities: string[];
    images: string[];
  };
  
  // Roommate Preferences
  preferences: {
    maxRoommates: number;
    genderPreference: 'MALE' | 'FEMALE' | 'MIXED' | 'NO_PREFERENCE';
    ageRange: { min: number; max: number };
    lifestyle: ('QUIET' | 'SOCIAL' | 'STUDIOUS' | 'PARTY')[];
    smokingAllowed: boolean;
    petsAllowed: boolean;
    cleanlinessLevel: 1 | 2 | 3 | 4 | 5;
  };
  
  // Financial Details
  financials: {
    rentPerPerson: number;
    securityDeposit: number;
    utilitiesSplit: 'EQUAL' | 'USAGE_BASED';
    additionalCosts: string;
  };
  
  // Posting Details
  description: string;
  moveInDate: Date;
  leaseDuration: number; // months
  status: 'ACTIVE' | 'PAUSED' | 'FILLED' | 'EXPIRED';
  createdAt: Date;
  expiresAt: Date;
}
```

### **2. Application & Messaging System**

#### **Application Workflow:**
```typescript
interface RoommateApplication {
  id: string;
  announcementId: string;
  applicantId: string;
  posterId: string;
  
  message: string;
  compatibilityScore: number; // ML-generated
  status: 'PENDING' | 'ACCEPTED' | 'REJECTED' | 'WITHDRAWN';
  
  appliedAt: Date;
  respondedAt?: Date;
}
```

#### **Messaging System:**
```typescript
interface Message {
  id: string;
  conversationId: string;
  senderId: string;
  receiverId: string;
  content: string;
  messageType: 'TEXT' | 'IMAGE' | 'ANNOUNCEMENT_REFERENCE';
  timestamp: Date;
  isRead: boolean;
}

interface Conversation {
  id: string;
  participants: string[];
  announcementId?: string;
  lastMessage?: Message;
  updatedAt: Date;
}
```

---

## 🏗️ **System Architecture**### **Enhanced Monolithic Architecture (Compatible with Existing Spring Boot)**```┌─────────────────────────────────────────────────────────────────┐│                    Angular Frontend (Port 4200)                ││              (Extends existing Angular app)                     │└─────────────────────────┬───────────────────────────────────────┘                          │ HTTP/WebSocket┌─────────────────────────▼───────────────────────────────────────┐│              Spring Boot Application (Port 8080)               ││                   (Enhanced Monolith)                          ││  ┌─────────────────────────────────────────────────────────────┐││  │              NEW Roommate Features                         │││  │  - RoommateController      - CompatibilityService         │││  │  - MessagingController     - WebSocketHandler             │││  │  - RecommendationController - RoommateService             │││  └─────────────────────────────────────────────────────────────┘││  ┌─────────────────────────────────────────────────────────────┐││  │              EXISTING Features                             │││  │  - InquiryController       - PropertyController           │││  │  - UserController          - AnalyticsService             │││  │  - AuthController          - InquiryService               │││  └─────────────────────────────────────────────────────────────┘│└─────────────────────────┬───────────────────────────────────────┘                          │┌─────────────────────────▼───────────────────────────────────────┐│                PostgreSQL Database                             ││            (Extended with new roommate tables)                 │└─────────────────────────────────────────────────────────────────┘```### **Project Structure Integration**```spring-security/ (Your existing project)├── src/main/java/com/example/spring_security/│   ├── controller/ (EXISTING + NEW)│   │   ├── InquiryController.java (existing)│   │   ├── PropertyController.java (existing)│   │   ├── UserController.java (existing)│   │   ├── RoommateController.java (NEW)│   │   ├── MessagingController.java (NEW)│   │   └── RecommendationController.java (NEW)│   ├── service/ (EXISTING + NEW)│   │   ├── InquiryService.java (existing)│   │   ├── PropertyService.java (existing)│   │   ├── RoommateService.java (NEW)│   │   ├── MessagingService.java (NEW)│   │   └── CompatibilityService.java (NEW)│   ├── repository/ (EXISTING + NEW)│   │   ├── UserRepository.java (existing)│   │   ├── PropertyRepository.java (existing)│   │   ├── RoommateAnnouncementRepository.java (NEW)│   │   ├── RoommateApplicationRepository.java (NEW)│   │   └── MessageRepository.java (NEW)│   ├── model/ (EXISTING + NEW)│   │   ├── User.java (existing)│   │   ├── Property.java (existing)│   │   ├── RoommateAnnouncement.java (NEW)│   │   ├── RoommateApplication.java (NEW)│   │   └── Message.java (NEW)│   └── config/ (EXISTING + NEW)│       ├── SecurityConfig.java (existing)│       └── WebSocketConfig.java (NEW)└── frontend/ui/src/app/ (Your existing Angular app)    ├── pages/ (EXISTING + NEW)    │   ├── student-dashboard/ (existing)    │   ├── my-inquiries/ (existing)    │   ├── browse-roommates/ (NEW)    │   ├── post-roommate-announcement/ (NEW)    │   └── roommate-conversations/ (NEW)    ├── services/ (EXISTING + NEW)    │   ├── inquiry.service.ts (existing)    │   ├── analytics.service.ts (existing)    │   ├── roommate.service.ts (NEW)    │   └── messaging.service.ts (NEW)    └── models/ (EXISTING + NEW)        ├── inquiry.model.ts (existing)        ├── roommate.model.ts (NEW)        └── message.model.ts (NEW)```

### **Database Schema Design**

#### **New Tables:**
```sql
-- Roommate Announcements
CREATE TABLE roommate_announcements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    poster_id UUID NOT NULL REFERENCES users(id),
    property_id UUID REFERENCES properties(id), -- For Type A announcements
    
    -- Property details (JSON for flexibility)
    property_details JSONB NOT NULL,
    preferences JSONB NOT NULL,
    financials JSONB NOT NULL,
    
    description TEXT,
    move_in_date DATE NOT NULL,
    lease_duration INTEGER NOT NULL,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    
    created_at TIMESTAMP DEFAULT NOW(),
    expires_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Roommate Applications
CREATE TABLE roommate_applications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    announcement_id UUID NOT NULL REFERENCES roommate_announcements(id),
    applicant_id UUID NOT NULL REFERENCES users(id),
    poster_id UUID NOT NULL REFERENCES users(id),
    
    message TEXT NOT NULL,
    compatibility_score DECIMAL(3,2),
    status VARCHAR(20) DEFAULT 'PENDING',
    
    applied_at TIMESTAMP DEFAULT NOW(),
    responded_at TIMESTAMP,
    
    UNIQUE(announcement_id, applicant_id)
);

-- Conversations
CREATE TABLE conversations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    announcement_id UUID REFERENCES roommate_announcements(id),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Conversation Participants
CREATE TABLE conversation_participants (
    conversation_id UUID REFERENCES conversations(id),
    user_id UUID REFERENCES users(id),
    joined_at TIMESTAMP DEFAULT NOW(),
    PRIMARY KEY (conversation_id, user_id)
);

-- Messages
CREATE TABLE messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_id UUID NOT NULL REFERENCES conversations(id),
    sender_id UUID NOT NULL REFERENCES users(id),
    content TEXT NOT NULL,
    message_type VARCHAR(30) DEFAULT 'TEXT',
    timestamp TIMESTAMP DEFAULT NOW(),
    is_read BOOLEAN DEFAULT FALSE
);

-- User Roommate Preferences (for ML)
CREATE TABLE user_roommate_preferences (
    user_id UUID PRIMARY KEY REFERENCES users(id),
    lifestyle_tags TEXT[],
    cleanliness_level INTEGER CHECK (cleanliness_level BETWEEN 1 AND 5),
    social_level INTEGER CHECK (social_level BETWEEN 1 AND 5),
    study_habits TEXT[],
    budget_range JSONB,
    location_preferences JSONB,
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Roommate Matches (for tracking ML recommendations)
CREATE TABLE roommate_matches (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    recommended_user_id UUID NOT NULL REFERENCES users(id),
    announcement_id UUID REFERENCES roommate_announcements(id),
    compatibility_score DECIMAL(3,2) NOT NULL,
    match_factors JSONB,
    created_at TIMESTAMP DEFAULT NOW(),
    
    UNIQUE(user_id, recommended_user_id, announcement_id)
);
```

---

## 🔄 **User Flow Scenarios**

### **Scenario 1: Student with Closed Deal Posts Announcement**

```
1. Student navigates to "Find Roommates" → "Post Announcement"
2. System detects closed deals in student's profile
3. Pre-populate form with property details from closed deal
4. Student adds preferences, lifestyle info, rent split details
5. System validates and publishes announcement
6. ML service generates initial compatibility scores for potential matches
7. Other students receive recommendations if they match criteria
```

### **Scenario 2: Student without Property Posts Announcement**

```
1. Student navigates to "Find Roommates" → "Post Announcement"
2. Student selects "I need to find a property together"
3. Manual form for property requirements and preferences
4. Student adds ideal location, budget range, property type
5. System publishes "looking for property + roommate" announcement
6. ML service matches with students who have compatible requirements
```

### **Scenario 3: Student Browses and Applies**

```
1. Student navigates to "Browse Roommates"
2. System shows personalized recommendations based on ML scoring
3. Student can filter by location, budget, lifestyle, etc.
4. Student views detailed announcement with compatibility indicators
5. Student sends application with personal message
6. Real-time notification sent to poster
7. Conversation thread created for follow-up messaging
```

---

## 🤖 **Compatibility Scoring System (Java-based)**### **Simplified ML Algorithm (Compatible with Spring Boot)**#### **Compatibility Service Implementation:**```java@Servicepublic class CompatibilityService {        public double calculateCompatibility(User applicant, RoommateAnnouncement announcement) {        double totalScore = 0.0;                // Age compatibility (weight: 20%)        totalScore += calculateAgeCompatibility(applicant, announcement) * 0.20;                // Lifestyle compatibility (weight: 30%)        totalScore += calculateLifestyleCompatibility(applicant, announcement) * 0.30;                // Budget compatibility (weight: 25%)        totalScore += calculateBudgetCompatibility(applicant, announcement) * 0.25;                // Location compatibility (weight: 15%)        totalScore += calculateLocationCompatibility(applicant, announcement) * 0.15;                // Study field compatibility (weight: 10%)        totalScore += calculateStudyFieldCompatibility(applicant, announcement) * 0.10;                return Math.min(totalScore, 1.0);    }        private double calculateAgeCompatibility(User applicant, RoommateAnnouncement announcement) {        int applicantAge = applicant.getAge();        RoommatePreferences prefs = announcement.getPreferences();                if (applicantAge >= prefs.getAgeRange().getMin() &&             applicantAge <= prefs.getAgeRange().getMax()) {            return 1.0;        }                // Calculate proximity penalty        int distance = Math.min(            Math.abs(applicantAge - prefs.getAgeRange().getMin()),            Math.abs(applicantAge - prefs.getAgeRange().getMax())        );                return Math.max(0.0, 1.0 - (distance * 0.1));    }        private double calculateLifestyleCompatibility(User applicant, RoommateAnnouncement announcement) {        Set<String> applicantLifestyle = new HashSet<>(applicant.getLifestylePreferences());        Set<String> announcementLifestyle = new HashSet<>(announcement.getPreferences().getLifestyle());                // Find common lifestyle traits        Set<String> intersection = new HashSet<>(applicantLifestyle);        intersection.retainAll(announcementLifestyle);                Set<String> union = new HashSet<>(applicantLifestyle);        union.addAll(announcementLifestyle);                if (union.isEmpty()) return 0.5; // Neutral if no preferences specified                return (double) intersection.size() / union.size();    }        private double calculateBudgetCompatibility(User applicant, RoommateAnnouncement announcement) {        double applicantBudget = applicant.getBudgetRange().getMax();        double requiredRent = announcement.getFinancials().getRentPerPerson();                if (applicantBudget >= requiredRent) {            // Calculate how comfortable the budget is            double comfortRatio = applicantBudget / requiredRent;            return Math.min(1.0, comfortRatio / 1.5); // Optimal at 1.5x rent        } else {            // Penalty for insufficient budget            return Math.max(0.0, applicantBudget / requiredRent);        }    }        private double calculateLocationCompatibility(User applicant, RoommateAnnouncement announcement) {        if (applicant.getPreferredLocation() == null ||             announcement.getPropertyDetails().getCoordinates() == null) {            return 0.5; // Neutral if location not specified        }                double distance = calculateDistance(            applicant.getPreferredLocation(),            announcement.getPropertyDetails().getCoordinates()        );                // Distance in km, prefer within 10km        if (distance <= 5) return 1.0;        if (distance <= 10) return 0.8;        if (distance <= 20) return 0.5;        return 0.2;    }        private double calculateStudyFieldCompatibility(User applicant, RoommateAnnouncement announcement) {        String applicantField = applicant.getStudyField();        String posterField = announcement.getPoster().getStudyField();                if (applicantField == null || posterField == null) {            return 0.5; // Neutral if not specified        }                if (applicantField.equals(posterField)) {            return 1.0; // Same field        }                // Check for related fields (engineering, sciences, etc.)        return calculateFieldSimilarity(applicantField, posterField);    }        private double calculateDistance(double[] point1, double[] point2) {        double lat1 = Math.toRadians(point1[0]);        double lon1 = Math.toRadians(point1[1]);        double lat2 = Math.toRadians(point2[0]);        double lon2 = Math.toRadians(point2[1]);                double dlat = lat2 - lat1;        double dlon = lon2 - lon1;        double a = Math.sin(dlat/2) * Math.sin(dlat/2) +                   Math.cos(lat1) * Math.cos(lat2) *                   Math.sin(dlon/2) * Math.sin(dlon/2);        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));                return 6371 * c; // Earth radius in km    }        private double calculateFieldSimilarity(String field1, String field2) {        // Define field categories        Map<String, Set<String>> fieldCategories = Map.of(            "ENGINEERING", Set.of("Computer Science", "Electrical Engineering", "Mechanical Engineering"),            "SCIENCES", Set.of("Biology", "Chemistry", "Physics", "Mathematics"),            "BUSINESS", Set.of("Business Administration", "Economics", "Finance", "Marketing"),            "HUMANITIES", Set.of("Literature", "History", "Philosophy", "Languages")        );                for (Set<String> category : fieldCategories.values()) {            if (category.contains(field1) && category.contains(field2)) {                return 0.7; // Same category            }        }                return 0.3; // Different categories    }}```#### **Enhanced Recommendation Service:**```java@Servicepublic class RecommendationService {        @Autowired    private CompatibilityService compatibilityService;        @Autowired    private RoommateAnnouncementRepository announcementRepository;        @Autowired    private UserRepository userRepository;        public List<AnnouncementWithScore> getRecommendationsForUser(String userId, int limit) {        User user = userRepository.findById(userId)            .orElseThrow(() -> new UserNotFoundException("User not found"));                List<RoommateAnnouncement> allAnnouncements = announcementRepository            .findByStatusAndPosterIdNot("ACTIVE", userId);                return allAnnouncements.stream()            .map(announcement -> {                double score = compatibilityService.calculateCompatibility(user, announcement);                return new AnnouncementWithScore(announcement, score);            })            .filter(aws -> aws.getScore() > 0.3) // Minimum compatibility threshold            .sorted((a, b) -> Double.compare(b.getScore(), a.getScore()))            .limit(limit)            .collect(Collectors.toList());    }        public List<UserWithScore> getCompatibleApplicants(String announcementId) {        RoommateAnnouncement announcement = announcementRepository.findById(announcementId)            .orElseThrow(() -> new AnnouncementNotFoundException("Announcement not found"));                List<RoommateApplication> applications = announcement.getApplications();                return applications.stream()            .map(application -> {                double score = compatibilityService.calculateCompatibility(                    application.getApplicant(), announcement);                return new UserWithScore(application.getApplicant(), score);            })            .sorted((a, b) -> Double.compare(b.getScore(), a.getScore()))            .collect(Collectors.toList());    }}```

### **Recommendation API Endpoints:**
```typescript
// Get personalized roommate recommendations
GET /api/v1/roommates/recommendations/{userId}
  - Query params: limit, location_radius, budget_range
  - Returns: Ranked list of announcements with compatibility scores

// Get compatible applicants for announcement
GET /api/v1/roommates/announcements/{announcementId}/compatible-applicants
  - Returns: Ranked list of potential applicants with compatibility scores

// Update user preferences for improved recommendations
PUT /api/v1/users/{userId}/roommate-preferences
  - Updates user profile for better ML matching
```

---

## 🎨 **Frontend Implementation**

### **Component Architecture**

```
src/app/roommates/
├── components/
│   ├── announcement-card/
│   ├── announcement-form/
│   ├── application-modal/
│   ├── compatibility-indicator/
│   ├── message-thread/
│   ├── recommendation-list/
│   └── roommate-filter/
├── pages/
│   ├── browse-roommates/
│   ├── my-announcements/
│   ├── my-applications/
│   ├── post-announcement/
│   └── roommate-conversations/
├── services/
│   ├── roommate.service.ts
│   ├── messaging.service.ts
│   └── recommendation.service.ts
└── models/
    ├── announcement.model.ts
    ├── application.model.ts
    └── conversation.model.ts
```

### **Key Components Design**

#### **1. Announcement Form Component**
```typescript
@Component({
  selector: 'app-announcement-form',
  templateUrl: './announcement-form.component.html'
})
export class AnnouncementFormComponent implements OnInit {
  announcementForm: FormGroup;
  userClosedDeals: PropertyDeal[] = [];
  selectedDeal?: PropertyDeal;
  announcementType: 'EXISTING_PROPERTY' | 'FIND_TOGETHER' = 'EXISTING_PROPERTY';
  
  ngOnInit() {
    this.loadUserClosedDeals();
    this.initializeForm();
  }
  
  onDealSelected(deal: PropertyDeal) {
    this.selectedDeal = deal;
    this.populateFormFromDeal(deal);
  }
  
  populateFormFromDeal(deal: PropertyDeal) {
    this.announcementForm.patchValue({
      propertyDetails: {
        title: deal.property.title,
        address: deal.property.address,
        totalRent: deal.agreedPrice,
        totalRooms: deal.property.rooms,
        // ... other fields
      }
    });
  }
}
```

#### **2. Browse Roommates Component**
```typescript
@Component({
  selector: 'app-browse-roommates',
  templateUrl: './browse-roommates.component.html'
})
export class BrowseRoommatesComponent implements OnInit {
  recommendations: AnnouncementWithScore[] = [];
  allAnnouncements: RoommateAnnouncement[] = [];
  filters: RoommateFilters = {};
  isLoadingRecommendations = true;
  
  ngOnInit() {
    this.loadPersonalizedRecommendations();
    this.loadAllAnnouncements();
  }
  
  loadPersonalizedRecommendations() {
    this.roommateService.getRecommendations(this.userId)
      .subscribe(recommendations => {
        this.recommendations = recommendations;
        this.isLoadingRecommendations = false;
      });
  }
  
  applyForRoommate(announcement: RoommateAnnouncement) {
    // Open application modal
    this.openApplicationModal(announcement);
  }
}
```

#### **3. Real-time Messaging Component**
```typescript
@Component({
  selector: 'app-message-thread',
  templateUrl: './message-thread.component.html'
})
export class MessageThreadComponent implements OnInit, OnDestroy {
  conversation: Conversation;
  messages: Message[] = [];
  newMessage = '';
  private socketSubscription: Subscription;
  
  ngOnInit() {
    this.loadConversation();
    this.subscribeToNewMessages();
  }
  
  subscribeToNewMessages() {
    this.socketSubscription = this.messagingService
      .getMessagesForConversation(this.conversationId)
      .subscribe(message => {
        this.messages.push(message);
        this.markAsRead(message);
      });
  }
  
  sendMessage() {
    if (this.newMessage.trim()) {
      this.messagingService.sendMessage({
        conversationId: this.conversationId,
        content: this.newMessage,
        receiverId: this.getOtherParticipantId()
      });
      this.newMessage = '';
    }
  }
}
```

---

## 🌐 **Real-time Features Implementation**

### **WebSocket Integration**

#### **Backend WebSocket Handler**
```java
@Component
public class MessagingWebSocketHandler extends TextWebSocketHandler {
    
    private final Map<String, WebSocketSession> userSessions = new ConcurrentHashMap<>();
    private final MessagingService messagingService;
    
    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String userId = extractUserIdFromSession(session);
        userSessions.put(userId, session);
        notifyUserOnline(userId);
    }
    
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        MessageDto messageDto = parseMessage(message.getPayload());
        
        // Save message to database
        Message savedMessage = messagingService.saveMessage(messageDto);
        
        // Send to recipient if online
        WebSocketSession recipientSession = userSessions.get(messageDto.getReceiverId());
        if (recipientSession != null && recipientSession.isOpen()) {
            sendMessage(recipientSession, savedMessage);
        }
        
        // Send push notification if recipient offline
        else {
            notificationService.sendPushNotification(messageDto.getReceiverId(), savedMessage);
        }
    }
}
```

#### **Frontend WebSocket Service**
```typescript
@Injectable({
  providedIn: 'root'
})
export class MessagingService {
  private socket: WebSocketSubject<any>;
  private messagesSubject = new Subject<Message>();
  
  constructor() {
    this.connect();
  }
  
  connect() {
    this.socket = webSocket({
      url: `ws://localhost:8080/ws/messaging?token=${this.authService.getToken()}`,
      openObserver: {
        next: () => console.log('WebSocket connected')
      }
    });
    
    this.socket.subscribe(
      message => this.messagesSubject.next(message),
      error => this.handleError(error)
    );
  }
  
  sendMessage(message: MessageRequest) {
    this.socket.next(message);
  }
  
  getMessagesForConversation(conversationId: string): Observable<Message> {
    return this.messagesSubject.pipe(
      filter(message => message.conversationId === conversationId)
    );
  }
}
```

---

## 📊 **Analytics & Success Metrics**

### **Key Performance Indicators**

#### **Engagement Metrics:**
- Announcement posting rate
- Application submission rate
- Message response rate
- Successful roommate matches

#### **ML Model Performance:**
- Recommendation click-through rate
- Application success rate from recommendations
- User satisfaction scores
- Model accuracy metrics

#### **Business Metrics:**
- Time to successful match
- User retention in roommate feature
- Property utilization improvement
- Student satisfaction scores

---

## 📦 **Dependencies & Configuration**### **Backend Dependencies (Add to pom.xml)**```xml<dependencies>    <!-- Existing dependencies remain unchanged -->        <!-- WebSocket for real-time messaging -->    <dependency>        <groupId>org.springframework</groupId>        <artifactId>spring-websocket</artifactId>    </dependency>        <dependency>        <groupId>org.springframework</groupId>        <artifactId>spring-messaging</artifactId>    </dependency>        <!-- For JSON handling in WebSocket -->    <dependency>        <groupId>com.fasterxml.jackson.core</groupId>        <artifactId>jackson-databind</artifactId>    </dependency>        <!-- For mathematical calculations -->    <dependency>        <groupId>org.apache.commons</groupId>        <artifactId>commons-math3</artifactId>        <version>3.6.1</version>    </dependency>        <!-- For improved JSON handling -->    <dependency>        <groupId>org.springframework.boot</groupId>        <artifactId>spring-boot-starter-validation</artifactId>    </dependency></dependencies>```### **Frontend Dependencies (Add to package.json)**```json{  "dependencies": {    "@angular/core": "^17.0.0",    "@angular/common": "^17.0.0",    "@angular/forms": "^17.0.0",    "rxjs": "^7.8.0",    "socket.io-client": "^4.7.0"  }}```### **WebSocket Configuration**```java@Configuration@EnableWebSocketpublic class WebSocketConfig implements WebSocketConfigurer {        @Override    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {        registry.addHandler(new MessagingWebSocketHandler(), "/ws/messaging")                .setAllowedOrigins("http://localhost:4200") // Your Angular app                .withSockJS(); // Fallback for older browsers    }}```---## 🚀 **Implementation Timeline (Updated for Monolithic Approach)**### **Phase 1: Database & Core Setup (Week 1-2)**- [ ] Add new tables to existing PostgreSQL database- [ ] Create new entity classes (RoommateAnnouncement, RoommateApplication, Message)- [ ] Create new repository interfaces extending JpaRepository- [ ] Set up basic Spring Boot service classes- [ ] Add new Angular routes to existing app.routes.ts### **Phase 2: Announcement System (Week 3-4)**- [ ] Implement RoommateController with CRUD endpoints- [ ] Create announcement posting forms (with closed deal integration)- [ ] Build browse roommates page with filtering- [ ] Implement application submission system- [ ] Add compatibility scoring service### **Phase 3: Messaging System (Week 5-6)**- [ ] Implement WebSocket configuration and handler- [ ] Create MessagingController and MessageService- [ ] Build real-time chat components in Angular- [ ] Integrate WebSocket service with Angular- [ ] Add conversation management### **Phase 4: Recommendation Integration (Week 7-8)**- [ ] Implement CompatibilityService with Java algorithms- [ ] Create RecommendationService and endpoints- [ ] Integrate recommendations into browse page- [ ] Add compatibility indicators in UI- [ ] Implement recommendation notifications### **Phase 5: Polish & Testing (Week 9-10)**- [ ] UI/UX improvements and responsive design- [ ] Performance optimization and caching- [ ] Comprehensive testing (unit + integration)- [ ] Add analytics tracking for roommate features- [ ] Documentation and deployment preparation

---

## ⚠️ **Technical Considerations (Monolithic Architecture)**### **Database Optimization**- **Indexing Strategy**:   ```sql  CREATE INDEX idx_roommate_announcements_status ON roommate_announcements(status);  CREATE INDEX idx_roommate_announcements_location ON roommate_announcements USING GIN (property_details);  CREATE INDEX idx_messages_conversation ON messages(conversation_id, timestamp);  CREATE INDEX idx_applications_announcement ON roommate_applications(announcement_id);  ```- **Connection Pooling**: Configure HikariCP for optimal database connections- **Query Optimization**: Use @Query annotations for complex searches### **Performance Considerations**- **Caching**: Use Spring Cache with local cache for compatibility scores  ```java  @Cacheable(value = "compatibility", key = "#userId + '_' + #announcementId")  public double calculateCompatibility(String userId, String announcementId) { ... }  ```- **Pagination**: Implement proper pagination for announcement listings- **Lazy Loading**: Use @JsonIgnore for heavy relationship mappings### **Security Measures (Spring Boot)**- **WebSocket Security**:   ```java  @Override  public void configureMessageBroker(MessageBrokerRegistry registry) {      registry.enableSimpleBroker("/topic", "/queue")             .setUserDestinationPrefix("/user");  }  ```- **Input Validation**: Use @Valid annotations and custom validators- **Rate Limiting**: Implement using Spring AOP and annotations- **Data Protection**: Extend existing GDPR compliance to roommate features### **Integration with Existing System**- **Seamless Authentication**: Leverage existing JWT/Spring Security setup- **Shared User Model**: Extend current User entity with roommate preferences- **Consistent API Structure**: Follow existing REST API patterns- **Unified Analytics**: Add roommate events to existing analytics tracking### **WebSocket Scalability**- **Session Management**: Use in-memory session storage for single instance- **Message Queue**: For future scaling, consider Redis pub/sub- **Connection Limits**: Configure appropriate WebSocket connection limits- **Graceful Degradation**: Fallback to polling if WebSocket fails

---

## 🎯 **Success Criteria**

### **Technical Success:**
- [ ] 99% uptime for messaging system
- [ ] <2s response time for recommendations
- [ ] >80% accuracy for ML compatibility scoring
- [ ] Real-time message delivery <1s

### **Business Success:**
- [ ] >70% of posted announcements receive applications
- [ ] >50% application success rate from ML recommendations
- [ ] >4.0/5.0 user satisfaction rating
- [ ] >30% increase in successful property utilizations

### **User Experience Success:**
- [ ] Intuitive announcement posting process
- [ ] Seamless messaging experience
- [ ] Relevant and accurate recommendations
- [ ] Mobile-responsive design

---

This comprehensive plan covers all aspects of the roommate connecting feature with proper technical depth, user experience considerations, and business value alignment. The ML-powered recommendation system will be the key differentiator, making the platform intelligent and user-centric. 