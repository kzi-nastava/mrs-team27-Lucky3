import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { BehaviorSubject, Observable, tap } from 'rxjs';
import { JwtHelperService } from '@auth0/angular-jwt';
import { environment } from '../../../env/environment';
import { Login } from './model/login.model';
import { AuthResponse } from './model/auth-response.model';
import { PassengerRegistrationRequest } from './model/registration.model';

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

    return this.http.post<AuthResponse>(`${environment.apiHost}/auth/login`, auth, { headers }).pipe(
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

    return this.http.post(`${environment.apiHost}/auth/register`, formData, { headers });
  }

  activateAccount(token: string): Observable<void> {
    const headers = new HttpHeaders({ 'skip': 'true' });
    return this.http.get<void>(`${environment.apiHost}/auth/activate/${token}`, { headers });
  }

  logout(): void {
    localStorage.removeItem(this.userKey);
    this.user$.next(null);
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