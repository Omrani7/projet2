# Inquiry Feature Implementation Plan

## Phase 1: Backend Implementation (Spring Boot)

### 1.1. Data Model (Entity)
- **Task:** Create the `Inquiry` entity.
  - **File:** `spring-security/src/main/java/com/example/spring_security/model/Inquiry.java`
  - **Fields:**
    - `id` (Long, PK, GeneratedValue)
    - `student` (User, ManyToOne, JoinColumn `student_id`) - *User with STUDENT role*
    - `owner` (User, ManyToOne, JoinColumn `owner_id`) - *User with OWNER role (property owner)*
    - `property` (PropertyListing, ManyToOne, JoinColumn `property_listing_id`)
    - `message` (String, TEXT)
    - `timestamp` (LocalDateTime, CreationTimestamp)
    - `reply` (String, TEXT, nullable)
    - `replyTimestamp` (LocalDateTime, nullable)
    - `status` (Enum `InquiryStatus` - e.g., PENDING_REPLY, REPLIED, CLOSED) - *Essential for state management*
  - **Relationships:** Ensure correct cascade types and fetch strategies.
- **Task:** Create `InquiryStatus` Enum.
  - **File:** `spring-security/src/main/java/com/example/spring_security/model/InquiryStatus.java`
  - **Values:** `PENDING_REPLY`, `REPLIED`, `CLOSED` (or as appropriate)

### 1.2. Repository
- **Task:** Create `InquiryRepository`.
  - **File:** `spring-security/src/main/java/com/example/spring_security/dao/InquiryRepository.java`
  - **Methods:**
    - `findByOwnerIdOrderByTimestampDesc(Long ownerId, Pageable pageable)`
    - `findByStudentIdOrderByTimestampDesc(Long studentId, Pageable pageable)`
    - (Potentially) `findByPropertyId(Long propertyId)`

### 1.3. DTOs
- **Task:** Create `InquiryDTO.java`.
  - **File:** `spring-security/src/main/java/com/example/spring_security/dto/InquiryDTO.java`
  - **Fields:** Reflect `Inquiry` entity, include nested DTOs for User (student/owner) and PropertyListing if needed to avoid exposing full entities. (e.g., `UserBasicDTO` with id, name, email).
- **Task:** Create `InquiryCreateDTO.java`.
  - **File:** `spring-security/src/main/java/com/example/spring_security/dto/InquiryCreateDTO.java`
  - **Fields:** `propertyId` (Long), `message` (String). `studentId` will be derived from the authenticated principal.
- **Task:** Create `InquiryReplyDTO.java`.
  - **File:** `spring-security/src/main/java/com/example/spring_security/dto/InquiryReplyDTO.java`
  - **Fields:** `reply` (String).

### 1.4. Service (`InquiryService`)
- **Task:** Create `InquiryService.java`.
  - **File:** `spring-security/src/main/java/com/example/spring_security/service/InquiryService.java`
  - **Methods:**
    - `createInquiry(InquiryCreateDTO createDTO, UserPrincipal currentUser)`:
      - Validate student role.
      - Fetch `PropertyListing` and its owner.
      - Save new `Inquiry`.
      - Trigger notifications (Email to owner, WebSocket to owner).
    - `getInquiriesForOwner(Long ownerId, UserPrincipal currentUser, Pageable pageable)`:
      - Authorize: Ensure `currentUser.id` matches `ownerId` and user has OWNER role.
      - Fetch inquiries.
    - `getInquiriesForStudent(Long studentId, UserPrincipal currentUser, Pageable pageable)`:
      - Authorize: Ensure `currentUser.id` matches `studentId`.
      - Fetch inquiries.
    - `replyToInquiry(Long inquiryId, InquiryReplyDTO replyDTO, UserPrincipal currentUser)`:
      - Validate inquiry exists.
      - Authorize: Ensure `currentUser` is the owner of the property associated with the inquiry.
      - Update `Inquiry` with reply and `replyTimestamp`.
      - Set status (e.g., to REPLIED).
      - Trigger notifications (Email to student, WebSocket to student).
    - `markInquiryStatus(Long inquiryId, InquiryStatus status, UserPrincipal currentUser)`: 
      - Authorize.
      - Update status. Used internally or via specific admin/moderation endpoints if added later.
  - **Dependencies:** `InquiryRepository`, `UserRepository`, `PropertyListingRepository`, `NotificationService` (see 1.6), `ModelMapper`.

### 1.5. Controller (`InquiryController`)
- **Task:** Create `InquiryController.java`.
  - **File:** `spring-security/src/main/java/com/example/spring_security/controller/InquiryController.java`
  - **Base Path:** `/api/v1/inquiries`
  - **Endpoints:**
    - `POST /`: Create new inquiry. (Payload: `InquiryCreateDTO`). Protected: `isAuthenticated()`.
    - `GET /owner`: Get inquiries for the authenticated owner. Protected: `hasRole('OWNER')`.
    - `GET /student`: Get inquiries for the authenticated student. Protected: `isAuthenticated()`.
    - `PUT /{inquiryId}/reply`: Owner replies to an inquiry. (Payload: `InquiryReplyDTO`). Protected: `hasRole('OWNER')`.
    - `PUT /{inquiryId}/status`: (Consider deferring) Endpoint to explicitly update inquiry status if needed beyond automated updates. Protected by role.

### 1.6. Notification System
#### 1.6.1. Email Notifications (`NotificationService`)
- **Task:** Enhance or create `NotificationService.java`.
  - **File:** (Likely exists or new) `spring-security/src/main/java/com/example/spring_security/service/NotificationService.java` (or `EmailService.java`).
  - **Dependencies:** `JavaMailSender`.
  - **Methods:**
    - `sendInquiryReceivedEmail(Inquiry inquiry)`: To owner.
    - `sendInquiryReplyEmail(Inquiry inquiry)`: To student.
  - **Configuration:** Ensure `application.properties` has SMTP settings.
  - **Async:** Use `@Async` for email sending.

#### 1.6.2. Real-Time Notifications (WebSocket/STOMP)
- **Task:** Configure WebSocket and STOMP.
  - **File:** `spring-security/src/main/java/com/example/spring_security/config/WebSocketConfig.java`
  - Annotate with `@Configuration`, `@EnableWebSocketMessageBroker`.
  - Configure message broker (e.g., `/topic`, `/queue`).
  - Register STOMP endpoints (e.g., `/ws`) with SockJS fallback.
- **Task:** Create `WebSocketNotificationService.java` (or integrate into `NotificationService`).
  - **Dependencies:** `SimpMessagingTemplate`.
  - **Methods:**
    - `notifyOwnerOfNewInquiry(Long ownerUserId, InquiryDTO inquiry)`: Send to `/user/{ownerUserId}/queue/inquiries`.
    - `notifyStudentOfReply(Long studentUserId, InquiryDTO inquiry)`: Send to `/user/{studentUserId}/queue/inquiries`.
- **Task:** Secure WebSocket endpoint.
  - Integrate with Spring Security. Ensure user principal is propagated.
  - **File:** `spring-security/src/main/java/com/example/spring_security/config/WebSocketSecurityConfig.java` (if needed, or update existing SecurityConfig).

### 1.7. Updates to `PropertyListingService` & `PropertyListingController`
- **Task:** Implement "Archive" functionality for `PropertyListing`.
  - **Service (`PropertyListingService`):**
    - Method: `archiveProperty(Long propertyId, UserPrincipal currentUser)`
      - Authorize: Ensure `currentUser` is the owner.
      - Fetch property, set `active = false`.
      - Save property.
  - **Controller (`PropertyListingController`):**
    - Endpoint: `PUT /{propertyId}/archive`. Protected: `hasRole('OWNER')`.

### 1.8. Security Configuration
- **Task:** Update `SecurityConfig.java`.
  - Ensure new `/api/v1/inquiries/**` endpoints are appropriately secured.
  - Ensure `/ws/**` WebSocket endpoint is secured.

### 1.9. Global Exception Handling (New Section)
- **Task:** Implement Global Exception Handling.
  - **File:** `spring-security/src/main/java/com/example/spring_security/exception/GlobalExceptionHandler.java` (or similar package).
  - Use `@ControllerAdvice`.
  - Handle common exceptions: `ResourceNotFoundException`, `AccessDeniedException`, `ValidationException` (from `@Valid`), `DataIntegrityViolationException`, etc.
  - Return standardized JSON error responses with appropriate HTTP status codes.
  - Log errors effectively.

## Phase 2: Frontend Implementation (Angular)

### 2.1. Angular Service (`InquiryService`)
- **Task:** Create `inquiry.service.ts`.
  - **File:** `spring-security/frontend/ui/src/app/services/inquiry.service.ts`
  - **Methods:**
    - `createInquiry(propertyId: number, message: string): Observable<Inquiry>`
    - `getOwnerInquiries(page: number, size: number): Observable<Page<Inquiry>>`
    - `getStudentInquiries(page: number, size: number): Observable<Page<Inquiry>>`
    - `replyToInquiry(inquiryId: number, reply: string): Observable<Inquiry>`
  - **Models:** Define `Inquiry`, `UserBasic`, `PropertyBasic` interfaces/classes.

### 2.2. WebSocket Integration (`WebSocketService` / STOMP Client)
- **Task:** Create or integrate `websocket.service.ts`.
  - **File:** `spring-security/frontend/ui/src/app/services/websocket.service.ts`
  - Use `@stomp/rx-stomp` or `ngx-stompjs`.
  - **Methods:**
    - `connect()`: Connect to `/ws` endpoint, pass auth token.
    - `subscribeToUserInquiries(): Observable<Inquiry>`: Subscribe to `/user/queue/inquiries`.
    - `disconnect()`.
  - Manage connection state and token.

### 2.3. Student-Side Components
#### 2.3.1. Inquiry Form (on Property Detail Page)
- **Task:** Enhance `property-details.component.ts` and `property-details.component.html`.
  - **File:** `spring-security/frontend/ui/src/app/pages/property-details/...`
  - Add a form section (textarea for message, "Send Inquiry" button).
  - Only show if user is authenticated and has STUDENT role (or just authenticated, backend can verify role).
  - On submit, call `inquiry.service.ts#createInquiry()`.
  - Show success/error toast/snackbar.
#### 2.3.2. "My Inquiries" Page for Student
- **Task:** Create `MyInquiriesComponent`.
  - **Files:** `my-inquiries.component.ts`, `my-inquiries.component.html`, `my-inquiries.component.css`
  - **Location:** `spring-security/frontend/ui/src/app/pages/my-inquiries/`
  - Display a list/table of inquiries made by the student (property thumbnail, message, owner reply, timestamps, status).
  - Fetch data using `inquiry.service.ts#getStudentInquiries()`.
  - Integrate with `WebSocketService` for real-time updates on replies.
  - Add routing for this page.

### 2.4. Owner-Side Components
#### 2.4.1. "Owner Inquiries" Page/Section (in Owner Dashboard)
- **Task:** Create `OwnerInquiriesComponent`.
  - **Files:** `owner-inquiries.component.ts`, `owner-inquiries.component.html`, `owner-inquiries.component.css`
  - **Location:** `spring-security/frontend/ui/src/app/pages/owner-dashboard/owner-inquiries/` (or similar, TBD based on current dashboard structure).
  - Display a list/table of inquiries received for the owner's properties (property title, student name, date, message, reply form).
  - Fetch data using `inquiry.service.ts#getOwnerInquiries()`.
  - Inline reply form (textarea, "Send Reply" button). On submit, call `inquiry.service.ts#replyToInquiry()`.
  - Integrate with `WebSocketService` for real-time updates on new inquiries.
  - Add routing for this page/integrate into owner dashboard.
#### 2.4.2. "Archive" Button for Properties
- **Task:** Add "Archive" button to owner's property management view (e.g., `my-listings.component.ts/html`).
  - **File:** `spring-security/frontend/ui/src/app/pages/my-listings/...`
  - Button calls a new method in `property-listing.service.ts` which in turn calls the backend `PUT /{propertyId}/archive` endpoint.
  - Update UI to reflect archived status (e.g., grey out, "Archived" label).

### 2.5. UI Notifications (Toasts/Snackbars)
- **Task:** Integrate a toast/snackbar library (e.g., `ngx-toastr` or Angular Material SnackBar if Material is used).
  - Create a `NotificationDisplayService` to centralize showing these alerts.
  - Use for:
    - Inquiry sent confirmation.
    - Reply sent confirmation.
    - Errors.
    - Real-time new inquiry/reply alerts (triggered by `WebSocketService`).

### 2.6. Routing and Navigation
- **Task:** Add new routes for:
  - Student's "My Inquiries" page.
  - Owner's "Inquiries" section/page.
- Update navigation menus/links (e.g., in user profile dropdown, owner dashboard sidebar).

### 2.7. Frontend State Management (New Section)
- **Task:** Define and implement frontend state management for inquiries and notifications.
  - **Approach:**
    - Create a dedicated Angular service (e.g., `InquiryStateService` or `NotificationStateService`) to hold and manage the state of inquiry lists and real-time notifications.
    - This service will use RxJS Subjects/BehaviorSubjects to stream data to components.
    - It will interact with `InquiryService` (for REST calls) and `WebSocketService` (for real-time updates).
    - Components will subscribe to observables from this state service to display data and updates.
  - **Consideration:** If NgRx or another formal state management library is already in use, integrate this feature's state accordingly. For simplicity, a dedicated service is often sufficient for feature-specific state.
  - Ensure proper unsubscription patterns in components.

## Phase 3: Testing and Refinement

### 3.1. Backend Testing
- Unit tests for `InquiryService` methods.
- Integration tests for `InquiryController` endpoints.
- Test WebSocket message delivery.
- Test email notifications.

### 3.2. Frontend Testing
- Unit tests for new components and services.
- E2E tests for the inquiry workflow (student sends, owner receives, owner replies, student sees reply).
- Test real-time UI updates.

### 3.3. Manual End-to-End Testing
- Thoroughly test the entire workflow for both student and owner roles.
- Test edge cases and error handling.
- Verify notifications (email and real-time).

## Considerations & Best Practices
- **Authentication & Authorization:** Rigorously enforce at every step, both backend and frontend.
- **Error Handling:** Implement robust error handling and user feedback.
- **User Experience:** Ensure the flow is intuitive for both students and owners.
- **Code Quality:** Follow existing coding styles and best practices.
- **Modularity:** Keep components and services focused and reusable.
- **Database Schema:** Ensure `inquiries` table has appropriate indexes (on `student_id`, `owner_id`, `property_listing_id`).
- **DTO Design Rationale (New Point):** Maintain separate DTOs for create/update commands (`InquiryCreateDTO`, `InquiryReplyDTO`) and read models (`InquiryDTO`). This promotes clearer API contracts, better validation, and aligns with CQRS principles at the DTO level, even if it means a few more classes.
- **Pagination Strategy (New Point):** Establish and document default page sizes for paginated API endpoints. Ensure consistent handling and user experience for pagination in the frontend.
- **Status Enum Management (Refinement):** The `InquiryStatus` enum is crucial for tracking the inquiry lifecycle. Ensure all relevant service methods (`createInquiry`, `replyToInquiry`) correctly set and update this status.

This plan outlines the major steps. We can break these down further during implementation. 