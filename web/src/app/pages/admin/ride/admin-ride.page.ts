import { ChangeDetectorRef, Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { Subject, Subscription, takeUntil, debounceTime, switchMap, of, catchError, Observable } from 'rxjs';
import { HttpClient } from '@angular/common/http';

import { RideService } from '../../../infrastructure/rest/ride.service';
import { RideResponse } from '../../../infrastructure/rest/model/ride-response.model';
import { CreateRideRequest } from '../../../infrastructure/rest/model/create-ride.model';
import { ActiveRideMapComponent, ActiveRideMapData, MapPoint } from '../../../shared/ui/active-ride-map/active-ride-map.component';
import { SocketService } from '../../../infrastructure/rest/socket.service';
import { environment } from '../../../../env/environment';

@Component({
  selector: 'app-admin-ride-page',
  standalone: true,
  imports: [CommonModule, ActiveRideMapComponent],
  templateUrl: './admin-ride.page.html',
})
export class AdminRidePage implements OnInit, OnDestroy {
  ride: RideResponse | null = null;
  isLoading = true;
  errorMessage: string | null = null;

  // Map data
  rideMapData: ActiveRideMapData | null = null;
  routePolyline: MapPoint[] | null = null;
  approachRoute: MapPoint[] | null = null;
  remainingRoute: MapPoint[] | null = null;
  driverLocation: MapPoint | null = null;
  completedStopIndexes: Set<number> = new Set();
  panicActivated: boolean = false;

  // Panic modal
  showPanicModal = false;

  private destroy$ = new Subject<void>();
  private locationSubscription: Subscription | null = null;
  private locationUpdates$ = new Subject<{ ride: RideResponse; location: MapPoint }>();
  private routeUpdateSubscription: Subscription | null = null;
  private ridePoller: any;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private rideService: RideService,
    private socketService: SocketService,
    private http: HttpClient,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit() {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.loadRide(Number(id));
      this.setupRouteUpdates();
    } else {
      this.errorMessage = 'No ride ID provided';
      this.isLoading = false;
    }
  }

  ngOnDestroy(): void {
    if (this.locationSubscription) {
      this.locationSubscription.unsubscribe();
    }
    if (this.routeUpdateSubscription) {
      this.routeUpdateSubscription.unsubscribe();
    }
    if (this.ridePoller) {
      clearInterval(this.ridePoller);
    }
    this.destroy$.next();
    this.destroy$.complete();
  }

  private loadRide(id: number): void {
    this.rideService.getRide(id).subscribe({
      next: (ride) => {
        this.ride = ride;
        this.panicActivated = ride.panicPressed ?? false;
        this.completedStopIndexes = new Set(ride.completedStopIndexes ?? []);
        this.buildMapData(ride);
        this.isLoading = false;
        
        // Subscribe to vehicle location if ride is active
        if (this.isRideActive) {
          this.subscribeToVehicleLocation(ride);
          this.startRidePolling(id);
        }
        
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Error loading ride:', err);
        this.errorMessage = 'Failed to load ride details';
        this.isLoading = false;
        this.cdr.detectChanges();
      }
    });
  }

  private startRidePolling(rideId: number): void {
    // Poll ride data every 10 seconds to get updated status, cost, etc.
    this.ridePoller = setInterval(() => {
      this.rideService.getRide(rideId).subscribe({
        next: (ride) => {
          this.ride = ride;
          this.panicActivated = ride.panicPressed ?? false;
          this.completedStopIndexes = new Set(ride.completedStopIndexes ?? []);
          
          // Stop polling if ride is no longer active
          if (!this.isRideActive) {
            clearInterval(this.ridePoller);
          }
          
          this.cdr.detectChanges();
        }
      });
    }, 10000);
  }

  private subscribeToVehicleLocation(ride: RideResponse): void {
    const vehicleId = ride.driver?.vehicle ? (ride.driver as any).vehicleId : null;
    const driverId = ride.driver?.id;
    
    if (!vehicleId && !driverId) return;

    // Unsubscribe from previous subscription
    if (this.locationSubscription) {
      this.locationSubscription.unsubscribe();
    }

    // Subscribe to vehicle location updates via WebSocket
    const subId = vehicleId || driverId;
    this.locationSubscription = this.socketService.getVehicleLocationUpdates(subId!)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (location) => {
          if (location && location.latitude && location.longitude) {
            const newLocation: MapPoint = {
              latitude: location.latitude,
              longitude: location.longitude
            };
            this.driverLocation = newLocation;
            
            // Trigger route update
            this.locationUpdates$.next({ ride: this.ride!, location: newLocation });
            this.cdr.detectChanges();
          }
        }
      });
  }

  private setupRouteUpdates(): void {
    // Debounce route updates to avoid excessive API calls
    this.routeUpdateSubscription = this.locationUpdates$.pipe(
      debounceTime(2000),
      switchMap(({ ride, location }) => this.calculateRouteFromLocation(ride, location)),
      takeUntil(this.destroy$)
    ).subscribe({
      next: (result) => {
        if (result) {
          this.approachRoute = result.approachRoute;
          this.remainingRoute = result.remainingRoute;
          this.cdr.detectChanges();
        }
      }
    });
  }

  private calculateRouteFromLocation(ride: RideResponse, location: MapPoint): Observable<{ approachRoute: MapPoint[]; remainingRoute: MapPoint[] } | null> {
    const startLoc = ride.departure ?? ride.start ?? ride.startLocation;
    const endLoc = ride.destination ?? ride.endLocation;
    const stops = ride.stops ?? [];
    
    if (!endLoc) return of(null);

    // Determine next destination based on completed stops
    const allPoints = [startLoc, ...stops, endLoc];
    const completedCount = this.completedStopIndexes.size;
    const nextDestIdx = completedCount + 1; // +1 because start is at 0
    
    if (nextDestIdx >= allPoints.length) return of(null);
    
    const nextDest = allPoints[nextDestIdx];
    if (!nextDest || !this.isValidCoordinate(nextDest.latitude, nextDest.longitude)) return of(null);

    // Build request for route from current location to end (through remaining stops)
    const remainingStops = stops.slice(completedCount);
    
    const request: CreateRideRequest = {
      start: { latitude: location.latitude, longitude: location.longitude, address: '' },
      destination: endLoc,
      stops: remainingStops,
      passengerEmails: [],
      scheduledTime: null,
      requirements: {
        vehicleType: ride.vehicleType || 'STANDARD',
        babyTransport: ride.babyTransport ?? false,
        petTransport: ride.petTransport ?? false
      }
    };

    return this.rideService.estimateRide(request).pipe(
      switchMap(est => {
        if (!est.routePoints || est.routePoints.length < 2) return of(null);
        
        const sorted = est.routePoints.sort((a, b) => a.order - b.order);
        const fullRoute = sorted
          .filter(p => p.location && this.isValidCoordinate(p.location.latitude, p.location.longitude))
          .map(p => ({ latitude: p.location.latitude, longitude: p.location.longitude }));

        if (fullRoute.length < 2) return of(null);

        // Find the split point (next destination)
        let splitIdx = fullRoute.length;
        const nextLat = nextDest.latitude;
        const nextLng = nextDest.longitude;
        
        for (let i = 0; i < fullRoute.length; i++) {
          const dist = Math.sqrt(
            Math.pow(fullRoute[i].latitude - nextLat, 2) +
            Math.pow(fullRoute[i].longitude - nextLng, 2)
          );
          if (dist < 0.001) { // ~100m threshold
            splitIdx = i;
            break;
          }
        }

        const approachRoute = fullRoute.slice(0, splitIdx + 1);
        const remainingRoute = fullRoute.slice(splitIdx);

        return of({ approachRoute, remainingRoute });
      }),
      catchError(() => of(null))
    );
  }

  private buildMapData(ride: RideResponse): void {
    const startLoc = ride.departure ?? ride.start ?? ride.startLocation;
    const endLoc = ride.destination ?? ride.endLocation;
    const stops = ride.stops ?? [];

    if (!startLoc || !this.isValidCoordinate(startLoc.latitude, startLoc.longitude)) return;
    if (!endLoc || !this.isValidCoordinate(endLoc.latitude, endLoc.longitude)) return;

    const start: MapPoint = { latitude: startLoc.latitude, longitude: startLoc.longitude };
    const end: MapPoint = { latitude: endLoc.latitude, longitude: endLoc.longitude };
    const stopPoints: MapPoint[] = stops
      .filter(s => this.isValidCoordinate(s.latitude, s.longitude))
      .map(s => ({ latitude: s.latitude!, longitude: s.longitude! }));

    this.rideMapData = { start, stops: stopPoints, end };

    // Set initial driver location from ride data
    if (ride.vehicleLocation && this.isValidCoordinate(ride.vehicleLocation.latitude, ride.vehicleLocation.longitude)) {
      this.driverLocation = {
        latitude: ride.vehicleLocation.latitude,
        longitude: ride.vehicleLocation.longitude
      };
      
      // Immediately calculate approach/remaining routes if ride is in progress
      if (this.isRideInProgress) {
        this.fetchInitialRoutes(ride, this.driverLocation);
      } else if (this.isRideActive) {
        // For pending/accepted rides, calculate approach from vehicle to pickup
        this.fetchApproachToPickup(ride, this.driverLocation);
      }
    }

    // Fetch detailed route (full planned route)
    this.fetchDetailedRoute(ride);
  }

  private fetchApproachToPickup(ride: RideResponse, vehicleLoc: MapPoint): void {
    const pickup = ride.departure ?? ride.start ?? ride.startLocation;
    if (!pickup) return;

    const request: CreateRideRequest = {
      start: { latitude: vehicleLoc.latitude, longitude: vehicleLoc.longitude, address: '' },
      destination: pickup,
      stops: [],
      passengerEmails: [],
      scheduledTime: null,
      requirements: {
        vehicleType: ride.vehicleType || 'STANDARD',
        babyTransport: ride.babyTransport ?? false,
        petTransport: ride.petTransport ?? false
      }
    };

    this.rideService.estimateRide(request).subscribe({
      next: (est) => {
        if (est.routePoints && est.routePoints.length > 0) {
          const sorted = est.routePoints.sort((a, b) => a.order - b.order);
          this.approachRoute = sorted
            .filter(p => p.location && this.isValidCoordinate(p.location.latitude, p.location.longitude))
            .map(p => ({ latitude: p.location.latitude, longitude: p.location.longitude }));
          this.cdr.detectChanges();
        }
      }
    });
  }

  private fetchInitialRoutes(ride: RideResponse, vehicleLoc: MapPoint): void {
    const startLoc = ride.departure ?? ride.start ?? ride.startLocation;
    const endLoc = ride.destination ?? ride.endLocation;
    const stops = ride.stops ?? [];

    if (!endLoc) return;

    // Calculate route from vehicle to destination through remaining stops
    const remainingStops = stops.slice(this.completedStopIndexes.size);

    const request: CreateRideRequest = {
      start: { latitude: vehicleLoc.latitude, longitude: vehicleLoc.longitude, address: '' },
      destination: endLoc,
      stops: remainingStops,
      passengerEmails: [],
      scheduledTime: null,
      requirements: {
        vehicleType: ride.vehicleType || 'STANDARD',
        babyTransport: ride.babyTransport ?? false,
        petTransport: ride.petTransport ?? false
      }
    };

    this.rideService.estimateRide(request).subscribe({
      next: (est) => {
        if (!est.routePoints || est.routePoints.length < 2) return;

        const sorted = est.routePoints.sort((a, b) => a.order - b.order);
        const fullRoute = sorted
          .filter(p => p.location && this.isValidCoordinate(p.location.latitude, p.location.longitude))
          .map(p => ({ latitude: p.location.latitude, longitude: p.location.longitude }));

        if (fullRoute.length < 2) return;

        // Determine next destination
        const allPoints = [startLoc, ...stops, endLoc];
        const nextDestIdx = this.completedStopIndexes.size + 1;
        
        if (nextDestIdx >= allPoints.length) {
          // Going to final destination
          this.approachRoute = fullRoute;
          this.remainingRoute = null;
        } else {
          const nextDest = allPoints[nextDestIdx];
          if (!nextDest || !this.isValidCoordinate(nextDest.latitude, nextDest.longitude)) {
            this.approachRoute = fullRoute;
            this.remainingRoute = null;
          } else {
            // Find split point
            let splitIdx = fullRoute.length;
            for (let i = 0; i < fullRoute.length; i++) {
              const dist = Math.sqrt(
                Math.pow(fullRoute[i].latitude - nextDest.latitude, 2) +
                Math.pow(fullRoute[i].longitude - nextDest.longitude, 2)
              );
              if (dist < 0.001) {
                splitIdx = i;
                break;
              }
            }

            this.approachRoute = fullRoute.slice(0, splitIdx + 1);
            this.remainingRoute = fullRoute.slice(splitIdx);
          }
        }

        this.cdr.detectChanges();
      }
    });
  }

  private fetchDetailedRoute(ride: RideResponse): void {
    const startLoc = ride.departure ?? ride.start ?? ride.startLocation;
    const endLoc = ride.destination ?? ride.endLocation;
    const stops = ride.stops ?? [];

    if (!startLoc || !endLoc) return;

    const request: CreateRideRequest = {
      start: startLoc,
      destination: endLoc,
      stops: stops,
      passengerEmails: [],
      scheduledTime: null,
      requirements: {
        vehicleType: ride.vehicleType || 'STANDARD',
        babyTransport: ride.babyTransport ?? false,
        petTransport: ride.petTransport ?? false
      }
    };

    this.rideService.estimateRide(request).subscribe({
      next: (est) => {
        if (est.routePoints && est.routePoints.length > 0) {
          const sorted = est.routePoints.sort((a, b) => a.order - b.order);
          this.routePolyline = sorted
            .filter(p => p.location && this.isValidCoordinate(p.location.latitude, p.location.longitude))
            .map(p => ({ latitude: p.location.latitude, longitude: p.location.longitude }));
          this.cdr.detectChanges();
        }
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

  get isRideActive(): boolean {
    const status = this.ride?.status;
    return status === 'IN_PROGRESS' || status === 'ACTIVE' || status === 'ACCEPTED' || status === 'PENDING' || status === 'SCHEDULED';
  }

  get isRideInProgress(): boolean {
    const status = this.ride?.status;
    return status === 'IN_PROGRESS' || status === 'ACTIVE';
  }

  get statusLabel(): string {
    switch (this.ride?.status) {
      case 'IN_PROGRESS':
      case 'ACTIVE':
        return 'In Progress';
      case 'ACCEPTED':
        return 'Accepted';
      case 'PENDING':
        return 'Pending';
      case 'SCHEDULED':
        return 'Scheduled';
      case 'FINISHED':
        return 'Finished';
      case 'CANCELLED':
      case 'CANCELLED_BY_DRIVER':
      case 'CANCELLED_BY_PASSENGER':
        return 'Cancelled';
      default:
        return this.ride?.status || '—';
    }
  }

  get statusClass(): string {
    switch (this.ride?.status) {
      case 'IN_PROGRESS':
      case 'ACTIVE':
        return 'bg-green-500/15 border-green-500/30 text-green-400';
      case 'ACCEPTED':
        return 'bg-blue-500/15 border-blue-500/30 text-blue-400';
      case 'PENDING':
        return 'bg-yellow-500/15 border-yellow-500/30 text-yellow-400';
      case 'SCHEDULED':
        return 'bg-purple-500/15 border-purple-500/30 text-purple-400';
      case 'FINISHED':
        return 'bg-gray-500/15 border-gray-500/30 text-gray-400';
      default:
        return 'bg-red-500/15 border-red-500/30 text-red-400';
    }
  }

  getDriverImageUrl(): string {
    if (this.ride?.driver?.id) {
      return `${environment.apiHost}users/${this.ride.driver.id}/profile-image`;
    }
    return '';
  }

  getDriverName(): string {
    if (this.ride?.driver) {
      const name = this.ride.driver.name || '';
      const surname = this.ride.driver.surname || '';
      return `${name} ${surname}`.trim() || '—';
    }
    return 'Unassigned';
  }

  getVehicleInfo(): string {
    const model = this.ride?.model || this.ride?.driver?.vehicle?.model || '—';
    const plates = this.ride?.licensePlates || this.ride?.driver?.vehicle?.licensePlates || '';
    return plates ? `${model} • ${plates}` : model;
  }

  getVehicleType(): string {
    return this.ride?.vehicleType || this.ride?.driver?.vehicle?.vehicleType || '—';
  }

  getPassengerCount(): number {
    return this.ride?.passengers?.length || 0;
  }

  getPassengers(): { name: string; email: string }[] {
    return (this.ride?.passengers || []).map(p => ({
      name: [p.name, p.surname].filter(Boolean).join(' ') || 'Unknown',
      email: p.email || ''
    }));
  }

  getPickupAddress(): string {
    const loc = this.ride?.departure ?? this.ride?.start ?? this.ride?.startLocation;
    return loc?.address || '—';
  }

  getDestinationAddress(): string {
    const loc = this.ride?.destination ?? this.ride?.endLocation;
    return loc?.address || '—';
  }

  getStops(): { address: string }[] {
    return (this.ride?.stops || []).map(s => ({ address: s.address || '—' }));
  }

  formatCost(): string {
    const cost = this.ride?.totalCost ?? this.ride?.estimatedCost ?? 0;
    return cost.toFixed(2);
  }

  formatDistance(): string {
    const dist = this.ride?.distanceKm ?? this.ride?.estimatedDistance ?? 0;
    return dist.toFixed(2);
  }

  formatTime(): string {
    const time = this.ride?.estimatedTimeInMinutes;
    return time ? `~${time} min` : '—';
  }

  formatPanicTime(): string {
    // This would come from panic entity - for now show current time if panic is active
    if (this.panicActivated) {
      return new Date().toLocaleString();
    }
    return '—';
  }

  goBack(): void {
    this.router.navigate(['/admin/dashboard']);
  }

  openPanicModal(): void {
    this.showPanicModal = true;
  }

  closePanicModal(): void {
    this.showPanicModal = false;
  }
}
