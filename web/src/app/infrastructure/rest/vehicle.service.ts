
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../env/environment';
import { VehicleLocationResponse } from './model/vehicle-location.model';

export interface LocationDto {
  address: string;
  latitude: number;
  longitude: number;
}

export interface SimulationLockResponse {
  acquired: boolean;
  reason?: string;
}

@Injectable({
  providedIn: 'root'
})
export class VehicleService {
  private apiUrl = `${environment.apiHost}vehicles`;

  constructor(private http: HttpClient) {}

  getActiveVehicles(): Observable<VehicleLocationResponse[]> {
    return this.http.get<VehicleLocationResponse[]>(`${this.apiUrl}/active`);
  }

  updateVehicleLocation(vehicleId: number, location: LocationDto): Observable<void> {
    return this.http.put<void>(`${this.apiUrl}/${vehicleId}/location`, location);
  }

  acquireSimulationLock(vehicleId: number, sessionId: string): Observable<SimulationLockResponse> {
    return this.http.put<SimulationLockResponse>(
      `${this.apiUrl}/${vehicleId}/simulation-lock`,
      { sessionId }
    );
  }

  releaseSimulationLock(vehicleId: number, sessionId: string): Observable<void> {
    return this.http.request<void>('DELETE', `${this.apiUrl}/${vehicleId}/simulation-lock`, {
      body: { sessionId }
    });
  }
}