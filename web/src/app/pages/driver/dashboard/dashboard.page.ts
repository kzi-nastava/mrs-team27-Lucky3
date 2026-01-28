import { ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { StatCardComponent } from '../../../shared/ui/stat-card/stat-card.component';
import { ToggleSwitchComponent } from '../../../shared/ui/toggle-switch/toggle-switch.component';
import { RideRequestCardComponent } from '../../../shared/rides/ride-request-card/ride-request-card.component';
import { ActiveRideMapComponent, ActiveRideMapData, MapPoint } from '../../../shared/ui/active-ride-map/active-ride-map.component';
import { VehicleService } from '../../../infrastructure/rest/vehicle.service';
import { AuthService } from '../../../infrastructure/auth/auth.service';
import { RideService } from '../../../infrastructure/rest/ride.service';
import { DriverService } from '../../../infrastructure/rest/driver.service';
import { ToastComponent } from '../../../shared/ui/toast/toast.component';
import { CreateRideRequest } from '../../../infrastructure/rest/model/create-ride.model';
import { Subject, takeUntil, timer } from 'rxjs';
import { RideResponse } from '../../../infrastructure/rest/model/ride-response.model';

type DashboardRide = {
  id: number;
  type: string;
  price: number;
  distance: string;
  time: string;
  pickup: string;
  dropoff: string;
  status: string;
  scheduledTime?: string;
};

@Component({
  selector: 'app-driver-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    StatCardComponent,
    ToggleSwitchComponent,
    RideRequestCardComponent,
    ActiveRideMapComponent,
    ToastComponent
  ],
  templateUrl: './dashboard.page.html',
  styleUrls: ['./dashboard.page.css']
})
export class DashboardPage implements OnInit, OnDestroy {
  isOnline: boolean = false;
  isStatusLoading: boolean = false;
  isInactiveRequested: boolean = false;  // Track when driver requested to go offline but has active ride
  workingHoursExceeded: boolean = false;  // Track if 8h limit exceeded
  workedHoursToday: string = '0h 0m';      // Current worked hours
  driverName: string = 'James';

  // Toast notification
  toastMessage: string = '';
  toastType: 'success' | 'error' | 'warning' | 'info' = 'info';
  showToast: boolean = false;

  driverLocation: MapPoint | null = null;
  showAllFutureRides = false;

  // Map data for ActiveRideMapComponent
  rideMapData: ActiveRideMapData | null = null;
  routePolyline: MapPoint[] | null = null;
  approachRoute: MapPoint[] | null = null;
  remainingRoute: MapPoint[] | null = null;
  completedStopIndexes: Set<number> = new Set();
  isNextRideInProgress: boolean = false;
  panicActivated: boolean = false; // Track if current ride has panic activated

  private readonly destroy$ = new Subject<void>();
  private driverId: number | null = null;
  
  stats = {
    earnings: 0,
    ridesCompleted: 0,
    rating: 0,
    onlineHours: '0h 0m'
  };

  futureRides: DashboardRide[] = [];

  get nextRide(): DashboardRide | null {
    return this.futureRides.length > 0 ? this.futureRides[0] : null;
  }

  get dashboardRides(): DashboardRide[] {
    // Next ride is shown in its own card, so dashboard list shows the next two.
    return this.futureRides.slice(1, 3);
  }

  constructor(
    private vehicleService: VehicleService,
    private authService: AuthService,
    private rideService: RideService,
    private driverService: DriverService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.driverId = this.authService.getUserId();

    // Load current driver status from backend
    this.loadDriverStatus();
    
    // Load driver statistics
    this.loadDriverStats();

    // Listen for status refresh events (e.g., when a ride ends)
    this.driverService.onStatusRefresh
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => {
        this.loadDriverStatus();
        this.loadFutureRides();
        this.loadDriverStats();
        // Clear the buffer so we don't re-process on next navigation
        this.driverService.clearStatusRefresh();
      });

    // Driver live location polling
    timer(0, 10000)
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => this.fetchDriverLocation());

    // Stats polling every 60 seconds (to update online hours in real-time)
    timer(60000, 60000)
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => this.loadDriverStats());

    // Upcoming rides (backend)
    this.loadFutureRides();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private loadDriverStatus(): void {
    if (!this.driverId) return;

    this.driverService.getStatus(this.driverId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (status) => {
          this.isOnline = status.active;
          this.isInactiveRequested = status.inactiveRequested;
          this.workingHoursExceeded = status.workingHoursExceeded || false;
          this.workedHoursToday = status.workedHoursToday || '0h 0m';
          
          // If driver is truly offline (not active and not inactive requested), clear ride data
          if (!status.active && !status.inactiveRequested) {
            this.clearRideData();
          }
          
          this.cdr.markForCheck();
        },
        error: () => {
          // Default to offline on error
          this.isOnline = false;
          this.isInactiveRequested = false;
          this.workingHoursExceeded = false;
          this.workedHoursToday = '0h 0m';
          this.clearRideData();
          this.cdr.markForCheck();
        }
      });
  }
  
  private clearRideData(): void {
    this.rideMapData = null;
    this.routePolyline = null;
    this.approachRoute = null;
    this.remainingRoute = null;
    this.completedStopIndexes = new Set();
    this.isNextRideInProgress = false;
    this.panicActivated = false;
    this.futureRides = [];
    this.rawRides = [];
  }

  private loadDriverStats(): void {
    if (!this.driverId) return;

    this.driverService.getStats(this.driverId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (stats) => {
          this.stats = {
            earnings: stats.totalEarnings,
            ridesCompleted: stats.completedRides,
            rating: stats.averageRating,
            onlineHours: stats.onlineHoursToday
          };
          this.cdr.markForCheck();
        },
        error: () => {
          // Keep default stats on error
        }
      });
  }

  onStatusChange(newStatus: boolean): void {
    if (!this.driverId || this.isStatusLoading) {
      // Revert if no driver ID
      this.isOnline = !newStatus;
      return;
    }

    // Prevent going online if working hours exceeded (extra frontend safeguard)
    if (newStatus && this.workingHoursExceeded) {
      this.displayToast('Cannot go online: You have exceeded the 8-hour working limit in the last 24 hours. Please rest before continuing.', 'error');
      return;
    }

    this.isStatusLoading = true;
    const previousStatus = this.isOnline;

    this.driverService.toggleStatus(this.driverId, newStatus)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response) => {
          this.isOnline = response.active;
          this.isInactiveRequested = response.inactiveRequested;
          this.workingHoursExceeded = response.workingHoursExceeded || false;
          this.workedHoursToday = response.workedHoursToday || '0h 0m';
          this.isStatusLoading = false;
          
          if (response.inactiveRequested) {
            // Driver requested to go offline but has active ride
            // Keep showing as online until ride completes
            this.displayToast('You have an active ride. You will go offline once the ride is complete.', 'warning');
          } else if (response.active) {
            this.displayToast('You are now online and available for rides.', 'success');
            // Reload rides when coming online
            this.loadFutureRides();
          } else {
            this.displayToast('You are now offline.', 'info');
            // Clear ride data when going offline
            this.clearRideData();
          }
          
          this.cdr.markForCheck();
        },
        error: (err) => {
          // Revert to previous status
          this.isOnline = previousStatus;
          this.isStatusLoading = false;
          
          // Check for 8-hour limit error
          const errorMessage = err.message || '';
          if (errorMessage.includes('8-hour') || errorMessage.includes('working limit')) {
            this.workingHoursExceeded = true;
            this.displayToast('Cannot go online: You have exceeded the 8-hour working limit. Please rest before continuing.', 'error');
          } else {
            this.displayToast(errorMessage || 'Failed to change status. Please try again.', 'error');
          }
          
          this.cdr.markForCheck();
        }
      });
  }

  displayToast(message: string, type: 'success' | 'error' | 'warning' | 'info'): void {
    this.toastMessage = message;
    this.toastType = type;
    this.showToast = true;
  }

  onToastClose(): void {
    this.showToast = false;
  }

  toggleAllFutureRides(): void {
    this.showAllFutureRides = !this.showAllFutureRides;
  }

  cancelRide(id: number): void {
    const reason = window.prompt('Please enter a reason for cancellation:');
    if (!reason) return;

    const previous = [...this.futureRides];
    this.futureRides = this.futureRides.filter(r => r.id !== id);

    this.rideService.cancelRide(id, { reason }).subscribe({
      error: () => {
        // revert on failure
        this.futureRides = previous;
        alert('Failed to cancel the ride. Please try again.');
      }
    });
  }

  private loadFutureRides(): void {
    if (!this.driverId) {
      this.futureRides = [];
      return;
    }

    this.rideService
      .getRidesHistory({ driverId: this.driverId, page: 0, size: 20 })
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (page) => {
          const validStatuses = ['PENDING', 'ACCEPTED', 'IN_PROGRESS', 'SCHEDULED'];
          const relevant = (page.content ?? []).filter(r => validStatuses.includes(r.status as string));
          
          this.rawRides = relevant;

          const mapped = relevant.map(r => this.toDashboardRide(r));
          
          // Sort: IN_PROGRESS first, then by scheduledTime
          mapped.sort((a, b) => {
             // Find original objects to check status
             const rideA = relevant.find(r => r.id === a.id);
             const rideB = relevant.find(r => r.id === b.id);
             
             const statusA = rideA?.status === 'IN_PROGRESS' ? 0 : 1;
             const statusB = rideB?.status === 'IN_PROGRESS' ? 0 : 1;
             
             if (statusA !== statusB) return statusA - statusB;
             
             return (a.scheduledTime ?? '').localeCompare(b.scheduledTime ?? '');
          });
          
          this.futureRides = mapped;
          
          // If we have an active/pending ride, fetch its details to draw routes
          const activeRide = relevant.find(r => r.id === mapped[0]?.id);
          if (activeRide) {
             this.fetchRoutesForRide(activeRide);
          } else {
             this.rideMapData = null;
             this.routePolyline = null;
             this.approachRoute = null;
             this.remainingRoute = null;
             this.completedStopIndexes = new Set();
             this.isNextRideInProgress = false;
          }

          this.cdr.detectChanges();
        },
        error: () => {
          // keep previous list on failure
        }
      });
  }

  private toDashboardRide(r: RideResponse): DashboardRide {
    const pickup = r.departure?.address ?? r.start?.address ?? r.startLocation?.address ?? '—';
    const dropoff = r.destination?.address ?? r.endLocation?.address ?? '—';

    const distanceKm = r.distanceKm ?? r.estimatedDistance;
    const distance = distanceKm != null ? `${Number(distanceKm).toFixed(1)} km` : '—';

    const mins = r.estimatedTimeInMinutes;
    const time = mins != null ? `${Math.round(Number(mins))} min` : '—';

    const price = Number(r.totalCost ?? r.estimatedCost ?? 0);
    const type = String(r.vehicleType ?? '—');
    const status = String(r.status ?? 'PENDING');

    return {
      id: Number(r.id),
      type,
      price,
      distance,
      time,
      pickup,
      dropoff,
      status,
      scheduledTime: r.scheduledTime
    };
  }

  private fetchRoutesForRide(ride: RideResponse): void {
      const start = ride.departure ?? ride.start ?? ride.startLocation;
      const end = ride.destination ?? ride.endLocation;
      const stops = ride.stops ?? [];
      
      this.isNextRideInProgress = ride.status === 'IN_PROGRESS';
      this.completedStopIndexes = new Set(ride.completedStopIndexes ?? []);
      this.panicActivated = ride.panicPressed === true;

      // Build rideMapData for the map component
      if (start && end) {
          this.rideMapData = {
              start: { latitude: start.latitude, longitude: start.longitude },
              stops: stops.map(s => ({ latitude: s.latitude, longitude: s.longitude })),
              end: { latitude: end.latitude, longitude: end.longitude }
          };
      } else {
          this.rideMapData = null;
      }

      // Get detailed route polyline
      if (ride.routePoints && ride.routePoints.length > 2) {
          this.routePolyline = ride.routePoints
            .sort((a, b) => a.order - b.order)
            .map(p => ({ latitude: p.location.latitude, longitude: p.location.longitude }));
      } else if (start && end) {
         const req: CreateRideRequest = {
             start, destination: end, stops: stops,
             passengerEmails: [], scheduledTime: null,
             requirements: { vehicleType: 'STANDARD', babyTransport: false, petTransport: false }
         };
         this.rideService.estimateRide(req).subscribe(res => {
            if(res.routePoints?.length) {
                this.routePolyline = res.routePoints
                  .sort((a, b) => a.order - b.order)
                  .map(p => ({ latitude: p.location.latitude, longitude: p.location.longitude }));
                this.cdr.detectChanges();
            }
         });
      }

      // Prefer vehicleLocation from ride response, else use driver's polled location
      const vehicleLoc = ride.vehicleLocation ?? 
        (this.driverLocation ? { address: 'Current Location', latitude: this.driverLocation.latitude, longitude: this.driverLocation.longitude } : null);
      
      // Update driver marker position from vehicle location if available from ride
      if (ride.vehicleLocation && !this.driverLocation) {
          setTimeout(() => {
              this.driverLocation = { latitude: ride.vehicleLocation!.latitude, longitude: ride.vehicleLocation!.longitude };
              this.cdr.detectChanges();
          }, 0);
      }

      if (this.isNextRideInProgress) {
          // For in-progress rides: calculate approach (vehicle -> next stop) and remaining (next stop -> end)
          this.fetchApproachRouteForInProgress(ride, vehicleLoc);
          this.fetchRemainingRoute(ride);
      } else {
          // For pending rides: calculate approach (vehicle -> pickup), no remaining route
          this.remainingRoute = null;
          const pickup = ride.departure ?? ride.start ?? ride.startLocation;
          
          if (vehicleLoc && pickup) {
              const req: CreateRideRequest = {
                  start: { address: vehicleLoc.address || 'Current Location', latitude: vehicleLoc.latitude, longitude: vehicleLoc.longitude },
                  destination: pickup,
                  stops: [],
                  passengerEmails: [], scheduledTime: null,
                  requirements: { vehicleType: 'STANDARD', babyTransport: false, petTransport: false }
              };
              
              this.rideService.estimateRide(req).subscribe({
                  next: (res) => {
                      if (res.routePoints?.length) {
                          this.approachRoute = res.routePoints
                            .sort((a, b) => a.order - b.order)
                            .map(p => ({ latitude: p.location.latitude, longitude: p.location.longitude }));
                          this.cdr.detectChanges();
                      }
                  },
                  error: () => {
                    this.approachRoute = null;
                    this.cdr.detectChanges();
                  }
              });
          } else {
              this.approachRoute = null;
          }
      }
  }

  private fetchApproachRouteForInProgress(ride: RideResponse, vehicleLoc: { address: string; latitude: number; longitude: number } | null): void {
      if (!vehicleLoc) {
          this.approachRoute = null;
          return;
      }

      const stops = ride.stops ?? [];
      const end = ride.destination ?? ride.endLocation;

      // Find next uncompleted stop, or end if all stops completed
      let nextDestination: { address: string; latitude: number; longitude: number } | null = null;
      let nextStopIndex = -1;
      
      for (let i = 0; i < stops.length; i++) {
          if (!this.completedStopIndexes.has(i)) {
              nextStopIndex = i;
              break;
          }
      }

      if (nextStopIndex >= 0) {
          nextDestination = stops[nextStopIndex];
      } else if (end) {
          nextDestination = end;
      }

      if (!nextDestination) {
          this.approachRoute = null;
          return;
      }

      const req: CreateRideRequest = {
          start: { address: vehicleLoc.address || 'Current Location', latitude: vehicleLoc.latitude, longitude: vehicleLoc.longitude },
          destination: nextDestination,
          stops: [],
          passengerEmails: [], scheduledTime: null,
          requirements: { vehicleType: 'STANDARD', babyTransport: false, petTransport: false }
      };

      this.rideService.estimateRide(req).subscribe({
          next: (res) => {
              if (res.routePoints?.length) {
                  this.approachRoute = res.routePoints
                    .sort((a, b) => a.order - b.order)
                    .map(p => ({ latitude: p.location.latitude, longitude: p.location.longitude }));
                  this.cdr.detectChanges();
              }
          },
          error: () => {
              this.approachRoute = null;
          }
      });
  }

  private fetchRemainingRoute(ride: RideResponse): void {
      const stops = ride.stops ?? [];
      const end = ride.destination ?? ride.endLocation;

      if (!end) {
          this.remainingRoute = null;
          return;
      }

      // Find the first uncompleted stop
      let nextStopIndex = -1;
      for (let i = 0; i < stops.length; i++) {
          if (!this.completedStopIndexes.has(i)) {
              nextStopIndex = i;
              break;
          }
      }

      // If all stops are completed, no remaining route needed (approach goes directly to end)
      if (nextStopIndex === -1) {
          this.remainingRoute = null;
          return;
      }

      // Yellow line starts from the next uncompleted stop
      const yellowStartPoint = stops[nextStopIndex];

      // Gather remaining uncompleted stops (excluding the first one which is the start)
      const remainingUncompletedStops: { address: string; latitude: number; longitude: number }[] = [];
      for (let i = nextStopIndex + 1; i < stops.length; i++) {
          if (!this.completedStopIndexes.has(i)) {
              remainingUncompletedStops.push(stops[i]);
          }
      }

      const req: CreateRideRequest = {
          start: yellowStartPoint,
          destination: end,
          stops: remainingUncompletedStops,
          passengerEmails: [],
          scheduledTime: null,
          requirements: { vehicleType: 'STANDARD', babyTransport: false, petTransport: false }
      };

      this.rideService.estimateRide(req).subscribe({
          next: (res) => {
              if (res.routePoints?.length) {
                  this.remainingRoute = res.routePoints
                    .sort((a, b) => a.order - b.order)
                    .map(p => ({ latitude: p.location.latitude, longitude: p.location.longitude }));
                  this.cdr.detectChanges();
              }
          },
          error: () => {
              this.remainingRoute = null;
          }
      });
  }

  private fetchDriverLocation(): void {
    if (!this.driverId) return;

    this.vehicleService
      .getActiveVehicles()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
      next: (vehicles) => {
        const mine = vehicles.find(v => v.driverId === this.driverId);
        if (mine) {
           this.driverLocation = { latitude: mine.latitude, longitude: mine.longitude };
           
           if (this.nextRide && !this.approachRoute) {
               this.reloadRoutesIfMissing();
           }
           this.cdr.detectChanges();
        } else {
           this.driverLocation = null;
           this.cdr.detectChanges();
        }
      },
      error: () => {
        // Keep last known location if the request fails.
      }
    });
  }

  // Cache of raw ride objects
  private rawRides: RideResponse[] = [];

  private reloadRoutesIfMissing() {
      if (!this.futureRides.length) return;
      const activeId = this.futureRides[0].id;
      const raw = this.rawRides.find(r => r.id === activeId);
      if (raw) {
          this.fetchRoutesForRide(raw);
      }
  }
}
