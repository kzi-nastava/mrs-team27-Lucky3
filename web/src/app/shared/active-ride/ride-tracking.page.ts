import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { ActiveRideMapComponent, ActiveRideMapData, MapPoint } from '../ui/active-ride-map/active-ride-map.component';
import { RideTrackingService } from '../../infrastructure/rest/ride-tracking.service';
import { RideService } from '../../infrastructure/rest/ride.service';
import { RideResponse } from '../../infrastructure/rest/model/ride-response.model';
import { SocketService } from '../../infrastructure/rest/socket.service';
import { AuthService } from '../../infrastructure/auth/auth.service';
import { Subscription } from 'rxjs';

interface VehicleLocation {
  latitude: number;
  longitude: number;
}

/**
 * Read-only ride tracking page for linked passengers.
 * Uses a tracking token to view ride status without authentication.
 * Shows login popup when user tries to navigate elsewhere.
 */
@Component({
  selector: 'app-ride-tracking',
  standalone: true,
  imports: [CommonModule, FormsModule, ActiveRideMapComponent],
  templateUrl: './ride-tracking.page.html',
  styleUrls: ['./active-ride.page.css']
})
export class RideTrackingPage implements OnInit, OnDestroy {
  token: string | null = null;
  ride: RideResponse | null = null;
  rideMapData: ActiveRideMapData | null = null;
  routePolyline: MapPoint[] | null = null;
  driverLocation: MapPoint | null = null;
  
  isLoading = true;
  loadingError: string | null = null;
  rideStatus: string = '';
  
  // Live metrics
  currentCost = 0;
  
  // Login modal
  showLoginModal = false;

  // Report inconsistency modal
  showReportModal = false;
  reportRemark = '';
  isReporting = false;
  reportError = '';
  reportSuccess = false;
  
  private pollingInterval: any;
  private locationSubscription: Subscription | null = null;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private rideTrackingService: RideTrackingService,
    private rideService: RideService,
    private socketService: SocketService,
    private authService: AuthService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.token = this.route.snapshot.queryParamMap.get('token');
    
    if (!this.token) {
      this.router.navigate(['/404']);
      return;
    }
    
    this.loadRide();
    
    // Poll ride data every 5 seconds
    this.pollingInterval = setInterval(() => this.loadRide(), 5000);
  }

  ngOnDestroy(): void {
    if (this.pollingInterval) {
      clearInterval(this.pollingInterval);
    }
    if (this.locationSubscription) {
      this.locationSubscription.unsubscribe();
    }
  }

  loadRide(): void {
    if (!this.token) return;
    
    this.rideTrackingService.getRideByToken(this.token).subscribe({
      next: (ride) => {
        this.ride = ride;
        this.rideStatus = ride.status || '';
        this.currentCost = ride.totalCost || ride.estimatedCost || 0;
        this.updateMapData(ride);
        this.isLoading = false;
        this.cdr.detectChanges();
        
        // Subscribe to vehicle location updates if we have driver info
        if (ride.driver?.id && !this.locationSubscription) {
          this.subscribeToDriverLocation(ride.driver.id);
        }
      },
      error: (err) => {
        console.error('Error loading ride:', err);
        if (err.status === 401 || err.status === 410) {
          // Token revoked or ride no longer trackable
          this.loadingError = err.error?.message || err.error?.error || 'Ride is no longer available for tracking';
        } else {
          this.loadingError = 'Failed to load ride data';
        }
        this.isLoading = false;
        this.cdr.detectChanges();
      }
    });
  }

  private updateMapData(ride: RideResponse): void {
    // Build map data - use departure/destination or start/endLocation based on what's available
    const startLoc = ride.departure || ride.startLocation;
    const endLoc = ride.destination || ride.endLocation;

    const start: MapPoint | null = startLoc ? {
      latitude: startLoc.latitude,
      longitude: startLoc.longitude
    } : null;

    const end: MapPoint | null = endLoc ? {
      latitude: endLoc.latitude,
      longitude: endLoc.longitude
    } : null;

    const stops: MapPoint[] = ride.stops?.map(s => ({
      latitude: s.latitude,
      longitude: s.longitude
    })) || [];

    this.rideMapData = {
      start: start!,
      end: end!,
      stops
    };

    // Build route polyline
    if (ride.routePoints && ride.routePoints.length > 0) {
      this.routePolyline = ride.routePoints.map(rp => ({
        latitude: rp.location.latitude,
        longitude: rp.location.longitude
      }));
    }
  }

  private subscribeToDriverLocation(driverId: number): void {
    this.locationSubscription = this.socketService
      .getVehicleLocationUpdates(driverId)
      .subscribe({
        next: (location: VehicleLocation) => {
          if (location && location.latitude && location.longitude) {
            this.driverLocation = {
              latitude: location.latitude,
              longitude: location.longitude
            };
            this.cdr.detectChanges();
          }
        },
        error: (err: Error) => console.warn('Vehicle location subscription error:', err)
      });
  }

  get statusDisplayText(): string {
    switch (this.rideStatus) {
      case 'PENDING': return 'Waiting for driver';
      case 'SCHEDULED': return 'Scheduled';
      case 'ACCEPTED': return 'Driver accepted';
      case 'IN_PROGRESS': return 'In Progress';
      case 'ACTIVE': return 'Active';
      case 'FINISHED': return 'Completed';
      case 'CANCELLED':
      case 'CANCELLED_BY_DRIVER':
      case 'CANCELLED_BY_PASSENGER': return 'Cancelled';
      default: return this.rideStatus || 'Unknown';
    }
  }

  get isRideInProgress(): boolean {
    return this.rideStatus === 'IN_PROGRESS' || this.rideStatus === 'ACTIVE';
  }

  get isRidePending(): boolean {
    return this.rideStatus === 'PENDING' || this.rideStatus === 'ACCEPTED' || this.rideStatus === 'SCHEDULED';
  }

  get currentCostDisplay(): string {
    return `${this.currentCost?.toFixed(2) ?? '0.00'} RSD`;
  }

  // Navigation with login requirement
  navigateToLogin(): void {
    // Close modal and navigate to login
    this.showLoginModal = false;
    this.router.navigate(['/login']);
  }

  stayOnPage(): void {
    this.showLoginModal = false;
  }

  // Called when user tries to do something that requires login
  requireLogin(): void {
    if (!this.authService.isLoggedIn()) {
      this.showLoginModal = true;
    }
  }

  // Report Inconsistency methods
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
    if (!this.ride?.id || !this.reportRemark.trim()) {
      this.reportError = 'Please provide a description.';
      return;
    }

    this.isReporting = true;
    this.reportError = '';

    this.rideService.reportInconsistency(this.ride.id, { remark: this.reportRemark.trim() }).subscribe({
      next: () => {
        this.isReporting = false;
        this.reportSuccess = true;
        this.cdr.detectChanges();
        // Show success briefly then close
        setTimeout(() => {
          this.showReportModal = false;
          this.reportSuccess = false;
          this.cdr.detectChanges();
        }, 2000);
      },
      error: (err: Error) => {
        this.isReporting = false;
        console.error('Failed to report inconsistency', err);
        this.reportError = 'Failed to submit report. Please try again.';
        this.cdr.detectChanges();
      }
    });
  }
}
