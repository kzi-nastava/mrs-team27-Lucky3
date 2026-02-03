// admin-requests.page.ts
import { CommonModule } from '@angular/common';
import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { UserService, ChangeInformationResponse } from '../../../infrastructure/rest/user.service';

@Component({
  selector: 'app-admin-requests-page',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './admin-requests.page.html',
})
export class AdminRequestsPage implements OnInit {
  requests: ChangeInformationResponse[] = [];
  isLoading = false;
  errorMessage = '';
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
      next: () => {
         this.isLoading = false;
         this.cdr.detectChanges();
         this.loadRequests();
       },
       error: (error) => {
         this.errorMessage = 'Failed to approve driver change request';
         this.isLoading = false;
         this.busyIds.delete(requestId);
         console.error('Error approving driver change request:', error);
       }
    });
  }

  reject(requestId: number): void {
    if (this.isBusy(requestId)) return;

    this.busyIds.add(requestId);
    this.errorMessage = '';
    this.userService.rejectDriverChangeRequest(requestId).subscribe({
      next: () => {
         this.isLoading = false;
         this.cdr.detectChanges();
         alert('Request rejected successfully.');
         this.loadRequests();
       },
       error: (error) => {
         this.errorMessage = 'Failed to reject driver change request';
         this.isLoading = false;
         this.busyIds.delete(requestId);
         console.error('Error rejecting driver change request:', error);
       }
    });
  }

  // Template helpers
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

  getId(r: any): number {
    return Number(r?.id);
  }
}
