import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../env/environment';

export interface ReviewTokenData {
  valid: boolean;
  rideId: number;
  passengerId: number;
  driverId: number;
  driverName?: string;
  pickupAddress?: string;
  dropoffAddress?: string;
}

export interface ReviewRequest {
  token?: string;
  rideId?: number;
  driverRating: number;
  vehicleRating: number;
  comment?: string;
}

export interface ReviewResponse {
  id: number;
  rideId: number;
  passengerId: number;
  driverRating: number;
  vehicleRating: number;
  comment?: string;
  createdAt: string;
}

@Injectable({
  providedIn: 'root'
})
export class ReviewService {
  private apiUrl = `${environment.apiHost}reviews`;

  constructor(private http: HttpClient) {}

  /**
   * Validate a review token and get ride information.
   * This endpoint does not require authentication.
   */
  validateReviewToken(token: string): Observable<ReviewTokenData> {
    // Add header to skip auth interceptor (use 'skip' header as per interceptor)
    const headers = new HttpHeaders().set('skip', 'true');
    return this.http.get<ReviewTokenData>(`${this.apiUrl}/validate-token?token=${encodeURIComponent(token)}`, { headers });
  }

  /**
   * Submit a review using a JWT token.
   * This endpoint does not require authentication.
   */
  submitReviewWithToken(request: ReviewRequest): Observable<ReviewResponse> {
    // Add header to skip auth interceptor (use 'skip' header as per interceptor)
    const headers = new HttpHeaders().set('skip', 'true');
    return this.http.post<ReviewResponse>(`${this.apiUrl}/with-token`, request, { headers });
  }
}
