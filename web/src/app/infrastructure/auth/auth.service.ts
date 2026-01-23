import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse, HttpHeaders, HttpParams } from '@angular/common/http';
import { BehaviorSubject, Observable, catchError, map, of, tap, throwError } from 'rxjs';
import { JwtHelperService } from '@auth0/angular-jwt';
import { environment } from '../../../env/environment';
import { Login } from '../../model/login.model';
import { AuthResponse } from '../../model/auth-response.model';
import { PassengerRegistrationRequest } from '../../model/registration.model';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly userKey = 'user'; // Key for LocalStorage
  private jwtHelper = new JwtHelperService();

  // Holds the current role (or null). Components subscribe to this
  user$ = new BehaviorSubject<string | null>(this.getRole());

  constructor(private http: HttpClient) {}

  login(auth: Login): Observable<AuthResponse> {
    // "skip" header tells interceptor NOT to add the token for this request
    const headers = new HttpHeaders({ 'skip': 'true' });

    return this.http.post<AuthResponse>(`${environment.apiHost}auth/login`, auth, { headers }).pipe(
      tap((response: AuthResponse) => {
        // Save token to Local Storage (persistent across tabs)
        localStorage.setItem(this.userKey, response.accessToken);
        // Update the state so the rest of the app knows we are logged in
        this.user$.next(this.getRole());
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

    return this.http.post<void>(`${environment.apiHost}auth/reset-password`, body, { headers });
  }

  logout(): void {
    localStorage.removeItem(this.userKey);
    this.user$.next(null);
  }

  getUserId(): number | null {
    const token = localStorage.getItem(this.userKey);
    if (!token) return null;

    if (this.jwtHelper.isTokenExpired(token)) {
      this.logout();
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
      this.logout();
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
