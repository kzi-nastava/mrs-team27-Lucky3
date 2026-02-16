import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Ride } from '../../data/ride.model';

export type RideSortField = 'startTime' | 'endTime' | 'distance' | 'departure' | 'passengerCount' | 'totalCost';

@Component({
  selector: 'app-rides-table',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './rides-table.component.html',
  styleUrl: './rides-table.component.css'
})
export class RidesTableComponent {
  @Input() rides: Ride[] = [];
  @Input() sortField: RideSortField = 'startTime';
  @Input() sortDirection: 'asc' | 'desc' = 'desc';
  @Input() reviewableRideIds: Set<string> = new Set();
  
  @Output() sortChange = new EventEmitter<RideSortField>();
  @Output() viewDetails = new EventEmitter<string>();
  
  @Output() rideSelected = new EventEmitter<Ride>();
  selectedRide: Ride | null = null;

  handleSort(field: RideSortField) {
    this.sortChange.emit(field);
  }

  onViewDetails(id: string) {
    this.viewDetails.emit(id);
  }

  formatDate(dateStr?: string): string {
    if (!dateStr) return '—';
    return new Date(dateStr).toLocaleDateString('en-GB', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric'
    });
  }

  formatTime(dateStr?: string): string {
    if (!dateStr) return '—';
    return new Date(dateStr).toLocaleTimeString('en-US', {
      hour: 'numeric',
      minute: '2-digit',
      hour12: true
    });
  }

  getDuration(start?: string, end?: string): string {
    if (!start || !end) return '—';
    const startTime = new Date(start).getTime();
    const endTime = new Date(end).getTime();
    const diff = Math.round((endTime - startTime) / 60000);
    return `${diff} min`;
  }

  formatPrice(price: number): string {
    if (price == null) return '—';
    return price.toFixed(2);
  }

  formatDistance(distance: number): string {
    if (distance == null) return '—';
    return distance.toFixed(2);
  }

  selectRide(ride: Ride) {
    this.selectedRide = ride;
    this.rideSelected.emit(ride);
  }
  
  isSelected(ride: Ride): boolean {
    return this.selectedRide?.id === ride.id;
  }

  isReviewable(ride: Ride): boolean {
    return this.reviewableRideIds.has(ride.id);
  }
}
