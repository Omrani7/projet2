import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Subscription } from 'rxjs';

import { ConnectionRequestService, ConnectionRequestDTO, ConnectionRequestResponseDTO, PageResponse } from '../../services/connection-request.service';
import { AuthService } from '../../auth/auth.service';
import { WebSocketService } from '../../services/websocket.service';

@Component({
  selector: 'app-connection-requests',
  standalone: true,
  imports: [CommonModule, RouterModule, ReactiveFormsModule],
  templateUrl: './connection-requests.component.html',
  styleUrls: ['./connection-requests.component.css']
})
export class ConnectionRequestsComponent implements OnInit, OnDestroy {
  
  // Data properties
  sentRequests: ConnectionRequestDTO[] = [];
  receivedRequests: ConnectionRequestDTO[] = [];
  pendingRequests: ConnectionRequestDTO[] = [];
  acceptedConnections: ConnectionRequestDTO[] = [];
  
  // UI state
  activeTab: 'received' | 'sent' | 'connections' = 'received';
  isLoading = false;
  error: string | null = null;
  
  // Pagination
  currentPage = 0;
  pageSize = 10;
  totalElements = 0;
  totalPages = 0;
  
  // Response modal
  showResponseModal = false;
  selectedRequest?: ConnectionRequestDTO;
  responseForm: FormGroup;
  
  // WebSocket subscription
  private notificationSubscription?: Subscription;
  
  constructor(
    private connectionRequestService: ConnectionRequestService,
    private authService: AuthService,
    private webSocketService: WebSocketService,
    private fb: FormBuilder,
    private router: Router
  ) {
    this.responseForm = this.fb.group({
      responseMessage: ['', [Validators.maxLength(500)]]
    });
  }
  
  ngOnInit(): void {
    this.loadInitialData();
    this.setupWebSocketNotifications();
  }
  
  ngOnDestroy(): void {
    if (this.notificationSubscription) {
      this.notificationSubscription.unsubscribe();
    }
  }
  
  /**
   * Load initial data based on active tab
   */
  private loadInitialData(): void {
    this.loadReceivedRequests();
  }
  
  /**
   * Setup WebSocket notifications for real-time updates
   */
  private setupWebSocketNotifications(): void {
    this.notificationSubscription = this.webSocketService.notifications$.subscribe({
      next: (notification: any) => {
        if (notification.type === 'NEW_CONNECTION_REQUEST') {
          // Refresh received requests when new request arrives
          if (this.activeTab === 'received') {
            this.loadReceivedRequests();
          }
        } else if (notification.type === 'CONNECTION_REQUEST_ACCEPTED' || 
                   notification.type === 'CONNECTION_REQUEST_REJECTED') {
          // Refresh sent requests when response received
          if (this.activeTab === 'sent') {
            this.loadSentRequests();
          } else if (this.activeTab === 'connections') {
            this.loadAcceptedConnections();
          }
        }
      },
      error: (error: any) => {
        console.error('WebSocket notification error:', error);
      }
    });
  }
  
  /**
   * Switch between tabs
   */
  switchTab(tab: 'received' | 'sent' | 'connections'): void {
    this.activeTab = tab;
    this.currentPage = 0;
    this.error = null;
    
    switch (tab) {
      case 'received':
        this.loadReceivedRequests();
        break;
      case 'sent':
        this.loadSentRequests();
        break;
      case 'connections':
        this.loadAcceptedConnections();
        break;
    }
  }
  
  /**
   * Load received connection requests
   */
  loadReceivedRequests(): void {
    this.isLoading = true;
    this.error = null;
    
    this.connectionRequestService.getReceivedRequests(this.currentPage, this.pageSize).subscribe({
      next: (response: PageResponse<ConnectionRequestDTO>) => {
        this.receivedRequests = response.content;
        this.totalElements = response.totalElements;
        this.totalPages = response.totalPages;
        this.isLoading = false;
        
        console.log(`Loaded ${response.content.length} received requests`);
        console.log('Received requests data:', response.content);
        
        // Debug: Log each request's status and isPending
        response.content.forEach((request, index) => {
          console.log(`Request ${index + 1}:`, {
            id: request.id,
            status: request.status,
            isPending: request.isPending,
            isAccepted: request.isAccepted,
            isRejected: request.isRejected,
            sender: request.sender.username
          });
        });
      },
      error: (error) => {
        console.error('Error loading received requests:', error);
        this.error = 'Failed to load received requests. Please try again.';
        this.isLoading = false;
      }
    });
  }
  
  /**
   * Load sent connection requests
   */
  loadSentRequests(): void {
    this.isLoading = true;
    this.error = null;
    
    this.connectionRequestService.getSentRequests(this.currentPage, this.pageSize).subscribe({
      next: (response: PageResponse<ConnectionRequestDTO>) => {
        this.sentRequests = response.content;
        this.totalElements = response.totalElements;
        this.totalPages = response.totalPages;
        this.isLoading = false;
        
        console.log(`Loaded ${response.content.length} sent requests`);
      },
      error: (error) => {
        console.error('Error loading sent requests:', error);
        this.error = 'Failed to load sent requests. Please try again.';
        this.isLoading = false;
      }
    });
  }
  
  /**
   * Load accepted connections (network)
   */
  loadAcceptedConnections(): void {
    this.isLoading = true;
    this.error = null;
    
    this.connectionRequestService.getAcceptedConnections(this.currentPage, this.pageSize).subscribe({
      next: (response: PageResponse<ConnectionRequestDTO>) => {
        this.acceptedConnections = response.content;
        this.totalElements = response.totalElements;
        this.totalPages = response.totalPages;
        this.isLoading = false;
        
        console.log(`Loaded ${response.content.length} accepted connections`);
      },
      error: (error) => {
        console.error('Error loading accepted connections:', error);
        this.error = 'Failed to load connections. Please try again.';
        this.isLoading = false;
      }
    });
  }
  
  /**
   * Open response modal for a connection request
   */
  openResponseModal(request: ConnectionRequestDTO): void {
    this.selectedRequest = request;
    this.showResponseModal = true;
    this.responseForm.reset();
  }
  
  /**
   * Close response modal
   */
  closeResponseModal(): void {
    this.showResponseModal = false;
    this.selectedRequest = undefined;
    this.responseForm.reset();
  }
  
  /**
   * Accept a connection request
   */
  acceptRequest(request: ConnectionRequestDTO): void {
    if (!request) return;
    
    const responseMessage = this.responseForm.get('responseMessage')?.value || '';
    
    const response: ConnectionRequestResponseDTO = {
      status: 'ACCEPTED',
      responseMessage: responseMessage
    };
    
    this.connectionRequestService.respondToConnectionRequest(request.id, response).subscribe({
      next: (updatedRequest) => {
        console.log('Connection request accepted:', updatedRequest);
        
        // Update the request in the list
        const index = this.receivedRequests.findIndex(r => r.id === request.id);
        if (index !== -1) {
          this.receivedRequests[index] = updatedRequest;
        }
        
        this.closeResponseModal();
        
        // Show success message
        alert(`✅ Connection request from ${request.sender.username} accepted!\n\nYou can now message each other and start planning your roommate arrangement.`);
      },
      error: (error) => {
        console.error('Error accepting connection request:', error);
        alert(`❌ Failed to accept connection request. Please try again.`);
      }
    });
  }
  
  /**
   * Reject a connection request
   */
  rejectRequest(request: ConnectionRequestDTO): void {
    if (!request) return;
    
    const responseMessage = this.responseForm.get('responseMessage')?.value || '';
    
    const response: ConnectionRequestResponseDTO = {
      status: 'REJECTED',
      responseMessage: responseMessage
    };
    
    this.connectionRequestService.respondToConnectionRequest(request.id, response).subscribe({
      next: (updatedRequest) => {
        console.log('Connection request rejected:', updatedRequest);
        
        // Update the request in the list
        const index = this.receivedRequests.findIndex(r => r.id === request.id);
        if (index !== -1) {
          this.receivedRequests[index] = updatedRequest;
        }
        
        this.closeResponseModal();
        
        // Show confirmation message
        alert(`Connection request from ${request.sender.username} declined.`);
      },
      error: (error) => {
        console.error('Error rejecting connection request:', error);
        alert(`❌ Failed to decline connection request. Please try again.`);
      }
    });
  }
  
  /**
   * Get current data based on active tab
   */
  getCurrentData(): ConnectionRequestDTO[] {
    switch (this.activeTab) {
      case 'received':
        return this.receivedRequests;
      case 'sent':
        return this.sentRequests;
      case 'connections':
        return this.acceptedConnections;
      default:
        return [];
    }
  }
  
  /**
   * Get status badge class
   */
  getStatusBadgeClass(status: string): string {
    switch (status.toLowerCase()) {
      case 'pending':
        return 'status-pending';
      case 'accepted':
        return 'status-accepted';
      case 'rejected':
        return 'status-rejected';
      default:
        return 'status-default';
    }
  }
  
  /**
   * Get status icon
   */
  getStatusIcon(status: string): string {
    switch (status.toLowerCase()) {
      case 'pending':
        return '⏳';
      case 'accepted':
        return '✅';
      case 'rejected':
        return '❌';
      default:
        return '📝';
    }
  }
  
  /**
   * Navigate to messaging with a connected user
   */
  startConversation(request: ConnectionRequestDTO): void {
    console.log('startConversation called with request:', {
      id: request.id,
      status: request.status,
      isAccepted: request.isAccepted,
      otherUser: request.otherUser?.username
    });
    
    // Check if connection is accepted (either by status or isAccepted flag)
    const isConnectionAccepted = request.status === 'ACCEPTED' || request.isAccepted === true;
    
    if (!isConnectionAccepted) {
      console.warn('Connection not accepted:', {
        status: request.status,
        isAccepted: request.isAccepted
      });
      alert('You can only message users who have accepted your connection request.');
      return;
    }
    
    const otherUser = request.otherUser;
    if (otherUser) {
      console.log('Starting conversation with:', otherUser.username, 'userId:', otherUser.id);
      // Navigate to messaging page with the other user's ID
      this.router.navigate(['/messages'], { 
        queryParams: { userId: otherUser.id, username: otherUser.username } 
      });
    } else {
      console.error('No otherUser found in request:', request);
      alert('Unable to start conversation: User information not available.');
    }
  }
  
  /**
   * Refresh current view
   */
  refresh(): void {
    this.switchTab(this.activeTab);
  }
  
  /**
   * Handle pagination
   */
  goToPage(page: number): void {
    if (page >= 0 && page < this.totalPages) {
      this.currentPage = page;
      this.switchTab(this.activeTab);
    }
  }
  
  /**
   * Get pagination array for display
   */
  getPaginationArray(): number[] {
    const pages: number[] = [];
    const maxVisible = 5;
    const start = Math.max(0, this.currentPage - Math.floor(maxVisible / 2));
    const end = Math.min(this.totalPages, start + maxVisible);
    
    for (let i = start; i < end; i++) {
      pages.push(i);
    }
    
    return pages;
  }
  
  /**
   * TrackBy function for request list performance
   */
  trackByRequestId(index: number, request: ConnectionRequestDTO): number {
    return request.id;
  }
} 