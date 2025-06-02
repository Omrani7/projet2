import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
export interface ConnectionRequestCreateDTO {
  receiverId: number;
  message?: string;
}

export interface ConnectionRequestResponseDTO {
  status: 'ACCEPTED' | 'REJECTED';
  responseMessage?: string;
}

export interface UserBasicDTO {
  id: number;
  username: string;
  email: string;
  role: string;
  institute?: string;
  fieldOfStudy?: string;
  educationLevel?: string;
}

export interface ConnectionRequestDTO {
  id: number;
  sender: UserBasicDTO;
  receiver: UserBasicDTO;
  message?: string;
  status: string;
  createdAt: string;
  respondedAt?: string;
  responseMessage?: string;
  canBeWithdrawn: boolean;
  isSender: boolean;
  isReceiver: boolean;
  otherUser?: UserBasicDTO;
  statusDisplayText: string;
  timeAgo: string;
  isPending: boolean;
  isAccepted: boolean;
  isRejected: boolean;
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
export class ConnectionRequestService {
  private apiUrl = 'http://localhost:8080/api/v1/connections';

  constructor(private http: HttpClient) {}

  /**
   * Send a connection request to another student
   */
  sendConnectionRequest(request: ConnectionRequestCreateDTO): Observable<ConnectionRequestDTO> {
    return this.http.post<ConnectionRequestDTO>(`${this.apiUrl}/request`, request);
  }

  /**
   * Get connection requests sent by the current user
   */
  getSentRequests(page: number = 0, size: number = 10): Observable<PageResponse<ConnectionRequestDTO>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    
    return this.http.get<PageResponse<ConnectionRequestDTO>>(`${this.apiUrl}/sent`, { params });
  }

  /**
   * Get connection requests received by the current user
   */
  getReceivedRequests(page: number = 0, size: number = 10): Observable<PageResponse<ConnectionRequestDTO>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    
    return this.http.get<PageResponse<ConnectionRequestDTO>>(`${this.apiUrl}/received`, { params });
  }

  /**
   * Get pending connection requests received by the current user
   */
  getPendingReceivedRequests(page: number = 0, size: number = 10): Observable<PageResponse<ConnectionRequestDTO>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    
    return this.http.get<PageResponse<ConnectionRequestDTO>>(`${this.apiUrl}/pending`, { params });
  }

  /**
   * Respond to a connection request (accept or reject)
   */
  respondToConnectionRequest(requestId: number, response: ConnectionRequestResponseDTO): Observable<ConnectionRequestDTO> {
    return this.http.put<ConnectionRequestDTO>(`${this.apiUrl}/${requestId}/respond`, response);
  }

  /**
   * Get a specific connection request by ID
   */
  getConnectionRequestById(requestId: number): Observable<ConnectionRequestDTO> {
    return this.http.get<ConnectionRequestDTO>(`${this.apiUrl}/${requestId}`);
  }

  /**
   * Get count of pending connection requests received by user
   */
  getPendingRequestsCount(): Observable<{ pendingCount: number }> {
    return this.http.get<{ pendingCount: number }>(`${this.apiUrl}/pending/count`);
  }

  /**
   * Check if a connection exists between current user and another user
   */
  checkConnectionExists(userId: number): Observable<{ connectionExists: boolean }> {
    return this.http.get<{ connectionExists: boolean }>(`${this.apiUrl}/exists/${userId}`);
  }

  /**
   * Get accepted connections for a user (their network)
   */
  getAcceptedConnections(page: number = 0, size: number = 10): Observable<PageResponse<ConnectionRequestDTO>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    
    return this.http.get<PageResponse<ConnectionRequestDTO>>(`${this.apiUrl}/network`, { params });
  }
} 