import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Inquiry, InquiryCreate, InquiryReply, Page } from '../models/inquiry.model';

@Injectable({
  providedIn: 'root'
})
export class InquiryService {
  private readonly apiUrl = 'http://localhost:8080/api/v1/inquiries';

  constructor(private http: HttpClient) {}

  /**
   * Create a new inquiry (Student only)
   */
  createInquiry(inquiryData: InquiryCreate): Observable<Inquiry> {
    return this.http.post<Inquiry>(this.apiUrl, inquiryData);
  }

  /**
   * Get inquiries for the authenticated student
   */
  getStudentInquiries(page?: number, size?: number): Observable<Page<Inquiry>> {
    const params = new HttpParams()
      .set('page', (page || 0).toString())
      .set('size', (size || 10).toString());
    
    return this.http.get<Page<Inquiry>>(`${this.apiUrl}/student`, { params });
  }

  /**
   * Get inquiries for the authenticated owner
   */
  getOwnerInquiries(page: number = 0, size: number = 10): Observable<Page<Inquiry>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', 'timestamp,desc');

    return this.http.get<Page<Inquiry>>(`${this.apiUrl}/owner`, { params });
  }

  /**
   * Reply to an inquiry (Owner only)
   */
  replyToInquiry(inquiryId: number, replyData: InquiryReply): Observable<Inquiry> {
    return this.http.put<Inquiry>(`${this.apiUrl}/${inquiryId}/reply`, replyData);
  }

  /**
   * Update inquiry status (Owner only)
   */
  updateInquiryStatus(inquiryId: number, status: string): Observable<Inquiry> {
    const params = new HttpParams().set('status', status);
    return this.http.put<Inquiry>(`${this.apiUrl}/${inquiryId}/status`, null, { params });
  }

  /**
   * Close deal with a specific student (NEW ENHANCED FEATURE)
   * This will mark the inquiry as CLOSED and automatically notify other students
   * that the property is no longer available (Owner only)
   */
  closeDealWithStudent(inquiryId: number): Observable<Inquiry> {
    return this.http.post<Inquiry>(`${this.apiUrl}/${inquiryId}/close-deal`, {});
  }

  /**
   * Get unread inquiry count for owner
   */
  getUnreadInquiryCount(): Observable<{ unreadCount: number }> {
    return this.http.get<{ unreadCount: number }>(`${this.apiUrl}/owner/unread-count`);
  }

  /**
   * Get closed deals for student (authenticated user)
   */
  getStudentClosedDeals(page?: number, size?: number): Observable<Page<Inquiry>> {
    const params = new HttpParams()
      .set('page', (page || 0).toString())
      .set('size', (size || 10).toString());
    
    return this.http.get<Page<Inquiry>>(`${this.apiUrl}/student/closed-deals`, { params });
  }
} 