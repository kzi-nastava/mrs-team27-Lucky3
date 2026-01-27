import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AuthService } from '../auth/auth.service'; // Adjust path as needed
import { environment } from '../../../env/environment';
import { VehicleType } from '../../infrastructure/rest/model/order-ride.model';

export interface UserProfile {
  name: string;
  surname: string;
  email: string;
  phoneNumber: string;
  address: string;
  imageUrl?: string;
}


export interface VehicleInformation {
  model: string;
  vehicleType: string;
  licenseNumber: string;
  passengerSeats: number;
  babyTransport: boolean;
  petTransport: boolean;
  driverId?: number;
}

export interface ChangeInformationRequest{
  name: string;
  surname: string;
  email: string;
  phone: string;
  address: string;
  vehicle: VehicleInformation;
}

export interface ChangeInformationResponse extends ChangeInformationRequest {
  id: number;
  requestedDriverId: number;
  dateCreated: string;
  imageId: number;
  status: string;
}

// user.interface.ts (or add to existing file)
export enum UserRole {
  PASSENGER = 'PASSENGER',
  DRIVER = 'DRIVER',
  ADMIN = 'ADMIN'
}

export interface DriverResponse extends UserProfile {
  id: number;
  role: UserRole;
  vehicle: VehicleInformation;
  isActive: boolean;
  isBlocked: boolean;
  active24h: string;
}

@Injectable({
  providedIn: 'root'
})
export class UserService {
  private apiUrl = `${environment.apiHost}users`;
  private driverApiUrl = `${environment.apiHost}drivers`;
  private driveChangeRequestUrl = `${environment.apiHost}driver-change-requests`;

  constructor(
    private http: HttpClient,
    private authService: AuthService
  ) {}

  // ========== User Profile Methods ==========
  getCurrentUser(): Observable<UserProfile> {
    const userId = this.authService.getUserId();
    return this.http.get<UserProfile>(`${this.apiUrl}/${userId}`);
  }

  updateUserProfile(userId: number, userProfile: UserProfile, profileImage?: File): Observable<UserProfile> {
    const formData = new FormData();
    
    // Create a Blob for the JSON data with proper content type
    const userBlob = new Blob([JSON.stringify(userProfile)], {
      type: 'application/json'
    });
    
    // Append JSON as 'user' part
    formData.append('user', userBlob);
    
    // Append image file if provided
    if (profileImage) {
      formData.append('profileImage', profileImage);
    }
    
    return this.http.put<UserProfile>(`${this.apiUrl}/${userId}`, formData);
  }

  updateCurrentUserProfile(userProfile: UserProfile, profileImage?: File): Observable<UserProfile> {
    const userId = this.authService.getUserId();
    if (userId === null) {
      throw new Error('User is not authenticated');
    }
    return this.updateUserProfile(userId, userProfile, profileImage);
  }

  updateDriverProfile(driverId: number, changeRequest: ChangeInformationRequest, profileImage?: File): Observable<DriverResponse> {
    const formData = new FormData();
    
    // Create a Blob for the JSON data with proper content type
    const requestBlob = new Blob([JSON.stringify(changeRequest)], {
      type: 'application/json'
    });
    
    // Append JSON as 'user' part
    formData.append('request', requestBlob);
    
    // Append image file if provided
    if (profileImage) {
      formData.append('profileImage', profileImage);
    }
    return this.http.put<DriverResponse>(`${this.driverApiUrl}/${driverId}`, formData);
  }

  updateCurrentDriverProfile(changeRequest: ChangeInformationRequest, profileImage?: File): Observable<DriverResponse> {
    const driverId = this.authService.getUserId();
    if (driverId === null) {
      throw new Error('User is not authenticated');
    }
    return this.updateDriverProfile(driverId, changeRequest, profileImage);
  }

  // ========== Driver Methods ==========
  getCurrentDriver(): Observable<DriverResponse> {
    const driverId = this.authService.getUserId();
    return this.http.get<DriverResponse>(`${this.driverApiUrl}/${driverId}`);
  }

  // ========== Driver Change Request Methods ==========
  getDriverChangeRequests(): Observable<ChangeInformationResponse[]> {
    return this.http.get<ChangeInformationResponse[]>(this.driveChangeRequestUrl);
  }

  approveDriverChangeRequest(requestId: number): Observable<void> {
    return this.http.post<void>(`${this.driveChangeRequestUrl}/${requestId}/review`, {"approve": true});
  }
  rejectDriverChangeRequest(requestId: number): Observable<void> {
    return this.http.post<void>(`${this.driveChangeRequestUrl}/${requestId}/review`, {"approve": false});
  }
}
