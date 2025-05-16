import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable, of, throwError, BehaviorSubject } from 'rxjs';
import { catchError, tap, finalize } from 'rxjs/operators';
import { Router } from '@angular/router';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private apiUrl = 'http://localhost:8080/auth';
  private tokenKey = 'auth_token';
  private authStateSubject = new BehaviorSubject<boolean>(this.hasValidToken());
  public authState$ = this.authStateSubject.asObservable();

  constructor(private http: HttpClient, private router: Router) {
    // Check token validity on service initialization
    this.checkTokenValidity();
  }

  private hasValidToken(): boolean {
    return !!localStorage.getItem(this.tokenKey);
  }

  private checkTokenValidity(): void {
    // If you want to validate the token with the backend, you could do that here
    // For now, we'll just check if it exists
    const isLoggedIn = this.hasValidToken();
    this.authStateSubject.next(isLoggedIn);
  }

  login(email: string, password: string): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/login`, { email, password })
      .pipe(
        tap(response => {
          if (response && response.token) {
            this.saveToken(response.token);
            this.authStateSubject.next(true);
          }
        }),
        catchError(this.handleError<any>('login'))
      );
  }

  register(userData: any): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/register`, userData)
      .pipe(
        tap(response => {
          if (response && response.token) {
            this.saveToken(response.token);
            this.authStateSubject.next(true);
          }
        }),
        catchError(this.handleError<any>('register'))
      );
  }

  // Add password reset methods
  forgotPassword(email: string): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/password/forgot`, { email })
      .pipe(
        catchError(this.handleError<any>('forgotPassword'))
      );
  }

  validateResetToken(token: string): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/password/validate?token=${token}`)
      .pipe(
        catchError(this.handleError<any>('validateResetToken'))
      );
  }

  resetPassword(token: string, password: string, confirmPassword: string): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/password/reset`, {
      token,
      password,
      confirmPassword
    }).pipe(
      catchError(this.handleError<any>('resetPassword'))
    );
  }

  oauthLogin(provider: string): void {
    // Save the current URL to redirect back after auth
    localStorage.setItem('oauth_redirect', window.location.href);

    // Redirect to OAuth2 authorization endpoint with force lowercase provider name
    const baseUrl = this.apiUrl.substring(0, this.apiUrl.lastIndexOf('/'));
    window.location.href = `${baseUrl}/oauth2/authorization/${provider.toLowerCase()}`;
  }

  logout(): Observable<any> {
    // Create an observable for the logout process
    if (this.getToken()) {
      return this.http.post<any>(`${this.apiUrl}/logout`, {})
        .pipe(
          catchError(this.handleError<any>('logout')),
          finalize(() => {
            // Always remove the token from local storage
            localStorage.removeItem(this.tokenKey);
            this.authStateSubject.next(false);
            this.router.navigate(['/']);
          })
        );
    } else {
      // If no token, just navigate to home
      this.router.navigate(['/']);
      // Return an observable that completes immediately
      return of({ success: true });
    }
  }

  isLoggedIn(): boolean {
    return this.authStateSubject.value;
  }

  getToken(): string | null {
    return localStorage.getItem(this.tokenKey);
  }

  saveToken(token: string): void {
    localStorage.setItem(this.tokenKey, token);
    this.authStateSubject.next(true);
  }

  decodeToken(token: string): any {
    try {
      // Split the token to get the payload part
      const payload = token.split('.')[1];
      // Decode base64 and parse JSON
      const decoded = JSON.parse(atob(payload));
      return decoded;
    } catch (e) {
      console.error('Error decoding token:', e);
      return null;
    }
  }

  handleOAuth2Callback(): Observable<any> {
    const baseUrl = this.apiUrl.substring(0, this.apiUrl.lastIndexOf('/'));
    console.log('Calling OAuth2 success endpoint:', `${baseUrl}/oauth2/success`);

    return this.http.get<any>(`${baseUrl}/oauth2/success`)
      .pipe(
        tap(response => {
          console.log('OAuth2 success response:', response);
          if (response && response.token) {
            this.saveToken(response.token);
            this.authStateSubject.next(true);

            // Check if we should close a login modal
            const loginModalOpen = localStorage.getItem('login_modal_open');
            if (loginModalOpen === 'true') {
              localStorage.removeItem('login_modal_open');
            }

            const redirectUrl = localStorage.getItem('oauth_redirect') || '/';
            localStorage.removeItem('oauth_redirect');

            // Clean redirect URL to avoid navigation issues
            const cleanRedirectUrl = redirectUrl.split('#')[0].split('?')[0];
            this.router.navigateByUrl(cleanRedirectUrl || '/');
          }
        }),
        catchError(error => {
          console.error('OAuth2 callback error:', error);
          // Clear any saved auth state flags
          localStorage.removeItem('login_modal_open');

          // On error, redirect to login page
          this.router.navigate(['/auth/login']);
          return this.handleError<any>('oauth2_callback')(error);
        })
      );
  }

  private handleError<T>(operation = 'operation') {
    return (error: HttpErrorResponse): Observable<T> => {
      console.error(`${operation} failed: ${error.message}`);

      if (error.status === 401) {
        console.error('Authentication failed');
        // Clear token if unauthorized
        localStorage.removeItem(this.tokenKey);
        this.authStateSubject.next(false);
      }

      return throwError(() => ({
        error: true,
        message: error.error?.message || error.error?.errorDescription || `${operation} failed`
      }));
    };
  }
}
