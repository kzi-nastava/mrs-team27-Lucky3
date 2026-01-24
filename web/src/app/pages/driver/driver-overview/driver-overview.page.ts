import { ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { Ride } from '../../../shared/data/mock-data';
import { RidesTableComponent } from '../../../shared/rides/rides-table/rides-table.component';
import { RideService } from '../../../infrastructure/rest/ride.service';
import { AuthService } from '../../../infrastructure/auth/auth.service';
import { Subject, takeUntil } from 'rxjs';
import { RideResponse } from '../../../infrastructure/rest/model/ride-response.model';

@Component({
  selector: 'app-driver-overview',
  standalone: true,
  imports: [CommonModule, FormsModule, RidesTableComponent],
  templateUrl: './driver-overview.page.html',
  styleUrl: './driver-overview.page.css'
})
export class DriverOverviewPage implements OnInit, OnDestroy {
  timeFilter: 'today' | 'week' | 'month' | 'all' = 'week';
  filter: 'all' | 'completed' | 'cancelled' = 'all';
  dateFilter: string = '';
  sortField: 'startDate' | 'endDate' | 'distance' | 'route' | 'passengers' = 'startDate';
  sortDirection: 'asc' | 'desc' = 'desc';

  sortedRides: Ride[] = [];
  private backendRides: Ride[] = [];
  
  stats = {
    totalEarnings: 0,
    earnings: 0, // legacy field?
    totalRides: 0,
    avgRating: 0,
    totalDistance: 0
  };

  driver = {
    ratingCount: 0 // Mock for now, until we fetch driver details
  };

  private destroy$ = new Subject<void>();
  private driverId: number | null = null;

  constructor(
    private router: Router,
    private rideService: RideService,
    private authService: AuthService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit() {
    this.driverId = this.authService.getUserId();
    this.loadRides();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadRides(): void {
    if (!this.driverId) return;

    this.rideService.getRidesHistory({ driverId: this.driverId, page: 0, size: 100 }).pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (page) => {
          // Filter to only past rides (FINISHED, CANCELLED)
          const pastStatuses = ['FINISHED', 'CANCELLED'];
          const relevant = (page.content ?? []).filter(r => pastStatuses.includes(r.status as string));
          
          this.backendRides = relevant.map(r => this.mapToRide(r));
          this.updateView();
          this.cdr.detectChanges();
        },
        error: (err) => console.error('Failed to load history', err)
      });
  }

  private mapToRide(r: RideResponse): Ride {
    return {
      id: String(r.id),
      driverId: String(this.driverId),
      startedAt: r.startTime,
      requestedAt: r.startTime ?? '', // Fallback or add field to RideResponse if needed
      completedAt: r.endTime,
      status: r.status === 'FINISHED' ? 'completed' : 'cancelled', // map enum
      fare: r.totalCost ?? 0,
      distance: r.distanceKm ?? 0,
      pickup: { address: r.departure?.address ?? r.start?.address ?? r.startLocation?.address ?? '—' },
      destination: { address: r.destination?.address ?? r.endLocation?.address ?? '—' },
      hasPanic: r.panicPressed,
      passengerName: r.passengers?.[0]?.name ?? 'Unknown',
      cancelledBy: 'driver', // simplified
      cancellationReason: r.rejectionReason
    };
  }

  updateView() {
    this.sortAndFilterRides();
    this.calculateStats();
  }

  calculateStats() {
    let statsRides = this.backendRides;
    statsRides = this.filterByTime(statsRides);

    // Only count completed rides for earnings and distance
    const completedRides = statsRides.filter(r => r.status === 'completed');

    this.stats = {
      totalEarnings: completedRides.reduce((sum, r) => sum + r.fare, 0),
      earnings: 0,
      totalRides: statsRides.length,
      // avgRating: this.driver.rating, // need rating endpoint
      avgRating: 0,
      totalDistance: completedRides.reduce((sum, r) => sum + r.distance, 0)
    };
  }

  sortAndFilterRides() {
    let filtered = this.backendRides;
    
    // Apply Time Filter
    filtered = this.filterByTime(filtered);

    // Apply Status Filter
    if (this.filter !== 'all') {
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

  handleSort(field: 'startDate' | 'endDate' | 'distance' | 'route' | 'passengers') {
    if (this.sortField === field) {
      this.sortDirection = this.sortDirection === 'asc' ? 'desc' : 'asc';
    } else {
      this.sortField = field;
      this.sortDirection = 'desc';
    }
    this.sortAndFilterRides();
  }

  setTimeFilter(period: 'today' | 'week' | 'month' | 'all') {
    this.timeFilter = period;
    this.updateView();
  }

  setFilter(status: 'all' | 'completed' | 'cancelled') {
    this.filter = status;
    this.updateView();
  }

  onViewDetails(rideId: string) {
    this.router.navigate(['/driver/overview/ride', rideId]);
  }
}
