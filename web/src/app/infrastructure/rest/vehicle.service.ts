
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../env/environment';
import { VehicleLocationResponse } from './model/vehicle-location.model';

@Injectable({
  providedIn: 'root'
})
export class VehicleService {
  private apiUrl = `${environment.apiHost}vehicles`;

  constructor(private http: HttpClient) {}

  getActiveVehicles(): Observable<VehicleLocationResponse[]> {
    return this.http.get<VehicleLocationResponse[]>(`${this.apiUrl}/active`);
  }
}