import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { PanicService, PanicResponse } from '../../../infrastructure/rest/panic.service';
import { SocketService } from '../../../infrastructure/rest/socket.service';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-admin-panic',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './admin-panic.page.html',
  styles: [`
    @keyframes slide-down {
      from { opacity: 0; transform: translate(-50%, -100%); }
      to { opacity: 1; transform: translate(-50%, 0); }
    }
    :host ::ng-deep .animate-slide-down {
      animation: slide-down 0.3s ease-out;
    }
  `]
})
export class AdminPanicPage implements OnInit, OnDestroy {
  panics: PanicResponse[] = [];
  isLoading = true;
  errorMessage = '';
  
  private panicSocketSub: Subscription | null = null;
  private audioContext: AudioContext | null = null;

  /** Tracks IDs of panics that just arrived via WebSocket for the flash animation */
  newPanicIds = new Set<number>();

  /** Toast notification state */
  showToast = false;
  toastMessage = '';
  private toastTimer: any;

  constructor(
    private panicService: PanicService,
    private socketService: SocketService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadPanics();
    this.subscribeToPanicAlerts();
  }

  ngOnDestroy(): void {
    this.panicSocketSub?.unsubscribe();
    if (this.toastTimer) clearTimeout(this.toastTimer);
  }

  /**
   * Load full panic list from REST API (initial load + manual refresh).
   */
  loadPanics(): void {
    this.panicService.getPanics(0, 50).subscribe({
      next: (response) => {
        this.panics = response.content || [];
        this.isLoading = false;
        this.errorMessage = '';
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.isLoading = false;
        this.errorMessage = 'Failed to load panic notifications';
        console.error('Failed to load panics', err);
        this.cdr.detectChanges();
      }
    });
  }

  /**
   * Subscribe to real-time panic alerts via WebSocket.
   * When a new panic arrives: play sound, show toast, prepend to list.
   */
  private subscribeToPanicAlerts(): void {
    this.panicSocketSub = this.socketService.subscribeToPanicAlerts().subscribe({
      next: (panic: PanicResponse) => {
        // Avoid duplicates
        if (this.panics.some(p => p.id === panic.id)) return;

        // Prepend new panic to the list
        this.panics = [panic, ...this.panics];

        // Mark as new for flash animation
        this.newPanicIds.add(panic.id);
        setTimeout(() => {
          this.newPanicIds.delete(panic.id);
          this.cdr.detectChanges();
        }, 5000);

        // Play urgent alert sound
        this.playAlertSound();

        // Show toast notification
        const who = panic.user ? `${panic.user.name} ${panic.user.surname}` : 'Unknown';
        const rideId = panic.ride?.id || '?';
        this.showToastNotification(`🚨 PANIC on Ride #${rideId} by ${who}`);

        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Panic WebSocket subscription error:', err);
      }
    });
  }

  /**
   * Show a temporary toast notification at the top of the page.
   */
  private showToastNotification(message: string): void {
    this.toastMessage = message;
    this.showToast = true;
    this.cdr.detectChanges();

    if (this.toastTimer) clearTimeout(this.toastTimer);
    this.toastTimer = setTimeout(() => {
      this.showToast = false;
      this.cdr.detectChanges();
    }, 8000);
  }

  dismissToast(): void {
    this.showToast = false;
    if (this.toastTimer) clearTimeout(this.toastTimer);
  }

  playAlertSound(): void {
    try {
      if (!this.audioContext) {
        this.audioContext = new (window.AudioContext || (window as any).webkitAudioContext)();
      }
      
      const oscillator = this.audioContext.createOscillator();
      const gainNode = this.audioContext.createGain();
      
      oscillator.connect(gainNode);
      gainNode.connect(this.audioContext.destination);
      
      oscillator.frequency.value = 800;
      oscillator.type = 'sine';
      gainNode.gain.value = 0.3;
      
      oscillator.start();
      
      // Beep pattern: beep-beep-beep
      setTimeout(() => {
        gainNode.gain.value = 0;
        setTimeout(() => {
          gainNode.gain.value = 0.3;
          setTimeout(() => {
            gainNode.gain.value = 0;
            setTimeout(() => {
              gainNode.gain.value = 0.3;
              setTimeout(() => {
                oscillator.stop();
              }, 150);
            }, 100);
          }, 150);
        }, 100);
      }, 150);
    } catch (e) {
      console.warn('Could not play alert sound', e);
    }
  }

  isNewPanic(panicId: number): boolean {
    return this.newPanicIds.has(panicId);
  }

  formatTime(dateStr: string): string {
    if (!dateStr) return '—';
    const date = new Date(dateStr);
    return date.toLocaleString();
  }

  getTimeSince(dateStr: string): string {
    if (!dateStr) return '';
    const date = new Date(dateStr);
    const now = new Date();
    const diffMs = now.getTime() - date.getTime();
    const diffMins = Math.floor(diffMs / 60000);
    
    if (diffMins < 1) return 'Just now';
    if (diffMins < 60) return `${diffMins} min ago`;
    
    const diffHours = Math.floor(diffMins / 60);
    if (diffHours < 24) return `${diffHours}h ago`;
    
    const diffDays = Math.floor(diffHours / 24);
    return `${diffDays}d ago`;
  }
}
