import { Component, OnInit, ChangeDetectorRef, OnDestroy, AfterViewInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { RideResponse, RideStatus } from '../../../infrastructure/rest/model/ride-response.model';
import { RideService } from '../../../infrastructure/rest/ride.service';
import { AuthService } from '../../../infrastructure/auth/auth.service';
import { Subject, takeUntil } from 'rxjs';
import * as L from 'leaflet';

export type PassengerRideSortField = 'startTime' | 'endTime' | 'distance' | 'departure' | 'totalCost' | 'status' | 'cancelledBy' | 'panic';

@Component({
  selector: 'app-ride-history',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './ride-history.component.html',
  styles: [`
    .leaflet-div-icon {
      background: transparent;
      border: none;
    }
    .reviewable-row {
      box-shadow: inset 0 0 0 1px rgba(59, 130, 246, 0.5);
      background-color: rgba(59, 130, 246, 0.05);
    }
  `]
})
export class RideHistoryComponent implements OnInit, OnDestroy, AfterViewInit {
  rides: RideResponse[] = [];
  selectedRide: RideResponse | null = null;

  // Filter controls
  dateFrom: string = '';
  dateTo: string = '';
  statusFilter: string = 'all';
  sortField: PassengerRideSortField = 'startTime';
  sortDirection: 'asc' | 'desc' = 'desc';

  // Pagination
  currentPage: number = 0;
  pageSize: number = 5;
  totalElements: number = 0;
  totalPages: number = 0;

  private passengerId: number | null = null;
  private destroy$ = new Subject<void>();
  private map: L.Map | undefined;
  private pendingRideId: number | null = null;

  readonly statusOptions = ['all', 'FINISHED', 'CANCELLED', 'CANCELLED_BY_DRIVER', 'CANCELLED_BY_PASSENGER'];

  constructor(
    private router: Router,
    private route: ActivatedRoute,
    private rideService: RideService,
    private authService: AuthService,
    private http: HttpClient,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit() {
    this.passengerId = this.authService.getUserId();

    // Check for rideId query param (e.g. from notification click)
    const rideIdParam = this.route.snapshot.queryParamMap.get('rideId');
    if (rideIdParam) {
      this.pendingRideId = Number(rideIdParam);
    }

    this.loadRides();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    if (this.map) {
      this.map.remove();
    }
  }

  ngAfterViewInit(): void {
    // Map init handled when details are shown
  }

  loadRides(): void {
    if (!this.passengerId) return;

    let fromDate: string | undefined;
    let toDate: string | undefined;

    if (this.dateFrom) {
      const start = new Date(this.dateFrom);
      start.setHours(0, 0, 0, 0);
      fromDate = start.toISOString();
    }
    if (this.dateTo) {
      const end = new Date(this.dateTo);
      end.setHours(23, 59, 59, 999);
      toDate = end.toISOString();
    }

    const backendSort = this.mapSortFieldToBackend(this.sortField);
    const historyStatuses = 'FINISHED,CANCELLED,CANCELLED_BY_DRIVER,CANCELLED_BY_PASSENGER';
    const statusParam = this.statusFilter === 'all' ? historyStatuses : this.statusFilter;

    this.rideService.getRidesHistory({
      page: this.currentPage,
      size: this.pageSize,
      sort: `${backendSort},${this.sortDirection}`,
      fromDate,
      toDate,
      passengerId: this.passengerId,
      status: statusParam
    }).pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (page) => {
          this.rides = page.content || [];
          this.totalElements = page.totalElements || 0;
          this.totalPages = page.totalPages || 0;
          this.cdr.detectChanges();

          // Auto-select ride from query param (e.g. notification deep link)
          if (this.pendingRideId) {
            const targetRide = this.rides.find(r => r.id === this.pendingRideId);
            if (targetRide) {
              this.onRideSelected(targetRide);
            } else {
              // Ride not in current page/filter — fetch and select it directly
              this.rideService.getRide(this.pendingRideId).subscribe({
                next: (details) => {
                  this.rides = [details, ...this.rides];
                  this.onRideSelected(details);
                },
                error: () => console.error('Failed to load ride from notification')
              });
            }
            this.pendingRideId = null;
          }
        },
        error: (err) => console.error('Failed to load history', err)
      });
  }

  private mapSortFieldToBackend(field: PassengerRideSortField): string {
    switch (field) {
      case 'startTime': return 'startTime';
      case 'endTime': return 'endTime';
      case 'distance': return 'distance';
      case 'totalCost': return 'totalCost';
      case 'departure': return 'startLocation.address';
      case 'status': return 'status';
      case 'cancelledBy': return 'status';
      case 'panic': return 'panicPressed';
      default: return 'startTime';
    }
  }

  handleSort(field: PassengerRideSortField) {
    if (this.sortField === field) {
      this.sortDirection = this.sortDirection === 'asc' ? 'desc' : 'asc';
    } else {
      this.sortField = field;
      this.sortDirection = 'desc';
    }
    this.currentPage = 0;
    this.loadRides();
  }

  // Pagination
  goToPage(page: number) {
    if (page < 0 || page >= this.totalPages) return;
    this.currentPage = page;
    this.loadRides();
  }

  nextPage() {
    this.goToPage(this.currentPage + 1);
  }

  prevPage() {
    this.goToPage(this.currentPage - 1);
  }

  get visiblePages(): number[] {
    const pages: number[] = [];
    const maxVisible = 5;
    let start = Math.max(0, this.currentPage - Math.floor(maxVisible / 2));
    let end = Math.min(this.totalPages, start + maxVisible);
    if (end - start < maxVisible) {
      start = Math.max(0, end - maxVisible);
    }
    for (let i = start; i < end; i++) {
      pages.push(i);
    }
    return pages;
  }

  get startItem(): number {
    return this.totalElements === 0 ? 0 : this.currentPage * this.pageSize + 1;
  }

  get endItem(): number {
    return Math.min((this.currentPage + 1) * this.pageSize, this.totalElements);
  }

  // Detail panel
  onRideSelected(ride: RideResponse) {
    this.selectedRide = ride;
    this.cdr.detectChanges();

    // Fetch full details to get route points, reviews, etc.
    this.rideService.getRide(ride.id).pipe(takeUntil(this.destroy$)).subscribe({
      next: (fullRide) => {
        this.selectedRide = fullRide;
        this.cdr.detectChanges();
        setTimeout(() => this.initMap(), 100);
      },
      error: () => {
        setTimeout(() => this.initMap(), 100);
      }
    });
  }

  closeDetails() {
    this.selectedRide = null;
    if (this.map) {
      this.map.remove();
      this.map = undefined;
    }
  }

  private initMap() {
    if (!this.selectedRide) return;

    const mapContainer = document.getElementById('history-map');
    if (!mapContainer) return;

    if (this.map) {
      this.map.remove();
    }

    const start = this.selectedRide.departure || this.selectedRide.start || this.selectedRide.startLocation;
    if (!start) return;

    const startCoords: [number, number] = [start.latitude!, start.longitude!];

    this.map = L.map('history-map', {
      center: startCoords,
      zoom: 14,
      zoomControl: true
    });

    L.tileLayer('https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png', {
      maxZoom: 19
    }).addTo(this.map);

    // Markers
    const pickupIcon = L.divIcon({
      className: '',
      html: `<div style="width:12px;height:12px;border-radius:50%;background:#22c55e;box-shadow:0 0 10px #22c55e;"></div>`,
      iconSize: [12, 12]
    });

    const destIcon = L.divIcon({
      className: '',
      html: `<div style="width:12px;height:12px;border-radius:50%;background:#ef4444;box-shadow:0 0 10px #ef4444;"></div>`,
      iconSize: [12, 12]
    });

    L.marker(startCoords, { icon: pickupIcon }).addTo(this.map);
    const end = this.selectedRide.destination || this.selectedRide.endLocation;
    if (end) {
      L.marker([end.latitude!, end.longitude!], { icon: destIcon }).addTo(this.map);
    }

    if (end) {
      this.fetchAndDrawRoute(start, end, startCoords);
    }
  }

  private fetchAndDrawRoute(start: any, end: any, startCoords: [number, number]) {
    const validStops = (this.selectedRide?.stops || [])
      .filter((s: any) => s && s.latitude && s.longitude);

    const waypoints = [
      { longitude: start.longitude, latitude: start.latitude },
      ...validStops.map((s: any) => ({ longitude: s.longitude, latitude: s.latitude })),
      { longitude: end.longitude, latitude: end.latitude }
    ];

    const coordinatesString = waypoints
      .map((p: any) => `${p.longitude},${p.latitude}`)
      .join(';');

    const osrmUrl = `/osrm/route/v1/driving/${coordinatesString}?overview=full&geometries=geojson`;

    this.http.get(osrmUrl).subscribe({
      next: (response: any) => {
        const coordinates = response?.routes?.[0]?.geometry?.coordinates;
        if (coordinates && coordinates.length > 0 && this.map) {
          const latlngs: [number, number][] = coordinates.map((coord: number[]) => [coord[1], coord[0]] as [number, number]);
          L.polyline(latlngs, { color: '#fbbf24', weight: 4, opacity: 0.8, dashArray: '10, 6' }).addTo(this.map);
          this.map.fitBounds(latlngs, { padding: [30, 30] });
        } else {
          this.drawFromBackendOrFallback(startCoords, [end.latitude, end.longitude]);
        }
      },
      error: () => {
        this.drawFromBackendOrFallback(startCoords, [end.latitude, end.longitude]);
      }
    });
  }

  private drawFromBackendOrFallback(startCoords: [number, number], endCoords: [number, number]) {
    if (!this.map) return;

    if (this.selectedRide?.routePoints && this.selectedRide.routePoints.length > 1) {
      const latlngs: [number, number][] = this.selectedRide.routePoints
        .sort((a, b) => a.order - b.order)
        .map(rp => [rp.location.latitude, rp.location.longitude]);
      L.polyline(latlngs, { color: '#fbbf24', weight: 4, opacity: 0.7, dashArray: '10, 6' }).addTo(this.map);
      this.map.fitBounds(latlngs, { padding: [20, 20] });
    } else {
      const simpleRoute: [number, number][] = [startCoords, endCoords];
      L.polyline(simpleRoute, { color: '#fbbf24', weight: 3, opacity: 0.6, dashArray: '8, 8' }).addTo(this.map);
      const bounds = L.latLngBounds([startCoords, endCoords]);
      this.map.fitBounds(bounds, { padding: [50, 50] });
    }
  }

  // Helper methods
  getLocationAddress(ride: RideResponse, type: 'start' | 'end'): string {
    if (type === 'start') {
      return ride.departure?.address || ride.start?.address || ride.startLocation?.address || '—';
    }
    return ride.destination?.address || ride.endLocation?.address || '—';
  }

  formatDate(dateString: string | undefined): string {
    if (!dateString) return '—';
    const date = new Date(dateString);
    return date.toLocaleString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  formatShortDate(dateString: string | undefined): string {
    if (!dateString) return '—';
    return new Date(dateString).toLocaleDateString('en-GB', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric'
    });
  }

  formatTime(dateString: string | undefined): string {
    if (!dateString) return '—';
    return new Date(dateString).toLocaleTimeString('en-US', {
      hour: 'numeric',
      minute: '2-digit',
      hour12: true
    });
  }

  formatPrice(price: number | undefined): string {
    if (price == null) return '—';
    return `$${price.toFixed(2)}`;
  }

  formatDistance(distance: number | undefined): string {
    if (distance == null) return '—';
    return `${distance.toFixed(2)} km`;
  }

  getStatusClass(status: string | undefined): string {
    const s = status?.toUpperCase();
    switch (s) {
      case 'FINISHED': return 'bg-green-500/20 text-green-400 border-green-500/30';
      case 'CANCELLED':
      case 'CANCELLED_BY_DRIVER':
      case 'CANCELLED_BY_PASSENGER': return 'bg-red-500/20 text-red-400 border-red-500/30';
      case 'IN_PROGRESS': return 'bg-blue-500/20 text-blue-400 border-blue-500/30';
      case 'ACCEPTED': return 'bg-yellow-500/20 text-yellow-400 border-yellow-500/30';
      case 'PENDING': return 'bg-gray-500/20 text-gray-400 border-gray-500/30';
      case 'PANIC': return 'bg-red-600/30 text-red-300 border-red-600/50';
      case 'REJECTED': return 'bg-orange-500/20 text-orange-400 border-orange-500/30';
      default: return 'bg-gray-500/20 text-gray-400 border-gray-500/30';
    }
  }

  formatStatus(status: string | undefined): string {
    if (!status) return 'Unknown';
    return status.replace(/_/g, ' ').toLowerCase().replace(/\b\w/g, l => l.toUpperCase());
  }

  getCancelledBy(ride: RideResponse): string {
    if (ride.status === 'CANCELLED_BY_DRIVER') return 'Driver';
    if (ride.status === 'CANCELLED_BY_PASSENGER') return 'Passenger';
    if (ride.status === 'CANCELLED') return 'System';
    return '—';
  }

  getDriverName(ride: RideResponse): string {
    if (ride.driver?.name || ride.driver?.surname) {
      return `${ride.driver.name || ''} ${ride.driver.surname || ''}`.trim();
    }
    return '—';
  }

  // Passenger-specific actions
  isRideReviewable(ride: RideResponse): boolean {
    if (!this.passengerId) return false;
    if (ride.status !== 'FINISHED') return false;
    if (!ride.endTime) return false;
    const threeDaysMs = 3 * 24 * 60 * 60 * 1000;
    if (Date.now() - new Date(ride.endTime).getTime() > threeDaysMs) return false;
    return !ride.reviews?.some(rev => rev.passengerId === this.passengerId);
  }

  canReviewSelectedRide(): boolean {
    if (!this.selectedRide || !this.passengerId) return false;
    if (this.selectedRide.status !== 'FINISHED') return false;
    if (!this.selectedRide.endTime) return false;
    const threeDaysMs = 3 * 24 * 60 * 60 * 1000;
    if (Date.now() - new Date(this.selectedRide.endTime).getTime() > threeDaysMs) return false;
    return !this.selectedRide.reviews?.some(rev => rev.passengerId === this.passengerId);
  }

  onReviewRide(): void {
    if (this.selectedRide) {
      this.router.navigate(['/review'], { queryParams: { rideId: this.selectedRide.id } });
    }
  }

  orderAgain() {
    if (!this.selectedRide) return;
    const start = this.selectedRide.departure || this.selectedRide.startLocation;
    const end = this.selectedRide.destination || this.selectedRide.endLocation;
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

  addToFavorites() {
    if (!this.passengerId || !this.selectedRide) {
      alert('Unable to add to favorites. Please log in again.');
      return;
    }

    const start = this.selectedRide.departure || this.selectedRide.startLocation;
    const end = this.selectedRide.destination || this.selectedRide.endLocation;

    const favoriteRouteRequest = {
      start: start?.address || '',
      destination: end?.address || ''
    };

    this.rideService.addRouteToFavorites(this.passengerId, favoriteRouteRequest).subscribe({
      next: () => {
        alert('Route added to favorites successfully!');
      },
      error: () => {
        alert('Failed to add route to favorites. Please try again.');
      }
    });
  }
}
