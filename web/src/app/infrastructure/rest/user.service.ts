import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AuthService } from '../auth/auth.service'; // Adjust path as needed
import { environment } from '../../../env/environment';

export interface UserProfile {
  name: string;
  surname: string;
  email: string;
  phoneNumber: string;
  address: string;
  imageUrl?: string;
}

@Injectable({
  providedIn: 'root'
})
export class UserService {
  private apiUrl = `${environment.apiHost}users`;

  constructor(
    private http: HttpClient,
    private authService: AuthService
  ) {}

  getCurrentUser(): Observable<UserProfile> {
    const userId = this.authService.getUserId();
    return this.http.get<UserProfile>(`${this.apiUrl}/${userId}`);
  }

}
