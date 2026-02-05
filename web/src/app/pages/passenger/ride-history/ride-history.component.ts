import { Component, OnInit, ChangeDetectorRef, OnDestroy, AfterViewInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { RideResponse, RideStatus } from '../../../infrastructure/rest/model/ride-response.model';
import { RidesTableComponent, RideSortField } from '../../../shared/rides/rides-table/rides-table.component';
import { RouterModule } from '@angular/router';
import { RideService } from '../../../infrastructure/rest/ride.service';
import { AuthService } from '../../../infrastructure/auth/auth.service';
import { Subject, takeUntil } from 'rxjs';
import { Ride } from '../../../shared/data/mock-data';
import * as L from 'leaflet';

@Component({
  selector: 'app-ride-history',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, RidesTableComponent],
  templateUrl: './ride-history.component.html',
  styles: [`
    .leaflet-div-icon {
      background: transparent;
      border: none;
    }
  `]
})
export class RideHistoryComponent implements OnInit, OnDestroy, AfterViewInit {
  sortedRides: Ride[] = [];
  selectedRide: Ride | null = null;
  selectedRideDetails: RideResponse | null = null;
  private backendRides: Ride[] = [];
  private fullRideResponses: RideResponse[] = [];
  
  dateFilter: string = '';
  filter: 'all' | 'Pending' | 'Accepted' | 'Finished' | 'Rejected' | 'Cancelled' = 'all';
  sortField: RideSortField = 'startTime';
  timeFilter: 'today' | 'week' | 'month' | 'all' = 'all';
  sortDirection: 'asc' | 'desc' = 'desc';
  
  private passengerId: number | null = null;
  private destroy$ = new Subject<void>();
  private map: L.Map | undefined;

  constructor(
    private router: Router,
    private rideService: RideService,
    private authService: AuthService,
    private http: HttpClient,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit() {
    this.passengerId = this.authService.getUserId();
    this.loadRides();
  }
  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  ngAfterViewInit(): void {
      // Map init handled when details are shown
  }

  loadRides(): void {
    if (!this.passengerId) return;

    let fromDate: string | undefined;
    let toDate: string | undefined;

    // Date Filter (Specific Date) takes precedence
    if (this.dateFilter) {
      const start = new Date(this.dateFilter);
      start.setHours(0,0,0,0);
      fromDate = start.toISOString();
      const end = new Date(this.dateFilter);
      end.setHours(23,59,59,999);
      toDate = end.toISOString();
    } 
    // Time Filter (Ranges)
    else if (this.timeFilter !== 'all') {
       const today = new Date();
       const to = new Date(); // now
       let from = new Date();
       
       if (this.timeFilter === 'today') {
         from.setHours(0,0,0,0);
       } else if (this.timeFilter === 'week') {
         from.setDate(today.getDate() - 7);
         from.setHours(0,0,0,0);
       } else if (this.timeFilter === 'month') {
         from.setMonth(today.getMonth() - 1);
         from.setHours(0,0,0,0);
       }
       fromDate = from.toISOString();
       toDate = to.toISOString();
    }

    const backendSort = this.mapSortFieldToBackend(this.sortField);

    // Backend expects uppercase status (PENDING, ACCEPTED, etc.)
    const statusParam = this.filter === 'all' ? undefined : this.filter.toUpperCase();

    this.rideService.getRidesHistory({
        page: 0,
        size: 100,
        sort: `${backendSort},${this.sortDirection}`,
        fromDate: fromDate,
        toDate: toDate,
        passengerId: this.passengerId,
        status: statusParam
    }).pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (page) => {
          this.fullRideResponses = page.content || [];
          
          // Since we are filtering on backend, we just map what we got
          this.sortedRides = this.fullRideResponses.map(r => this.mapToRide(r));
          this.cdr.detectChanges();
        },
        error: (err) => console.error('Failed to load history', err)
      });
  }

  private mapSortFieldToBackend(field: RideSortField): string {
      switch(field) {
          case 'startTime': return 'startTime';
          case 'endTime': return 'endTime';
          case 'distance': return 'distance';
          case 'totalCost': return 'totalCost';
          case 'departure': return 'startLocation.address';
          case 'passengerCount': return 'passengers'; 
          default: return 'startTime';
      }
  }

  private mapToRide(r: RideResponse): Ride {
    return {
      id: String(r.id),
      driverId: String(r.driverId),
      startedAt: r.startTime,
      requestedAt: r.startTime ?? '', 
      completedAt: r.endTime,
      status: r.status === 'FINISHED' ? 'Finished' :
              r.status === 'CANCELLED' ? 'Cancelled' :
              r.status === 'CANCELLED_BY_DRIVER' ? 'Cancelled' :
              r.status === 'CANCELLED_BY_PASSENGER' ? 'Cancelled' :
              r.status === 'PENDING' ? 'Pending' :
              r.status === 'ACCEPTED' ? 'Accepted' :
              r.status === 'REJECTED' ? 'Rejected' : (r.status as any), // Use raw status or map nicely
      fare: r.totalCost ?? 0,
      distance: r.distanceKm ?? 0,
      pickup: { address: r.departure?.address ?? r.start?.address ?? r.startLocation?.address ?? '—' },
      destination: { address: r.destination?.address ?? r.endLocation?.address ?? '—' },
      hasPanic: r.panicPressed,
      passengerName: r.passengers?.[0]?.name ?? 'Unknown',
      cancelledBy: r.status === 'CANCELLED_BY_DRIVER' ? 'driver' : r.status === 'CANCELLED_BY_PASSENGER' ? 'passenger' : 'driver',
      cancellationReason: r.rejectionReason
    };
  }

  setTimeFilter(period: 'today' | 'week' | 'month' | 'all') {
    this.timeFilter = period;
    this.updateView();
  }

  setFilter(status: 'all' | 'Pending' | 'Accepted' | 'Finished' | 'Rejected' | 'Cancelled') {
    this.filter = status;
    this.updateView();
  }

  updateView() {
    this.loadRides();
  }

  handleSort(field: RideSortField) {
    if (this.sortField === field) {
      this.sortDirection = this.sortDirection === 'asc' ? 'desc' : 'asc';
    } else {
      this.sortField = field;
      this.sortDirection = 'desc';
    }
    this.loadRides();
  }

  // Removed sortAndFilterRides and filterByTime as they are now handled by backend params in loadRides

  onViewDetails(ride: RideResponse): void {
    console.log('View ride details:', ride);
    // Implement navigation to ride details page or open modal
    // this.router.navigate(['/rides', ride.id]);
  }

  getDriverName(ride: RideResponse): string {
    if (ride.driver?.name && ride.driver?.surname) {
      return `${ride.driver.name} ${ride.driver.surname}`;
    }
    return 'N/A';
  }

  getPassengerNames(ride: RideResponse): string {
    if (!ride.passengers || ride.passengers.length === 0) {
      return 'N/A';
    }
    return ride.passengers
      .map(p => `${p.name || ''} ${p.surname || ''}`.trim())
      .join(', ');
  }

  getLocationString(location: any): string {
    if (!location) return 'N/A';
    if (location.address) return location.address;
    if (location.latitude && location.longitude) {
      return `${location.latitude.toFixed(4)}, ${location.longitude.toFixed(4)}`;
    }
    return 'N/A';
  }

  formatDate(dateString: string | undefined): string {
    if (!dateString) return 'N/A';
    const date = new Date(dateString);
    return date.toLocaleString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  getStatusClass(status: RideStatus | undefined): string {
    const statusLower = status?.toLowerCase();
    switch (statusLower) {
      case 'finished':
        return 'bg-green-500/20 text-green-400';
      case 'cancelled':
        return 'bg-red-500/20 text-red-400';
      case 'in_progress':
        return 'bg-blue-500/20 text-blue-400';
      case 'accepted':
        return 'bg-yellow-500/20 text-yellow-400';
      case 'pending':
        return 'bg-gray-500/20 text-gray-400';
      default:
        return 'bg-gray-500/20 text-gray-400';
    }
  }

  formatStatus(status: RideStatus | undefined): string {
    if (!status) return 'Unknown';
    return status.replace('_', ' ');
  }

  onRideSelected(ride: Ride) {
    this.selectedRide = ride;
    this.selectedRideDetails = null;
    
    // Fetch full details from backend to ensure we have route points and reviews
    this.rideService.getRide(Number(ride.id)).subscribe({
      next: (details) => {
        this.selectedRideDetails = details;
        this.cdr.detectChanges();
        setTimeout(() => this.initMap(), 100);
      },
      error: (err) => console.error('Failed to load ride details', err)
    });
  }

  initMap() {
    if (!this.selectedRideDetails || !document.getElementById('history-map')) return;

    if (this.map) {
      this.map.remove();
      this.map = undefined;
    }

    const start = this.selectedRideDetails.departure || this.selectedRideDetails.startLocation;
    
    if (!start) return; 

    const startCoords: [number, number] = [start.latitude, start.longitude];
    
    this.map = L.map('history-map', { zoomControl: false }).setView(startCoords, 13);
    
    L.tileLayer('https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png', {
      maxZoom: 19
    }).addTo(this.map);

    // Markers
    const pickupIcon = L.divIcon({
        className: '',
        html: `<div style="width:12px;height:12px;border-radius:50%;background:#eab308;box-shadow:0 0 10px #eab308;"></div>`,
        iconSize: [12, 12]
    });
    
    const destIcon = L.divIcon({
        className: '',
        html: `<div style="width:12px;height:12px;border-radius:50%;background:#ef4444;box-shadow:0 0 10px #ef4444;"></div>`,
        iconSize: [12, 12]
    });

    L.marker(startCoords, { icon: pickupIcon }).addTo(this.map);
    const end = this.selectedRideDetails.destination || this.selectedRideDetails.endLocation;
    if (end) {
        L.marker([end.latitude, end.longitude], { icon: destIcon }).addTo(this.map);
    }
    
    // Always fetch fresh route from OSRM for accurate road-following display
    if (end) {
        this.fetchAndDrawRoute(start, end, startCoords);
    }
  }

  private fetchAndDrawRoute(start: any, end: any, startCoords: [number, number]) {
    // Build waypoints - only include valid stops with coordinates
    const validStops = (this.selectedRideDetails?.stops || [])
        .filter((s: any) => s && s.latitude && s.longitude);
    
    const waypoints = [
        { longitude: start.longitude, latitude: start.latitude },
        ...validStops.map((s: any) => ({ longitude: s.longitude, latitude: s.latitude })),
        { longitude: end.longitude, latitude: end.latitude }
    ];

    const coordinatesString = waypoints
        .map((p: any) => `${p.longitude},${p.latitude}`)
        .join(';');

    // Use OSRM via Angular proxy to avoid CORS issues
    const osrmUrl = `/osrm/route/v1/driving/${coordinatesString}?overview=full&geometries=geojson`;

    console.log('Fetching route from OSRM:', osrmUrl);
    
    this.http.get(osrmUrl).subscribe({
        next: (response: any) => {
            console.log('OSRM response:', response);
            const coordinates = response?.routes?.[0]?.geometry?.coordinates;
            if (coordinates && coordinates.length > 0 && this.map) {
               const latlngs: [number, number][] = coordinates.map((coord: number[]) => [coord[1], coord[0]] as [number, number]);
               L.polyline(latlngs, { color: '#fbbf24', weight: 4, opacity: 0.8, dashArray: '10, 6' }).addTo(this.map);
               this.map.fitBounds(latlngs, { padding: [30, 30] });
            } else {
               console.warn('No route coordinates in OSRM response');
               this.drawFromBackendOrFallback(startCoords, [end.latitude, end.longitude]);
            }
        },
        error: (err) => {
            console.error('OSRM request failed:', err);
            this.drawFromBackendOrFallback(startCoords, [end.latitude, end.longitude]);
        }
    });
  }

  private drawFromBackendOrFallback(startCoords: [number, number], endCoords: [number, number]) {
      if (!this.map) return;
      
      // Try to use backend routePoints first
      if (this.selectedRideDetails?.routePoints && this.selectedRideDetails.routePoints.length > 1) {
          const latlngs: [number, number][] = this.selectedRideDetails.routePoints
              .sort((a,b) => a.order - b.order)
              .map(rp => [rp.location.latitude, rp.location.longitude]);
          L.polyline(latlngs, { color: '#fbbf24', weight: 4, opacity: 0.7, dashArray: '10, 6' }).addTo(this.map);
          this.map.fitBounds(latlngs, { padding: [20, 20] });
      } else {
          // Last resort: straight dashed line
          const simpleRoute: [number, number][] = [startCoords, endCoords];
          L.polyline(simpleRoute, { color: '#fbbf24', weight: 3, opacity: 0.6, dashArray: '8, 8' }).addTo(this.map);
          const bounds = L.latLngBounds([startCoords, endCoords]);
          this.map.fitBounds(bounds, { padding: [50, 50] });
      }
  }

  closeDetails() {
    this.selectedRide = null;
    this.selectedRideDetails = null;
    if (this.map) {
      this.map.remove();
      this.map = undefined;
    }
  }

  orderAgain() {
    if (!this.selectedRideDetails) return;
    const start = this.selectedRideDetails.departure || this.selectedRideDetails.startLocation;
    const end = this.selectedRideDetails.destination || this.selectedRideDetails.endLocation;
    
    if (start && end) {
      this.router.navigate(['/passenger/home'], { 
        state: { 
          fromFavorites: true, 
          startLocation: start,
          endLocation: end
        } 
      });
    }
  }

  scheduleLater() {
     this.orderAgain();
  }

  addToFavorites(ride: Ride) {
    if (!this.passengerId) {
      console.error('Passenger ID is not available');
      alert('Unable to add to favorites. Please log in again.');
      return;
    }

    const favoriteRouteRequest = {
      start: ride.pickup.address,
      destination: ride.destination.address,
      // TODO route name could be added here if needed
    };
    
    //console.log('Adding route to favorites:', favoriteRouteRequest);

    this.rideService.addRouteToFavorites(this.passengerId, favoriteRouteRequest).subscribe({
      next: () => {
        //console.log('Route added to favorites');
        alert('Route added to favorites successfully!');
        this.selectedRide = null;
      },
      error: (error) => {
        //console.error('Error adding route to favorites:', error);
        alert('Failed to add route to favorites. Please try again.');
      }
    });
  }
  
}
