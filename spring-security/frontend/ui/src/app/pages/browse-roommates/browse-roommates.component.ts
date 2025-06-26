import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup } from '@angular/forms';
import { Subscription } from 'rxjs';

import { RoommateService, RoommateAnnouncementDTO } from '../../services/roommate.service';
import { RecommendationService, AnnouncementWithScore, UserWithScore, RecommendationStats } from '../../services/recommendation.service';
import { AuthService } from '../../auth/auth.service';
import { WebSocketService } from '../../services/websocket.service';
import { ConnectionRequestService, ConnectionRequestCreateDTO } from '../../services/connection-request.service';
import { WebSocketNotification } from '../../models/inquiry.model';

interface FilterOptions {
  minRent?: number;
  maxRent?: number;
  location?: string;
  roomType?: string;
  moveInDate?: string;
  compatibilityThreshold?: number;
}

@Component({
  selector: 'app-browse-roommates',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule, ReactiveFormsModule],
  templateUrl: './browse-roommates.component.html',
  styleUrls: ['./browse-roommates.component.css']
})
export class BrowseRoommatesComponent implements OnInit, OnDestroy {
  
  // Data properties
  personalizedRecommendations: AnnouncementWithScore[] = [];
  highQualityMatches: AnnouncementWithScore[] = [];
  compatibleStudents: UserWithScore[] = [];
  allAnnouncements: RoommateAnnouncementDTO[] = [];
  recommendationStats?: RecommendationStats;
  
  // Filter and search
  filterForm: FormGroup;
  searchQuery = '';
  activeFilter: 'all' | 'recommendations' | 'high-quality' | 'compatible-students' = 'recommendations';
  
  // Pagination
  currentPage = 0;
  pageSize = 10;
  totalElements = 0;
  totalPages = 0;
  
  // Loading and error states
  isLoadingRecommendations = true;
  isLoadingAnnouncements = false;
  isLoadingHighQuality = false;
  isLoadingCompatibleStudents = false;
  error: string | null = null;
  
  // UI state
  showFilters = false;
  selectedAnnouncement?: RoommateAnnouncementDTO;
  showApplicationModal = false;
  applicationMessage = '';
  
  // WebSocket subscription
  private notificationSubscription?: Subscription;

  constructor(
    private roommateService: RoommateService,
    private recommendationService: RecommendationService,
    private authService: AuthService,
    private webSocketService: WebSocketService,
    private connectionRequestService: ConnectionRequestService,
    private fb: FormBuilder,
    private router: Router
  ) {
    this.filterForm = this.fb.group({
      minRent: [''],
      maxRent: [''],
      location: [''],
      roomType: [''],
      moveInDate: [''],
      compatibilityThreshold: [30] // Default 30% minimum compatibility
    });
  }

  ngOnInit(): void {
    // Check access permissions for roommate functionality
    this.checkRoommateAccessPermissions();
    
    this.loadInitialData();
    this.setupWebSocketNotifications();
    this.setupFilterWatcher();
  }

  ngOnDestroy(): void {
    if (this.notificationSubscription) {
      this.notificationSubscription.unsubscribe();
    }
  }

  /**
   * Load initial data on component load
   */
  private loadInitialData(): void {
    // Load personalized ML recommendations first (main feature)
    this.loadPersonalizedRecommendations();
    
    // Load stats for premium feature display
    this.loadRecommendationStats();
    
    // Load high-quality matches if user qualifies
    this.loadHighQualityMatches();

    // NEW: Load compatible students (preload for quick access)
    this.loadCompatibleStudents();
  }

  /**
   * Load ML-powered personalized recommendations (MAIN FEATURE)
   */
  loadPersonalizedRecommendations(): void {
    this.isLoadingRecommendations = true;
    this.error = null;

    this.recommendationService.getPersonalizedRecommendations(20).subscribe({
      next: (recommendations) => {
        this.personalizedRecommendations = recommendations;
        this.isLoadingRecommendations = false;
        
        console.log(`🎯 Loaded ${recommendations.length} personalized recommendations`);
        
        // Auto-select recommendations tab if we have good matches
        if (recommendations.length > 0) {
          this.activeFilter = 'recommendations';
        } else {
          // Fallback to all announcements if no recommendations
          this.activeFilter = 'all';
          this.loadAllAnnouncements();
        }
      },
      error: (error) => {
        console.error('Error loading personalized recommendations:', error);
        this.error = 'Failed to load personalized recommendations. Showing all announcements.';
        this.isLoadingRecommendations = false;
        
        // Fallback to all announcements
        this.activeFilter = 'all';
        this.loadAllAnnouncements();
      }
    });
  }

  /**
   * Load high-quality matches (70%+ compatibility)
   */
  loadHighQualityMatches(): void {
    this.isLoadingHighQuality = true;
    
    this.recommendationService.getHighQualityMatches(10).subscribe({
      next: (matches) => {
        this.highQualityMatches = matches;
        this.isLoadingHighQuality = false;
        
        console.log(`⭐ Found ${matches.length} high-quality matches (70%+ compatibility)`);
      },
      error: (error) => {
        console.error('Error loading high-quality matches:', error);
        this.isLoadingHighQuality = false;
      }
    });
  }

  /**
   * NEW: Load compatible students (not announcement-based)
   * Shows students you might want to connect with for roommate opportunities
   */
  loadCompatibleStudents(): void {
    this.isLoadingCompatibleStudents = true;
    this.error = null;

    this.recommendationService.getCompatibleStudents(15).subscribe({
      next: (students) => {
        this.compatibleStudents = students;
        this.isLoadingCompatibleStudents = false;
        
        console.log(`👥 Found ${students.length} compatible students`);
        
        // Log top matches for debugging
        students.slice(0, 3).forEach(student => {
          console.log(`${student.rank}. ${student.user.username} (${student.user.institute}): ${student.compatibilityPercentage}% - ${student.recommendationReason}`);
        });
      },
      error: (error) => {
        console.error('Error loading compatible students:', error);
        this.error = 'Failed to load compatible students. Please try again.';
        this.isLoadingCompatibleStudents = false;
      }
    });
  }

  /**
   * Load all announcements (fallback/browse all option)
   */
  loadAllAnnouncements(): void {
    this.isLoadingAnnouncements = true;
    
    this.roommateService.getAnnouncementsForBrowsing(this.currentPage, this.pageSize).subscribe({
      next: (response) => {
        this.allAnnouncements = response.content;
        this.totalElements = response.totalElements;
        this.totalPages = response.totalPages;
        this.isLoadingAnnouncements = false;
        
        console.log(`📋 Loaded ${response.content.length} announcements`);
      },
      error: (error) => {
        console.error('Error loading announcements:', error);
        this.error = 'Failed to load roommate announcements.';
        this.isLoadingAnnouncements = false;
      }
    });
  }

  /**
   * Load recommendation statistics
   */
  loadRecommendationStats(): void {
    this.recommendationService.getRecommendationStats().subscribe({
      next: (stats) => {
        this.recommendationStats = stats;
        console.log(`📊 Recommendation stats: ${(stats.averageCompatibility * 100).toFixed(1)}% avg compatibility`);
      },
      error: (error) => {
        console.error('Error loading recommendation stats:', error);
      }
    });
  }

  /**
   * Setup WebSocket notifications for real-time updates
   */
  private setupWebSocketNotifications(): void {
    if (!this.webSocketService.isConnected()) {
      this.webSocketService.connect();
    }

    this.notificationSubscription = this.webSocketService.notifications$.subscribe({
      next: (notification: WebSocketNotification) => {
        if (notification.type === 'NEW_ROOMMATE_APPLICATION') {
          console.log('New roommate application notification received', notification);
          // Could refresh data or show notification
        } else if (notification.type === 'ROOMMATE_MATCH_FOUND') {
          console.log('New roommate match found!', notification);
          // Refresh recommendations to include new match
          this.refreshCurrentView();
        }
      },
      error: (error) => {
        console.error('WebSocket notification error:', error);
      }
    });
  }

  /**
   * Setup filter form watcher
   */
  private setupFilterWatcher(): void {
    this.filterForm.valueChanges.subscribe(() => {
      // Auto-apply filters when changed (debounced)
      this.applyFilters();
    });
  }

  // ========== UI ACTIONS ==========

  /**
   * Switch between view modes
   */
  switchFilter(filter: 'all' | 'recommendations' | 'high-quality' | 'compatible-students'): void {
    this.activeFilter = filter;
    
    switch (filter) {
      case 'recommendations':
        if (this.personalizedRecommendations.length === 0) {
          this.loadPersonalizedRecommendations();
        }
        break;
      case 'high-quality':
        if (this.highQualityMatches.length === 0) {
          this.loadHighQualityMatches();
        }
        break;
      case 'all':
        if (this.allAnnouncements.length === 0) {
          this.loadAllAnnouncements();
        }
        break;
      case 'compatible-students':
        if (this.compatibleStudents.length === 0) {
          this.loadCompatibleStudents();
        }
        break;
    }
  }

  /**
   * Toggle filter panel
   */
  toggleFilters(): void {
    this.showFilters = !this.showFilters;
  }

  /**
   * Apply filters to current view
   */
  applyFilters(): void {
    const filters = this.filterForm.value as FilterOptions;
    console.log('Applying filters:', filters);
    
    // Refresh current view with filters
    this.refreshCurrentView();
  }

  /**
   * Clear all filters
   */
  clearFilters(): void {
    this.filterForm.reset({
      compatibilityThreshold: 30 // Keep default threshold
    });
    this.searchQuery = '';
    this.refreshCurrentView();
  }

  /**
   * Refresh current view
   */
  refreshCurrentView(): void {
    switch (this.activeFilter) {
      case 'recommendations':
        this.loadPersonalizedRecommendations();
        break;
      case 'high-quality':
        this.loadHighQualityMatches();
        break;
      case 'all':
        this.loadAllAnnouncements();
        break;
      case 'compatible-students':
        this.loadCompatibleStudents();
        break;
    }
  }

  /**
   * Open application modal for announcement
   */
  openApplicationModal(announcement: RoommateAnnouncementDTO): void {
    this.selectedAnnouncement = announcement;
    this.showApplicationModal = true;
    this.applicationMessage = ''; // Reset message
  }

  /**
   * Close application modal
   */
  closeApplicationModal(): void {
    this.showApplicationModal = false;
    this.selectedAnnouncement = undefined;
    this.applicationMessage = '';
  }

  /**
   * Apply to roommate announcement
   */
  applyToAnnouncement(announcement: RoommateAnnouncementDTO, message: string): void {
    const application = {
      announcementId: announcement.id,
      message: message.trim()
    };

    this.roommateService.applyToAnnouncement(application).subscribe({
      next: (result) => {
        console.log(`✅ Applied to announcement: ${result.compatibilityScore * 100}% compatibility`);
        
        // Close modal and show success
        this.closeApplicationModal();
        
        // Could show success message or refresh data
        this.showSuccessMessage(`Application sent! Compatibility: ${Math.round(result.compatibilityScore * 100)}%`);
      },
      error: (error) => {
        console.error('Error applying to announcement:', error);
        this.error = 'Failed to send application. Please try again.';
      }
    });
  }

  /**
   * Navigate to post announcement page
   */
  navigateToPostAnnouncement(): void {
    console.log('Button clicked - navigating to post page');
    this.router.navigate(['/roommates/post']);
  }

  /**
   * Navigate to roommate preferences page
   */
  navigateToPreferences(): void {
    console.log('Button clicked - navigating to preferences page');
    this.router.navigate(['/roommates/preferences']);
  }

  // ========== UTILITY METHODS ==========

  /**
   * Get current data source based on active filter
   * Returns announcements for most tabs, students for compatible-students tab
   */
  getCurrentDataSource(): any[] {
    switch (this.activeFilter) {
      case 'recommendations':
        return this.personalizedRecommendations;
      case 'high-quality':
        return this.highQualityMatches;
      case 'all':
        return this.allAnnouncements;
      case 'compatible-students':
        return this.compatibleStudents;
      default:
        return [];
    }
  }

  /**
   * Get current announcements (for announcement-based tabs)
   */
  getCurrentAnnouncements(): (AnnouncementWithScore | RoommateAnnouncementDTO)[] {
    switch (this.activeFilter) {
      case 'recommendations':
        return this.personalizedRecommendations;
      case 'high-quality':
        return this.highQualityMatches;
      case 'all':
        return this.allAnnouncements;
      default:
        return [];
    }
  }

  /**
   * Get current students (for student-based tabs)
   */
  getCurrentStudents(): UserWithScore[] {
    if (this.activeFilter === 'compatible-students') {
      return this.compatibleStudents;
    }
    return [];
  }

  /**
   * Check if current tab shows announcements
   */
  isAnnouncementTab(): boolean {
    return ['recommendations', 'high-quality', 'all'].includes(this.activeFilter);
  }

  /**
   * Check if current tab shows students
   */
  isStudentTab(): boolean {
    return this.activeFilter === 'compatible-students';
  }

  /**
   * Check if current data source is loading
   */
  isCurrentViewLoading(): boolean {
    switch (this.activeFilter) {
      case 'recommendations':
        return this.isLoadingRecommendations;
      case 'high-quality':
        return this.isLoadingHighQuality;
      case 'all':
        return this.isLoadingAnnouncements;
      case 'compatible-students':
        return this.isLoadingCompatibleStudents;
      default:
        return false;
    }
  }

  /**
   * Check if announcement has compatibility score
   */
  hasCompatibilityScore(item: any): item is AnnouncementWithScore {
    return 'compatibilityScore' in item;
  }

  /**
   * Get announcement from item (works for both types)
   */
  getAnnouncement(item: AnnouncementWithScore | RoommateAnnouncementDTO): RoommateAnnouncementDTO {
    return this.hasCompatibilityScore(item) ? item.announcement : item;
  }

  /**
   * Get compatibility score from item
   */
  getCompatibilityScore(item: AnnouncementWithScore | RoommateAnnouncementDTO): number | null {
    return this.hasCompatibilityScore(item) ? item.compatibilityScore : null;
  }

  /**
   * Get compatibility percentage for display
   */
  getCompatibilityPercentage(item: AnnouncementWithScore | RoommateAnnouncementDTO): number | null {
    const score = this.getCompatibilityScore(item);
    return score ? Math.round(score * 100) : null;
  }

  /**
   * Get compatibility level text
   */
  getCompatibilityLevel(item: AnnouncementWithScore | RoommateAnnouncementDTO): string {
    const score = this.getCompatibilityScore(item);
    if (!score) return '';
    
    return this.recommendationService.getRecommendationExplanation(score);
  }

  /**
   * Get compatibility color
   */
  getCompatibilityColor(item: AnnouncementWithScore | RoommateAnnouncementDTO): string {
    const score = this.getCompatibilityScore(item);
    return score ? this.recommendationService.getCompatibilityColor(score) : '#6b7280';
  }

  /**
   * Get compatibility emoji
   */
  getCompatibilityEmoji(item: AnnouncementWithScore | RoommateAnnouncementDTO): string {
    const score = this.getCompatibilityScore(item);
    return score ? this.recommendationService.getCompatibilityEmoji(score) : '📋';
  }

  /**
   * Format date for display
   */
  formatDate(dateString: string): string {
    const date = new Date(dateString);
    return date.toLocaleDateString();
  }

  /**
   * Calculate days until move-in
   */
  getDaysUntilMoveIn(moveInDate: string): number {
    const today = new Date();
    const moveIn = new Date(moveInDate);
    const diffTime = moveIn.getTime() - today.getTime();
    return Math.ceil(diffTime / (1000 * 60 * 60 * 24));
  }

  /**
   * Show success message (placeholder)
   */
  private showSuccessMessage(message: string): void {
    console.log('Success:', message);
    // Could integrate with toast service
  }

  /**
   * Get compatibility display with null safety
   */
  getCompatibilityDisplay(compatibility: number | null | undefined): string {
    if (compatibility == null || isNaN(compatibility)) {
      return '0.0';
    }
    return (compatibility * 100).toFixed(1);
  }

  /**
   * TrackBy function for announcement list performance
   */
  trackByAnnouncementId(index: number, item: AnnouncementWithScore | RoommateAnnouncementDTO): number {
    if ('announcement' in item) {
      return item.announcement.id;
    }
    return item.id;
  }

  /**
   * NEW: TrackBy function for student list performance
   */
  trackByStudentId(index: number, student: UserWithScore): number {
    return student.user.id;
  }

  // ========== STUDENT COMPATIBILITY METHODS ==========

  /**
   * Get compatibility emoji for students
   */
  getStudentCompatibilityEmoji(student: UserWithScore): string {
    const score = student.compatibilityScore;
    if (score >= 0.90) return '🎯';
    if (score >= 0.75) return '⭐';
    if (score >= 0.60) return '👍';
    if (score >= 0.40) return '👌';
    return '📝';
  }

  /**
   * Get compatibility color for students
   */
  getStudentCompatibilityColor(student: UserWithScore): string {
    const score = student.compatibilityScore;
    if (score >= 0.90) return '#22c55e'; // Green
    if (score >= 0.75) return '#84cc16'; // Light green
    if (score >= 0.60) return '#eab308'; // Yellow
    if (score >= 0.40) return '#f97316'; // Orange
    return '#ef4444'; // Red
  }

  /**
   * Get compatibility level for students
   */
  getStudentCompatibilityLevel(student: UserWithScore): string {
    const score = student.compatibilityScore;
    if (score >= 0.90) return 'Excellent Match';
    if (score >= 0.75) return 'Very Good';
    if (score >= 0.60) return 'Good Match';
    if (score >= 0.40) return 'Fair Match';
    return 'Low Match';
  }

  // ========== STUDENT ACTION METHODS ==========

  /**
   * Connect with a compatible student
   */
  connectWithStudent(student: UserWithScore): void {
    console.log('Connecting with student:', student.user.username);
    
    // Show confirmation dialog with student info
    const message = `Send connection request to ${student.user.username}?\n\n` +
                   `Institute: ${student.user.institute}\n` +
                   `Field: ${student.user.fieldOfStudy}\n` +
                   `Compatibility: ${student.compatibilityPercentage}%\n` +
                   `Reason: ${student.recommendationReason}`;
    
    if (confirm(message)) {
      // Prompt for optional message
      const personalMessage = prompt(
        `Add a personal message to your connection request (optional):`,
        `Hi ${student.user.username}! I found your profile through our compatibility matching system. We have ${student.compatibilityPercentage}% compatibility and I think we could be great roommates. Would you like to connect?`
      );
      
      // User cancelled the message prompt
      if (personalMessage === null) {
        return;
      }
      
      // Create connection request
      const connectionRequest: ConnectionRequestCreateDTO = {
        receiverId: student.user.id,
        message: personalMessage || undefined
      };
      
      // Send the connection request
      this.connectionRequestService.sendConnectionRequest(connectionRequest).subscribe({
        next: (response) => {
          console.log('Connection request sent successfully:', response);
          alert(`✅ Connection request sent to ${student.user.username}!\n\nThey will receive a notification and can accept or decline your request. You can view the status in your connection requests.`);
          
          // Optional: Remove student from list or mark as "request sent"
          // You could add a property to track this state
        },
        error: (error) => {
          console.error('Error sending connection request:', error);
          
          // Handle specific error cases
          if (error.status === 400) {
            if (error.error?.message?.includes('already exists')) {
              alert(`❌ You already have a connection request with ${student.user.username}.`);
            } else if (error.error?.message?.includes('yourself')) {
              alert(`❌ You cannot send a connection request to yourself.`);
            } else {
              alert(`❌ Unable to send connection request: ${error.error?.message || 'Invalid request'}`);
            }
          } else if (error.status === 404) {
            alert(`❌ Student ${student.user.username} not found.`);
          } else if (error.status === 403) {
            alert(`❌ You don't have permission to send connection requests. Make sure you're logged in as a student.`);
          } else {
            alert(`❌ Failed to send connection request to ${student.user.username}. Please try again later.`);
          }
        }
      });
    }
  }

  /**
   * View student profile details
   */
  viewStudentProfile(student: UserWithScore): void {
    console.log('Viewing profile for student:', student.user.username);
    
    // For now, show detailed info in alert
    // In the future, this could navigate to a profile page or open a modal
    const profileInfo = `Profile: ${student.user.username}\n\n` +
                       `Email: ${student.user.email}\n` +
                       `Institute: ${student.user.institute || 'Not specified'}\n` +
                       `Field of Study: ${student.user.fieldOfStudy || 'Not specified'}\n` +
                       `Education Level: ${student.user.educationLevel || 'Not specified'}\n` +
                       `Age: ${student.user.age ? student.user.age + ' years' : 'Not specified'}\n\n` +
                       `Compatibility Score: ${student.compatibilityPercentage}%\n` +
                       `Match Reason: ${student.recommendationReason || 'General compatibility'}\n` +
                       `Rank: #${student.rank || 'N/A'}`;
    
    alert(profileInfo);
  }

  /**
   * Check if user has access to roommate functionality
   * Only STUDENT users are allowed, OWNER and ADMIN are restricted
   */
  private checkRoommateAccessPermissions(): void {
    if (this.authService.isLoggedIn()) {
      const userRole = this.authService.getUserRole();
      
      // If user is OWNER or ADMIN, show toast and redirect
      if (userRole === 'OWNER' || userRole === 'ADMIN') {
        this.showRoommateAccessDeniedToast(userRole);
        this.redirectBasedOnRole(userRole);
        return;
      }
      
      // Students are allowed to access roommate functionality
      if (userRole === 'STUDENT') {
        return; // Allow access
      }
    }
    
    // If not authenticated, redirect to login
    if (!this.authService.isLoggedIn()) {
      this.router.navigate(['/auth/login']);
    }
  }

  private showRoommateAccessDeniedToast(userRole: string): void {
    // Create a toast notification
    const toast = document.createElement('div');
    toast.className = 'fixed top-4 right-4 z-50 bg-red-600 text-white px-6 py-4 rounded-lg shadow-lg max-w-sm animate-slide-in';
    toast.innerHTML = `
      <div class="flex items-center">
        <svg class="w-5 h-5 mr-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"></path>
        </svg>
        <div>
          <div class="font-medium">Access Restricted</div>
          <div class="text-sm opacity-90">The roommate feature is reserved for students only</div>
        </div>
        <button onclick="this.parentElement.parentElement.remove()" 
                class="ml-4 text-red-200 hover:text-white">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"></path>
          </svg>
        </button>
      </div>
    `;
    
    document.body.appendChild(toast);
    
    // Auto remove after 5 seconds
    setTimeout(() => {
      if (toast.parentElement) {
        toast.remove();
      }
    }, 5000);
  }

  private redirectBasedOnRole(userRole: string): void {
    // Small delay to let the toast show before redirecting
    setTimeout(() => {
      switch (userRole) {
        case 'OWNER':
          this.router.navigate(['/owner/dashboard']);
          break;
        case 'ADMIN':
          this.router.navigate(['/admin/dashboard']);
          break;
        default:
          this.router.navigate(['/']);
          break;
      }
    }, 1500);
  }
} 