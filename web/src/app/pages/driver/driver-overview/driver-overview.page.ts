import { ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { Ride } from '../../../shared/data/mock-data';
import { RidesTableComponent, RideSortField } from '../../../shared/rides/rides-table/rides-table.component';
import { RideService, PageResponse } from '../../../infrastructure/rest/ride.service';
import { AuthService } from '../../../infrastructure/auth/auth.service';
import { DriverService } from '../../../infrastructure/rest/driver.service';
import { Subject, takeUntil } from 'rxjs';
import { RideResponse } from '../../../infrastructure/rest/model/ride-response.model';

@Component({
  selector: 'app-driver-overview',
  standalone: true,
  imports: [CommonModule, FormsModule, RidesTableComponent],
  templateUrl: './driver-overview.page.html',
  styleUrl: './driver-overview.page.css'
})
export class DriverOverviewPage implements OnInit, OnDestroy {
  // Expose Math for template
  Math = Math;
  
  // Time filter for stats (today, week, month, all)
  timeFilter: 'today' | 'week' | 'month' | 'all' = 'week';
  
  // Status filter (all, FINISHED, CANCELLED)
  statusFilter: 'all' | 'FINISHED' | 'CANCELLED' = 'all';
  
  // Date range filters
  dateFrom: string = '';
  dateTo: string = '';
  
  // Sorting
  sortField: RideSortField = 'startTime';
  sortDirection: 'asc' | 'desc' = 'desc';

  // Pagination
  currentPage: number = 0;
  pageSize: number = 10;
  totalElements: number = 0;
  totalPages: number = 0;

  // Data
  sortedRides: Ride[] = [];
  isLoading: boolean = false;
  
  stats = {
    totalEarnings: 0,
    earnings: 0,
    finishedRides: 0,
    totalRides: 0,
    avgRating: 0,
    avgVehicleRating: 0,
    totalDistance: 0
  };

  driver = {
    ratingCount: 0,
    vehicleRatingCount: 0
  };

  private destroy$ = new Subject<void>();
  private driverId: number | null = null;
  
  // LocalStorage key for persisting filters
  private readonly STORAGE_KEY = 'driver_overview_filters';

  constructor(
    private router: Router,
    private rideService: RideService,
    private authService: AuthService,
    private driverService: DriverService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit() {
    this.driverId = this.authService.getUserId();
    // Restore filters from localStorage
    this.restoreFilters();
    // Only apply time filter dates if no custom date range was restored
    if (!this.dateFrom && !this.dateTo) {
      this.applyTimeFilterDates();
    }
    this.loadRides();
    this.loadDriverStats();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  /**
   * Apply date range based on time filter buttons
   */
  private applyTimeFilterDates(): void {
    const now = new Date();
    let fromDate: Date | null = null;
    
    switch (this.timeFilter) {
      case 'today':
        fromDate = new Date(now.getFullYear(), now.getMonth(), now.getDate(), 0, 0, 0);
        break;
      case 'week':
        fromDate = new Date(now);
        fromDate.setDate(now.getDate() - 7);
        fromDate.setHours(0, 0, 0, 0);
        break;
      case 'month':
        fromDate = new Date(now.getFullYear(), now.getMonth(), 1, 0, 0, 0);
        break;
      case 'all':
        fromDate = null;
        break;
    }

    if (fromDate) {
      this.dateFrom = this.formatDateForInput(fromDate);
      this.dateTo = this.formatDateForInput(now);
    } else {
      this.dateFrom = '';
      this.dateTo = '';
    }
  }

  /**
   * Format date for HTML date input (YYYY-MM-DD)
   */
  private formatDateForInput(date: Date): string {
    return date.toISOString().split('T')[0];
  }

  /**
   * Format date for backend API (ISO 8601)
   */
  private formatDateForApi(dateStr: string, endOfDay: boolean = false): string | undefined {
    if (!dateStr) return undefined;
    const date = new Date(dateStr);
    if (endOfDay) {
      date.setHours(23, 59, 59, 999);
    } else {
      date.setHours(0, 0, 0, 0);
    }
    return date.toISOString();
  }

  /**
   * Build sort string for backend (e.g., 'startTime,desc')
   */
  private buildSortString(): string {
    // Map frontend field names to backend entity field names
    // Note: passengerCount can't be sorted by backend (it's a collection size), so we skip it
    const fieldMap: Record<RideSortField, string | null> = {
      'startTime': 'startTime',
      'endTime': 'endTime',
      'distance': 'distance',
      'departure': 'startLocation.address',
      'passengerCount': null, // Not sortable on backend
      'totalCost': 'totalCost'
    };
    
    const backendField = fieldMap[this.sortField];
    // If field is not sortable on backend, default to startTime
    if (!backendField) {
      return `startTime,${this.sortDirection}`;
    }
    return `${backendField},${this.sortDirection}`;
  }

  loadRides(): void {
    if (!this.driverId) return;

    this.isLoading = true;

    // Build status filter for backend
    let statusParam: string | undefined;
    if (this.statusFilter === 'FINISHED') {
      statusParam = 'FINISHED';
    } else if (this.statusFilter === 'CANCELLED') {
      // Backend might need multiple statuses - for now we'll filter on frontend for cancelled
      statusParam = undefined; // We'll handle this below
    }

    this.rideService.getRidesHistory({
      driverId: this.driverId,
      page: this.currentPage,
      size: this.pageSize,
      sort: this.buildSortString(),
      fromDate: this.formatDateForApi(this.dateFrom),
      toDate: this.formatDateForApi(this.dateTo, true),
      status: statusParam
    }).pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (page) => {
          // Filter to only past rides (FINISHED, CANCELLED variants)
          const pastStatuses = ['FINISHED', 'CANCELLED', 'CANCELLED_BY_DRIVER', 'CANCELLED_BY_PASSENGER'];
          let relevant = (page.content ?? []).filter(r => pastStatuses.includes(r.status as string));
          
          // Apply status filter on frontend if needed (for CANCELLED which includes multiple statuses)
          if (this.statusFilter === 'CANCELLED') {
            relevant = relevant.filter(r => 
              r.status === 'CANCELLED' || 
              r.status === 'CANCELLED_BY_DRIVER' || 
              r.status === 'CANCELLED_BY_PASSENGER'
            );
          } else if (this.statusFilter === 'FINISHED') {
            relevant = relevant.filter(r => r.status === 'FINISHED');
          }
          
          // Map and re-sort on frontend to handle null startTime correctly
          let mappedRides = relevant.map(r => this.mapToRide(r));
          mappedRides = this.sortRidesOnFrontend(mappedRides);
          
          this.sortedRides = mappedRides;
          this.totalElements = page.totalElements ?? 0;
          this.totalPages = page.totalPages ?? 0;
          
          this.calculateStats(page.content ?? []);
          this.isLoading = false;
          this.cdr.detectChanges();
        },
        error: (err) => {
          console.error('Failed to load history', err);
          this.isLoading = false;
          this.cdr.detectChanges();
        }
      });
  }

  /**
   * Re-sort rides on frontend to handle null values properly
   */
  private sortRidesOnFrontend(rides: Ride[]): Ride[] {
    return rides.sort((a, b) => {
      let aVal: any;
      let bVal: any;
      
      switch (this.sortField) {
        case 'startTime':
          // Use startedAt, fallback to requestedAt for cancelled rides
          aVal = a.startedAt || a.requestedAt || '';
          bVal = b.startedAt || b.requestedAt || '';
          break;
        case 'endTime':
          aVal = a.completedAt || '';
          bVal = b.completedAt || '';
          break;
        case 'distance':
          aVal = a.distance ?? 0;
          bVal = b.distance ?? 0;
          break;
        case 'departure':
          aVal = a.pickup?.address?.toLowerCase() || '';
          bVal = b.pickup?.address?.toLowerCase() || '';
          break;
        case 'passengerCount':
          aVal = a.passengerCount ?? 0;
          bVal = b.passengerCount ?? 0;
          break;
        case 'totalCost':
          aVal = a.fare ?? 0;
          bVal = b.fare ?? 0;
          break;
        default:
          aVal = a.startedAt || '';
          bVal = b.startedAt || '';
      }
      
      // Compare
      let comparison = 0;
      if (typeof aVal === 'string' && typeof bVal === 'string') {
        comparison = aVal.localeCompare(bVal);
      } else {
        comparison = aVal < bVal ? -1 : aVal > bVal ? 1 : 0;
      }
      
      return this.sortDirection === 'desc' ? -comparison : comparison;
    });
  }

  private mapToRide(r: RideResponse): Ride {
    // Keep startedAt and requestedAt separate
    // startedAt = when the ride actually started (null for cancelled rides that never started)
    // requestedAt = when the ride was requested/scheduled
    const startedAt = r.startTime ?? '';
    const requestedAt = r.scheduledTime ?? r.startTime ?? '';
    const completedAt = r.endTime ?? '';
    
    return {
      id: String(r.id),
      driverId: String(this.driverId),
      startedAt: startedAt,
      requestedAt: requestedAt,
      completedAt: completedAt,
      status: r.status === 'FINISHED' ? 'Finished' :
              r.status === 'CANCELLED' ? 'Cancelled' :
              r.status === 'CANCELLED_BY_DRIVER' ? 'Cancelled' :
              r.status === 'CANCELLED_BY_PASSENGER' ? 'Cancelled' :
              r.status === 'PENDING' ? 'Pending' :
              r.status === 'ACCEPTED' ? 'Accepted' :
              r.status === 'REJECTED' ? 'Rejected' : 'all',
      fare: r.totalCost ?? 0,
      distance: r.distanceKm ?? 0,
      pickup: { address: r.departure?.address ?? r.start?.address ?? r.startLocation?.address ?? '—' },
      destination: { address: r.destination?.address ?? r.endLocation?.address ?? '—' },
      hasPanic: r.panicPressed,
      passengerName: r.passengers?.[0]?.name ?? 'Unknown',
      passengerCount: r.passengers?.length ?? 1,
      cancelledBy: r.status === 'CANCELLED_BY_DRIVER' ? 'driver' : r.status === 'CANCELLED_BY_PASSENGER' ? 'passenger' : 'driver',
      cancellationReason: r.rejectionReason
    };
  }

  /**
   * Load driver stats (ratings) from backend - same as dashboard
   */
  loadDriverStats(): void {
    if (!this.driverId) return;

    this.driverService.getStats(this.driverId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (stats) => {
          this.stats.avgRating = stats.averageRating;
          this.stats.avgVehicleRating = stats.averageVehicleRating;
          this.driver.ratingCount = stats.totalRatings;
          this.driver.vehicleRatingCount = stats.totalVehicleRatings;
          this.cdr.detectChanges();
        },
        error: (err) => {
          console.error('Failed to load driver stats', err);
        }
      });
  }

  calculateStats(rides: RideResponse[]) {
    // Only count completed (FINISHED) rides for earnings and distance
    const completedRides = rides.filter(r => r.status === 'FINISHED');

    // Preserve ratings (loaded from getStats) while updating ride-based stats
    this.stats = {
      totalEarnings: completedRides.reduce((sum, r) => sum + (r.totalCost ?? 0), 0),
      earnings: 0,
      finishedRides: completedRides.length,
      totalRides: rides.length,
      avgRating: this.stats.avgRating, // Preserve from getStats
      avgVehicleRating: this.stats.avgVehicleRating, // Preserve from getStats
      totalDistance: completedRides.reduce((sum, r) => sum + (r.distanceKm ?? 0), 0)
    };
  }

  /**
   * Handle sort change from table component
   */
  handleSort(field: RideSortField) {
    if (this.sortField === field) {
      this.sortDirection = this.sortDirection === 'asc' ? 'desc' : 'asc';
    } else {
      this.sortField = field;
      this.sortDirection = 'desc';
    }
    this.currentPage = 0; // Reset to first page on sort change
    this.saveFilters();
    this.loadRides();
  }

  /**
   * Handle time filter button click
   */
  setTimeFilter(period: 'today' | 'week' | 'month' | 'all') {
    this.timeFilter = period;
    this.applyTimeFilterDates();
    this.currentPage = 0;
    this.saveFilters();
    this.loadRides();
  }

  /**
   * Handle status filter button click
   */
  setStatusFilter(status: 'all' | 'FINISHED' | 'CANCELLED') {
    this.statusFilter = status;
    this.currentPage = 0;
    this.saveFilters();
    this.loadRides();
  }

  /**
   * Handle date range change
   */
  onDateRangeChange() {
    // Clear time filter when manual date range is set
    this.timeFilter = 'all';
    this.currentPage = 0;
    this.saveFilters();
    this.loadRides();
  }

  /**
   * Clear date filters
   */
  clearDateFilters() {
    this.dateFrom = '';
    this.dateTo = '';
    this.timeFilter = 'all';
    this.currentPage = 0;
    this.saveFilters();
    this.loadRides();
  }

  /**
   * Navigate to ride details
   */
  onViewDetails(rideId: string) {
    this.router.navigate(['/driver/overview/ride', rideId]);
  }

  /**
   * Pagination: go to next page
   */
  nextPage() {
    if (this.currentPage < this.totalPages - 1) {
      this.currentPage++;
      this.saveFilters();
      this.loadRides();
    }
  }

  /**
   * Pagination: go to previous page
   */
  prevPage() {
    if (this.currentPage > 0) {
      this.currentPage--;
      this.saveFilters();
      this.loadRides();
    }
  }

  /**
   * Pagination: go to specific page
   */
  goToPage(page: number) {
    if (page >= 0 && page < this.totalPages) {
      this.currentPage = page;
      this.saveFilters();
      this.loadRides();
    }
  }

  /**
   * Save current filters to localStorage
   */
  private saveFilters(): void {
    const filters = {
      timeFilter: this.timeFilter,
      statusFilter: this.statusFilter,
      dateFrom: this.dateFrom,
      dateTo: this.dateTo,
      sortField: this.sortField,
      sortDirection: this.sortDirection,
      currentPage: this.currentPage,
      pageSize: this.pageSize
    };
    localStorage.setItem(this.STORAGE_KEY, JSON.stringify(filters));
  }

  /**
   * Restore filters from localStorage
   */
  private restoreFilters(): void {
    const stored = localStorage.getItem(this.STORAGE_KEY);
    if (!stored) return;

    try {
      const filters = JSON.parse(stored);
      this.timeFilter = filters.timeFilter ?? 'week';
      this.statusFilter = filters.statusFilter ?? 'all';
      this.dateFrom = filters.dateFrom ?? '';
      this.dateTo = filters.dateTo ?? '';
      this.sortField = filters.sortField ?? 'startTime';
      this.sortDirection = filters.sortDirection ?? 'desc';
      this.currentPage = filters.currentPage ?? 0;
      this.pageSize = filters.pageSize ?? 10;
    } catch (e) {
      // Invalid stored data, use defaults
      console.warn('Failed to restore filters from localStorage', e);
    }
  }
}
