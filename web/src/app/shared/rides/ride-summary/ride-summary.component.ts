import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Ride } from '../../data/mock-data';

@Component({
  selector: 'app-ride-summary',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './ride-summary.component.html',
  styleUrl: './ride-summary.component.css'
})
export class RideSummaryComponent {
  @Input() ride!: Ride;

  calculateDuration(start?: string, end?: string): string {
    if (!start || !end) return '—';
    const startTime = new Date(start).getTime();
    const endTime = new Date(end).getTime();
    const diff = endTime - startTime;
    const minutes = Math.floor(diff / 60000);
    return `${minutes} min`;
  }
  
  formatDate(dateStr?: string): string {
    if (!dateStr) return '—';
    return new Date(dateStr).toLocaleDateString('en-US', {
      month: 'short',
      day: 'numeric',
      year: 'numeric'
    });
  }

  formatTime(dateStr?: string): string {
    if (!dateStr) return '—';
    return new Date(dateStr).toLocaleTimeString('en-US', {
      hour: 'numeric',
      minute: '2-digit',
      hour12: true
    });
  }

  formatDistance(distance: number): string {
    if (distance == null || !Number.isFinite(distance)) return '0';
    // Cap to 0-2 decimal places (no trailing zeros)
    return Number(distance.toFixed(2)).toString();
  }
}
