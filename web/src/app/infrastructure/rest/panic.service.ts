import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../env/environment';

export interface PanicUserResponse {
  id: number;
  name: string;
  surname: string;
  email: string;
  profilePictureUrl?: string;
  role: string;
  phoneNumber?: string;
  address?: string;
}

export interface PanicRideResponse {
  id: number;
  startLocation?: { address?: string; latitude?: number; longitude?: number };
  endLocation?: { address?: string; latitude?: number; longitude?: number };
  status: string;
  driver?: PanicUserResponse;
  passengers?: PanicUserResponse[];
}

export interface PanicResponse {
  id: number;
  user: PanicUserResponse;
  ride: PanicRideResponse;
  time: string;
  reason?: string;
}

export interface PageResponse<T> {
  content: T[];
  totalElements?: number;
  totalPages?: number;
  number?: number;
  size?: number;
}

@Injectable({
  providedIn: 'root'
})
export class PanicService {
  private apiUrl = `${environment.apiHost}panic`;

  constructor(private http: HttpClient) {}

  /**
   * Get all panic notifications (admin only)
   */
  getPanics(page: number = 0, size: number = 20): Observable<PageResponse<PanicResponse>> {
    return this.http.get<PageResponse<PanicResponse>>(`${this.apiUrl}?page=${page}&size=${size}`);
  }
}
