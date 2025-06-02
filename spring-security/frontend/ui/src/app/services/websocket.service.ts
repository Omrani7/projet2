import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable, Subject } from 'rxjs';
import { AuthService } from '../auth/auth.service';
import { UserProfileService } from './user-profile.service';
import { WebSocketNotification } from '../models/inquiry.model';

@Injectable({
  providedIn: 'root'
})
export class WebSocketService {
  private ws: WebSocket | null = null;
  private connectionStatus = new BehaviorSubject<boolean>(false);
  private notificationSubject = new Subject<WebSocketNotification>();
  private reconnectAttempts = 0;
  private maxReconnectAttempts = 5;
  private reconnectInterval = 5000; // 5 seconds

  public connectionStatus$ = this.connectionStatus.asObservable();
  public notifications$ = this.notificationSubject.asObservable();

  constructor(
    private authService: AuthService,
    private userProfileService: UserProfileService
  ) {}

  /**
   * Connect to WebSocket server using native WebSocket
   */
  connect(): void {
    if (this.ws?.readyState === WebSocket.OPEN) {
      console.log('WebSocket already connected');
      return;
    }

    if (!this.authService.isLoggedIn()) {
      console.log('User not authenticated, skipping WebSocket connection');
      return;
    }

    try {
      // Get current user ID for messaging WebSocket
      const currentUserId = this.userProfileService.getCurrentUserId();
      if (!currentUserId) {
        console.error('Cannot connect to WebSocket: No user ID available');
        return;
      }
      
      // Connect to messaging WebSocket endpoint with user ID
      const wsUrl = `ws://localhost:8080/ws/messaging?userId=${currentUserId}`;
      console.log('Connecting to WebSocket:', wsUrl);
      
      this.ws = new WebSocket(wsUrl);

      this.ws.onopen = (event) => {
        console.log('WebSocket Connected:', event);
        this.connectionStatus.next(true);
        this.reconnectAttempts = 0;
        
        // Send initial subscription message
        this.sendSubscription();
      };

      this.ws.onmessage = (event) => {
        try {
          const notification: any = JSON.parse(event.data);
          console.log('Received WebSocket notification:', notification);
          
          // Handle different notification types
          if (notification.type === 'NEW_MESSAGE') {
            console.log('New message notification received:', notification);
          } else if (this.isRoommateNotification(notification.type)) {
            console.log(`Roommate notification received: ${notification.type}`, notification);
            if (notification.compatibilityScore) {
              console.log(`Compatibility score: ${(notification.compatibilityScore * 100).toFixed(1)}%`);
            }
          }
          
          this.notificationSubject.next(notification);
        } catch (error) {
          console.error('Error parsing WebSocket message:', error);
        }
      };

      this.ws.onclose = (event) => {
        console.log('WebSocket Disconnected:', event);
        this.connectionStatus.next(false);
        this.ws = null;
        
        // Attempt to reconnect if not manually closed
        if (event.code !== 1000 && this.reconnectAttempts < this.maxReconnectAttempts) {
          this.scheduleReconnect();
        }
      };

      this.ws.onerror = (error) => {
        console.error('WebSocket Error:', error);
        this.connectionStatus.next(false);
      };

    } catch (error) {
      console.error('Error creating WebSocket connection:', error);
      this.connectionStatus.next(false);
    }
  }

  /**
   * Disconnect from WebSocket server
   */
  disconnect(): void {
    if (this.ws) {
      this.ws.close(1000, 'Client disconnect');
      this.ws = null;
      this.connectionStatus.next(false);
    }
  }

  /**
   * Send subscription message to server
   */
  private sendSubscription(): void {
    if (this.ws?.readyState === WebSocket.OPEN) {
      const subscriptionMessage = {
        type: 'SUBSCRIBE',
        channels: ['messaging', 'notifications']
      };
      this.ws.send(JSON.stringify(subscriptionMessage));
      console.log('Sent messaging subscription message');
    }
  }

  /**
   * Schedule reconnection attempt
   */
  private scheduleReconnect(): void {
    this.reconnectAttempts++;
    console.log(`Scheduling reconnect attempt ${this.reconnectAttempts}/${this.maxReconnectAttempts} in ${this.reconnectInterval}ms`);
    
    setTimeout(() => {
      if (this.authService.isLoggedIn()) {
        console.log(`Reconnect attempt ${this.reconnectAttempts}`);
        this.connect();
      }
    }, this.reconnectInterval);
  }

  /**
   * Check if WebSocket is connected
   */
  isConnected(): boolean {
    return this.ws?.readyState === WebSocket.OPEN;
  }

  /**
   * Send a message (if needed for future features)
   */
  sendMessage(message: any): void {
    if (this.ws?.readyState === WebSocket.OPEN) {
      this.ws.send(JSON.stringify(message));
    } else {
      console.warn('Cannot send message: WebSocket not connected');
    }
  }

  /**
   * Check if notification type is roommate-related
   */
  private isRoommateNotification(type: string): boolean {
    return ['NEW_ROOMMATE_APPLICATION', 'ROOMMATE_APPLICATION_RESPONSE', 'ROOMMATE_MATCH_FOUND'].includes(type);
  }
} 