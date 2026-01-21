import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient, HttpClientModule } from '@angular/common/http';

interface VehicleInformation {
  model: string;
  vehicleType: string;
  licenseNumber: string;
  passengerSeats: number;
  babyTransport: boolean;
  petTransport: boolean;
  driverId: number;
}

interface DriverResponse {
  id: number;
  name: string;
  surname: string;
  email: string;
  profilePictureUrl: string;
  role: string;
  phoneNumber: string;
  address: string;
  vehicle: VehicleInformation;
  isActive: boolean;
  isBlocked: boolean;
  active24h: string;
}

@Component({
  selector: 'app-admin-drivers',
  templateUrl: './admin-drivers.page.html',
  standalone: true,
  imports: [CommonModule, HttpClientModule]
})
export class AdminDriversPage implements OnInit {
  drivers: DriverResponse[] = [];
  filteredDrivers: DriverResponse[] = [];
  
  totalDrivers: number = 0;
  activeDrivers: number = 0;
  inactiveDrivers: number = 0;
  suspendedDrivers: number = 0;
  avgRating: number = 0;
  
  isLoading: boolean = true;
  errorMessage: string = '';

  constructor(private http: HttpClient) {}

  ngOnInit() {
    this.loadDrivers();
  }

  loadDrivers() {
    this.isLoading = true;
    this.errorMessage = '';
    
    this.http.get<DriverResponse[]>('http://localhost:8081/api/drivers')
      .subscribe({
        next: (data) => {
          this.drivers = data;
          this.filteredDrivers = data;
          this.calculateStats();
          this.isLoading = false;
        },
        error: (error) => {
          console.error('Error:', error);
          this.errorMessage = 'Failed to load drivers';
          this.isLoading = false;
        }
      });
  }

  calculateStats() {
    this.totalDrivers = this.drivers.length;
    this.activeDrivers = this.drivers.filter(d => d.isActive).length;
    this.inactiveDrivers = this.drivers.filter(d => !d.isActive).length;
  }

  filterByStatus(status: string) {
    if (status === 'ALL') {
      this.filteredDrivers = this.drivers;
    } else if (status === 'ACTIVE') {
      this.filteredDrivers = this.drivers.filter(d => d.isActive);
    } else if (status === 'INACTIVE') {
      this.filteredDrivers = this.drivers.filter(d => !d.isActive);
    }
  }

  refreshDrivers() {
    this.loadDrivers();
  }
}
