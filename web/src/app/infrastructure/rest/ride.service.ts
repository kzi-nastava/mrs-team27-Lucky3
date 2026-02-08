import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { environment } from '../../../env/environment';
import { CreateRideRequest } from './model/create-ride.model';
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

export interface AdminStatsResponse {
  activeRidesCount: number;
  averageDriverRating: number;
  driversOnlineCount: number;
  totalPassengersInRides: number;
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

  getRidesHistory(params: {
    page: number;
    size: number;
    sort?: string;
    fromDate?: string; 
    toDate?: string;
    driverId?: number;
    passengerId?: number;
    status?: string;
  }): Observable<PageResponse<RideResponse>> {
    let queryParams: any = {
      page: params.page.toString(),
      size: params.size.toString(),
    };

    if (params.sort) queryParams.sort = params.sort;
    if (params.fromDate) queryParams.fromDate = params.fromDate;
    if (params.toDate) queryParams.toDate = params.toDate;
    if (params.driverId) queryParams.driverId = params.driverId.toString();
    if (params.passengerId) queryParams.passengerId = params.passengerId.toString();
    if (params.status && params.status !== 'all') queryParams.status = params.status;

    return this.http.get<PageResponse<RideResponse>>(this.apiUrl, { params: queryParams });
  }

  orderRide(request: CreateRideRequest): Observable<RideResponse> {
    return this.http.post<RideResponse>(`${this.apiUrl}`, request);
  }

  getRide(id: number): Observable<RideResponse> {
    return this.http.get<RideResponse>(`${this.apiUrl}/${id}`);
  }

  getActiveRide(userId?: number): Observable<RideResponse | null> {
    const query = userId ? `?userId=${encodeURIComponent(String(userId))}` : '';
    return this.http.get<RideResponse>(`${this.apiUrl}/active${query}`).pipe(
      catchError(() => of(null))
    );
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

  addRouteToFavorites(rideId: number, favoriteRoute: any): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/${rideId}/favourite-route`, favoriteRoute);
  }

  getFavoriteRoutes(passengerId: number): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/${passengerId}/favourite-routes`);
  }

  removeFavoriteRoute(passengerId: number, routeId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${passengerId}/favourite-routes/${routeId}`);
  }

  stopRide(id: number, request: any): Observable<RideResponse> {
    return this.http.put<RideResponse>(`${this.apiUrl}/${id}/stop`, request);
  }

  /**
   * Get the active ride for a user (IN_PROGRESS first, then earliest PENDING/SCHEDULED)
   */
  getActiveRideForUser(userId: number): Observable<RideResponse | null> {
    return this.http.get<RideResponse>(`${this.apiUrl}/active?userId=${userId}`);
  }

  /**
   * Complete a stop during an in-progress ride
   */
  completeStop(rideId: number, stopIndex: number): Observable<RideResponse> {
    return this.http.put<RideResponse>(`${this.apiUrl}/${rideId}/stop/${stopIndex}/complete`, {});
  }

  /**
   * Report an inconsistency during a ride (passenger only)
   */
  reportInconsistency(rideId: number, request: { remark: string }): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/${rideId}/inconsistencies`, request);
  }

  /**
   * Cancel ride as passenger (reason is optional)
   */
  cancelRideAsPassenger(id: number, reason?: string): Observable<RideResponse> {
    return this.http.put<RideResponse>(`${this.apiUrl}/${id}/cancel`, { reason: reason || '' });
  }

  /**
   * Trigger panic button for a ride (driver or passenger)
   */
  panicRide(id: number, reason?: string): Observable<RideResponse> {
    return this.http.put<RideResponse>(`${this.apiUrl}/${id}/panic`, { reason: reason || null });
  }

  /**
   * Admin: Get all active rides (PENDING, ACCEPTED, SCHEDULED, IN_PROGRESS)
   */
  getAllActiveRides(params?: {
    page?: number;
    size?: number;
    sort?: string;
    search?: string;
    status?: string;
    vehicleType?: string;
  }): Observable<PageResponse<RideResponse>> {
    const query = new URLSearchParams();
    if (params?.page != null) query.set('page', String(params.page));
    if (params?.size != null) query.set('size', String(params.size));
    if (params?.sort) query.set('sort', params.sort);
    if (params?.search) query.set('search', params.search);
    if (params?.status) query.set('status', params.status);
    if (params?.vehicleType) query.set('vehicleType', params.vehicleType);

    const qs = query.toString();
    return this.http.get<PageResponse<RideResponse>>(`${this.apiUrl}/active/all${qs ? `?${qs}` : ''}`);
  }

  /**
   * Admin: Get dashboard statistics
   */
  getAdminStats(): Observable<AdminStatsResponse> {
    return this.http.get<AdminStatsResponse>(`${environment.apiHost}admin/stats`);
  }
}