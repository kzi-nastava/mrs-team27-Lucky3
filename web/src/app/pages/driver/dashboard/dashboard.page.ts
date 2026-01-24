import { ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { StatCardComponent } from '../../../shared/ui/stat-card/stat-card.component';
import { ToggleSwitchComponent } from '../../../shared/ui/toggle-switch/toggle-switch.component';
import { RideRequestCardComponent } from '../../../shared/rides/ride-request-card/ride-request-card.component';
import { LiveMapComponent, MapPoint } from '../../../shared/ui/live-map/live-map.component';
import { VehicleService } from '../../../infrastructure/rest/vehicle.service';
import { AuthService } from '../../../infrastructure/auth/auth.service';
import { RideService, CreateRideRequest } from '../../../infrastructure/rest/ride.service';
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
    LiveMapComponent
  ],
  templateUrl: './dashboard.page.html',
  styleUrls: ['./dashboard.page.css']
})
export class DashboardPage implements OnInit, OnDestroy {
  isOnline: boolean = true;
  driverName: string = 'James';

  driverLocation: MapPoint | null = null;
  showAllFutureRides = false;

  // Routes for map
  rideRoute: MapPoint[] | null = null;
  approachRoute: MapPoint[] | null = null;

  private readonly destroy$ = new Subject<void>();
  private driverId: number | null = null;
  
  stats = {
    earnings: 245.5,
    ridesCompleted: 12,
    rating: 4.9,
    onlineHours: 6.5
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
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.driverId = this.authService.getUserId();

    // Driver live location polling
    timer(0, 10000)
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => this.fetchDriverLocation());

    // Upcoming rides (backend)
    this.loadFutureRides();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  onStatusChange(status: boolean) {
    this.isOnline = status;
  }

  toggleAllFutureRides(): void {
    this.showAllFutureRides = !this.showAllFutureRides;
  }

  cancelRide(id: number): void {
    const previous = [...this.futureRides];
    this.futureRides = this.futureRides.filter(r => r.id !== id);

    this.rideService.cancelRide(id, { reason: 'Driver cancelled ride' }).subscribe({
      error: () => {
        // revert on failure
        this.futureRides = previous;
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
             this.rideRoute = null;
             this.approachRoute = null;
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

    return {
      id: Number(r.id),
      type,
      price,
      distance,
      time,
      pickup,
      dropoff,
      scheduledTime: r.scheduledTime
    };
  }

  private fetchRoutesForRide(ride: RideResponse): void {
      // 1. Get Ride Route (Yellow)
      // If backend gave detailed route points, use them. Else, estimate.
      if (ride.routePoints && ride.routePoints.length > 2) {
          this.rideRoute = ride.routePoints
            .sort((a, b) => a.order - b.order)
            .map(p => ({ latitude: p.location.latitude, longitude: p.location.longitude }));
      } else {
         // Fallback estimate ride itself if missing
         const start = ride.departure ?? ride.start ?? ride.startLocation;
         const end = ride.destination ?? ride.endLocation;
         if (start && end) {
             const req: CreateRideRequest = {
                 start, destination: end, stops: ride.stops || [],
                 passengerEmails: [], scheduledTime: null,
                 requirements: { vehicleType: 'STANDARD', babyTransport: false, petTransport: false }
             };
             this.rideService.estimateRide(req).subscribe(res => {
                if(res.routePoints?.length) {
                    this.rideRoute = res.routePoints.map(p => ({ latitude: p.location.latitude, longitude: p.location.longitude }));
                    this.cdr.detectChanges();
                }
             });
         }
      }

      // 2. Approach Route (Light Blue) - Vehicle to Pickup
      // Show approach route for any non-FINISHED ride
      
      const shouldShowApproach = ride.status !== 'FINISHED' && ride.status !== 'CANCELLED';
      
      // Prefer vehicleLocation from ride response (from backend), else use driver's polled location
      const vehicleLoc = ride.vehicleLocation ?? 
        (this.driverLocation ? { address: '', latitude: this.driverLocation.latitude, longitude: this.driverLocation.longitude } : null);
      
      // Update driver marker position from vehicle location if available from ride
      // Use setTimeout to avoid ExpressionChangedAfterItHasBeenCheckedError
      if (ride.vehicleLocation && !this.driverLocation) {
          setTimeout(() => {
              this.driverLocation = { latitude: ride.vehicleLocation!.latitude, longitude: ride.vehicleLocation!.longitude };
              this.cdr.detectChanges();
          }, 0);
      }
      
      const pickup = ride.departure ?? ride.start ?? ride.startLocation;
      
      if (shouldShowApproach && vehicleLoc && pickup) {
          // Calculate approach route using API (same as yellow line)
          const req: CreateRideRequest = {
              start: { address: vehicleLoc.address || '', latitude: vehicleLoc.latitude, longitude: vehicleLoc.longitude },
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
