import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { Ride } from '../../../shared/data/ride.model';
import { RideSummaryComponent } from '../../../shared/rides/ride-summary/ride-summary.component';
import { RideRouteComponent } from '../../../shared/rides/ride-route/ride-route.component';
import { RidePassengerComponent } from '../../../shared/rides/ride-passenger/ride-passenger.component';
import { RideMapComponent, RideMapData, MapPoint } from '../../../shared/rides/ride-map/ride-map.component';
import { RideVehicleComponent } from '../../../shared/rides/ride-vehicle/ride-vehicle.component';
import { RideTimelineComponent } from '../../../shared/rides/ride-timeline/ride-timeline.component';
import { RideService } from '../../../infrastructure/rest/ride.service';
import { RideResponse } from '../../../infrastructure/rest/model/ride-response.model';
import { CreateRideRequest } from '../../../infrastructure/rest/model/create-ride.model';

@Component({
  selector: 'app-ride-details',
  standalone: true,
  imports: [
    CommonModule,
    RideSummaryComponent,
    RideRouteComponent,
    RidePassengerComponent,
    RideMapComponent,
    RideVehicleComponent,
    RideTimelineComponent
  ],
  templateUrl: './ride-details.html',
  styleUrl: './ride-details.css'
})
export class RideDetails implements OnInit {
  ride: Ride | undefined;
  mapData: RideMapData | null = null;
  isLoading = true;
  errorMessage: string | null = null;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private rideService: RideService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit() {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.rideService.getRide(Number(id)).subscribe({
        next: (r) => {
          this.ride = this.mapToRide(r);
          this.buildMapData(r);
          this.isLoading = false;
          this.cdr.detectChanges();
        },
        error: (err) => {
          console.error(err);
          this.errorMessage = 'Failed to load ride details';
          this.isLoading = false;
          this.cdr.detectChanges();
        }
      });
    }
  }

  private mapToRide(r: RideResponse): Ride {
    const startLoc = r.departure ?? r.start ?? r.startLocation;
    const endLoc = r.destination ?? r.endLocation;
    const stops = r.stops ?? [];

    // Map passengers from backend with full details
    const passengers = (r.passengers ?? []).map(p => ({
      id: p.id,
      name: [p.name, p.surname].filter(Boolean).join(' ') || 'Unknown',
      email: p.email,
      phone: (p as any).phoneNumber || (p as any).phone
    }));

    // Cap distance to 2 decimal places
    const rawDistance = r.distanceKm ?? r.distanceTraveled ?? 0;
    const distance = Math.round(rawDistance * 100) / 100;

    return {
      id: String(r.id),
      driverId: String(r.driver?.id ?? r.driverId),
      startedAt: r.startTime,
      requestedAt: r.startTime ?? r.scheduledTime ?? '',
      completedAt: r.endTime,
      status: r.status === 'FINISHED' ? 'Finished' :
              r.status === 'CANCELLED' ? 'Cancelled' :
              r.status === 'CANCELLED_BY_DRIVER' ? 'Cancelled' :
              r.status === 'CANCELLED_BY_PASSENGER' ? 'Cancelled' :
              r.status === 'PENDING' ? 'Pending' :
              r.status === 'ACCEPTED' ? 'Accepted' :
              r.status === 'REJECTED' ? 'Rejected' : 'all',
      fare: r.totalCost ?? r.estimatedCost ?? 0,
      distance,
      pickup: { 
        address: startLoc?.address ?? '—',
        latitude: startLoc?.latitude,
        longitude: startLoc?.longitude
      },
      destination: { 
        address: endLoc?.address ?? '—',
        latitude: endLoc?.latitude,
        longitude: endLoc?.longitude
      },
      stops: stops.map(s => ({
        address: s.address,
        latitude: s.latitude,
        longitude: s.longitude
      })),
      hasPanic: r.panicPressed,
      passengerName: passengers[0]?.name ?? 'Unknown',
      passengerCount: passengers.length || 1,
      passengers,
      cancelledBy: r.status === 'CANCELLED_BY_DRIVER' ? 'driver' : 
                   r.status === 'CANCELLED_BY_PASSENGER' ? 'passenger' : 'driver',
      cancellationReason: r.rejectionReason
    };
  }

  private buildMapData(r: RideResponse): void {
    // Get start location
    const startLoc = r.departure ?? r.start ?? r.startLocation;
    const endLoc = r.destination ?? r.endLocation;
    const stops = r.stops ?? [];

    if (!startLoc || !this.isValidCoordinate(startLoc.latitude, startLoc.longitude)) {
      return;
    }
    if (!endLoc || !this.isValidCoordinate(endLoc.latitude, endLoc.longitude)) {
      return;
    }

    const start: MapPoint = { latitude: startLoc.latitude, longitude: startLoc.longitude };
    const end: MapPoint = { latitude: endLoc.latitude, longitude: endLoc.longitude };
    const stopPoints: MapPoint[] = stops
      .filter(s => this.isValidCoordinate(s.latitude, s.longitude))
      .map(s => ({ latitude: s.latitude, longitude: s.longitude }));

    // Initialize map data without route polyline - we'll fetch the detailed route
    this.mapData = {
      start,
      stops: stopPoints,
      end,
      routePolyline: undefined
    };

    // Always fetch detailed route from backend estimate endpoint to get road-following polyline
    this.fetchDetailedRoute(r);
  }

  private fetchDetailedRoute(r: RideResponse): void {
    const startLoc = r.departure ?? r.start ?? r.startLocation;
    const endLoc = r.destination ?? r.endLocation;
    const rideStops = r.stops ?? [];

    if (!startLoc || !endLoc) return;

    const request: CreateRideRequest = {
      start: startLoc,
      destination: endLoc,
      stops: rideStops,
      passengerEmails: [],
      scheduledTime: null,
      requirements: {
        vehicleType: r.vehicleType || 'STANDARD',
        babyTransport: r.babyTransport ?? false,
        petTransport: r.petTransport ?? false
      }
    };

    this.rideService.estimateRide(request).subscribe({
      next: (est) => {
        if (est.routePoints && est.routePoints.length > 0) {
          const sorted = est.routePoints.sort((a, b) => a.order - b.order);
          const routePolyline = sorted
            .filter(p => p.location && this.isValidCoordinate(p.location.latitude, p.location.longitude))
            .map(p => ({ latitude: p.location.latitude, longitude: p.location.longitude }));

          if (routePolyline.length >= 2 && this.mapData) {
            this.mapData = { ...this.mapData, routePolyline };
            this.cdr.detectChanges();
          }
        }
      },
      error: (err) => {
        console.error('Failed to fetch detailed route:', err);
        // Silently fail - we'll just show the coarse route
      }
    });
  }

  private isValidCoordinate(lat: any, lng: any): boolean {
    return (
      typeof lat === 'number' &&
      typeof lng === 'number' &&
      Number.isFinite(lat) &&
      Number.isFinite(lng) &&
      lat >= -90 && lat <= 90 &&
      lng >= -180 && lng <= 180
    );
  }

  goBack() {
    this.router.navigate(['/driver/overview']);
  }
}
