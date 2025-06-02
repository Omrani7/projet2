import { Component, OnInit, OnDestroy, ViewChild, ElementRef, AfterViewChecked } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, ActivatedRoute } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Subscription } from 'rxjs';

import { MessagingService, ConversationDTO, MessageDTO, MessageCreateDTO } from '../../services/messaging.service';
import { AuthService } from '../../auth/auth.service';
import { UserProfileService } from '../../services/user-profile.service';
import { WebSocketService } from '../../services/websocket.service';

@Component({
  selector: 'app-messages',
  standalone: true,
  imports: [CommonModule, RouterModule, ReactiveFormsModule],
  templateUrl: './messages.component.html',
  styleUrls: ['./messages.component.css']
})
export class MessagesComponent implements OnInit, OnDestroy, AfterViewChecked {
  
  @ViewChild('messagesContainer') private messagesContainer!: ElementRef;
  
  // Data properties
  conversations: ConversationDTO[] = [];
  selectedConversation?: ConversationDTO;
  messages: MessageDTO[] = [];
  currentUserId?: number;
  
  // UI state
  isLoadingConversations = false;
  isLoadingMessages = false;
  isSendingMessage = false;
  error: string | null = null;
  
  // Forms
  messageForm: FormGroup;
  
  // WebSocket subscription
  private notificationSubscription?: Subscription;
  private shouldScrollToBottom = false;
  
  // Query params for direct conversation
  private targetUserId?: number;
  private targetUsername?: string;
  
  constructor(
    private messagingService: MessagingService,
    private authService: AuthService,
    private userProfileService: UserProfileService,
    private webSocketService: WebSocketService,
    private fb: FormBuilder,
    private route: ActivatedRoute
  ) {
    this.messageForm = this.fb.group({
      content: ['', [Validators.required, Validators.maxLength(2000)]]
    });
  }
  
  ngOnInit(): void {
    this.currentUserId = this.userProfileService.getCurrentUserId() || undefined;
    this.loadConversations();
    this.setupWebSocketNotifications();
    this.handleQueryParams();
  }
  
  ngOnDestroy(): void {
    if (this.notificationSubscription) {
      this.notificationSubscription.unsubscribe();
    }
  }
  
  ngAfterViewChecked(): void {
    if (this.shouldScrollToBottom) {
      this.scrollToBottom();
      this.shouldScrollToBottom = false;
    }
  }
  
  /**
   * Handle query parameters for direct conversation
   */
  private handleQueryParams(): void {
    this.route.queryParams.subscribe(params => {
      console.log('Messages component received query params:', params);
      
      if (params['userId'] && params['username']) {
        this.targetUserId = parseInt(params['userId']);
        this.targetUsername = params['username'];
        
        console.log('Creating conversation with user:', {
          userId: this.targetUserId,
          username: this.targetUsername
        });
        
        // Create or get conversation with the target user
        this.createConversationWithUser(this.targetUserId);
      }
    });
  }
  
  /**
   * Create or get conversation with a specific user
   */
  private createConversationWithUser(userId: number): void {
    console.log('Making API call to create/get conversation with userId:', userId);
    
    this.messagingService.getOrCreateConversation(userId).subscribe({
      next: (conversation) => {
        console.log('Successfully created/retrieved conversation:', conversation);
        
        // Add to conversations list if not already there
        const existingIndex = this.conversations.findIndex(c => c.id === conversation.id);
        if (existingIndex === -1) {
          this.conversations.unshift(conversation);
        } else {
          this.conversations[existingIndex] = conversation;
        }
        
        // Select this conversation
        this.selectConversation(conversation);
      },
      error: (error) => {
        console.error('Error creating conversation:', error);
        console.error('Error details:', error.error);
        this.error = 'Failed to start conversation. Make sure you are connected with this user.';
      }
    });
  }
  
  /**
   * Load user's conversations
   */
  loadConversations(): void {
    this.isLoadingConversations = true;
    this.error = null;
    
    this.messagingService.getUserConversations().subscribe({
      next: (response) => {
        this.conversations = response.content;
        this.isLoadingConversations = false;
        
        console.log(`Loaded ${response.content.length} conversations`);
      },
      error: (error) => {
        console.error('Error loading conversations:', error);
        this.error = 'Failed to load conversations. Please try again.';
        this.isLoadingConversations = false;
      }
    });
  }
  
  /**
   * Select a conversation and load its messages
   */
  selectConversation(conversation: ConversationDTO): void {
    this.selectedConversation = conversation;
    this.loadMessages(conversation.id);
    
    // Mark messages as read
    this.messagingService.markMessagesAsRead(conversation.id).subscribe({
      next: () => {
        // Update unread count in the conversation
        conversation.unreadCount = 0;
      },
      error: (error) => {
        console.error('Error marking messages as read:', error);
      }
    });
  }
  
  /**
   * Load messages for the selected conversation
   */
  loadMessages(conversationId: number): void {
    this.isLoadingMessages = true;
    this.error = null;
    
    this.messagingService.getConversationMessages(conversationId).subscribe({
      next: (response) => {
        this.messages = response.content.reverse(); // Reverse to show oldest first
        this.isLoadingMessages = false;
        this.shouldScrollToBottom = true;
        
        console.log(`Loaded ${response.content.length} messages for conversation ${conversationId}`);
      },
      error: (error) => {
        console.error('Error loading messages:', error);
        this.error = 'Failed to load messages. Please try again.';
        this.isLoadingMessages = false;
      }
    });
  }
  
  /**
   * Send a message
   */
  sendMessage(): void {
    if (!this.selectedConversation || !this.messageForm.valid) {
      return;
    }
    
    const content = this.messageForm.get('content')?.value?.trim();
    if (!content) {
      return;
    }
    
    this.isSendingMessage = true;
    
    const messageData: MessageCreateDTO = {
      conversationId: this.selectedConversation.id,
      content: content,
      messageType: 'TEXT'
    };
    
    this.messagingService.sendMessage(messageData).subscribe({
      next: (message) => {
        // Add message to the list
        this.messages.push(message);
        this.shouldScrollToBottom = true;
        
        // Update conversation's last message
        if (this.selectedConversation) {
          this.selectedConversation.lastMessage = message;
          this.selectedConversation.updatedAt = message.timestamp;
          
          // Move conversation to top of list
          const index = this.conversations.findIndex(c => c.id === this.selectedConversation!.id);
          if (index > 0) {
            const conversation = this.conversations.splice(index, 1)[0];
            this.conversations.unshift(conversation);
          }
        }
        
        // Clear the form
        this.messageForm.reset();
        this.isSendingMessage = false;
        
        console.log('Message sent successfully:', message);
      },
      error: (error) => {
        console.error('Error sending message:', error);
        this.error = 'Failed to send message. Please try again.';
        this.isSendingMessage = false;
      }
    });
  }
  
  /**
   * Setup WebSocket notifications for real-time updates
   */
  private setupWebSocketNotifications(): void {
    this.notificationSubscription = this.webSocketService.notifications$.subscribe({
      next: (notification: any) => {
        if (notification.type === 'NEW_MESSAGE') {
          const messageData = notification.data as MessageDTO;
          
          // If the message is for the currently selected conversation, add it
          if (this.selectedConversation && messageData.conversationId === this.selectedConversation.id) {
            this.messages.push(messageData);
            this.shouldScrollToBottom = true;
            
            // Mark as read since user is viewing the conversation
            this.messagingService.markMessagesAsRead(this.selectedConversation.id).subscribe();
          } else {
            // Update unread count for the conversation
            const conversation = this.conversations.find(c => c.id === messageData.conversationId);
            if (conversation) {
              conversation.unreadCount++;
              conversation.lastMessage = messageData;
              conversation.updatedAt = messageData.timestamp;
              
              // Move conversation to top
              const index = this.conversations.findIndex(c => c.id === messageData.conversationId);
              if (index > 0) {
                const conv = this.conversations.splice(index, 1)[0];
                this.conversations.unshift(conv);
              }
            }
          }
        }
      },
      error: (error: any) => {
        console.error('WebSocket notification error:', error);
      }
    });
  }
  
  /**
   * Scroll to bottom of messages container
   */
  private scrollToBottom(): void {
    try {
      if (this.messagesContainer) {
        this.messagesContainer.nativeElement.scrollTop = this.messagesContainer.nativeElement.scrollHeight;
      }
    } catch (err) {
      console.error('Error scrolling to bottom:', err);
    }
  }
  
  /**
   * Check if message was sent by current user
   */
  isMyMessage(message: MessageDTO): boolean {
    return message.sender.id === this.currentUserId;
  }
  
  /**
   * Get display name for conversation
   */
  getConversationDisplayName(conversation: ConversationDTO): string {
    return conversation.otherParticipant.username;
  }
  
  /**
   * Get conversation preview text
   */
  getConversationPreview(conversation: ConversationDTO): string {
    if (conversation.lastMessage) {
      const content = conversation.lastMessage.content;
      return content.length > 50 ? content.substring(0, 47) + '...' : content;
    }
    return 'No messages yet';
  }
  
  /**
   * Handle Enter key press in message input
   */
  onKeyPress(event: KeyboardEvent): void {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      this.sendMessage();
    }
  }
  
  /**
   * Refresh conversations
   */
  refresh(): void {
    this.loadConversations();
    if (this.selectedConversation) {
      this.loadMessages(this.selectedConversation.id);
    }
  }
} 