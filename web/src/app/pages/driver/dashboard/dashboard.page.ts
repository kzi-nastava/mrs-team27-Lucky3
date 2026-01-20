import { Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { StatCardComponent } from '../../../shared/ui/stat-card/stat-card.component';
import { ToggleSwitchComponent } from '../../../shared/ui/toggle-switch/toggle-switch.component';
import { RideRequestCardComponent } from '../../../shared/rides/ride-request-card/ride-request-card.component';
import { LiveMapComponent, MapPoint } from '../../../shared/ui/live-map/live-map.component';
import { VehicleService } from '../../../infrastructure/rest/vehicle.service';
import { AuthService } from '../../../infrastructure/auth/auth.service';
import { FutureRide, mockFutureRides } from '../../../shared/data/future-rides';
import { RideService } from '../../../infrastructure/rest/ride.service';

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

  private refreshInterval: any;
  private driverId: number | null = null;
  
  stats = {
    earnings: 245.5,
    ridesCompleted: 12,
    rating: 4.9,
    onlineHours: 6.5
  };

  futureRides: FutureRide[] = [...mockFutureRides];

  get nextRide(): FutureRide | null {
    return this.futureRides.length > 0 ? this.futureRides[0] : null;
  }

  get dashboardRides(): FutureRide[] {
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
    this.fetchDriverLocation();
    this.refreshInterval = setInterval(() => this.fetchDriverLocation(), 10000);
  }

  ngOnDestroy(): void {
    if (this.refreshInterval) {
      clearInterval(this.refreshInterval);
    }
  }

  onStatusChange(status: boolean) {
    this.isOnline = status;
  }

  toggleAllFutureRides(): void {
    this.showAllFutureRides = !this.showAllFutureRides;
  }

  cancelRide(id: string): void {
    const previous = [...this.futureRides];
    this.futureRides = this.futureRides.filter(r => r.id !== id);

    const numericId = Number(id);
    if (!Number.isFinite(numericId)) return;

    this.rideService.cancelRide(numericId, { reason: 'Driver cancelled ride' }).subscribe({
      error: () => {
        // revert on failure
        this.futureRides = previous;
      }
    });
  }

  private fetchDriverLocation(): void {
    if (!this.driverId) return;

    this.vehicleService.getActiveVehicles().subscribe({
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
