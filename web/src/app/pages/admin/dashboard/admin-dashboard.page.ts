// admin-dashboard.page.ts
import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit, ChangeDetectorRef } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import {
  Subject,
  of,
  catchError,
  finalize,
  map,
  shareReplay,
  startWith,
  switchMap,
  tap,
} from 'rxjs';

import { UserService, ChangeInformationResponse } from '../../../infrastructure/rest/user.service';

@Component({
  selector: 'app-admin-dashboard-page',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './admin-dashboard.page.html',
})
export class AdminDashboardPage implements OnInit, OnDestroy {
  requests: ChangeInformationResponse[] = [];
  isLoading = false;
  errorMessage = '';
  name = "Admin";

  private busyIds = new Set<number>();
  private destroy$ = new Subject<void>();

   private refresh$ = new Subject<void>();

  requests$: Observable<ChangeInformationResponse[]> = this.refresh$.pipe(
    startWith(void 0),
    tap(() => { this.isLoading = true; this.errorMessage = ''; }),
    switchMap(() =>
      this.userService.getDriverChangeRequests().pipe(
        // if backend ever returns Page<> etc, normalize:
        map((res: any) => Array.isArray(res) ? res : (res?.content ?? [])),
        catchError((err) => {
          this.errorMessage = err?.error?.message || err?.message || 'Failed to load driver change requests.';
          return of([]);
        }),
        finalize(() => { this.isLoading = false; })
      )
    ),
    shareReplay(1)
  );


  loadRequests(): void {
    this.refresh$.next();
  }

  constructor(private userService: UserService, 
              private cdr: ChangeDetectorRef) {}

  ngOnInit(): void {
    this.loadRequests();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  

  isBusy(id: number): boolean {
    return this.busyIds.has(id);
  }

  getImageUrl(driverId : number | undefined): string | null {
    return driverId !== undefined ? "http://localhost:8081/api/users/" + driverId + "/profile-image" : null;
  }

  approve(requestId: number): void {
    if (this.isBusy(requestId)) return;

    this.busyIds.add(requestId);
    this.errorMessage = '';

    this.userService
      .approveDriverChangeRequest(requestId)
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => this.busyIds.delete(requestId))
      )
      .subscribe({
        next: () => {
          this.requests = this.requests.filter((r: ChangeInformationResponse) => r.id !== requestId);
        },
        error: (err) => {
          this.errorMessage =
            err?.error?.message || err?.message || 'Failed to approve request.';
        },
      });
  }

  reject(requestId: number): void {
    if (this.isBusy(requestId)) return;

    this.busyIds.add(requestId);
    this.errorMessage = '';

    this.userService
      .rejectDriverChangeRequest(requestId)
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => this.busyIds.delete(requestId))
      )
      .subscribe({
        next: () => {
          this.requests = this.requests.filter((r: ChangeInformationResponse) => r.id !== requestId);
        },
        error: (err) => {
          this.errorMessage =
            err?.error?.message || err?.message || 'Failed to reject request.';
        },
      });
  }
  // ===== Template helpers (safe even if some fields are missing) =====
  fmtBool(v: boolean | null | undefined): string {
    if (v === true) return 'Yes';
    if (v === false) return 'No';
    return '—';
  }

  fmtText(v: string | null | undefined): string {
    return v && String(v).trim().length ? String(v) : '—';
  }

  fmtNum(v: number | null | undefined): string {
    return typeof v === 'number' ? String(v) : '—';
  }

  // If your backend uses different field names, just map here:
  getId(r: any): number {
    return Number(r?.id);
  }
}
