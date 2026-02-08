import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../env/environment';
import { RideResponse } from './model/ride-response.model';

export interface TokenValidationResponse {
  valid: boolean;
  reason?: string;
  rideId?: number;
  email?: string;
  status?: string;
}

export interface TokenRideErrorResponse {
  error: string;
  status?: string;
  message?: string;
}

@Injectable({
  providedIn: 'root'
})
export class RideTrackingService {
  private readonly baseUrl = environment.apiHost + 'ride-tracking';

  constructor(private http: HttpClient) {}

  /**
   * Validate a tracking token.
   * Returns info about whether the token is valid and the ride is trackable.
   */
  validateToken(token: string): Observable<TokenValidationResponse> {
    return this.http.get<TokenValidationResponse>(
      `${this.baseUrl}/validate`,
      { 
        params: { token },
        headers: new HttpHeaders({ 'skip': 'true' }) // Skip auth interceptor
      }
    );
  }

  /**
   * Get ride details using a tracking token.
   * Returns full ride response for tracking purposes.
   */
  getRideByToken(token: string): Observable<RideResponse> {
    return this.http.get<RideResponse>(
      `${this.baseUrl}/ride`,
      { 
        params: { token },
        headers: new HttpHeaders({ 'skip': 'true' }) // Skip auth interceptor
      }
    );
  }
}
