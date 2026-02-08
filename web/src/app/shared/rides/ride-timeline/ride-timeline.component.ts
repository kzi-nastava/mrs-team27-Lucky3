import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Ride } from '../../data/ride.model';

@Component({
  selector: 'app-ride-timeline',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './ride-timeline.component.html',
  styleUrl: './ride-timeline.component.css'
})
export class RideTimelineComponent {
  @Input() ride!: Ride;

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
