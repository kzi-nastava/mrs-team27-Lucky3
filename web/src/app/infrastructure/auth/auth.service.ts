import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { BehaviorSubject, Observable } from 'rxjs';
import { tap } from 'rxjs/operators';
import { JwtHelperService } from '@auth0/angular-jwt';
import { environment } from '../../../env/environment';
import { AuthResponse } from './model/auth-response.model';
import { Login } from './model/login.model';

@Injectable({
  providedIn: 'root'
})
export class AuthService {

  private headers = new HttpHeaders({
    'Content-Type': 'application/json',
    skip: 'true',
  });

  user$ = new BehaviorSubject<string>("");
  userState = this.user$.asObservable();

  constructor(private http: HttpClient) {
    this.user$.next(this.getRole());
  }

  login(auth: Login): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(environment.apiHost + '/auth/login', auth, {
      headers: this.headers,
    }).pipe(
      tap((response: AuthResponse) => {
        
        localStorage.setItem('user', response.accessToken);
        this.setUser();
      })
    );
  }

  getRole(): any {
    if (this.isLoggedIn()) {
      const accessToken: any = localStorage.getItem('user');
      const helper = new JwtHelperService();
      const decodedToken = helper.decodeToken(accessToken);

      return decodedToken.role;
    }
    return null;
  }

  isLoggedIn(): boolean {
    return localStorage.getItem('user') != null;
  }

  setUser(): void {
    this.user$.next(this.getRole());
  }

  logout(): void {
    localStorage.removeItem('user');
    this.user$.next("");
  }
}