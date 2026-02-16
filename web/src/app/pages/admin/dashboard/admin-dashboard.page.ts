// admin-dashboard.page.ts
import { CommonModule } from '@angular/common';
import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { Subject, Subscription, takeUntil, debounceTime, distinctUntilChanged } from 'rxjs';

import { RideService, PageResponse, AdminStatsResponse } from '../../../infrastructure/rest/ride.service';
import { RideResponse } from '../../../infrastructure/rest/model/ride-response.model';
import { ActiveRidesTableComponent, ActiveRideSortField } from './active-rides-table/active-rides-table.component';
import { SocketService } from '../../../infrastructure/rest/socket.service';
import { PanicResponse } from '../../../infrastructure/rest/panic.service';
import { DriverService, DriverStatsResponse } from '../../../infrastructure/rest/driver.service';

@Component({
  selector: 'app-admin-dashboard-page',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, ActiveRidesTableComponent],
  templateUrl: './admin-dashboard.page.html',
})
export class AdminDashboardPage implements OnInit, OnDestroy {
  // Expose Math for template
  Math = Math;

  // Stats
  stats: AdminStatsResponse = {
    activeRidesCount: 0,
    averageDriverRating: 0,
    driversOnlineCount: 0,
    totalPassengersInRides: 0
  };

  // Active rides
  rides: RideResponse[] = [];
  isLoading = false;
  errorMessage = '';

  // Driver ratings cache: driverId -> averageRating
  driverRatings: Record<number, number> = {};

  // Filters
  searchQuery = '';
  statusFilter: 'all' | 'PENDING' | 'ACCEPTED' | 'IN_PROGRESS' | 'SCHEDULED' = 'all';
  vehicleTypeFilter: 'all' | 'STANDARD' | 'LUXURY' | 'VAN' = 'all';

  // Sorting
  sortField: ActiveRideSortField = 'status';
  sortDirection: 'asc' | 'desc' = 'desc';

  // Pagination
  currentPage = 0;
  pageSize = 10;
  totalElements = 0;
  totalPages = 0;

  // Debounce search
  private searchSubject = new Subject<string>();
  private destroy$ = new Subject<void>();

  // Panic alert
  panicAlert: string | null = null;
  private panicSub: Subscription | null = null;
  private panicAudioContext: AudioContext | null = null;
  private panicAlertTimer: any;

  constructor(
    private rideService: RideService,
    private cdr: ChangeDetectorRef,
    private socketService: SocketService,
    private driverService: DriverService
  ) {}

  ngOnInit(): void {
    this.loadStats();
    this.loadRides();
    this.subscribeToPanicAlerts();

    // Setup search debounce
    this.searchSubject.pipe(
      debounceTime(300),
      distinctUntilChanged(),
      takeUntil(this.destroy$)
    ).subscribe(query => {
      this.searchQuery = query;
      this.currentPage = 0;
      this.loadRides();
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    this.panicSub?.unsubscribe();
    if (this.panicAlertTimer) clearTimeout(this.panicAlertTimer);
  }

  private subscribeToPanicAlerts(): void {
    this.panicSub = this.socketService.subscribeToPanicAlerts().subscribe({
      next: (panic: PanicResponse) => {
        const who = panic.user ? `${panic.user.name} ${panic.user.surname}` : 'Unknown';
        const rideId = panic.ride?.id || '?';
        this.panicAlert = `PANIC on Ride #${rideId} by ${who} — ${panic.reason || 'No reason given'}`;
        this.playPanicSound();
        // Also refresh the rides list to show updated panic state
        this.loadRides();
        this.cdr.detectChanges();
        // Auto-dismiss after 15s
        if (this.panicAlertTimer) clearTimeout(this.panicAlertTimer);
        this.panicAlertTimer = setTimeout(() => {
          this.panicAlert = null;
          this.cdr.detectChanges();
        }, 15000);
      }
    });
  }

  dismissPanicAlert(): void {
    this.panicAlert = null;
    if (this.panicAlertTimer) clearTimeout(this.panicAlertTimer);
  }

  private playPanicSound(): void {
    try {
      if (!this.panicAudioContext) {
        this.panicAudioContext = new (window.AudioContext || (window as any).webkitAudioContext)();
      }
      const osc = this.panicAudioContext.createOscillator();
      const gain = this.panicAudioContext.createGain();
      osc.connect(gain);
      gain.connect(this.panicAudioContext.destination);
      osc.frequency.value = 800;
      osc.type = 'sine';
      gain.gain.value = 0.3;
      osc.start();
      setTimeout(() => { gain.gain.value = 0; setTimeout(() => { gain.gain.value = 0.3; setTimeout(() => { gain.gain.value = 0; setTimeout(() => { gain.gain.value = 0.3; setTimeout(() => { osc.stop(); }, 150); }, 100); }, 150); }, 100); }, 150);
    } catch (e) {
      console.warn('Could not play panic sound', e);
    }
  }

  loadStats(): void {
    this.rideService.getAdminStats().subscribe({
      next: (stats) => {
        this.stats = stats;
        this.cdr.detectChanges();
      },
      error: (error) => {
        console.error('Error loading admin stats:', error);
      }
    });
  }

  loadRides(): void {
    this.isLoading = true;
    this.errorMessage = '';

    const params: any = {
      page: this.currentPage,
      size: this.pageSize,
      sort: this.buildSortString()
    };

    if (this.searchQuery.trim()) {
      params.search = this.searchQuery.trim();
    }

    if (this.statusFilter !== 'all') {
      params.status = this.statusFilter;
    }

    if (this.vehicleTypeFilter !== 'all') {
      params.vehicleType = this.vehicleTypeFilter;
    }

    this.rideService.getAllActiveRides(params).subscribe({
      next: (page) => {
        this.rides = page.content || [];
        this.totalElements = page.totalElements || 0;
        this.totalPages = page.totalPages || 0;
        this.isLoading = false;
        this.fetchDriverRatings();
        this.cdr.detectChanges();
      },
      error: (error) => {
        this.errorMessage = 'Failed to load active rides';
        this.isLoading = false;
        console.error('Error loading active rides:', error);
      }
    });
  }

  private buildSortString(): string {
    // Map frontend field names to backend entity field names
    const fieldMap: Record<ActiveRideSortField, string> = {
      'driver': 'driver.name',
      'vehicle': 'model',
      'status': 'status',
      'passengerCount': 'status', // Can't sort by collection size on backend
      'rating': 'status', // Would need driver rating in ride entity
      'timeActive': 'startTime',
      'estimatedTime': 'estimatedTimeInMinutes'
    };
    
    const backendField = fieldMap[this.sortField] || 'status';
    return `${backendField},${this.sortDirection}`;
  }

  private fetchDriverRatings(): void {
    const uniqueDriverIds = new Set<number>();
    for (const ride of this.rides) {
      if (ride.driver?.id && !(ride.driver.id in this.driverRatings)) {
        uniqueDriverIds.add(ride.driver.id);
      }
    }
    for (const driverId of uniqueDriverIds) {
      this.driverService.getStats(driverId).subscribe({
        next: (stats) => {
          this.driverRatings[driverId] = stats.averageRating || 0;
          this.cdr.detectChanges();
        },
        error: () => { /* silently ignore */ }
      });
    }
  }

  onSearchChange(event: Event): void {
    const query = (event.target as HTMLInputElement).value;
    this.searchSubject.next(query);
  }

  setStatusFilter(status: 'all' | 'PENDING' | 'ACCEPTED' | 'IN_PROGRESS' | 'SCHEDULED'): void {
    this.statusFilter = status;
    this.currentPage = 0;
    this.loadRides();
  }

  setVehicleTypeFilter(type: 'all' | 'STANDARD' | 'LUXURY' | 'VAN'): void {
    this.vehicleTypeFilter = type;
    this.currentPage = 0;
    this.loadRides();
  }

  handleSort(field: ActiveRideSortField): void {
    if (this.sortField === field) {
      this.sortDirection = this.sortDirection === 'asc' ? 'desc' : 'asc';
    } else {
      this.sortField = field;
      this.sortDirection = 'desc';
    }
    this.loadRides();
  }

  // Pagination
  prevPage(): void {
    if (this.currentPage > 0) {
      this.currentPage--;
      this.loadRides();
    }
  }

  nextPage(): void {
    if (this.currentPage < this.totalPages - 1) {
      this.currentPage++;
      this.loadRides();
    }
  }

  goToPage(page: number): void {
    this.currentPage = page;
    this.loadRides();
  }

  refresh(): void {
    this.loadStats();
    this.loadRides();
  }
}
