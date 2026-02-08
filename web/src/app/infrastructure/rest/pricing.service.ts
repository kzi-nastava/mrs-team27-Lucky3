import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../env/environment';

export interface VehiclePriceResponse {
  id: number;
  vehicleType: string;
  baseFare: number;
  pricePerKm: number;
}

@Injectable({
  providedIn: 'root'
})
export class PricingService {
  private apiUrl = `${environment.apiHost}admin/vehicle-prices`;

  constructor(private http: HttpClient) {}

  getAllPrices(): Observable<VehiclePriceResponse[]> {
    return this.http.get<VehiclePriceResponse[]>(this.apiUrl);
  }

  updatePrice(vehicleType: string, baseFare: number, pricePerKm: number): Observable<VehiclePriceResponse> {
    return this.http.put<VehiclePriceResponse>(this.apiUrl, {
      vehicleType,
      baseFare,
      pricePerKm
    });
  }
}
