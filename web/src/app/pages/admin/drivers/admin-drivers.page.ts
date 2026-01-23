import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient, HttpClientModule } from '@angular/common/http';
import { FormsModule } from '@angular/forms';  // Add this import
import { RouterModule } from '@angular/router';  // Add this import
import { ChangeDetectorRef } from '@angular/core';
import { finalize } from 'rxjs/operators';
import { DriverResponse } from '../../../model/driver-response.interface';

@Component({
  selector: 'app-admin-drivers',
  templateUrl: './admin-drivers.page.html',
  standalone: true,
  imports: [CommonModule, HttpClientModule, FormsModule, RouterModule]
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

  constructor(private http: HttpClient, private cdr: ChangeDetectorRef) {}

  ngOnInit() {
    this.loadDrivers();
  }

  loadDrivers() {
    this.isLoading = true;
    this.errorMessage = '';
    this.cdr.markForCheck();

    this.http.get<any>('http://localhost:8081/api/drivers')
      .pipe(finalize(() => {
        this.isLoading = false;
        this.cdr.markForCheck();
      }))
      .subscribe({
        next: (res) => {
          const data = Array.isArray(res) ? res : (res?.content ?? res?.results ?? []);
          this.drivers = data;
          this.filteredDrivers = data;

          this.cdr.markForCheck(); // this is the key to update the UI
          this.calculateStats();
        },
        error: (err) => {
          this.errorMessage = `Failed to load drivers (${err.status})`;
          this.cdr.markForCheck();
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
    this.suspendedDrivers = this.drivers.filter(d => d.blocked).length;
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
