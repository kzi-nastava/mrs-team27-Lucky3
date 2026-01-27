// admin-dashboard.page.ts
import { CommonModule } from '@angular/common';
import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { finalize } from 'rxjs';
import { FormsModule } from '@angular/forms';

import { UserService, ChangeInformationResponse } from '../../../infrastructure/rest/user.service';

@Component({
  selector: 'app-admin-dashboard-page',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './admin-dashboard.page.html',
})
export class AdminDashboardPage implements OnInit {
  requests: ChangeInformationResponse[] = [];
  isLoading = false;
  errorMessage = '';
  name = "Admin";
  busyIds: Set<number> = new Set<number>();

  constructor(private userService: UserService, 
              private cdr: ChangeDetectorRef) {}
  
  ngOnInit(): void {
    this.loadRequests();
  }

  loadRequests(): void {
    this.isLoading = true;
    this.errorMessage = '';
    
    this.userService.getDriverChangeRequests().subscribe({
      next: (requests) => {
         this.requests = requests;
         this.isLoading = false;
         this.cdr.detectChanges();
       },
       error: (error) => {
         this.errorMessage = 'Failed to load driver change requests';
         this.isLoading = false;
         console.error('Error loading driver change requests:', error);
       }
    });
  }

  getImageUrl(driverId : number | undefined): string | null {
    //console.log("Getting image URL for driver ID:", driverId);
    return driverId !== undefined ? "http://localhost:8081/api/users/" + driverId + "/profile-image" : null;
  }

  isBusy(requestId: number): boolean {
    return this.busyIds.has(requestId);
  }

  approve(requestId: number): void {
    if (this.isBusy(requestId)) return;

    this.busyIds.add(requestId);
    this.errorMessage = '';
    this.userService.approveDriverChangeRequest(requestId).subscribe({
      next: (requests) => {
         this.isLoading = false;
         this.cdr.detectChanges();
          
         this.loadRequests();
       },
       error: (error) => {
         this.errorMessage = 'Failed to load driver change requests';
         this.isLoading = false;
         console.error('Error loading driver change requests:', error);
       }
    });
  }

  reject(requestId: number): void {
    if (this.isBusy(requestId)) return;

    this.busyIds.add(requestId);
    this.errorMessage = '';
    this.userService.rejectDriverChangeRequest(requestId).subscribe({
      next: (requests) => {
         this.isLoading = false;
         this.cdr.detectChanges();
          alert('Request rejected successfully.');
         this.loadRequests();
       },
       error: (error) => {
         this.errorMessage = 'Failed to load driver change requests';
         this.isLoading = false;
         console.error('Error loading driver change requests:', error);
       }
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
