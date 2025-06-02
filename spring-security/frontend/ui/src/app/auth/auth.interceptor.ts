import { Injectable } from '@angular/core';
import { HttpInterceptor, HttpRequest, HttpHandler, HttpEvent, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { AuthService } from './auth.service';

@Injectable()
export class AuthInterceptor implements HttpInterceptor {
  constructor(private authService: AuthService) {}

  intercept(request: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
    console.log('[AuthInterceptor] Intercepting request to:', request.url);
    const token = this.authService.getToken();

    // Debug for inquiry requests specifically
    if (request.url.includes('/api/v1/inquiries')) {
      console.log('[AuthInterceptor] INQUIRY REQUEST - Token present:', !!token);
      console.log('[AuthInterceptor] INQUIRY REQUEST - Token value:', token);
    }

    if (request.url.includes('/api/v1/properties/owner')) {
      console.log('[AuthInterceptor] Request to /api/v1/properties/owner. Token:', token);
    }

    if (token) {
      request = request.clone({
        setHeaders: {
          Authorization: `Bearer ${token}`
        }
      });
      
      if (request.url.includes('/api/v1/inquiries')) {
        console.log('[AuthInterceptor] INQUIRY REQUEST - Added Authorization header');
      }
      
      if (request.url.includes('/api/v1/properties/owner')) {
        console.log('[AuthInterceptor] Token IS present. Cloned request with Auth header for /api/v1/properties/owner.');
      }
    } else {
      if (request.url.includes('/api/v1/inquiries')) {
        console.log('[AuthInterceptor] INQUIRY REQUEST - NO TOKEN AVAILABLE');
      }
      
      if (request.url.includes('/api/v1/properties/owner')) {
        console.log('[AuthInterceptor] Token IS NOT present for /api/v1/properties/owner.');
      }
    }

    return next.handle(request).pipe(
      catchError((error: HttpErrorResponse) => {
        // Detailed error message extraction from response body
        let detailedErrorMessage = '';
        if (error.error) {
          if (typeof error.error === 'string') {
            detailedErrorMessage = error.error;
          } else if (error.error && typeof error.error.message === 'string') {
            detailedErrorMessage = error.error.message;
          } else if (error.error && typeof error.error.error === 'string') { // Check for nested error messages
            detailedErrorMessage = error.error.error;
          } else if (error.error && typeof error.error.detail === 'string') { // Another common field for error details
            detailedErrorMessage = error.error.detail;
          }
        }
        // Fallback to the general HttpErrorResponse.message if no specific message found in the body
        if (!detailedErrorMessage) {
          detailedErrorMessage = error.message; // General HTTP error like "500 Internal Server Error"
        }

        const isExplicitAuthStatus = error.status === 401 || error.status === 403;
        const isLikelyJwtOrTokenIssueInBody = detailedErrorMessage && (
                                detailedErrorMessage.toLowerCase().includes('jwt') ||
                                detailedErrorMessage.toLowerCase().includes('token') || // Catches "token expired", "invalid token"
                                detailedErrorMessage.toLowerCase().includes('signature') || // Catches "JWT signature does not match"
                                detailedErrorMessage.toLowerCase().includes('unauthorized') ||
                                detailedErrorMessage.toLowerCase().includes('authentication')
                              );

        // Determine if this error should be handled as an authentication problem
        // This includes 401/403, or other statuses (like 500) if the error body suggests a token/auth issue.
        // error.status !== 0 check is to differentiate from network errors handled below.
        if (isExplicitAuthStatus || (error.status !== 0 && !isExplicitAuthStatus && isLikelyJwtOrTokenIssueInBody)) {
          console.warn(
            'AuthInterceptor: Authentication-related error caught. Status:', error.status,
            'Original error message from HttpErrorResponse:', error.message,
            'Detailed message from response body:', detailedErrorMessage
          );
          
          let userFriendlyMessage = ''; // Initialize to empty

          if (error.status === 403) { // HIGHEST PRIORITY
            userFriendlyMessage = 'You do not have permission to access this resource. Please check your credentials or contact support.';
            console.log('[AuthInterceptor] Status 403 detected. User-friendly message set to:', userFriendlyMessage);
          } else if (error.status === 401) { // Next, handle 401
            userFriendlyMessage = 'Your session is invalid or has expired. Please log in again.'; // Standard 401 message
            console.log('[AuthInterceptor] Status 401 detected. User-friendly message set to:', userFriendlyMessage);
          } else if (isLikelyJwtOrTokenIssueInBody) { // Then, other JWT-like issues
            userFriendlyMessage = 'A security token issue occurred with your session. Please try logging in again.';
            console.log('[AuthInterceptor] JWT/Token issue in body detected (non-401/403). User-friendly message set to:', userFriendlyMessage);
          } else { // Fallback if none of the above (should be rare if caught by outer condition)
            userFriendlyMessage = 'An unexpected authentication issue occurred. Please log in again.';
            console.log('[AuthInterceptor] Defaulting to unexpected auth issue message for status:', error.status);
          }
          
          console.log('[AuthInterceptor] About to call authService.handleAuthError with message:', userFriendlyMessage);
          this.authService.handleAuthError(userFriendlyMessage);
          
          return throwError(() => new Error('Authentication error handled by interceptor. User is being redirected.'));
        } else if (error.status === 0) { // Handle network errors / server unreachable
            console.error('AuthInterceptor: Network error or server unreachable. Status:', error.status, 'Message:', error.message, error);
            this.authService.handleAuthError('Could not connect to the server. Please check your network connection and try again.');
            return throwError(() => new Error('Network error or server unreachable. Handled by interceptor.'));
        }

        // For other types of errors not handled as auth problems, re-throw the original error object.
        // This allows component-specific error handling if needed.
        console.log('AuthInterceptor: Error not handled as auth/network, passing through.', error);
        return throwError(() => error);
      })
    );
  }
} 