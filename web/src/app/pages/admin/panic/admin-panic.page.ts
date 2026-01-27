import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { PanicService, PanicResponse } from '../../../infrastructure/rest/panic.service';
import { Subscription, interval } from 'rxjs';

@Component({
  selector: 'app-admin-panic',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './admin-panic.page.html'
})
export class AdminPanicPage implements OnInit, OnDestroy {
  panics: PanicResponse[] = [];
  isLoading = true;
  errorMessage = '';
  
  private poller: Subscription | null = null;
  private audioContext: AudioContext | null = null;
  private lastPanicCount = 0;

  constructor(
    private panicService: PanicService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadPanics();
    // Poll every 10 seconds for new panic notifications
    this.poller = interval(10000).subscribe(() => this.loadPanics());
  }

  ngOnDestroy(): void {
    this.poller?.unsubscribe();
  }

  loadPanics(): void {
    this.panicService.getPanics(0, 50).subscribe({
      next: (response) => {
        const newCount = response.content?.length || 0;
        
        // Play sound if there are new panics
        if (newCount > this.lastPanicCount && this.lastPanicCount > 0) {
          this.playAlertSound();
        }
        
        this.lastPanicCount = newCount;
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
