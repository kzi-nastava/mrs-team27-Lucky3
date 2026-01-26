import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, catchError, throwError } from 'rxjs';
import { environment } from '../../../env/environment';

export interface DriverStatusResponse {
  driverId: number;
  active: boolean;
  inactiveRequested: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class DriverService {
  private readonly apiUrl = `${environment.apiHost}drivers`;

  constructor(private http: HttpClient) {}

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
}
