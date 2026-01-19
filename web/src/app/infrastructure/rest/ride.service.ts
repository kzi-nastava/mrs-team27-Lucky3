import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../env/environment';
import { LocationDto } from './model/location.model';

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
}