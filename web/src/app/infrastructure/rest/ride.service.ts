import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../env/environment';
import { LocationDto } from './model/location.model';
import { EndRideRequest, RideCancellationRequest, RideResponse } from './model/ride-response.model';

export interface RoutePoint {
  location: LocationDto;
  order: number;
}

export interface RideEstimationResponse {
  estimatedTimeInMinutes: number;
  estimatedCost: number;
  estimatedDriverArrivalInMinutes: number;
  estimatedDistance: number;
  routePoints: RoutePoint[];
}

export interface RideRequirements {
  vehicleType: string;
  babyTransport: boolean;
  petTransport: boolean;
}

export interface CreateRideRequest {
  start: LocationDto;
  destination: LocationDto;
  stops: LocationDto[];
  passengerEmails: string[];
  scheduledTime: string | null;
  requirements: RideRequirements;
}

@Injectable({
  providedIn: 'root'
})
export class RideService {
  private apiUrl = `${environment.apiHost}rides`;

  constructor(private http: HttpClient) {}

  estimateRide(request: CreateRideRequest): Observable<RideEstimationResponse> {
    return this.http.post<RideEstimationResponse>(`${this.apiUrl}/estimate`, request);
  }

  getRide(id: number): Observable<RideResponse> {
    return this.http.get<RideResponse>(`${this.apiUrl}/${id}`);
  }

  getActiveRide(userId?: number): Observable<RideResponse> {
    const query = userId ? `?userId=${encodeURIComponent(String(userId))}` : '';
    return this.http.get<RideResponse>(`${this.apiUrl}/active${query}`);
  }

  startRide(id: number): Observable<RideResponse> {
    return this.http.put<RideResponse>(`${this.apiUrl}/${id}/start`, {});
  }

  endRide(id: number, request: EndRideRequest): Observable<RideResponse> {
    return this.http.put<RideResponse>(`${this.apiUrl}/${id}/end`, request);
  }

  cancelRide(id: number, request: RideCancellationRequest): Observable<RideResponse> {
    return this.http.put<RideResponse>(`${this.apiUrl}/${id}/cancel`, request);
  }
}