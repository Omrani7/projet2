import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup } from '@angular/forms';
import { Subscription } from 'rxjs';

import { RoommateService, RoommateAnnouncementDTO, RoommateApplicationDTO } from '../../services/roommate.service';
import { AuthService } from '../../auth/auth.service';
import { WebSocketService } from '../../services/websocket.service';
import { WebSocketNotification } from '../../models/inquiry.model';

@Component({
  selector: 'app-my-announcements',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule, ReactiveFormsModule],
  templateUrl: './my-announcements.component.html',
  styleUrls: ['./my-announcements.component.css']
})
export class MyAnnouncementsComponent implements OnInit, OnDestroy {
  
  // Data properties
  myAnnouncements: RoommateAnnouncementDTO[] = [];
  selectedAnnouncement?: RoommateAnnouncementDTO;
  applications: RoommateApplicationDTO[] = [];
  
  // Pagination
  currentPage = 0;
  pageSize = 10;
  totalElements = 0;
  totalPages = 0;
  
  // Loading and error states
  isLoadingAnnouncements = true;
  isLoadingApplications = false;
  error: string | null = null;
  successMessage: string | null = null;
  
  // UI state
  showApplicationsModal = false;
  showResponseModal = false;
  selectedApplication?: RoommateApplicationDTO;
  responseForm: FormGroup;
  
  // WebSocket subscription
  private notificationSubscription?: Subscription;

  constructor(
    private roommateService: RoommateService,
    private authService: AuthService,
    private webSocketService: WebSocketService,
    private fb: FormBuilder,
    private router: Router
  ) {
    this.responseForm = this.fb.group({
      status: ['', []],
      responseMessage: ['']
    });
  }

  ngOnInit(): void {
    this.loadMyAnnouncements();
    this.setupWebSocketNotifications();
  }

  ngOnDestroy(): void {
    if (this.notificationSubscription) {
      this.notificationSubscription.unsubscribe();
    }
  }

  /**
   * Load user's own announcements
   */
  loadMyAnnouncements(): void {
    this.isLoadingAnnouncements = true;
    this.error = null;
    
    this.roommateService.getMyAnnouncements(this.currentPage, this.pageSize).subscribe({
      next: (response) => {
        this.myAnnouncements = response.content;
        this.totalElements = response.totalElements;
        this.totalPages = response.totalPages;
        this.isLoadingAnnouncements = false;
        
        console.log(`📋 Loaded ${response.content.length} of my announcements`);
      },
      error: (error) => {
        console.error('Error loading my announcements:', error);
        this.error = 'Failed to load your announcements.';
        this.isLoadingAnnouncements = false;
      }
    });
  }

  /**
   * Load applications for specific announcement
   */
  loadApplicationsForAnnouncement(announcement: RoommateAnnouncementDTO): void {
    this.selectedAnnouncement = announcement;
    this.isLoadingApplications = true;
    this.showApplicationsModal = true;
    
    this.roommateService.getApplicationsForAnnouncement(announcement.id, 0, 50).subscribe({
      next: (response) => {
        this.applications = response.content;
        this.isLoadingApplications = false;
        
        console.log(`📝 Loaded ${this.applications.length} applications for announcement ${announcement.id}`);
      },
      error: (error) => {
        console.error('Error loading applications:', error);
        this.error = 'Failed to load applications.';
        this.isLoadingApplications = false;
      }
    });
  }

  /**
   * Open response modal for application
   */
  openResponseModal(application: RoommateApplicationDTO): void {
    this.selectedApplication = application;
    this.showResponseModal = true;
    this.responseForm.patchValue({
      status: '',
      responseMessage: ''
    });
  }

  /**
   * Close response modal
   */
  closeResponseModal(): void {
    this.showResponseModal = false;
    this.selectedApplication = undefined;
    this.responseForm.reset();
  }

  /**
   * Respond to application
   */
  respondToApplication(status: 'ACCEPTED' | 'REJECTED'): void {
    if (!this.selectedApplication) return;
    
    const response = {
      status: status,
      responseMessage: this.responseForm.get('responseMessage')?.value || ''
    };
    
    this.roommateService.respondToApplication(this.selectedApplication.id, response).subscribe({
      next: (result) => {
        console.log(`✅ Responded to application: ${status}`);
        
        // Update application in list
        const index = this.applications.findIndex(app => app.id === this.selectedApplication!.id);
        if (index !== -1) {
          this.applications[index] = result;
        }
        
        // Close modal and show success
        this.closeResponseModal();
        this.successMessage = `Application ${status.toLowerCase()} successfully!`;
        
        // Clear success message after 3 seconds
        setTimeout(() => {
          this.successMessage = null;
        }, 3000);
      },
      error: (error) => {
        console.error('Error responding to application:', error);
        this.error = 'Failed to respond to application. Please try again.';
      }
    });
  }

  /**
   * Close applications modal
   */
  closeApplicationsModal(): void {
    this.showApplicationsModal = false;
    this.selectedAnnouncement = undefined;
    this.applications = [];
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
          console.log('New application received!', notification);
          this.loadMyAnnouncements(); // Refresh to update application counts
          this.successMessage = 'New application received!';
          setTimeout(() => this.successMessage = null, 3000);
        }
      },
      error: (error) => {
        console.error('WebSocket notification error:', error);
      }
    });
  }

  /**
   * Navigate to announcement details
   */
  viewAnnouncementDetails(announcement: RoommateAnnouncementDTO): void {
    this.router.navigate(['/roommates/announcement', announcement.id]);
  }

  /**
   * Navigate to post new announcement
   */
  postNewAnnouncement(): void {
    this.router.navigate(['/roommates/post']);
  }

  /**
   * Format date for display
   */
  formatDate(dateString: string): string {
    const date = new Date(dateString);
    return date.toLocaleDateString();
  }

  /**
   * Get status color for application
   */
  getStatusColor(status: string): string {
    switch (status) {
      case 'PENDING': return '#f59e0b';
      case 'ACCEPTED': return '#10b981';
      case 'REJECTED': return '#ef4444';
      default: return '#6b7280';
    }
  }

  /**
   * Get status icon for application
   */
  getStatusIcon(status: string): string {
    switch (status) {
      case 'PENDING': return '⏳';
      case 'ACCEPTED': return '✅';
      case 'REJECTED': return '❌';
      default: return '📝';
    }
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
   * Clear messages
   */
  clearMessages(): void {
    this.error = null;
    this.successMessage = null;
  }
} 