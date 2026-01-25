import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { RideResponse, RideStatus } from '../../../infrastructure/rest/model/ride-response.model';
import { RouterModule } from '@angular/router';

type FilterType = 'all' | 'completed' | 'cancelled';
type SortField = keyof RideResponse;
type SortDirection = 'asc' | 'desc' | '';

export interface SortEvent {
  field: SortField;
  direction: SortDirection;
}

@Component({
  selector: 'app-ride-history',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './ride-history.component.html'
})
export class RideHistoryComponent implements OnInit {
  rides: RideResponse[] = [];
  filteredRides: RideResponse[] = [];
  sortedRides: RideResponse[] = [];
  
  filter: FilterType = 'all';
  dateFilter: string = '';
  
  sortField: SortField = 'id';
  sortDirection: SortDirection = 'desc';

  private apiUrl = 'http://localhost:8081/api/rides';

  constructor(private http: HttpClient) {}

  ngOnInit(): void {
    this.loadRides();
  }

  loadRides(): void {
    this.http.get<RideResponse[]>(this.apiUrl).subscribe({
      next: (data) => {
        this.rides = data;
        this.updateView();
      },
      error: (error) => {
        console.error('Error fetching rides:', error);
      }
    });
  }

  setFilter(filter: FilterType): void {
    this.filter = filter;
    this.updateView();
  }

  updateView(): void {
    // Apply status filter
    this.filteredRides = this.rides.filter(ride => {
      const statusMatch = this.matchesStatusFilter(ride);
      const dateMatch = this.matchesDateFilter(ride);
      return statusMatch && dateMatch;
    });

    // Apply sorting
    this.sortedRides = this.sortRides(this.filteredRides);
  }

  private matchesStatusFilter(ride: RideResponse): boolean {
    if (this.filter === 'all') return true;
    
    const status = ride.status?.toLowerCase();
    if (this.filter === 'completed') {
      return status === 'finished';
    }
    if (this.filter === 'cancelled') {
      return status === 'cancelled';
    }
    return true;
  }

  private matchesDateFilter(ride: RideResponse): boolean {
    if (!this.dateFilter) return true;
    
    const rideDate = ride.scheduledTime || ride.startTime;
    if (!rideDate) return false;
    
    const rideDateOnly = new Date(rideDate).toISOString().split('T')[0];
    return rideDateOnly === this.dateFilter;
  }

  handleSort(event: SortEvent): void {
    this.sortField = event.field;
    this.sortDirection = event.direction;
    this.updateView();
  }

  private sortRides(rides: RideResponse[]): RideResponse[] {
    if (!this.sortDirection || !this.sortField) {
      return [...rides];
    }

    return [...rides].sort((a, b) => {
      const aValue = this.getNestedValue(a, this.sortField);
      const bValue = this.getNestedValue(b, this.sortField);
      
      const comparison = this.compare(aValue, bValue);
      return this.sortDirection === 'asc' ? comparison : -comparison;
    });
  }

  private getNestedValue(obj: any, field: string): any {
    // Handle nested properties like 'driver.name'
    const value = obj[field];
    return value !== undefined ? value : '';
  }

  private compare(a: any, b: any): number {
    if (a === null || a === undefined) return 1;
    if (b === null || b === undefined) return -1;
    
    if (typeof a === 'string' && typeof b === 'string') {
      return a.toLowerCase().localeCompare(b.toLowerCase());
    }
    
    if (a < b) return -1;
    if (a > b) return 1;
    return 0;
  }

  onViewDetails(ride: RideResponse): void {
    console.log('View ride details:', ride);
    // Implement navigation to ride details page or open modal
    // this.router.navigate(['/rides', ride.id]);
  }

  getDriverName(ride: RideResponse): string {
    if (ride.driver?.name && ride.driver?.surname) {
      return `${ride.driver.name} ${ride.driver.surname}`;
    }
    return 'N/A';
  }

  getPassengerNames(ride: RideResponse): string {
    if (!ride.passengers || ride.passengers.length === 0) {
      return 'N/A';
    }
    return ride.passengers
      .map(p => `${p.name || ''} ${p.surname || ''}`.trim())
      .join(', ');
  }

  getLocationString(location: any): string {
    if (!location) return 'N/A';
    if (location.address) return location.address;
    if (location.latitude && location.longitude) {
      return `${location.latitude.toFixed(4)}, ${location.longitude.toFixed(4)}`;
    }
    return 'N/A';
  }

  formatDate(dateString: string | undefined): string {
    if (!dateString) return 'N/A';
    const date = new Date(dateString);
    return date.toLocaleString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  getStatusClass(status: RideStatus | undefined): string {
    const statusLower = status?.toLowerCase();
    switch (statusLower) {
      case 'finished':
        return 'bg-green-500/20 text-green-400';
      case 'cancelled':
        return 'bg-red-500/20 text-red-400';
      case 'in_progress':
        return 'bg-blue-500/20 text-blue-400';
      case 'accepted':
        return 'bg-yellow-500/20 text-yellow-400';
      case 'pending':
        return 'bg-gray-500/20 text-gray-400';
      default:
        return 'bg-gray-500/20 text-gray-400';
    }
  }

  formatStatus(status: RideStatus | undefined): string {
    if (!status) return 'Unknown';
    return status.replace('_', ' ');
  }

  onSort(field: SortField): void {
    if (this.sortField === field) {
      // Toggle direction
      if (this.sortDirection === 'asc') {
        this.sortDirection = 'desc';
      } else if (this.sortDirection === 'desc') {
        this.sortDirection = '';
      } else {
        this.sortDirection = 'asc';
      }
    } else {
      this.sortField = field;
      this.sortDirection = 'asc';
    }
    
    this.updateView();
  }
}
