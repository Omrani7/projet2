import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable, of, throwError, BehaviorSubject } from 'rxjs';
import { catchError, tap, finalize } from 'rxjs/operators';
import { Router, NavigationEnd } from '@angular/router';
import { Location } from '@angular/common';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private apiUrl = 'http://localhost:8080/auth';
  private frontendUrl = 'http://localhost:4200';
  private backendUrl = 'http://localhost:8080';
  private tokenKey = 'auth_token';
  private authStateSubject = new BehaviorSubject<boolean>(this.hasValidToken());
  public authState$ = this.authStateSubject.asObservable();
  private oauthPopup: Window | null = null;
  private messageListener: ((event: MessageEvent) => void) | null = null;

  constructor(private http: HttpClient, private router: Router, private location: Location) {
    // Check token validity on service initialization
    this.checkTokenValidity();
    
    // Set up a navigation listener to handle browser back button
    this.setupNavigationHandler();
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

  private setupNavigationHandler(): void {
    // Listen to all router navigation events
    this.router.events.subscribe(event => {
      // Only handle NavigationEnd events
      if (event instanceof NavigationEnd) {
        // Check if we're navigating to /auth/login
        if (event.url === '/auth/login' && !event.url.includes('?')) {
          // Check if this is a browser back/forward navigation
          // We add a flag to localStorage when a user explicitly clicks the login button
          const isUserInitiatedLogin = localStorage.getItem('user_initiated_login') === 'true';
          
          if (!isUserInitiatedLogin) {
            console.log('Preventing direct navigation to /auth/login');
            this.router.navigate(['/']);
          } else {
            // If it was user initiated, clear the flag after it's used
            localStorage.removeItem('user_initiated_login');
          }
        }
      }
    });
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
    // Clear any previous listener or popup state
    this.cleanupOAuthPopupListener();

    // The backend's OAuth2 authorization endpoint is typically at the root, e.g., /oauth2/authorization/google
    // It's NOT prefixed by the /auth API path.
    const backendBaseUrl = this.apiUrl.substring(0, this.apiUrl.lastIndexOf('/auth')); // Get http://localhost:8080
    const googleAuthUrl = `${backendBaseUrl}/oauth2/authorization/${provider.toLowerCase()}`;
    console.log('AuthService: Initiating OAuth login. Popup URL:', googleAuthUrl);

    // Smaller popup dimensions
    const popupWidth = 450;  // Reduced from 600
    const popupHeight = 600; // Reduced from 700
    const left = (window.screen.width / 2) - (popupWidth / 2);
    const top = (window.screen.height / 2) - (popupHeight / 2);

    this.oauthPopup = window.open(
      googleAuthUrl,
      'googleOAuthLogin',
      `width=${popupWidth},height=${popupHeight},top=${top},left=${left},resizable=yes,scrollbars=yes`
    );

    if (this.oauthPopup) {
      // Setup the message event listener
      this.messageListener = (event: MessageEvent) => {
        // Allow messages from either the frontend or backend origins
        // This is necessary because the popup is served from the backend domain
        if (event.origin !== this.frontendUrl && event.origin !== this.backendUrl) {
          console.warn(`Message received from untrusted origin: ${event.origin}. Expected ${this.frontendUrl} or ${this.backendUrl}`);
          return;
        }
        
        console.log('Main window: Received message from popup', event.origin, event.data);

        if (event.data && event.data.type === 'oauth_success' && event.data.token) {
          console.log('Main window: OAuth success message received from popup');
          console.log('Main window: Token received from popup:', event.data.token);
          
          this.saveToken(event.data.token);
          this.authStateSubject.next(true);
          
          // Verify token was saved properly - MORE DETAILED DEBUGGING
          const savedToken = localStorage.getItem(this.tokenKey);
          console.log('Main window: Token saved verification:', savedToken ? 'SUCCESS' : 'FAILED');
          console.log('Main window: Saved token value:', savedToken);
          console.log('Main window: Auth state after token save:', this.authStateSubject.value);
          console.log('Main window: isLoggedIn() result:', this.isLoggedIn());
          
          // Test getToken() method directly
          const tokenFromMethod = this.getToken();
          console.log('Main window: getToken() returns:', tokenFromMethod);
          
          this.cleanupOAuthPopupListener();
          this.navigateToOriginalPage();
        } else if (event.data && event.data.type === 'oauth_error') {
          console.error('Main window: OAuth error message received from popup:', event.data.message || 'Unknown error');
          // You might want to display this error to the user in the main window
          alert(`Login failed: ${event.data.message || 'Unknown error'}`);
          this.cleanupOAuthPopupListener();
        }
      };
      
      window.addEventListener('message', this.messageListener, false);
      console.log('Main window: Added message listener for postMessage from popup');

      // Periodically check if the popup was closed by the user
      const checkPopupClosedInterval = setInterval(() => {
        try {
          // This might throw a cross-origin error if the popup navigated to Google's domain
          const popupClosed = !this.oauthPopup || this.oauthPopup.closed;
          
          if (popupClosed) {
            clearInterval(checkPopupClosedInterval);
            // If the listener is still active, it means the flow didn't complete via postMessage
            if (this.messageListener) {
              console.log('OAuth popup was closed by user before completion.');
              this.cleanupOAuthPopupListener();
              // Clear any loading indicators in UI
            }
          }
        } catch (e) {
          // If we can't access the popup's closed property due to COOP restrictions
          console.log('Cannot check if popup is closed due to COOP policy. Assuming still open.');
        }
      }, 1000);

    } else {
      console.error('OAuth popup was blocked. Please enable popups for this site.');
      alert('Login popup was blocked. Please enable popups for this site and try again.');
    }
  }

  private navigateToOriginalPage(): void {
    const originalPage = localStorage.getItem('original_page') || '/';
    console.log('AuthService: Navigating to original page or fallback:', originalPage);
    localStorage.removeItem('original_page');
    localStorage.removeItem('login_modal_open'); // If you use this for modal state
    // The user_initiated_login flag is for the /auth/login route guard, can be left or removed based on final flow
    // localStorage.removeItem('user_initiated_login'); 

    // Add a small delay to ensure token is fully saved before navigation
    setTimeout(() => {
      this.router.navigateByUrl(originalPage, { replaceUrl: true });
    }, 100);
  }

  private cleanupOAuthPopupListener(): void {
    if (this.messageListener) {
      console.log('Main window: Removing message listener');
      window.removeEventListener('message', this.messageListener);
      this.messageListener = null;
    }
    
    try {
      if (this.oauthPopup && !this.oauthPopup.closed) {
        console.log('Main window: Closing popup');
        this.oauthPopup.close();
      }
    } catch (e) {
      console.log('Cannot close popup due to COOP policy');
    }
    
    this.oauthPopup = null;
  }

  logout(): Observable<any> {
    this.cleanupOAuthPopupListener(); // Clean up any lingering popup/listener
    // Create an observable for the logout process
    if (this.getToken()) {
      return this.http.post<any>(`${this.apiUrl}/logout`, {})
        .pipe(
          catchError(this.handleError<any>('logout')),
          finalize(() => {
            // Always remove the token from local storage
            localStorage.removeItem(this.tokenKey);
            this.authStateSubject.next(false);
          })
        );
    } else {
      this.authStateSubject.next(false);
      // Return an observable that completes immediately
      return of({ success: true });
    }
  }

  isLoggedIn(): boolean {
    return this.authStateSubject.value;
  }

  getToken(): string | null {
    const token = localStorage.getItem(this.tokenKey);
    // console.log('[AuthService] getToken() called. Token from localStorage:', token);
    return token;
  }

  saveToken(token: string): void {
    localStorage.setItem(this.tokenKey, token);
    this.authStateSubject.next(true);
  }

  // Updated to accept an optional custom string message
  handleAuthError(customMessage?: string): void {
    const message = customMessage || 'Authentication error. Your session may be invalid or expired. Please log in again.';
    console.error('AuthService: Handling auth error - Input customMessage:', customMessage, '- Chosen message for alert:', message);
    
    this.clearToken();
    
    // Using alert for now, can be replaced with a more sophisticated notification service
    alert(message); 
    
    this.router.navigate(['/auth/login'], { queryParams: { session_expired: true } })
      .catch(err => console.error('AuthService: Navigation error during handleAuthError:', err));
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

  // Add a method to clear token
  clearToken(): void {
    localStorage.removeItem(this.tokenKey);
    this.authStateSubject.next(false); // Update auth state
    console.log('Token cleared and authStateSubject updated.');
  }

  getDecodedToken(): any {
    const token = this.getToken();
    if (token) {
      return this.decodeToken(token);
    }
    return null;
  }

  getUserRole(): string | null {
    const decodedToken = this.getDecodedToken();
    return decodedToken ? decodedToken.role : null;
  }
}
