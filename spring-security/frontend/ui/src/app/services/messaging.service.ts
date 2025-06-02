import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface MessageCreateDTO {
  conversationId: number;
  content: string;
  messageType?: 'TEXT' | 'IMAGE' | 'ANNOUNCEMENT_REFERENCE' | 'SYSTEM';
  metadata?: string;
}

export interface MessageDTO {
  id: number;
  conversationId: number;
  sender: {
    id: number;
    username: string;
    email: string;
    role: string;
    institute?: string;
    fieldOfStudy?: string;
    educationLevel?: string;
  };
  content: string;
  messageType: string;
  timestamp: string;
  isRead: boolean;
  timeAgo: string;
  metadata?: string;
}

export interface ConversationDTO {
  id: number;
  otherParticipant: {
    id: number;
    username: string;
    email: string;
    role: string;
    institute?: string;
    fieldOfStudy?: string;
    educationLevel?: string;
  };
  lastMessage?: MessageDTO;
  unreadCount: number;
  updatedAt: string;
  createdAt: string;
  isOneOnOne: boolean;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class MessagingService {
  private apiUrl = 'http://localhost:8080/api/v1/messages';

  constructor(private http: HttpClient) {}

  /**
   * Get all conversations for the current user
   */
  getUserConversations(page: number = 0, size: number = 20): Observable<PageResponse<ConversationDTO>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    
    return this.http.get<PageResponse<ConversationDTO>>(`${this.apiUrl}/conversations`, { params });
  }

  /**
   * Get or create a conversation with another user
   */
  getOrCreateConversation(otherUserId: number): Observable<ConversationDTO> {
    return this.http.post<ConversationDTO>(`${this.apiUrl}/conversations/${otherUserId}`, {});
  }

  /**
   * Send a message in a conversation
   */
  sendMessage(message: MessageCreateDTO): Observable<MessageDTO> {
    return this.http.post<MessageDTO>(`${this.apiUrl}`, message);
  }

  /**
   * Get messages for a specific conversation
   */
  getConversationMessages(conversationId: number, page: number = 0, size: number = 50): Observable<PageResponse<MessageDTO>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    
    return this.http.get<PageResponse<MessageDTO>>(`${this.apiUrl}/conversations/${conversationId}/messages`, { params });
  }

  /**
   * Mark messages as read in a conversation
   */
  markMessagesAsRead(conversationId: number): Observable<{ message: string }> {
    return this.http.put<{ message: string }>(`${this.apiUrl}/conversations/${conversationId}/read`, {});
  }

  /**
   * Get unread message count for the current user
   */
  getUnreadMessageCount(): Observable<{ unreadCount: number }> {
    return this.http.get<{ unreadCount: number }>(`${this.apiUrl}/unread-count`);
  }
} 