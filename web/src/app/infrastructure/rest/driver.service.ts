import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, ReplaySubject, catchError, throwError } from 'rxjs';
import { environment } from '../../../env/environment';

export interface DriverStatusResponse {
  driverId: number;
  active: boolean;
  inactiveRequested: boolean;
  hasActiveRide?: boolean;
  hasUpcomingRides?: boolean; // SCHEDULED or PENDING rides
  workingHoursExceeded?: boolean;
  workedHoursToday?: string; // e.g. "6h 30m"
  statusMessage?: string; // Message to display to the driver
}

export interface DriverStatsResponse {
  driverId: number;
  totalEarnings: number;
  completedRides: number;
  averageRating: number;
  totalRatings: number;
  averageVehicleRating: number;
  totalVehicleRatings: number;
  onlineHoursToday: string;  // Formatted as "Xh Ym"
}

@Injectable({
  providedIn: 'root'
})
export class DriverService {
  private readonly apiUrl = `${environment.apiHost}drivers`;
  
  // ReplaySubject buffers the last event so late subscribers (like dashboard after navigation) receive it
  private statusRefresh$ = new ReplaySubject<void>(1);

  constructor(private http: HttpClient) {}

  /**
   * Get observable to listen for status refresh events
   */
  get onStatusRefresh(): Observable<void> {
    return this.statusRefresh$.asObservable();
  }

  /**
   * Trigger a status refresh (call this after ride ends)
   */
  triggerStatusRefresh(): void {
    this.statusRefresh$.next();
  }

  /**
   * Clear the status refresh buffer (call after processing)
   */
  clearStatusRefresh(): void {
    // Create new ReplaySubject to clear the buffer
    this.statusRefresh$ = new ReplaySubject<void>(1);
  }

  /**
   * Toggle driver online/offline status
   */
  toggleStatus(driverId: number, active: boolean): Observable<DriverStatusResponse> {
    return this.http.put<DriverStatusResponse>(
      `${this.apiUrl}/${driverId}/status`,
      null,
      { params: { active: active.toString() } }
    ).pipe(
      catchError(err => {
        const message = err?.error?.message || err?.message || 'Failed to update status';
        return throwError(() => new Error(message));
      })
    );
  }

  /**
   * Get current driver status
   */
  getStatus(driverId: number): Observable<DriverStatusResponse> {
    return this.http.get<DriverStatusResponse>(`${this.apiUrl}/${driverId}/status`);
  }

  /**
   * Get driver statistics (earnings, rides completed, rating, online hours)
   */
  getStats(driverId: number): Observable<DriverStatsResponse> {
    return this.http.get<DriverStatsResponse>(`${this.apiUrl}/${driverId}/stats`);
  }

  createDriver(formData: FormData): Observable<any> {
    return this.http.post(this.apiUrl, formData);
  }
}
