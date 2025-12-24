import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Ride } from '../../data/mock-data';

@Component({
  selector: 'app-ride-route',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './ride-route.component.html',
  styleUrl: './ride-route.component.css'
})
export class RideRouteComponent {
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
}
