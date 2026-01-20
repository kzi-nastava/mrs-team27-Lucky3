import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../env/environment';
import { LocationDto } from './model/location.model';
import { EndRideRequest, RideCancellationRequest, RideResponse } from './model/ride-response.model';

export interface PageResponse<T> {
  content: T[];
  totalElements?: number;
  totalPages?: number;
  number?: number;
  size?: number;
}

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

  getRidesHistory(params?: {
    driverId?: number;
    passengerId?: number;
    status?: string;
    fromDate?: string;
    toDate?: string;
    page?: number;
    size?: number;
  }): Observable<PageResponse<RideResponse>> {
    const query = new URLSearchParams();
    if (params?.driverId != null) query.set('driverId', String(params.driverId));
    if (params?.passengerId != null) query.set('passengerId', String(params.passengerId));
    if (params?.status) query.set('status', params.status);
    if (params?.fromDate) query.set('fromDate', params.fromDate);
    if (params?.toDate) query.set('toDate', params.toDate);
    if (params?.page != null) query.set('page', String(params.page));
    if (params?.size != null) query.set('size', String(params.size));

    const qs = query.toString();
    return this.http.get<PageResponse<RideResponse>>(`${this.apiUrl}${qs ? `?${qs}` : ''}`);
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