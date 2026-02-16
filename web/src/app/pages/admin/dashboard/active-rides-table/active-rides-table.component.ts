import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { RideResponse } from '../../../../infrastructure/rest/model/ride-response.model';

export type ActiveRideSortField = 'driver' | 'vehicle' | 'status' | 'passengerCount' | 'rating' | 'timeActive';

@Component({
  selector: 'app-active-rides-table',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './active-rides-table.component.html',
})
export class ActiveRidesTableComponent {
  @Input() rides: RideResponse[] = [];
  @Input() sortField: ActiveRideSortField = 'status';
  @Input() sortDirection: 'asc' | 'desc' = 'desc';
  @Input() driverRatings: Record<number, number> = {};
  @Input() driverOnlineHours: Record<number, string> = {};
  
  @Output() sortChange = new EventEmitter<ActiveRideSortField>();

  constructor(private router: Router) {}

  handleSort(field: ActiveRideSortField) {
    this.sortChange.emit(field);
  }

  onRowClick(ride: RideResponse): void {
    this.router.navigate(['/admin/ride', ride.id]);
  }

  getDriverName(ride: RideResponse): string {
    if (ride.driver) {
      const name = ride.driver.name || '';
      const surname = ride.driver.surname || '';
      return `${name} ${surname}`.trim() || '—';
    }
    return 'Unassigned';
  }

  getDriverImageUrl(ride: RideResponse): string {
    if (ride.driver?.id) {
      return `http://localhost:8081/api/users/${ride.driver.id}/profile-image`;
    }
    return '';
  }

  getVehicleInfo(ride: RideResponse): string {
    const model = ride.model || ride.driver?.vehicle?.model || '—';
    return model;
  }

  getVehiclePlates(ride: RideResponse): string {
    return ride.licensePlates || ride.driver?.vehicle?.licenseNumber || '—';
  }

  getVehicleType(ride: RideResponse): string {
    const type = ride.vehicleType || ride.driver?.vehicle?.vehicleType || '—';
    return type;
  }

  getVehicleTypeIcon(ride: RideResponse): string {
    const type = (ride.vehicleType || ride.driver?.vehicle?.vehicleType || '').toUpperCase();
    switch(type) {
      case 'STANDARD': return '🚗';
      case 'LUXURY': return '🚙';
      case 'VAN': return '🚐';
      default: return '🚗';
    }
  }

  getPassengerCount(ride: RideResponse): number {
    return ride.passengers?.length || 0;
  }

  getStatusClass(status: string | undefined): string {
    switch(status) {
      case 'IN_PROGRESS':
      case 'ACTIVE':
        return 'bg-green-500/15 border-green-500/30 text-green-400';
      case 'ACCEPTED':
        return 'bg-blue-500/15 border-blue-500/30 text-blue-400';
      case 'PENDING':
        return 'bg-yellow-500/15 border-yellow-500/30 text-yellow-400';
      case 'SCHEDULED':
        return 'bg-purple-500/15 border-purple-500/30 text-purple-400';
      default:
        return 'bg-gray-500/15 border-gray-500/30 text-gray-400';
    }
  }

  getStatusLabel(status: string | undefined): string {
    switch(status) {
      case 'IN_PROGRESS':
      case 'ACTIVE':
        return 'In Progress';
      case 'ACCEPTED':
        return 'Accepted';
      case 'PENDING':
        return 'Pending';
      case 'SCHEDULED':
        return 'Scheduled';
      default:
        return status || '—';
    }
  }

  getEstimatedTimeLeft(ride: RideResponse): string {
    // If ride is in progress and has distance info
    if ((ride.status === 'IN_PROGRESS' || ride.status === 'ACTIVE') && ride.distanceKm) {
      const traveled = ride.distanceTraveled || 0;
      const remaining = ride.distanceKm - traveled;
      if (remaining > 0) {
        // Assume average speed of 30 km/h in city
        const minutesLeft = Math.round((remaining / 30) * 60);
        return `~${minutesLeft} min`;
      }
      return 'Arriving';
    }
    
    // For non-started rides, show estimated duration
    if (ride.estimatedTimeInMinutes) {
      return `~${ride.estimatedTimeInMinutes} min`;
    }
    
    return '—';
  }

  // For driver rating - use cached driver stats
  getDriverRating(ride: RideResponse): string {
    if (ride.driver?.id && this.driverRatings[ride.driver.id] > 0) {
      return this.driverRatings[ride.driver.id].toFixed(1);
    }
    return '—';
  }

  getTimeActive(ride: RideResponse): string {
    if (ride.driver?.id && this.driverOnlineHours[ride.driver.id]) {
      return this.driverOnlineHours[ride.driver.id];
    }
    return '—';
  }
}
