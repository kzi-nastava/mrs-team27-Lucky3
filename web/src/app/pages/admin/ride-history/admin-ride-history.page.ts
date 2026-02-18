import { Component, OnInit, ChangeDetectorRef, OnDestroy, AfterViewInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { RideResponse, RideStatus } from '../../../infrastructure/rest/model/ride-response.model';
import { RouterModule } from '@angular/router';
import { RideService } from '../../../infrastructure/rest/ride.service';
import { Subject, takeUntil } from 'rxjs';
import * as L from 'leaflet';
import { environment } from '../../../../env/environment';

export type AdminRideSortField = 'startTime' | 'endTime' | 'distance' | 'departure' | 'totalCost' | 'status' | 'cancelledBy' | 'panic';

@Component({
  selector: 'app-admin-ride-history',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './admin-ride-history.page.html',
  styles: [`
    .leaflet-div-icon {
      background: transparent;
      border: none;
    }
  `]
})
export class AdminRideHistoryPage implements OnInit, OnDestroy, AfterViewInit {
  rides: RideResponse[] = [];
  selectedRide: RideResponse | null = null;
  
  // Search/filter controls
  searchType: 'driver' | 'passenger' = 'driver';
  searchId: number | null = null;
  dateFrom: string = '';
  dateTo: string = '';
  statusFilter: string = 'all';
  sortField: AdminRideSortField = 'startTime';
  sortDirection: 'asc' | 'desc' = 'desc';
  
  // Pagination
  currentPage: number = 0;
  pageSize: number = 5;
  totalElements: number = 0;
  totalPages: number = 0;

  // E2E test support: increments on each data load for fast refresh detection
  loadId: number = 0;
  
  // User info for display
  selectedUserName: string = '';
  selectedUserEmail: string = '';
  
  private destroy$ = new Subject<void>();
  private map: L.Map | undefined;

  readonly statusOptions = ['all', 'FINISHED', 'CANCELLED', 'CANCELLED_BY_DRIVER', 'CANCELLED_BY_PASSENGER'];

  constructor(
    private router: Router,
    private route: ActivatedRoute,
    private rideService: RideService,
    private http: HttpClient,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit() {
    // Load all rides by default
    this.loadRides();
    
    // Check for query params to pre-populate search
    this.route.queryParams.pipe(takeUntil(this.destroy$)).subscribe(params => {
      if (params['driverId']) {
        this.searchType = 'driver';
        this.searchId = +params['driverId'];
        this.loadRides();
      } else if (params['passengerId']) {
        this.searchType = 'passenger';
        this.searchId = +params['passengerId'];
        this.loadRides();
      }
    });
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

  onSearch() {
    this.currentPage = 0;
    this.loadRides();
  }

  clearSearch() {
    this.searchId = null;
    this.selectedUserName = '';
    this.selectedUserEmail = '';
    this.currentPage = 0;
    this.loadRides();
  }

  loadRides(): void {
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

    const params: any = {
      page: this.currentPage,
      size: this.pageSize,
      sort: `${backendSort},${this.sortDirection}`,
      fromDate,
      toDate,
      status: statusParam
    };

    // Only add driver/passenger filter if searchId is provided
    if (this.searchId) {
      if (this.searchType === 'driver') {
        params.driverId = this.searchId;
      } else {
        params.passengerId = this.searchId;
      }
    }

    this.rideService.getRidesHistory(params)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (page) => {
          this.rides = page.content || [];
          this.totalElements = page.totalElements || 0;
          this.totalPages = page.totalPages || 0;
          this.loadId++;
          // Only set user info when searching by specific ID
          if (this.searchId && this.rides.length > 0) {
            if (this.searchType === 'driver' && this.rides[0].driver) {
              this.selectedUserName = `${this.rides[0].driver.name || ''} ${this.rides[0].driver.surname || ''}`.trim();
              this.selectedUserEmail = this.rides[0].driver.email || '';
            } else if (this.searchType === 'passenger' && this.rides[0].passengers?.length) {
              const p = this.rides[0].passengers[0];
              this.selectedUserName = `${p.name || ''} ${p.surname || ''}`.trim();
              this.selectedUserEmail = p.email || '';
            }
          } else if (!this.searchId) {
            // Clear user info when viewing all rides
            this.selectedUserName = '';
            this.selectedUserEmail = '';
          } else {
            // Search returned no results - clear user info
            this.selectedUserName = '';
            this.selectedUserEmail = '';
          }
          this.cdr.detectChanges();
        },
        error: (err) => {
          console.error('Failed to load history', err);
          this.loadId++;
          this.cdr.detectChanges();
        }
      });
  }

  private mapSortFieldToBackend(field: AdminRideSortField): string {
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

  handleSort(field: AdminRideSortField) {
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

  onRideSelected(ride: RideResponse) {
    this.selectedRide = ride;
    this.cdr.detectChanges();

    // Fetch full details if needed
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

    const mapContainer = document.getElementById('admin-history-map');
    if (!mapContainer) return;

    if (this.map) {
      this.map.remove();
    }

    const start = this.selectedRide.departure || this.selectedRide.start || this.selectedRide.startLocation;
    if (!start) return;

    const startCoords: [number, number] = [start.latitude!, start.longitude!];

    this.map = L.map('admin-history-map', {
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

  getPassengerNames(ride: RideResponse): string {
    if (!ride.passengers || ride.passengers.length === 0) return '—';
    return ride.passengers.map(p => `${p.name || ''} ${p.surname || ''}`.trim()).join(', ');
  }

  getDriverName(ride: RideResponse): string {
    if (ride.driver?.name || ride.driver?.surname) {
      return `${ride.driver.name || ''} ${ride.driver.surname || ''}`.trim();
    }
    return '—';
  }

  orderAgain() {
    if (!this.selectedRide) return;
    const start = this.selectedRide.departure || this.selectedRide.startLocation;
    const end = this.selectedRide.destination || this.selectedRide.endLocation;
    if (start && end) {
      this.router.navigate(['/passenger/home'], {
        queryParams: {
          pickupLat: start.latitude,
          pickupLng: start.longitude,
          pickupAddress: start.address,
          destLat: end.latitude,
          destLng: end.longitude,
          destAddress: end.address
        }
      });
    }
  }

  scheduleLater() {
    if (!this.selectedRide) return;
    const start = this.selectedRide.departure || this.selectedRide.startLocation;
    const end = this.selectedRide.destination || this.selectedRide.endLocation;
    if (start && end) {
      this.router.navigate(['/passenger/home'], {
        queryParams: {
          pickupLat: start.latitude,
          pickupLng: start.longitude,
          pickupAddress: start.address,
          destLat: end.latitude,
          destLng: end.longitude,
          destAddress: end.address,
          schedule: 'true'
        }
      });
    }
  }
}
