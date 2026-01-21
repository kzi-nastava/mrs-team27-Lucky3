import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient, HttpClientModule } from '@angular/common/http';
import { FormsModule } from '@angular/forms';  // Add this import

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
  active: boolean;
  blocked: boolean;
  active24h: string;
}

@Component({
  selector: 'app-admin-drivers',
  templateUrl: './admin-drivers.page.html',
  standalone: true,
  imports: [CommonModule, HttpClientModule, FormsModule]
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
  searchTerm: string = '';

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

  filterDrivers() {
    if (!this.searchTerm.trim()) {
      this.filteredDrivers = this.drivers;
      return;
    }

    const search = this.searchTerm.toLowerCase();
    this.filteredDrivers = this.drivers.filter(driver => 
      driver.name.toLowerCase().includes(search) ||
      driver.surname.toLowerCase().includes(search) ||
      `${driver.name} ${driver.surname}`.toLowerCase().includes(search) ||
      driver.email.toLowerCase().includes(search)
    );
  }

  calculateStats() {
    this.totalDrivers = this.drivers.length;
    this.activeDrivers = this.drivers.filter(d => d.active).length;
    this.inactiveDrivers = this.drivers.filter(d => !d.active).length;
  }

  filterByStatus(status: string) {
    if (status === 'ALL') {
      this.filteredDrivers = this.drivers;
    } else if (status === 'ACTIVE') {
      this.filteredDrivers = this.drivers.filter(d => d.active);
    } else if (status === 'INACTIVE') {
      this.filteredDrivers = this.drivers.filter(d => !d.active);
    } else if (status === 'SUSPENDED') {
      this.filteredDrivers = this.drivers.filter(d => d.blocked);
    }
  }

  refreshDrivers() {
    this.loadDrivers();
  }

  getRandomRating(): number {
    return Math.floor(Math.random() * 5) + 1; // Random number between 1-5
  }

  getRandomRides(): number {
    return Math.floor(Math.random() * 500) + 50; // Random number between 50-550
  }

  getRandomEarnings(): number {
    return Math.floor(Math.random() * 5000) + 500; // Random number between 500-5500
  }

}
