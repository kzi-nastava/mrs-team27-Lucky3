import { AfterViewInit, ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { ActiveRideMapComponent, ActiveRideMapData, MapPoint } from '../../../shared/ui/active-ride-map/active-ride-map.component';
import { VehicleService } from '../../../infrastructure/rest/vehicle.service';
import { AuthService } from '../../../infrastructure/auth/auth.service';
import { RideService, CreateRideRequest } from '../../../infrastructure/rest/ride.service';
import { RideResponse } from '../../../infrastructure/rest/model/ride-response.model';
import { environment } from '../../../../env/environment';

@Component({
  selector: 'app-active-ride-page',
  standalone: true,
  imports: [CommonModule, FormsModule, ActiveRideMapComponent],
  templateUrl: './active-ride.page.html',
  styleUrl: './active-ride.page.css'
})
export class ActiveRidePage implements OnInit, AfterViewInit, OnDestroy {
  private rideId: number | null = null;
  // Updated type definition to include undefined/null
  ride: { type?: string; pickup?: string; dropoff?: string } | null | undefined = null;
  
  rideStops: { address: string; latitude?: number; longitude?: number }[] = [];

  backendRide: RideResponse | null = null;
  rideStatus: string = '';
  loadingError: string | null = null;

  rideMapData: ActiveRideMapData | null = null;
  routePolyline: MapPoint[] | null = null;
  approachRoute: MapPoint[] | null = null; // Blue line
  driverLocation: MapPoint | null = null;

  // Live metrics
  private startedAtMs = Date.now();
  private updateTimer: any;
  private locationPoller: any;

  private totalPlannedDistanceKm: number | null = null;
  private distanceTraveledKm = 0;
  private lastDriverLocation: MapPoint | null = null;

  currentCost = 0;
  timeLeftMin: number | null = null;
  distanceLeftKm: number | null = null;

  // End ride modal
  showEndModal = false;
  passengersLeft = false;
  passengersPaid = false;
  isEnding = false;
  endRideError = '';

  showCancelModal = false;
  cancelReason = '';
  isCancelling = false;
  cancelRideError = '';

  private driverId: number | null = null;

  // stop completion tracking
  private stopCompletionThresholdMeters = 80;
  private completedStopIndexes = new Set<number>();

  private readonly geocodeEnabled = true;
  private pollErrors = 0;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private http: HttpClient,
    private rideService: RideService,
    private vehicleService: VehicleService,
    private authService: AuthService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    const parsed = idParam ? Number(idParam) : NaN;
    this.rideId = Number.isFinite(parsed) ? parsed : null;

    this.driverId = this.authService.getUserId();
    
    // Safety check - if ID is bad, go back
    if (!this.rideId) {
        this.router.navigate(['/driver/dashboard']);
        return;
    }
    
    // Load immediately
    this.loadRide();
  }

  ngAfterViewInit(): void {
    this.startedAtMs = Date.now();

    // Start live updates
    this.pollDriverLocation();
    this.locationPoller = setInterval(() => this.pollDriverLocation(), 4000);

    this.updateTimer = setInterval(() => this.tick(), 1000);
  }

  ngOnDestroy(): void {
    if (this.updateTimer) clearInterval(this.updateTimer);
    if (this.locationPoller) clearInterval(this.locationPoller);
  }

  goBack(): void {
    this.router.navigate(['/driver/dashboard']);
  }

  startRide(): void {
    if (!this.rideId) return;
    this.rideService.startRide(this.rideId).subscribe({
      next: (r) => {
        void this.applyRideResponse(r);
        this.startedAtMs = Date.now(); // Reset start time for metric calculation
      },
      error: (err) => console.error('Failed to start ride', err)
    });
  }

  openCancelModal(): void {
    this.cancelRideError = '';
    this.cancelReason = '';
    this.showCancelModal = true;
  }

  closeCancelModal(): void {
    if (this.isCancelling) return;
    this.showCancelModal = false;
  }

  confirmCancelRide(): void {
    if (!this.rideId) return;
    if (!this.cancelReason || this.cancelReason.trim().length === 0) {
      this.cancelRideError = 'Please provide a reason.';
      return;
    }

    this.isCancelling = true;
    this.rideService.cancelRide(this.rideId, { reason: this.cancelReason }).subscribe({
      next: () => {
        this.isCancelling = false;
        this.showCancelModal = false;
        this.router.navigate(['/driver/dashboard']);
      },
      error: (err) => {
        this.isCancelling = false;
        console.error('Failed to cancel ride', err);
        this.cancelRideError = 'Failed to cancel. Please try again.';
      }
    });
  }

  cancelRide(): void {
    if (!this.rideId) return;
    this.rideService.cancelRide(this.rideId, { reason: 'Cancelled by driver' }).subscribe({
      next: () => {
        this.router.navigate(['/driver/dashboard']);
      },
      error: (err) => console.error('Failed to cancel ride', err)
    });
  }

  openEndModal(): void {
    this.endRideError = '';
    this.passengersLeft = false;
    this.passengersPaid = false;
    this.showEndModal = true;
  }

  closeEndModal(): void {
    if (this.isEnding) return;
    this.endRideError = '';
    this.showEndModal = false;
  }

  get currentCostDisplay(): string {
    return `$${this.currentCost.toFixed(2)}`;
  }

  get timeLeftDisplay(): string {
    if (this.timeLeftMin == null) return '—';
    if (this.timeLeftMin < 1) return '< 1 min';
    return `${Math.round(this.timeLeftMin)} min`;
  }

  get distanceLeftDisplay(): string {
    if (this.distanceLeftKm == null) return '—';
    return `${this.distanceLeftKm.toFixed(1)} km`;
  }

  get completedStopLabels(): string[] {
    return Array.from(this.completedStopIndexes)
      .sort((a, b) => a - b)
      .map(i => this.rideStops[i]?.address)
      .filter(Boolean) as string[];
  }

  isStopCompleted(index: number): boolean {
    return this.completedStopIndexes.has(index);
  }

  confirmEndRide(): void {
    if (!this.passengersLeft || !this.passengersPaid || this.isEnding) return;
    if (!this.rideId) return;

    this.isEnding = true;
    this.endRideError = '';

    this.rideService.endRide(this.rideId, {
      passengersExited: this.passengersLeft,
      paid: this.passengersPaid
    }).subscribe({
      next: () => {
        this.isEnding = false;
        this.showEndModal = false;
        this.router.navigate(['/driver/dashboard']);
      },
      error: () => {
        this.isEnding = false;
        this.endRideError = 'Could not end ride. Please try again.';
      }
    });
  }

  private tick(): void {
    if (this.rideStatus !== 'IN_PROGRESS') return;

    // Update distance traveled based on driver movement
    if (this.driverLocation && this.lastDriverLocation) {
      const moved = this.haversineKm(this.lastDriverLocation, this.driverLocation);
      if (moved > 0 && moved < 0.25) {
        // Ignore huge jumps (bad GPS); cap at 250m/s tick
        this.distanceTraveledKm += moved;
      }
    }
    if (this.driverLocation) {
      this.lastDriverLocation = { ...this.driverLocation };
    }

    // Cost model: keep it simple but live.
    // base + perKm + perMin
    const elapsedMin = (Date.now() - this.startedAtMs) / 60000;
    const base = 1.5;
    const perKm = 1.35;
    const perMin = 0.35;

    const computed = base + perKm * this.distanceTraveledKm + perMin * elapsedMin;
    // never go below initial estimate if present
    const minEstimate =
      this.backendRide?.totalCost ??
      this.backendRide?.estimatedCost ??
      0;
    this.currentCost = Math.max(minEstimate, computed);

    // Remaining distance/time (if we have route geometry)
    if (this.driverLocation) {
      const remainingKm = this.computeRemainingKm(this.driverLocation);
      this.distanceLeftKm = remainingKm;

      // Assume average speed (km/h)
      const avgSpeedKmh = 32;
      this.timeLeftMin = avgSpeedKmh > 0 ? (remainingKm / avgSpeedKmh) * 60 : null;

      // Update completed stops
      this.updateCompletedStops();
    }
    this.cdr.detectChanges();
  }

  private loadRide(): void {
    if (!this.rideId) return;

    this.rideService.getRide(this.rideId).subscribe({
      next: (r) => {
        this.loadingError = null;
        void this.applyRideResponse(r);
      },
      error: (err) => {
        console.error('Error loading ride', err);
        this.loadingError = 'Failed to load ride info.';
      }
    });
  }

  private async applyRideResponse(r: RideResponse): Promise<void> {
    this.backendRide = r;
    this.rideStatus = r.status || '';

    // If 'departure' / 'destination' fields are null, check legacy fields or address strings
    // Backend RideResponse structure guarantees location objects have 'address' if not null.
    const start = r.departure ?? r.start ?? r.startLocation;
    const end = r.destination ?? r.endLocation;
    const stops = r.stops ?? [];
    
    this.ride = {
      type: r.vehicleType ?? 'Standard',
      pickup: start?.address ?? 'Unknown Pickup',
      dropoff: end?.address ?? 'Unknown Dropoff'
    };
    
    // Explicitly detect changes to show the ride info immediately
    this.cdr.detectChanges();

    this.rideStops = stops.map(s => ({ address: s.address, latitude: s.latitude, longitude: s.longitude }));

    // routePoints (detailed polyline)
    const routePoints = (r.routePoints ?? [])
      .slice()
      .sort((a, b) => (a.order ?? 0) - (b.order ?? 0))
      .map(p => p.location)
      .filter(Boolean);

    const routePolyline = routePoints
      .filter(p => this.isValidCoordinate(p.latitude, p.longitude))
      .map(p => ({ latitude: p.latitude, longitude: p.longitude }));

    this.routePolyline = routePolyline.length >= 2 ? routePolyline : null;

    // Build map markers/geometry:
    // 1) Prefer backend coordinates.
    // 2) Else, if we have routePolyline, infer start/end from it.
    // 3) Else, try geocoding addresses.

    const backendStartOk = !!start && this.isValidCoordinate(start.latitude, start.longitude);
    const backendEndOk = !!end && this.isValidCoordinate(end.latitude, end.longitude);

    if (backendStartOk && backendEndOk) {
      const stopPoints = stops
        .filter(s => this.isValidCoordinate(s.latitude, s.longitude))
        .map(s => ({ latitude: s.latitude, longitude: s.longitude }));

      this.rideMapData = {
        start: { latitude: start!.latitude, longitude: start!.longitude },
        stops: stopPoints,
        end: { latitude: end!.latitude, longitude: end!.longitude }
      };
    } else if (this.routePolyline && this.routePolyline.length >= 2) {
      this.rideMapData = {
        start: this.routePolyline[0],
        stops: [],
        end: this.routePolyline[this.routePolyline.length - 1]
      };
    } else if (this.geocodeEnabled) {
      const geocoded = await this.tryGeocodeFallback();
      if (geocoded) {
        this.rideMapData = geocoded;
      }
    }

    // Final fallback: small synthetic route around default center, ONLY if we have absolutely nothing.
    // If backend data is missing, let's at least show something safe, but maybe alert?
    if (!this.rideMapData) {
      console.warn('No valid ride geometry found, using map default.');
      const { defaultLat, defaultLng } = environment.map;
      this.rideMapData = {
        start: { latitude: defaultLat, longitude: defaultLng },
        stops: [],
        end: { latitude: defaultLat + 0.004, longitude: defaultLng + 0.004 }
      };
    }

    this.totalPlannedDistanceKm = this.computePlannedDistanceKm(this.rideMapData);
    
    // Fallback: If map calculation is zero (e.g. single point), check if backend gave us distance
    if (this.totalPlannedDistanceKm === 0 && (r.distanceKm || r.estimatedDistance)) {
       this.totalPlannedDistanceKm = r.distanceKm ?? r.estimatedDistance ?? 0;
    }

    this.distanceLeftKm = this.totalPlannedDistanceKm;

    // Initialize cost floor from backend if present
    const costFloor = r.totalCost ?? r.estimatedCost ?? 0;
    if (costFloor > 0) {
      this.currentCost = Math.max(this.currentCost, costFloor);
    }
    
    this.cdr.detectChanges();

    // If routePolyline is missing or too simple, try to fetch detailed route from API
    if (r && (!this.routePolyline || this.routePolyline.length <= 2)) {
      this.fetchDetailedRoute(r);
    }
    
    // Also fetch approach route (Driver -> Pickup)
    this.fetchApproachRoute(r);
  }

  private fetchApproachRoute(r: RideResponse): void {
      // Show approach route for any non-FINISHED ride
      if (r.status === 'FINISHED' || r.status === 'CANCELLED') {
          this.approachRoute = null;
          return;
      }
      
      const pickup = r.departure ?? r.start ?? r.startLocation;
      if (!pickup) {
          this.approachRoute = null;
          return;
      }
      
      // Prefer vehicleLocation from ride response (from backend), else use driver's polled location
      const vehicleLoc = r.vehicleLocation ?? 
        (this.driverLocation ? { address: '', latitude: this.driverLocation.latitude, longitude: this.driverLocation.longitude } : null);
      
      // Update driver marker position from vehicle location if available from ride
      // Use setTimeout to avoid ExpressionChangedAfterItHasBeenCheckedError
      if (r.vehicleLocation && !this.driverLocation) {
          setTimeout(() => {
              this.driverLocation = { latitude: r.vehicleLocation!.latitude, longitude: r.vehicleLocation!.longitude };
              this.cdr.detectChanges();
          }, 0);
      }
      
      if (!vehicleLoc) {
          // No vehicle location yet, will retry when location is polled
          return;
      }
      
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
          }
      });
  }

  private fetchDetailedRoute(r: RideResponse): void {
    const start = r.departure ?? r.start ?? r.startLocation;
    const end = r.destination ?? r.endLocation;

    if (!start || !end) return;

    const request: CreateRideRequest = {
      start: start,
      destination: end,
      stops: r.stops ?? [],
      passengerEmails: [], // not needed
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
          const poly = sorted
            .map(p => ({ latitude: p.location.latitude, longitude: p.location.longitude }))
            .filter(p => this.isValidCoordinate(p.latitude, p.longitude));

          if (poly.length > 2) {
            this.routePolyline = poly;
            this.cdr.detectChanges();
          }
        }
      },
      error: () => { /* ignore */ }
    });
  }

  private isValidCoordinate(lat: any, lng: any): boolean {
    const la = typeof lat === 'number' ? lat : Number(lat);
    const lo = typeof lng === 'number' ? lng : Number(lng);
    if (!Number.isFinite(la) || !Number.isFinite(lo)) return false;
    if (Math.abs(la) > 90 || Math.abs(lo) > 180) return false;
    // common "missing" placeholder
    if (la === 0 && lo === 0) return false;
    return true;
  }

  private async tryGeocodeFallback(): Promise<ActiveRideMapData | null> {
    const pickup = this.ride?.pickup;
    const dropoff = this.ride?.dropoff;
    if (!pickup || !dropoff) return null;

    const start = await this.geocodeAddress(pickup);
    const end = await this.geocodeAddress(dropoff);
    if (!start || !end) return null;

    const stopPoints: MapPoint[] = [];
    for (const s of this.rideStops) {
      const p = await this.geocodeAddress(s.address);
      if (p) stopPoints.push(p);
    }

    return {
      start,
      stops: stopPoints,
      end
    };
  }

  private async geocodeAddress(address: string): Promise<MapPoint | null> {
    try {
      const encoded = encodeURIComponent(address);
      const url = `https://nominatim.openstreetmap.org/search?format=json&q=${encoded}&limit=1`;
      const data: any = await firstValueFrom(this.http.get(url));

      if (data && data.length > 0) {
        const lat = parseFloat(data[0].lat);
        const lon = parseFloat(data[0].lon);
        if (!this.isValidCoordinate(lat, lon)) return null;
        return { latitude: lat, longitude: lon };
      }
      return null;
    } catch {
      return null;
    }
  }

  private pollDriverLocation(): void {
    if (!this.driverId) return;

    // Stop polling if we have too many consecutive errors (e.g. backend down)
    if (this.pollErrors > 5) {
      if (this.locationPoller) {
        clearInterval(this.locationPoller);
        this.locationPoller = undefined;
      }
      return;
    }

    this.vehicleService.getActiveVehicles().subscribe({
      next: (vehicles) => {
        this.pollErrors = 0;
        const mine = vehicles.find(v => v.driverId === this.driverId);
        if (!mine) return;
        this.driverLocation = { latitude: mine.latitude, longitude: mine.longitude };

        // Fetch approach route if we have a ride and no approach route yet
        if (!this.approachRoute && this.backendRide) {
            this.fetchApproachRoute(this.backendRide);
        }

        this.cdr.detectChanges();
      },
      error: () => {
        this.pollErrors++;
      }
    });
  }

  private computeRemainingKm(current: MapPoint): number {
    // Prefer the detailed polyline when available.
    if (this.routePolyline && this.routePolyline.length >= 2) {
      return this.remainingAlongPolylineKm(current, this.routePolyline);
    }

    // Fall back to coarse start/stops/end markers.
    if (this.rideMapData) {
      return this.remainingDistanceAlongRouteKm(current, this.rideMapData);
    }

    return 0;
  }

  private remainingAlongPolylineKm(current: MapPoint, poly: MapPoint[]): number {
    // Find the nearest polyline vertex index.
    let bestIdx = 0;
    let bestDist = Number.POSITIVE_INFINITY;
    for (let i = 0; i < poly.length; i++) {
      const d = this.haversineKm(current, poly[i]);
      if (d < bestDist) {
        bestDist = d;
        bestIdx = i;
      }
    }

    // Sum remaining distance from nearest point to end.
    let remaining = bestDist;
    for (let i = bestIdx; i < poly.length - 1; i++) {
      remaining += this.haversineKm(poly[i], poly[i + 1]);
    }
    return remaining;
  }

  private computePlannedDistanceKm(ride: ActiveRideMapData): number {
    const seq: MapPoint[] = [ride.start, ...ride.stops, ride.end];
    let sum = 0;
    for (let i = 0; i < seq.length - 1; i++) {
      sum += this.haversineKm(seq[i], seq[i + 1]);
    }
    return sum;
  }

  private remainingDistanceAlongRouteKm(current: MapPoint, ride: ActiveRideMapData): number {
    const targets: MapPoint[] = [ride.start, ...ride.stops, ride.end];

    // Find the next target we haven't "completed".
    let nextIndex = 0;
    for (let i = 0; i < ride.stops.length; i++) {
      if (!this.completedStopIndexes.has(i)) {
        nextIndex = i + 1; // because targets includes start at index 0
        break;
      }
      // If all stops completed, next is end
      nextIndex = ride.stops.length + 1;
    }

    // Remaining is from current -> next target plus rest of segments.
    let remaining = 0;
    if (targets[nextIndex]) {
      remaining += this.haversineKm(current, targets[nextIndex]);
      for (let i = nextIndex; i < targets.length - 1; i++) {
        remaining += this.haversineKm(targets[i], targets[i + 1]);
      }
    }

    return Math.max(0, remaining);
  }

  private updateCompletedStops(): void {
    if (!this.driverLocation || !this.rideMapData) return;

    this.rideMapData.stops.forEach((stop, idx) => {
      const meters = this.haversineKm(this.driverLocation!, stop) * 1000;
      if (meters <= this.stopCompletionThresholdMeters) {
        this.completedStopIndexes.add(idx);
      }
    });
  }

  private haversineKm(a: MapPoint, b: MapPoint): number {
    const toRad = (deg: number) => (deg * Math.PI) / 180;
    const R = 6371;

    const dLat = toRad(b.latitude - a.latitude);
    const dLon = toRad(b.longitude - a.longitude);

    const lat1 = toRad(a.latitude);
    const lat2 = toRad(b.latitude);

    const sinDLat = Math.sin(dLat / 2);
    const sinDLon = Math.sin(dLon / 2);

    const h = sinDLat * sinDLat + Math.cos(lat1) * Math.cos(lat2) * sinDLon * sinDLon;
    const c = 2 * Math.atan2(Math.sqrt(h), Math.sqrt(1 - h));
    return R * c;
  }
}
