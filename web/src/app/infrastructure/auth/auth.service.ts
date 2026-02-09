import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse, HttpHeaders, HttpParams } from '@angular/common/http';
import { BehaviorSubject, Observable, catchError, map, of, tap, throwError } from 'rxjs';
import { JwtHelperService } from '@auth0/angular-jwt';
import { environment } from '../../../env/environment';
import { Login } from '../../model/login.model';
import { AuthResponse } from '../../model/auth-response.model';
import { PassengerRegistrationRequest } from '../../model/registration.model';
import { Router } from '@angular/router';

export interface LogoutResult {
  success: boolean;
  error?: string;
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly userKey = 'user'; // Key for LocalStorage
  private jwtHelper = new JwtHelperService();

  // Holds the current role (or null). Components subscribe to this
  user$ = new BehaviorSubject<string | null>(this.getRole());

  constructor(
    private http: HttpClient,
    private router: Router
  ) {}

  login(auth: Login): Observable<AuthResponse> {
    // "skip" header tells interceptor NOT to add the token for this request
    const headers = new HttpHeaders({ 'skip': 'true' });

    return this.http.post<AuthResponse>(`${environment.apiHost}auth/login`, auth, { headers }).pipe(
      tap((response: AuthResponse) => {
        // Save token to Local Storage (persistent across tabs)
        localStorage.setItem(this.userKey, response.accessToken);
        // Update the state so the rest of the app knows we are logged in
        this.user$.next(this.getRole());
      }),
      catchError((err: unknown) => {
        if (err instanceof HttpErrorResponse) {
          if (err.status === 401 || err.status === 403) {
            return throwError(() => new Error('Invalid email or password.'));
          }

          
          if (err.status === 0) {
            return throwError(() => new Error('Unable to reach the server. Please try again.'));
          }

          return throwError(() => new Error('Login failed. Please try again.'));
        }

        return throwError(() => err);
      })
    );
  }

  register(data: PassengerRegistrationRequest, profileImage?: File): Observable<any> {
    const headers = new HttpHeaders({ 'skip': 'true' });

    // We must use FormData to send both JSON and the File
    const formData = new FormData();

    // Spring Boot @RequestPart("data") expects a JSON Blob
    formData.append('data', new Blob([JSON.stringify(data)], {
        type: 'application/json'
    }));

    // Spring Boot @RequestPart("profileImage")
    if (profileImage) {
        formData.append('profileImage', profileImage);
    }

    return this.http.post(`${environment.apiHost}auth/register`, formData, { headers }).pipe(
      catchError((err: unknown) => {
        if (err instanceof HttpErrorResponse) {
          const message = this.extractHttpErrorMessage(err);
          const isDuplicateEmail =
            (err.status === 400 || err.status === 409) &&
            /email|e-mail/i.test(message) &&
            /exist|already|duplicate|taken/i.test(message);

          if (isDuplicateEmail) {
            return throwError(() => new Error('User with this email already exists.'));
          }
        }

        return throwError(() => err);
      })
    );
  }

  logout(): Observable<LogoutResult> {
    return this.http.post<void>(`${environment.apiHost}auth/logout`, {}).pipe(
      map(() => {
        this.doLocalLogout();
        return { success: true };
      }),
      catchError((err: unknown) => {
        if (err instanceof HttpErrorResponse) {
          // 409 Conflict means driver has an active ride or is_inactive_requested is set
          if (err.status === 409) {
            const message = this.extractHttpErrorMessage(err);
            return of({ 
              success: false, 
              error: message || 'You cannot log out because you are currently on a ride.' 
            });
          }
          
          // For other errors, still log out locally
          this.doLocalLogout();
          return of({ success: true });
        }
        
        // For unknown errors, still log out locally
        this.doLocalLogout();
        return of({ success: true });
      })
    );
  }

  // Legacy logout that always completes (for backward compatibility)
  logoutForced(): void {
    this.http.post(`${environment.apiHost}auth/logout`, {}).subscribe({
      next: () => this.doLocalLogout(),
      error: () => this.doLocalLogout()
    });
  }

  private doLocalLogout(): void {
    localStorage.removeItem(this.userKey);
    this.user$.next(null);
    this.router.navigate(['/login']);
  }

  private extractHttpErrorMessage(err: HttpErrorResponse): string {
    const payload = err.error;

    if (!payload) return err.message || '';
    if (typeof payload === 'string') return payload;

    // Common Spring Boot error shapes: { message }, { error }, { errors: [...] }
    if (typeof payload === 'object') {
      const anyPayload = payload as any;
      if (typeof anyPayload.message === 'string') return anyPayload.message;
      if (typeof anyPayload.error === 'string') return anyPayload.error;

      // Fallback: stringify safely
      try {
        return JSON.stringify(payload);
      } catch {
        return err.message || '';
      }
    }

    return err.message || '';
  }

  activateAccount(token: string): Observable<void> {
    const headers = new HttpHeaders({ 'skip': 'true' });
    const params = new HttpParams().set('token', token);
    return this.http.get<void>(`${environment.apiHost}auth/activate`, { headers, params });
  }

  resendActivation(email: string): Observable<void> {
    const headers = new HttpHeaders({ 'skip': 'true' });
    return this.http.post<void>(`${environment.apiHost}auth/resend-activation`, { email }, { headers });
  }

  requestPasswordReset(email: string): Observable<void> {
    const headers = new HttpHeaders({ 'skip': 'true' });
    return this.http.post<void>(`${environment.apiHost}auth/forgot-password`, { email }, { headers }).pipe(
      catchError((err: unknown) => {
        if (err instanceof HttpErrorResponse) {
          if (err.status === 409 || err.status === 404) {
            return of(void 0);
          }

          const message = this.extractHttpErrorMessage(err);

          if (err.status === 400 && /email/i.test(message) && /invalid|format/i.test(message)) {
            return throwError(() => new Error('Please enter a valid email address.'));
          }

          return throwError(() => new Error('Failed to send reset link. Please try again.'));
        }

        return throwError(() => err);
      })
    );
  }


  requestPasswordResetWithStatus(email: string): Observable<'sent' | 'alreadySent'> {
    const headers = new HttpHeaders({ 'skip': 'true' });

    return this.http.post<void>(`${environment.apiHost}auth/forgot-password`, { email }, { headers }).pipe(
      map(() => 'sent' as const),
      catchError((err: unknown) => {
        if (err instanceof HttpErrorResponse) {
          if (err.status === 409) {
            return of('alreadySent' as const);
          }

          if (err.status === 404) {
            return of('sent' as const);
          }

          const message = this.extractHttpErrorMessage(err);
          if (err.status === 400 && /email/i.test(message) && /invalid|format/i.test(message)) {
            return throwError(() => new Error('Please enter a valid email address.'));
          }

          return throwError(() => new Error('Failed to resend reset email. Please try again.'));
        }

        return throwError(() => err);
      })
    );
  }

  resetPassword(token: string, newPassword: string, confirmPassword?: string): Observable<void> {
    const headers = new HttpHeaders({ 'skip': 'true' });
    
    const body = { 
        token: token, 
        newPassword: newPassword 
    };

    return this.http.post<void>(`${environment.apiHost}auth/reset-password`, body, { headers }).pipe(
      catchError((err: unknown) => {
        if (err instanceof HttpErrorResponse) {
          const message = this.extractHttpErrorMessage(err);
          const messageLower = (message || '').toLowerCase();

          const isTokenExpired =
            err.status === 410 ||
            /expired/.test(messageLower);

          const isTokenInvalid =
            err.status === 400 ||
            err.status === 404 ||
            /invalid/.test(messageLower) ||
            /token/.test(messageLower);

          if (isTokenExpired) {
            return throwError(() => new Error('This reset link has expired. Please request a new one.'));
          }

          if (isTokenInvalid) {
            return throwError(() => new Error('This reset link is invalid. Please request a new one.'));
          }

          if (err.status === 0) {
            return throwError(() => new Error('Unable to reach the server. Please try again.'));
          }

          if (err.status === 400 && message) {
            return throwError(() => new Error(message));
          }

          return throwError(() => new Error('Failed to reset password. Please try again.'));
        }

        return throwError(() => err);
      })
    );
  }

  validatePasswordResetToken(token: string): Observable<{ valid: boolean; reason?: 'invalid' | 'expired' }> {
    const headers = new HttpHeaders({ 'skip': 'true' });
    const params = new HttpParams().set('token', token);

    return this.http.get<void>(`${environment.apiHost}auth/reset-password/validate`, { headers, params }).pipe(
      map(() => ({ valid: true as const })),
      catchError((err: unknown) => {
        if (err instanceof HttpErrorResponse) {
          const message = this.extractHttpErrorMessage(err);
          const messageLower = (message || '').toLowerCase();

          if (err.status === 410 || /expired/.test(messageLower)) {
            return of({ valid: false as const, reason: 'expired' as const });
          }

          // Backend contract: 204 = valid, 404 = invalid
          if (err.status === 404) {
            return of({ valid: false as const, reason: 'invalid' as const });
          }

          // Fail closed on validation errors (network/server/etc) to avoid
          // allowing invalid tokens onto the reset password page.
          return of({ valid: false as const, reason: 'invalid' as const });
        }

        return of({ valid: false as const, reason: 'invalid' as const });
      })
    );
  }

  getUserId(): number | null {
    const token = localStorage.getItem(this.userKey);
    if (!token) return null;

    if (this.jwtHelper.isTokenExpired(token)) {
      this.doLocalLogout();
      return null;
    }

    const decodedToken: any = this.jwtHelper.decodeToken(token);
    const candidate =
      decodedToken?.userId ??
      decodedToken?.id ??
      decodedToken?.driverId ??
      decodedToken?.sub;

    if (candidate == null) return null;

    const asNumber = typeof candidate === 'number' ? candidate : Number(candidate);
    return Number.isFinite(asNumber) ? asNumber : null;
  }

  getRole(): string | null {
    const token = localStorage.getItem(this.userKey);
    if (!token) return null;

    if (this.jwtHelper.isTokenExpired(token)) {
      this.doLocalLogout();
      return null;
    }

    const decodedToken = this.jwtHelper.decodeToken(token);
    return decodedToken.role || decodedToken.roles?.[0] || null;
  }

  isLoggedIn(): boolean {
    const token = localStorage.getItem(this.userKey);
    return token != null && !this.jwtHelper.isTokenExpired(token);
  }
}
