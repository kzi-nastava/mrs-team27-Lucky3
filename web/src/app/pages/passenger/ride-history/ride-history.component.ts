import { Component, OnInit, ChangeDetectorRef, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { RideResponse, RideStatus } from '../../../infrastructure/rest/model/ride-response.model';
import { RidesTableComponent } from '../../../shared/rides/rides-table/rides-table.component';
import { RouterModule } from '@angular/router';
import { RideService } from '../../../infrastructure/rest/ride.service';
import { AuthService } from '../../../infrastructure/auth/auth.service';
import { Subject, takeUntil } from 'rxjs';
import { Ride } from '../../../shared/data/mock-data';

@Component({
  selector: 'app-ride-history',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, RidesTableComponent],
  templateUrl: './ride-history.component.html'
})
export class RideHistoryComponent implements OnInit, OnDestroy  {
  sortedRides: Ride[] = [];
  selectedRide: Ride | null = null;
  private backendRides: Ride[] = [];
  
  dateFilter: string = '';
  filter: 'all' | 'Pending' | 'Accepted' | 'Finished' | 'Rejected' | 'Cancelled' = 'all';
  sortField: 'startDate' | 'endDate' | 'distance' | 'route' | 'passengers' = 'startDate';
  timeFilter: 'today' | 'week' | 'month' | 'all' = 'all';
  sortDirection: 'asc' | 'desc' = 'desc';
  
  private passengerId: number | null = null;
  private destroy$ = new Subject<void>();

  constructor(
    private router: Router,
    private rideService: RideService,
    private authService: AuthService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit() {
    this.passengerId = this.authService.getUserId();
    this.loadRides();
  }
  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadRides(): void {
    if (!this.passengerId) return;

    this.rideService.getRidesHistory({ passengerId: this.passengerId, page: 0, size: 100 }).pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (page) => {
          // Filter to only past rides (FINISHED, CANCELLED)
          const pastStatuses = ['FINISHED', 'CANCELLED', 'PENDING', 'SCHEDULED', 'ACCEPTED', 'ACTIVE', 'IN_PROGRESS', 'REJECTED', 'PANIC']; //TODO: adjust later if needed
          const relevant = (page.content ?? []).filter(r => pastStatuses.includes(r.status as string));
          console.log('Fetched rides from backend:', page.content);
          
          this.backendRides = relevant.map(r => this.mapToRide(r));
          console.log('Loaded rides:', this.backendRides);
          this.updateView();
          this.cdr.detectChanges();
        },
        error: (err) => console.error('Failed to load history', err)
      });
  }

  private mapToRide(r: RideResponse): Ride {
    return {
      id: String(r.id),
      driverId: String(r.driverId),
      startedAt: r.startTime,
      requestedAt: r.startTime ?? '', // Fallback or add field to RideResponse if needed
      completedAt: r.endTime,
      status: r.status === 'FINISHED' ? 'Finished' :
              r.status === 'CANCELLED' ? 'Cancelled' :
              r.status === 'PENDING' ? 'Pending' :
              r.status === 'ACCEPTED' ? 'Accepted' :
              r.status === 'REJECTED' ? 'Rejected' : 'all',
      fare: r.totalCost ?? 0,
      distance: r.distanceKm ?? 0,
      pickup: { address: r.departure?.address ?? r.start?.address ?? r.startLocation?.address ?? '—' },
      destination: { address: r.destination?.address ?? r.endLocation?.address ?? '—' },
      hasPanic: r.panicPressed,
      passengerName: r.passengers?.[0]?.name ?? 'Unknown',
      cancelledBy: 'driver', // TODO: fix this | simplified
      cancellationReason: r.rejectionReason
    };
  }

  setTimeFilter(period: 'today' | 'week' | 'month' | 'all') {
    this.timeFilter = period;
    this.updateView();
  }

  setFilter(status: 'all' | 'Pending' | 'Accepted' | 'Finished' | 'Rejected' | 'Cancelled') {
    this.filter = status;
    this.updateView();
  }

  updateView() {
    this.sortAndFilterRides();
  }

  handleSort(field: 'startDate' | 'endDate' | 'distance' | 'route' | 'passengers') {
    if (this.sortField === field) {
      this.sortDirection = this.sortDirection === 'asc' ? 'desc' : 'asc';
    } else {
      this.sortField = field;
      this.sortDirection = 'desc';
    }
    this.sortAndFilterRides();
  }

  sortAndFilterRides() {
    let filtered = this.backendRides;
    
    // Apply Time Filter
    filtered = this.filterByTime(filtered);

    // Apply Status Filter
    if (this.filter !== 'all') {
      console.log('Applying status filter:', this.filter);
      console.log('Rides before status filter:', filtered);
      filtered = filtered.filter(ride => ride.status === this.filter);
    }

    // Apply Specific Date Filter
    if (this.dateFilter) {
      const date = new Date(this.dateFilter);
      filtered = filtered.filter(ride => {
        const rideDate = new Date(ride.startedAt || ride.requestedAt);
        return rideDate.toDateString() === date.toDateString();
      });
    }

    console.log('Filtered rides:', filtered);
    this.sortedRides = filtered.sort((a, b) => {
      let aValue: any;
      let bValue: any;

      switch (this.sortField) {
        case 'startDate':
          aValue = new Date(a.startedAt || a.requestedAt).getTime();
          bValue = new Date(b.startedAt || b.requestedAt).getTime();
          break;
        case 'endDate':
          aValue = a.completedAt ? new Date(a.completedAt).getTime() : 0;
          bValue = b.completedAt ? new Date(b.completedAt).getTime() : 0;
          break;
        case 'distance':
          aValue = a.distance;
          bValue = b.distance;
          break;
        case 'route':
          aValue = a.pickup.address + a.destination.address;
          bValue = b.pickup.address + b.destination.address;
          break;
        case 'passengers':
          aValue = 1;
          bValue = 1;
          break;
        default:
          return 0;
      }

      if (aValue < bValue) return this.sortDirection === 'asc' ? -1 : 1;
      if (aValue > bValue) return this.sortDirection === 'asc' ? 1 : -1;
      return 0;
    });
  }

  filterByTime(rides: Ride[]): Ride[] {
    if (this.timeFilter === 'all') return rides;

    const today = new Date();
    
    return rides.filter(ride => {
      const rideDate = new Date(ride.startedAt || ride.requestedAt);
      
      if (this.timeFilter === 'today') {
        return rideDate.toDateString() === today.toDateString();
      }
      
      if (this.timeFilter === 'week') {
        const weekAgo = new Date(today);
        weekAgo.setDate(today.getDate() - 7);
        weekAgo.setHours(0, 0, 0, 0);
        return rideDate >= weekAgo && rideDate <= today;
      }
      
      if (this.timeFilter === 'month') {
        return rideDate.getMonth() === today.getMonth() && rideDate.getFullYear() === today.getFullYear();
      }
      
      return true;
    });
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

  onRideSelected(ride: Ride) {
    this.selectedRide = ride;
  }

  addToFavorites(ride: Ride) {
    if (!this.passengerId) {
      console.error('Passenger ID is not available');
      alert('Unable to add to favorites. Please log in again.');
      return;
    }

    const favoriteRouteRequest = {
      start: ride.pickup.address,
      destination: ride.destination.address,
      // TODO route name could be added here if needed
    };
    
    //console.log('Adding route to favorites:', favoriteRouteRequest);

    this.rideService.addRouteToFavorites(this.passengerId, favoriteRouteRequest).subscribe({
      next: () => {
        //console.log('Route added to favorites');
        alert('Route added to favorites successfully!');
        this.selectedRide = null;
      },
      error: (error) => {
        //console.error('Error adding route to favorites:', error);
        alert('Failed to add route to favorites. Please try again.');
      }
    });
  }
  
}
