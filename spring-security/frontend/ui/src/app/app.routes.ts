import { Routes } from '@angular/router';
import { LandingPageComponent } from './landing-page/landing-page.component'; // Restore LandingPageComponent
import { DiscoveryComponent } from './pages/discovery/discovery.component'; // Keep DiscoveryComponent for its own route
import { ProfileComponent } from './pages/profile/profile.component'; // Import the ProfileComponent
import { PropertyDetailsComponent } from './pages/property-details/property-details.component'; // Import PropertyDetailsComponent
import { AccessDeniedOwnerFeatureComponent } from './pages/access-denied-owner-feature/access-denied-owner-feature.component'; // Import the new component
import { OwnerProfileSetupComponent } from './pages/owner-profile-setup/owner-profile-setup.component'; // Import the owner profile setup component
import { OwnerProfileGuard } from './auth/owner-profile.guard'; // Import the owner profile guard
import { StudentGuard } from './auth/student.guard'; // Import the student guard
import { PropertyListingFormComponent } from './pages/property-listing-form/property-listing-form.component'; // Import the property listing form component
import { MyListingsComponent } from './pages/my-listings/my-listings.component'; // Import the My Listings component
import { MyInquiriesComponent } from './pages/my-inquiries/my-inquiries.component'; // Import the My Inquiries component
import { OwnerInquiriesComponent } from './pages/owner-inquiries/owner-inquiries.component';
import { OwnerDashboardComponent } from './pages/owner-dashboard/owner-dashboard.component'; // Import the new Owner Dashboard component
import { OwnerAnalyticsComponent} from "./pages/owner-analytics/owner-analytics.component";
import { StudentDashboardComponent } from './pages/student-dashboard/student-dashboard.component'; // Import the new Student Dashboard component

// Roommate feature imports (creating step by step)
import { BrowseRoommatesComponent } from './pages/browse-roommates/browse-roommates.component';
import { RoommateAnnouncementDetailsComponent } from './pages/roommate-announcement-details/roommate-announcement-details.component';
import { PostRoommateAnnouncementComponent } from './pages/post-roommate-announcement/post-roommate-announcement.component';
import { MyAnnouncementsComponent } from './pages/my-announcements/my-announcements.component';
import { CompatibleStudentsComponent } from './components/compatible-students/compatible-students.component';
import { RoommatePreferencesComponent } from './pages/roommate-preferences/roommate-preferences.component';
import { ConnectionRequestsComponent } from './pages/connection-requests/connection-requests.component';
import { MessagesComponent } from './pages/messages/messages.component';
// import { MyRoommateApplicationsComponent } from './pages/my-roommate-applications/my-roommate-applications.component';

// Admin Dashboard
import { AdminDashboardComponent } from './pages/admin-dashboard/admin-dashboard.component';
import { AdminUsersComponent } from './pages/admin-users/admin-users.component';
import { AdminPropertiesComponent } from './pages/admin-properties/admin-properties.component';
import { AdminRoommateAnnouncementsComponent } from './pages/admin-roommate-announcements/admin-roommate-announcements.component';

export const routes: Routes = [
  { path: '', component: LandingPageComponent }, // LandingPageComponent is the root
  { path: 'discovery', component: DiscoveryComponent, canActivate: [StudentGuard] }, // Route for DiscoveryComponent - STUDENT ONLY
  { path: 'profile', component: ProfileComponent }, // Route for ProfileComponent
  { path: 'property/:id', component: PropertyDetailsComponent }, // Route for PropertyDetailsComponent with id parameter
  { path: 'properties/:id', component: PropertyDetailsComponent }, // Alternate route matching the links in owner-property-card
  { path: 'my-inquiries', component: MyInquiriesComponent, canActivate: [StudentGuard] }, // Route for student inquiries - STUDENT ONLY
  { path: 'student/dashboard', component: StudentDashboardComponent, canActivate: [StudentGuard] }, // Route for student dashboard - STUDENT ONLY

  // Roommate Matching Routes (Primary Feature - creating step by step)
  { path: 'roommates/browse', component: BrowseRoommatesComponent, canActivate: [StudentGuard] }, // Main roommate browsing with ML recommendations - STUDENT ONLY
  { path: 'roommates/announcement/:id', component: RoommateAnnouncementDetailsComponent, canActivate: [StudentGuard] }, // Detailed view of specific announcement - STUDENT ONLY
  { path: 'roommates/post', component: PostRoommateAnnouncementComponent, canActivate: [StudentGuard] }, // Post new roommate announcement - STUDENT ONLY
  { path: 'roommates/my-announcements', component: MyAnnouncementsComponent, canActivate: [StudentGuard] }, // Manage my posted announcements - STUDENT ONLY
  { path: 'roommates/compatible-students', component: CompatibleStudentsComponent, canActivate: [StudentGuard] }, // Discover compatible students - STUDENT ONLY
  { path: 'roommates/preferences', component: RoommatePreferencesComponent, canActivate: [StudentGuard] }, // Set roommate preferences for ML matching - STUDENT ONLY
  { path: 'roommates/connections', component: ConnectionRequestsComponent, canActivate: [StudentGuard] }, // Manage connection requests and network - STUDENT ONLY
  { path: 'messages', component: MessagesComponent, canActivate: [StudentGuard] }, // Real-time messaging between connected students - STUDENT ONLY
  // { path: 'roommates/my-applications', component: MyRoommateApplicationsComponent }, // View my submitted applications
  { path: 'roommates', redirectTo: 'roommates/browse', pathMatch: 'full' }, // Default to browse when accessing /roommates

  { path: 'access-denied-owner', component: AccessDeniedOwnerFeatureComponent }, // Route for the new access denied page
  { path: 'owner/profile-setup', component: OwnerProfileSetupComponent }, // Route for owner profile setup

  // Admin Dashboard Route (requires ADMIN role)
  { path: 'admin/dashboard', component: AdminDashboardComponent }, // Admin dashboard with comprehensive system management
  { path: 'admin/users', component: AdminUsersComponent }, // Admin user management with CRUD operations
  { path: 'admin/properties', component: AdminPropertiesComponent }, // Admin property management with CRUD operations
  { path: 'admin/roommate-announcements', component: AdminRoommateAnnouncementsComponent }, // Admin roommate announcement management
  { path: 'admin/system', component: AdminDashboardComponent }, // Temporary: reuse dashboard until system component is created
  { path: 'admin', redirectTo: 'admin/dashboard', pathMatch: 'full' }, // Default to dashboard when accessing /admin

  // Owner routes that require a completed profile
  {
    path: 'owner',
    canActivate: [OwnerProfileGuard],
    children: [
      // Will add more owner pages here as they are created
      { path: 'dashboard', component: OwnerDashboardComponent }, // New beautiful owner dashboard
      { path: 'property/new', component: PropertyListingFormComponent }, // Use our new property listing form
      { path: 'property/edit/:id', component: PropertyListingFormComponent }, // Use property listing form for edit too
      { path: 'my-properties', component: MyListingsComponent }, // Use the My Listings component for property management
      { path: 'inquiries', component: OwnerInquiriesComponent }, // Owner inquiries management
      { path: 'analytics', component: OwnerAnalyticsComponent }, // Owner analytics dashboard
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' } // Default to dashboard when accessing /owner
    ]
  },

  { path: 'auth', loadChildren: () => import('./auth/auth.module').then(m => m.AuthModule) },
  { path: '**', redirectTo: '' }
];
