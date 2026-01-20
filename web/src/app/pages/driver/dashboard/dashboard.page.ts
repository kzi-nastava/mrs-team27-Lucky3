import { Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { StatCardComponent } from '../../../shared/ui/stat-card/stat-card.component';
import { ToggleSwitchComponent } from '../../../shared/ui/toggle-switch/toggle-switch.component';
import { RideRequestCardComponent } from '../../../shared/rides/ride-request-card/ride-request-card.component';
import { LiveMapComponent, MapPoint } from '../../../shared/ui/live-map/live-map.component';
import { VehicleService } from '../../../infrastructure/rest/vehicle.service';
import { AuthService } from '../../../infrastructure/auth/auth.service';
import { RideService } from '../../../infrastructure/rest/ride.service';
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
    private rideService: RideService
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
      .getRidesHistory({ driverId: this.driverId, status: 'ACCEPTED', page: 0, size: 20 })
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (page) => {
          const mapped = (page.content ?? []).map(r => this.toDashboardRide(r));
          mapped.sort((a, b) => (a.scheduledTime ?? '').localeCompare(b.scheduledTime ?? ''));
          this.futureRides = mapped;
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

  private fetchDriverLocation(): void {
    if (!this.driverId) return;

    this.vehicleService
      .getActiveVehicles()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
      next: (vehicles) => {
        const mine = vehicles.find(v => v.driverId === this.driverId);
        this.driverLocation = mine
          ? { latitude: mine.latitude, longitude: mine.longitude }
          : null;
      },
      error: () => {
        // Keep last known location if the request fails.
      }
    });
  }
}
