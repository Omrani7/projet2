import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, ActivatedRoute, Router } from '@angular/router';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Subscription } from 'rxjs';

import { RoommateService, RoommateAnnouncementDTO, RoommateApplicationDTO } from '../../services/roommate.service';
import { RecommendationService, AnnouncementWithScore, UserWithScore } from '../../services/recommendation.service';
import { AuthService } from '../../auth/auth.service';
import { WebSocketService } from '../../services/websocket.service';
import { WebSocketNotification } from '../../models/inquiry.model';

@Component({
  selector: 'app-roommate-announcement-details',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule, ReactiveFormsModule],
  templateUrl: './roommate-announcement-details.component.html',
  styleUrls: ['./roommate-announcement-details.component.css']
})
export class RoommateAnnouncementDetailsComponent implements OnInit, OnDestroy {
  
  // Core data
  announcement?: RoommateAnnouncementDTO;
  announcementWithScore?: AnnouncementWithScore;
  applications: RoommateApplicationDTO[] = [];
  compatibleApplicants: UserWithScore[] = [];
  
  // UI state
  isLoading = true;
  isApplicationsLoading = false;
  isLoadingCompatibleApplicants = false;
  error: string | null = null;
  successMessage: string | null = null;
  
  // User context
  currentUserId: number | null = null;
  isOwnAnnouncement = false;
  hasAlreadyApplied = false;
  userApplication?: RoommateApplicationDTO;
  
  // Application form
  applicationForm: FormGroup;
  showApplicationForm = false;
  isSubmittingApplication = false;
  
  // Response form (for announcement poster)
  showResponseModal = false;
  selectedApplication?: RoommateApplicationDTO;
  responseForm: FormGroup;
  isSubmittingResponse = false;
  
  // WebSocket subscription
  private notificationSubscription?: Subscription;
  private routeParamSubscription?: Subscription;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private roommateService: RoommateService,
    private recommendationService: RecommendationService,
    private authService: AuthService,
    private webSocketService: WebSocketService,
    private fb: FormBuilder
  ) {
    // Initialize forms
    this.applicationForm = this.fb.group({
      message: ['', [Validators.required, Validators.minLength(50), Validators.maxLength(500)]]
    });

    this.responseForm = this.fb.group({
      status: ['', Validators.required],
      responseMessage: ['', [Validators.required, Validators.minLength(20), Validators.maxLength(300)]]
    });
  }

  ngOnInit(): void {
    this.getCurrentUser();
    this.setupRouteParams();
    this.setupWebSocketNotifications();
  }

  ngOnDestroy(): void {
    if (this.notificationSubscription) {
      this.notificationSubscription.unsubscribe();
    }
    if (this.routeParamSubscription) {
      this.routeParamSubscription.unsubscribe();
    }
  }

    /**   * Get current user information   */  private getCurrentUser(): void {    const decodedToken = this.authService.getDecodedToken();    if (decodedToken && decodedToken.id) {      this.currentUserId = decodedToken.id;    } else {      console.error('Error getting current user from token');    }  }

  /**
   * Setup route parameter subscription
   */
  private setupRouteParams(): void {
    this.routeParamSubscription = this.route.params.subscribe(params => {
      const announcementId = parseInt(params['id']);
      if (announcementId) {
        this.loadAnnouncementDetails(announcementId);
      } else {
        this.error = 'Invalid announcement ID';
        this.isLoading = false;
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

        this.notificationSubscription = this.webSocketService.notifications$.subscribe({      next: (notification: WebSocketNotification) => {        if (notification.type === 'NEW_ROOMMATE_APPLICATION' &&             notification.roommateApplication?.announcementId === this.announcement?.id) {          console.log('New application received for this announcement');          this.refreshApplications();        } else if (notification.type === 'ROOMMATE_APPLICATION_RESPONSE' &&                   notification.roommateApplication?.id) {          console.log('Application response received');          this.refreshApplications();        }      },      error: (error: any) => {        console.error('WebSocket notification error:', error);      }    });
  }

  /**
   * Load announcement details and related data
   */
  private loadAnnouncementDetails(announcementId: number): void {
    this.isLoading = true;
    this.error = null;

    // Load basic announcement details
    this.roommateService.getAnnouncementById(announcementId).subscribe({
      next: (announcement) => {
        this.announcement = announcement;
        this.isOwnAnnouncement = this.currentUserId === announcement.poster.id;
        
        console.log(`📋 Loaded announcement: ${announcement.propertyTitle}`);
        console.log(`👤 Is own announcement: ${this.isOwnAnnouncement}`);
        
        // Load additional data based on ownership
        if (this.isOwnAnnouncement) {
          this.loadApplicationsForMyAnnouncement(announcementId);
          this.loadCompatibleApplicants(announcementId);
        } else {
          this.loadRecommendationWithScore(announcementId);
          this.checkIfAlreadyApplied(announcementId);
        }
        
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Error loading announcement:', error);
        this.error = 'Failed to load announcement details.';
        this.isLoading = false;
      }
    });
  }

  /**
   * Load announcement with ML compatibility score (for non-owners)
   */
  private loadRecommendationWithScore(announcementId: number): void {
    this.recommendationService.getPersonalizedRecommendations(100).subscribe({
      next: (recommendations) => {
        this.announcementWithScore = recommendations.find(
          rec => rec.announcement.id === announcementId
        );
        
        if (this.announcementWithScore) {
          console.log(`🎯 Compatibility: ${this.announcementWithScore.compatibilityPercentage}%`);
        }
      },
      error: (error) => {
        console.error('Error loading compatibility score:', error);
        // Continue without score - not critical
      }
    });
  }

  /**
   * Check if current user has already applied to this announcement
   */
  private checkIfAlreadyApplied(announcementId: number): void {
    this.roommateService.getMyApplications(0, 100).subscribe({
      next: (response) => {
        this.userApplication = response.content.find(
          app => app.poster.id === this.announcement?.poster.id
        );
        this.hasAlreadyApplied = !!this.userApplication;
        
        if (this.hasAlreadyApplied) {
          console.log(`✅ Already applied with status: ${this.userApplication?.status}`);
        }
      },
      error: (error) => {
        console.error('Error checking application status:', error);
        // Continue - not critical
      }
    });
  }

  /**
   * Load applications for announcement (for announcement poster)
   */
  private loadApplicationsForMyAnnouncement(announcementId: number): void {
    this.isApplicationsLoading = true;
    
    this.roommateService.getApplicationsForAnnouncement(announcementId, 0, 50).subscribe({
      next: (response) => {
        this.applications = response.content;
        this.isApplicationsLoading = false;
        
        console.log(`📝 Loaded ${this.applications.length} applications`);
      },
      error: (error) => {
        console.error('Error loading applications:', error);
        this.isApplicationsLoading = false;
      }
    });
  }

  /**
   * Load compatible applicants using ML recommendations
   */
  private loadCompatibleApplicants(announcementId: number): void {
    this.isLoadingCompatibleApplicants = true;
    
    this.recommendationService.getCompatibleApplicants(announcementId).subscribe({
      next: (applicants) => {
        this.compatibleApplicants = applicants;
        this.isLoadingCompatibleApplicants = false;
        
        console.log(`⭐ Found ${applicants.length} compatible applicants`);
      },
      error: (error) => {
        console.error('Error loading compatible applicants:', error);
        this.isLoadingCompatibleApplicants = false;
      }
    });
  }

  /**
   * Refresh applications data
   */
  private refreshApplications(): void {
    if (this.announcement && this.isOwnAnnouncement) {
      this.loadApplicationsForMyAnnouncement(this.announcement.id);
    } else if (this.announcement) {
      this.checkIfAlreadyApplied(this.announcement.id);
    }
  }

  // ========== UI ACTIONS ==========

  /**
   * Show application form
   */
  showApplicationModal(): void {
    this.showApplicationForm = true;
    this.applicationForm.reset();
  }

  /**
   * Hide application form
   */
  hideApplicationModal(): void {
    this.showApplicationForm = false;
    this.applicationForm.reset();
  }

  /**
   * Submit application
   */
  submitApplication(): void {
    if (!this.applicationForm.valid || !this.announcement) {
      return;
    }

    this.isSubmittingApplication = true;
    this.error = null;

    const applicationData = {
      announcementId: this.announcement.id,
      message: this.applicationForm.get('message')?.value.trim()
    };

    this.roommateService.applyToAnnouncement(applicationData).subscribe({
      next: (result) => {
        console.log(`✅ Application submitted with ${Math.round(result.compatibilityScore * 100)}% compatibility`);
        
        this.successMessage = `Application sent! Compatibility: ${Math.round(result.compatibilityScore * 100)}%`;
        this.userApplication = result;
        this.hasAlreadyApplied = true;
        this.hideApplicationModal();
        this.isSubmittingApplication = false;
        
        // Clear success message after 5 seconds
        setTimeout(() => {
          this.successMessage = null;
        }, 5000);
      },
      error: (error) => {
        console.error('Error submitting application:', error);
        this.error = 'Failed to submit application. Please try again.';
        this.isSubmittingApplication = false;
      }
    });
  }

  /**
   * Show response modal for application
   */
  showResponseModalForApplication(application: RoommateApplicationDTO): void {
    this.selectedApplication = application;
    this.showResponseModal = true;
    this.responseForm.reset();
  }

  /**
   * Hide response modal
   */
  hideResponseModalAction(): void {
    this.showResponseModal = false;
    this.selectedApplication = undefined;
    this.responseForm.reset();
  }

  /**
   * Submit response to application
   */
  submitResponse(): void {
    if (!this.responseForm.valid || !this.selectedApplication) {
      return;
    }

    this.isSubmittingResponse = true;
    this.error = null;

    const responseData = {
      status: this.responseForm.get('status')?.value as 'ACCEPTED' | 'REJECTED',
      responseMessage: this.responseForm.get('responseMessage')?.value.trim()
    };

    this.roommateService.respondToApplication(this.selectedApplication.id, responseData).subscribe({
      next: (result) => {
        console.log(`✅ Application ${responseData.status.toLowerCase()}`);
        
        this.successMessage = `Application ${responseData.status.toLowerCase()} successfully!`;
        this.refreshApplications();
        this.hideResponseModalAction();
        this.isSubmittingResponse = false;
        
        // Clear success message after 5 seconds
        setTimeout(() => {
          this.successMessage = null;
        }, 5000);
      },
      error: (error) => {
        console.error('Error submitting response:', error);
        this.error = 'Failed to submit response. Please try again.';
        this.isSubmittingResponse = false;
      }
    });
  }

  // ========== UTILITY METHODS ==========

  /**
   * Get compatibility score for display
   */
  getCompatibilityScore(): number | null {
    return this.announcementWithScore?.compatibilityScore ?? null;
  }

  /**
   * Get compatibility percentage for display
   */
  getCompatibilityPercentage(): number | null {
    const score = this.getCompatibilityScore();
    return score ? Math.round(score * 100) : null;
  }

  /**
   * Get compatibility level description
   */
  getCompatibilityLevel(): string {
    const score = this.getCompatibilityScore();
    return score ? this.recommendationService.getRecommendationExplanation(score) : '';
  }

  /**
   * Get compatibility color
   */
  getCompatibilityColor(): string {
    const score = this.getCompatibilityScore();
    return score ? this.recommendationService.getCompatibilityColor(score) : '#6b7280';
  }

  /**
   * Get compatibility emoji
   */
  getCompatibilityEmoji(): string {
    const score = this.getCompatibilityScore();
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
  getDaysUntilMoveIn(): number {
    if (!this.announcement) return 0;
    
    const today = new Date();
    const moveIn = new Date(this.announcement.moveInDate);
    const diffTime = moveIn.getTime() - today.getTime();
    return Math.ceil(diffTime / (1000 * 60 * 60 * 24));
  }

  /**
   * Get application status badge class
   */
  getApplicationStatusClass(status: string): string {
    switch (status.toLowerCase()) {
      case 'pending': return 'status-pending';
      case 'accepted': return 'status-accepted';
      case 'rejected': return 'status-rejected';
      case 'withdrawn': return 'status-withdrawn';
      default: return 'status-default';
    }
  }

  /**
   * Get application status icon
   */
  getApplicationStatusIcon(status: string): string {
    switch (status.toLowerCase()) {
      case 'pending': return '⏳';
      case 'accepted': return '✅';
      case 'rejected': return '❌';
      case 'withdrawn': return '↩️';
      default: return '📄';
    }
  }

  /**
   * Navigate back to browse
   */
  navigateBack(): void {
    this.router.navigate(['/roommates/browse']);
  }

    /**   * Clear success and error messages   */  clearMessages(): void {    this.successMessage = null;    this.error = null;  }  /**   * Track by function for applications   */  trackByApplicationId(index: number, application: RoommateApplicationDTO): number {    return application.id;  }  /**   * Track by function for users   */  trackByUserId(index: number, applicant: UserWithScore): number {    return applicant.user.id;  }  /**   * Get compatibility color for a specific score   */  getCompatibilityColorForScore(score: number): string {    return this.recommendationService.getCompatibilityColor(score);  }  /**   * Math utility for template   */  Math = Math;} 