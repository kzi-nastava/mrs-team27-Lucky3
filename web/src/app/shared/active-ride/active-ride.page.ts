import { AfterViewInit, ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { ActiveRideMapComponent, ActiveRideMapData, MapPoint } from '../ui/active-ride-map/active-ride-map.component';
import { VehicleService } from '../../infrastructure/rest/vehicle.service';
import { AuthService } from '../../infrastructure/auth/auth.service';
import { RideService } from '../../infrastructure/rest/ride.service';
import { DriverService } from '../../infrastructure/rest/driver.service';
import { CreateRideRequest } from '../../infrastructure/rest/model/create-ride.model';
import { RideResponse } from '../../infrastructure/rest/model/ride-response.model';
import { environment } from '../../../env/environment';

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
  approachRoute: MapPoint[] | null = null; // Blue line (vehicle to next stop)
  remainingRoute: MapPoint[] | null = null; // Yellow dotted (rest of route)
  driverLocation: MapPoint | null = null;

  // Live metrics
  private startedAtMs = Date.now();
  private updateTimer: any;
  private locationPoller: any;
  private ridePoller: any; // Poll ride data to get updated cost from backend

  private totalPlannedDistanceKm: number | null = null;
  private distanceTraveledKm = 0;
  private lastDriverLocation: MapPoint | null = null;
  private rideStartLocation: MapPoint | null = null; // Track ride start for distance calculation

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

  // Stop early modal
  showStopEarlyModal = false;
  isStoppingEarly = false;
  stopEarlyError = '';
  currentLocationAddress = 'Fetching location...';

  // Report inconsistency modal (passenger only)
  showReportModal = false;
  reportRemark = '';
  isReporting = false;
  reportError = '';
  reportSuccess = false;

  // Passenger cancel modal
  showPassengerCancelModal = false;
  isPassengerCancelling = false;
  passengerCancelError = '';

  // Panic modal
  showPanicModal = false;
  panicReason = '';
  isPanicking = false;
  panicError = '';

  private driverId: number | null = null;
  private userId: number | null = null;
  userRole: string = '';

  // stop completion tracking
  private stopCompletionThresholdMeters = 30; // 30m as per requirement
  completedStopIndexes = new Set<number>();
  private pendingStopCompletions = new Set<number>(); // Track stops being sent to backend

  private readonly geocodeEnabled = true;
  private pollErrors = 0;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private http: HttpClient,
    private rideService: RideService,
    private vehicleService: VehicleService,
    private authService: AuthService,
    private driverService: DriverService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    const parsed = idParam ? Number(idParam) : NaN;
    this.rideId = Number.isFinite(parsed) ? parsed : null;

    this.userId = this.authService.getUserId();
    this.userRole = this.authService.getRole() || '';
    this.driverId = this.userRole === 'DRIVER' ? this.userId : null;
    
    // Safety check - if ID is bad, go back
    if (!this.rideId) {
        this.goBack();
        return;
    }
    
    // Load immediately
    this.loadRide();
  }

  ngAfterViewInit(): void {
    this.startedAtMs = Date.now();

    // Start live updates
    this.pollDriverLocation();
    this.locationPoller = setInterval(() => this.pollDriverLocation(), 5000); // Every 5s as per requirement

    // Poll ride data to get updated cost from backend (backend updates cost every 5s)
    this.pollRideData();
    this.ridePoller = setInterval(() => this.pollRideData(), 5000);

    this.updateTimer = setInterval(() => this.tick(), 1000);
  }

  ngOnDestroy(): void {
    if (this.updateTimer) clearInterval(this.updateTimer);
    if (this.locationPoller) clearInterval(this.locationPoller);
    if (this.ridePoller) clearInterval(this.ridePoller);
  }

  goBack(): void {
    if (this.userRole === 'DRIVER') {
      this.router.navigate(['/driver/dashboard']);
    } else {
      this.router.navigate(['/passenger/home']);
    }
  }

  get isDriver(): boolean {
    return this.userRole === 'DRIVER';
  }

  get isPassenger(): boolean {
    return this.userRole === 'PASSENGER';
  }

  get isRideInProgress(): boolean {
    return this.rideStatus === 'IN_PROGRESS' || this.rideStatus === 'ACTIVE';
  }

  get isRidePending(): boolean {
    return this.rideStatus === 'PENDING' || this.rideStatus === 'ACCEPTED' || this.rideStatus === 'SCHEDULED';
  }

  // Threshold in meters for considering driver "close" to endpoint
  private readonly endPointThresholdMeters = 100;

  get canEndRide(): boolean {
    // Must be in progress
    if (!this.isRideInProgress) return false;
    
    // Must have map data with stops
    if (!this.rideMapData) return true; // Allow if no map data (fallback)
    
    // All stops must be completed
    const totalStops = this.rideMapData.stops.length;
    if (this.completedStopIndexes.size < totalStops) return false;
    
    // Must be close to the end point
    if (!this.driverLocation || !this.rideMapData.end) return false;
    
    const distanceToEndMeters = this.haversineKm(this.driverLocation, this.rideMapData.end) * 1000;
    return distanceToEndMeters <= this.endPointThresholdMeters;
  }

  get endRideDisabledReason(): string {
    if (!this.isRideInProgress) return '';
    if (!this.rideMapData) return '';
    
    const totalStops = this.rideMapData.stops.length;
    const stopsRemaining = totalStops - this.completedStopIndexes.size;
    
    if (stopsRemaining > 0) {
      return `Complete ${stopsRemaining} remaining stop${stopsRemaining > 1 ? 's' : ''} first`;
    }
    
    if (!this.driverLocation || !this.rideMapData.end) {
      return 'Waiting for location...';
    }
    
    const distanceToEndMeters = this.haversineKm(this.driverLocation, this.rideMapData.end) * 1000;
    if (distanceToEndMeters > this.endPointThresholdMeters) {
      const distanceText = distanceToEndMeters >= 1000 
        ? `${(distanceToEndMeters / 1000).toFixed(1)} km` 
        : `${Math.round(distanceToEndMeters)} m`;
      return `Drive closer to destination (${distanceText} away)`;
    }
    
    return '';
  }

  get statusDisplayText(): string {
    if (this.isRideInProgress) return 'Ride in Progress';
    if (this.rideStatus === 'PENDING') return 'Ride Pending';
    if (this.rideStatus === 'ACCEPTED' || this.rideStatus === 'SCHEDULED') return 'Ride Scheduled';
    return 'Ride Status: ' + this.rideStatus;
  }

  private static readonly PRICE_PER_KM = 120;

  private getBasePriceRsd(vehicleType?: string): number {
    switch ((vehicleType ?? '').toUpperCase()) {
      case 'LUXURY': return 360;
      case 'VAN': return 180;
      default: return 120;
    }
  }

  private getEstimatedTotalRsd(): number {
    return this.backendRide?.estimatedCost ?? this.backendRide?.totalCost ?? 0;
  }

  get currentCostDisplay(): string {
    return `RSD ${this.currentCost.toFixed(0)}`;
  }

  get savedRsd(): number {
    return Math.max(0, this.getEstimatedTotalRsd() - this.currentCost);
  }


  startRide(): void {
    if (!this.rideId) return;
    this.rideService.startRide(this.rideId).subscribe({
      next: (r) => {
        void this.applyRideResponse(r);
        this.startedAtMs = Date.now(); // Reset start time for metric calculation
        this.distanceTraveledKm = 0; // Reset distance traveled
        this.lastDriverLocation = this.driverLocation ? { ...this.driverLocation } : null; // Set initial location
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

  // --- PASSENGER SPECIFIC METHODS ---

  openReportModal(): void {
    this.reportError = '';
    this.reportRemark = '';
    this.reportSuccess = false;
    this.showReportModal = true;
  }

  closeReportModal(): void {
    if (this.isReporting) return;
    this.showReportModal = false;
  }

  confirmReport(): void {
    if (!this.rideId || !this.reportRemark.trim()) {
      this.reportError = 'Please provide a description.';
      return;
    }

    this.isReporting = true;
    this.reportError = '';

    this.rideService.reportInconsistency(this.rideId, { remark: this.reportRemark.trim() }).subscribe({
      next: () => {
        this.isReporting = false;
        this.reportSuccess = true;
        // Show success briefly then close
        setTimeout(() => {
          this.showReportModal = false;
          this.reportSuccess = false;
        }, 2000);
      },
      error: (err) => {
        this.isReporting = false;
        console.error('Failed to report inconsistency', err);
        this.reportError = 'Failed to submit report. Please try again.';
      }
    });
  }

  openPassengerCancelModal(): void {
    this.passengerCancelError = '';
    this.showPassengerCancelModal = true;
  }

  closePassengerCancelModal(): void {
    if (this.isPassengerCancelling) return;
    this.showPassengerCancelModal = false;
  }

  confirmPassengerCancel(): void {
    if (!this.rideId) return;

    this.isPassengerCancelling = true;
    this.passengerCancelError = '';

    this.rideService.cancelRideAsPassenger(this.rideId).subscribe({
      next: () => {
        this.isPassengerCancelling = false;
        this.showPassengerCancelModal = false;
        this.router.navigate(['/passenger/home']);
      },
      error: (err) => {
        this.isPassengerCancelling = false;
        console.error('Failed to cancel ride', err);
        this.passengerCancelError = err.error?.message || 'Failed to cancel. You may only cancel scheduled rides more than 10 minutes before start.';
      }
    });
  }

  // --- PANIC BUTTON METHODS (both driver and passenger) ---

  openPanicModal(): void {
    this.panicError = '';
    this.panicReason = '';
    this.showPanicModal = true;
  }

  closePanicModal(): void {
    if (this.isPanicking) return;
    this.showPanicModal = false;
  }

  confirmPanic(): void {
    if (!this.rideId) return;

    this.isPanicking = true;
    this.panicError = '';

    this.rideService.panicRide(this.rideId, this.panicReason.trim()).subscribe({
      next: () => {
        this.isPanicking = false;
        this.showPanicModal = false;
        // Redirect based on role
        if (this.isDriver) {
          this.router.navigate(['/driver/dashboard']);
        } else {
          this.router.navigate(['/passenger/home']);
        }
      },
      error: (err) => {
        this.isPanicking = false;
        console.error('Failed to trigger panic', err);
        this.panicError = err.error?.message || 'Failed to trigger panic. Please try again.';
      }
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
        // Trigger status refresh so dashboard updates immediately
        this.driverService.triggerStatusRefresh();
        this.router.navigate(['/driver/dashboard']);
      },
      error: () => {
        this.isEnding = false;
        this.endRideError = 'Could not end ride. Please try again.';
      }
    });
  }

  openStopEarlyModal(): void {
    if (!this.driverLocation) {
      this.stopEarlyError = 'Cannot stop ride: Driver location unknown.';
      return;
    }
    this.stopEarlyError = '';
    this.showStopEarlyModal = true;

    const { latitude, longitude } = this.driverLocation;
    this.currentLocationAddress = 'Fetching location...';
    this.reverseGeocode(latitude, longitude).then(addr => {
      this.currentLocationAddress = addr;
      this.cdr.detectChanges();
    });
  }

  closeStopEarlyModal(): void {
    if (this.isStoppingEarly) return;
    this.showStopEarlyModal = false;
  }

  confirmStopEarly(): void {
    if (!this.rideId || !this.driverLocation || this.isStoppingEarly) return;

    this.isStoppingEarly = true;
    this.stopEarlyError = '';

    const stopRequest = {
      stopLocation: {
        address: this.currentLocationAddress || 'Stopped early',
        latitude: this.driverLocation.latitude,
        longitude: this.driverLocation.longitude
      }
    };

    const handleSuccess = () => {
      this.isStoppingEarly = false;
      this.showStopEarlyModal = false;
      // Trigger status refresh so dashboard updates immediately
      this.driverService.triggerStatusRefresh();
      this.router.navigate(['driver/dashboard']);
    };

    const performStop = (forceOffline: boolean) => {
      if (!this.rideId) return;
      this.rideService.stopRide(this.rideId, stopRequest).subscribe({
        next: () => {
          if (forceOffline && this.driverId) {
            this.driverService.toggleStatus(this.driverId, false).subscribe({
              next: () => handleSuccess(),
              error: () => handleSuccess()
            });
          } else {
            handleSuccess();
          }
        },
        error: () => {
          this.isStoppingEarly = false;
          this.stopEarlyError = 'Failed to stop ride. Please try again.';
        }
      });
    };

    if (this.driverId) {
      this.driverService.getStatus(this.driverId).subscribe({
        next: (status) => performStop(status.inactiveRequested),
        error: () => performStop(false)
      });
    } else {
      performStop(false);
    }
  }

  private tick(): void {
    // Calculate distance and time for both pending and in-progress rides
    this.updateDistanceAndTime();

    const activeStatuses = ['INPROGRESS', 'IN_PROGRESS', 'ACTIVE'];
    if (activeStatuses.includes(this.rideStatus)) {
      // Calculate current cost: base price + (distance traveled * price per km)
      // distanceTraveledKm is accumulated in pollDriverLocation based on actual vehicle movement
      const base = this.getBasePriceRsd(this.backendRide?.vehicleType ?? this.ride?.type);
      const computed = base + this.distanceTraveledKm * ActiveRidePage.PRICE_PER_KM;

      this.currentCost = computed;

      this.updateCompletedStops();
    } else if (this.isRidePending && this.distanceLeftKm != null) {
      // For pending rides, estimate cost based on distance: base + distance * price per km
      const base = this.getBasePriceRsd(this.backendRide?.vehicleType ?? this.ride?.type);
      this.currentCost = base + this.distanceLeftKm * ActiveRidePage.PRICE_PER_KM;
    }
    
    this.cdr.detectChanges();
  }

  private updateDistanceAndTime(): void {
    // Calculate distance based on actual route polylines
    // In progress: blue line (approach) + yellow line (remaining)
    // Pending: yellow line (full route from pickup to destination)
    
    let distanceKm = 0;
    
    if (this.isRideInProgress) {
      // Distance = approach route (blue) + remaining route (yellow)
      const approachDistanceKm = this.computePolylineDistanceKm(this.approachRoute);
      const remainingDistanceKm = this.computePolylineDistanceKm(this.remainingRoute);
      distanceKm = approachDistanceKm + remainingDistanceKm;
    } else if (this.isRidePending) {
      // For pending rides, use the full route polyline or remaining route
      const routeDistanceKm = this.computePolylineDistanceKm(this.routePolyline) || 
                              this.computePolylineDistanceKm(this.remainingRoute);
      distanceKm = routeDistanceKm;
    }
    
    this.distanceLeftKm = distanceKm > 0 ? distanceKm : null;
    
    // Time = distance * 3.2 minutes per km
    this.timeLeftMin = distanceKm > 0 ? distanceKm * 3.2 : null;
  }

  private computePolylineDistanceKm(polyline: MapPoint[] | null): number {
    if (!polyline || polyline.length < 2) return 0;
    
    let totalKm = 0;
    for (let i = 0; i < polyline.length - 1; i++) {
      totalKm += this.haversineKm(polyline[i], polyline[i + 1]);
    }
    return totalKm;
  }

  /**
   * Fetch updated ride data from backend (cost is calculated by backend service).
   */
  private fetchRideCost(): void {
    if (!this.rideId || !this.isRideInProgress) return;
    
    this.rideService.getRide(this.rideId).subscribe({
      next: (r) => {
        if (r.totalCost != null && r.totalCost > 0) {
          this.currentCost = r.totalCost;
        }
        if (r.distanceTraveled != null) {
          this.distanceTraveledKm = r.distanceTraveled;
        }
        this.cdr.detectChanges();
      },
      error: () => { /* ignore */ }
    });
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

  /**
   * Poll ride data to get updated cost from backend.
   * The backend RideCostTrackingService updates costs every 5 seconds.
   */
  private pollRideData(): void {
    if (!this.rideId || !this.isRideInProgress) return;

    this.rideService.getRide(this.rideId).subscribe({
      next: (r) => {
        // Update cost from backend
        if (r.totalCost != null && r.totalCost > 0) {
          this.currentCost = r.totalCost;
        }
        if (r.distanceTraveled != null) {
          this.distanceTraveledKm = r.distanceTraveled;
        }
        // Update vehicle location from ride response
        if (r.vehicleLocation && this.isValidCoordinate(r.vehicleLocation.latitude, r.vehicleLocation.longitude)) {
          this.driverLocation = { latitude: r.vehicleLocation.latitude, longitude: r.vehicleLocation.longitude };
        }
        this.cdr.detectChanges();
      },
      error: () => {
        // Silently ignore poll errors
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

    // Load completed stop indexes from backend response
    if (r.completedStopIndexes && r.completedStopIndexes.length > 0) {
      this.completedStopIndexes = new Set(r.completedStopIndexes);
    }

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

    // Store ride start location for distance calculation
    const startLoc = r.departure ?? r.start ?? r.startLocation;
    if (startLoc && this.isValidCoordinate(startLoc.latitude, startLoc.longitude)) {
      this.rideStartLocation = { latitude: startLoc.latitude, longitude: startLoc.longitude };
    }

    // Initialize cost and distance traveled
    const base = this.getBasePriceRsd(r.vehicleType ?? this.ride?.type);
    
    // Get vehicle location from ride response
    const vehicleLoc = r.vehicleLocation;
    if (vehicleLoc && this.isValidCoordinate(vehicleLoc.latitude, vehicleLoc.longitude)) {
      this.driverLocation = { latitude: vehicleLoc.latitude, longitude: vehicleLoc.longitude };
      // DON'T set lastDriverLocation here - let the first poll handle it
      // This allows the poll to detect movement since ride start
    }
    
    if (this.isRideInProgress) {
      // PRIORITY 1: Use backend totalCost if it exists and is valid (ride is saved in DB)
      if (r.totalCost && r.totalCost >= base) {
        this.currentCost = r.totalCost;
        this.distanceTraveledKm = (r.totalCost - base) / ActiveRidePage.PRICE_PER_KM;
      } 
      // PRIORITY 2: Calculate from positions as fallback
      else if (this.rideStartLocation && this.driverLocation) {
        const calculatedDistance = this.haversineKm(this.rideStartLocation, this.driverLocation);
        this.distanceTraveledKm = calculatedDistance;
        this.currentCost = base + this.distanceTraveledKm * ActiveRidePage.PRICE_PER_KM;
      } 
      // PRIORITY 3: Start fresh
      else {
        this.currentCost = base;
        this.distanceTraveledKm = 0;
      }
    } else {
      // For pending rides, start fresh
      this.currentCost = base;
      this.distanceTraveledKm = 0;
    }
    
    this.cdr.detectChanges();

    // If routePolyline is missing or too simple, try to fetch detailed route from API
    if (r && (!this.routePolyline || this.routePolyline.length <= 2)) {
      this.fetchDetailedRoute(r);
    }
    
    // Fetch approach route (blue line: Driver -> next stop)
    this.fetchApproachRoute(r);
    
    // Fetch remaining route (yellow dotted line: next stop -> end, excluding completed stops)
    if (this.isRideInProgress) {
      this.fetchRemainingRoute(r);
    }
  }

  private fetchApproachRoute(r: RideResponse): void {
      // Show approach route for any non-FINISHED ride
      if (r.status === 'FINISHED' || r.status === 'CANCELLED') {
          this.approachRoute = null;
          this.remainingRoute = null;
          return;
      }
      
      // Prefer freshly polled driverLocation, then fall back to ride response vehicleLocation
      const vehicleLoc = this.driverLocation 
        ? { address: 'Current Location', latitude: this.driverLocation.latitude, longitude: this.driverLocation.longitude }
        : (r.vehicleLocation ?? null);
      
      if (!vehicleLoc) {
          // No vehicle location yet, will retry when location is polled
          return;
      }

      // Determine next destination based on ride status and completed stops
      let nextDestination: { address: string; latitude: number; longitude: number } | null = null;
      const stops = r.stops ?? [];
      const end = r.destination ?? r.endLocation;

      if (this.isRideInProgress) {
          // Find next uncompleted stop, or end if all stops completed
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
      } else {
          // For pending rides, show route to pickup
          const pickup = r.departure ?? r.start ?? r.startLocation;
          nextDestination = pickup ?? null;
      }

      if (!nextDestination) {
          this.approachRoute = null;
          return;
      }
      
      // Calculate approach route (blue line) from vehicle to next destination
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

  private async reverseGeocode(lat: number, lon: number): Promise<string> {
    try {
      const url = `https://nominatim.openstreetmap.org/reverse?format=json&lat=${lat}&lon=${lon}&zoom=18&addressdetails=1`;
      const data: any = await firstValueFrom(this.http.get(url));
      if (data && data.display_name) {
        // Return a shortened version of the address
        const parts = data.display_name.split(',');
        return parts.slice(0, 3).join(',').trim();
      }
      return `${lat.toFixed(5)}, ${lon.toFixed(5)}`;
    } catch {
      return `${lat.toFixed(5)}, ${lon.toFixed(5)}`;
    }
  }

  private pollDriverLocation(): void {
    // Stop polling if we have too many consecutive errors (e.g. backend down)
    if (this.pollErrors > 5) {
      if (this.locationPoller) {
        clearInterval(this.locationPoller);
        this.locationPoller = undefined;
      }
      return;
    }

    // For drivers, poll their own vehicle
    // For passengers, poll the driver's vehicle from the ride
    const targetDriverId = this.isDriver ? this.driverId : this.backendRide?.driver?.id;
    
    if (!targetDriverId) {
      return;
    }

    this.vehicleService.getActiveVehicles().subscribe({
      next: (vehicles) => {
        this.pollErrors = 0;
        const mine = vehicles.find(v => v.driverId === targetDriverId);
        if (!mine) {
          return;
        }
        
        const newLocation = { latitude: mine.latitude, longitude: mine.longitude };
        const locationChanged = !this.driverLocation || 
          this.driverLocation.latitude !== newLocation.latitude || 
          this.driverLocation.longitude !== newLocation.longitude;
        
        // When location changes during an in-progress ride, fetch updated cost from backend
        // The backend RideCostTrackingService calculates cost automatically
        if (this.isRideInProgress && locationChanged) {
          this.fetchRideCost();
        }
        
        // Update last known location for next distance calculation
        if (this.isRideInProgress) {
          this.lastDriverLocation = newLocation;
        }
        
        this.driverLocation = newLocation;

        // Always update routes when vehicle moves during an in-progress ride
        if (locationChanged && this.backendRide) {
          if (this.isRideInProgress) {
            // For in-progress rides, recalculate both routes
            this.fetchApproachRoute(this.backendRide);
            this.fetchRemainingRoute(this.backendRide);
          } else if (!this.approachRoute) {
            // For pending rides, fetch approach route if we don't have one yet
            this.fetchApproachRoute(this.backendRide);
          }
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
    // Only driver should complete stops
    if (!this.isDriver || !this.isRideInProgress) return;

    this.rideMapData.stops.forEach((stop, idx) => {
      // Skip if already completed or being processed
      if (this.completedStopIndexes.has(idx) || this.pendingStopCompletions.has(idx)) return;

      const meters = this.haversineKm(this.driverLocation!, stop) * 1000;
      if (meters <= this.stopCompletionThresholdMeters) {
        // Mark as pending to prevent duplicate calls
        this.pendingStopCompletions.add(idx);
        
        // Call backend to complete the stop
        this.rideService.completeStop(this.rideId!, idx).subscribe({
          next: (res) => {
            this.completedStopIndexes.add(idx);
            this.pendingStopCompletions.delete(idx);
            
            // Update remaining route (yellow line) after stop completion
            this.updateRemainingRoute();
            
            // Show green toast notification
            this.showStopCompletedToast(idx);
            
            this.cdr.detectChanges();
          },
          error: (err) => {
            console.error('Failed to complete stop', err);
            this.pendingStopCompletions.delete(idx);
            // Still mark locally completed to avoid repeated calls
            this.completedStopIndexes.add(idx);
          }
        });
      }
    });
  }

  private showStopCompletedToast(stopIndex: number): void {
    // Create and show a toast notification
    const toast = document.createElement('div');
    toast.className = 'fixed top-4 right-4 z-[10000] bg-green-500 text-white px-6 py-3 rounded-xl shadow-lg transform transition-all duration-300';
    toast.innerHTML = `
      <div class="flex items-center gap-2">
        <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" viewBox="0 0 20 20" fill="currentColor">
          <path fill-rule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clip-rule="evenodd" />
        </svg>
        <span>Stop ${stopIndex + 1} completed!</span>
      </div>
    `;
    document.body.appendChild(toast);
    
    // Remove after 3 seconds
    setTimeout(() => {
      toast.style.opacity = '0';
      setTimeout(() => toast.remove(), 300);
    }, 3000);
  }

  private updateRemainingRoute(): void {
    // Recalculate both routes after a stop is completed
    if (this.backendRide) {
      // Update yellow route (remaining route from next stop to end)
      this.fetchRemainingRoute(this.backendRide);
      // Update blue route (vehicle to next uncompleted stop/end)
      this.fetchApproachRoute(this.backendRide);
    }
  }

  private fetchRemainingRoute(r: RideResponse): void {
    if (!this.isRideInProgress) {
      // For pending rides, show the full route
      this.remainingRoute = this.routePolyline;
      return;
    }

    // Build remaining route from AFTER the next uncompleted stop to the end
    // The yellow dotted line shows the path AFTER the blue line ends
    const stops = r.stops ?? [];
    const end = r.destination ?? r.endLocation;
    
    if (!end) return;

    // Find next uncompleted stop (this is where blue line goes TO)
    let nextStopIndex = -1;
    for (let i = 0; i < stops.length; i++) {
      if (!this.completedStopIndexes.has(i)) {
        nextStopIndex = i;
        break;
      }
    }

    // If no uncompleted stops, blue line goes to end, no yellow line needed
    if (nextStopIndex < 0) {
      this.remainingRoute = null;
      this.cdr.detectChanges();
      return;
    }

    // Yellow route starts FROM the next uncompleted stop (where blue ends)
    // and goes through remaining uncompleted stops to the destination
    const yellowStartPoint = stops[nextStopIndex];
    
    // Collect remaining uncompleted stops AFTER the next one
    const remainingUncompletedStops: typeof stops = [];
    for (let i = nextStopIndex + 1; i < stops.length; i++) {
      if (!this.completedStopIndexes.has(i)) {
        remainingUncompletedStops.push(stops[i]);
      }
    }

    // Build request for remaining route (from next stop through remaining stops to end)
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
