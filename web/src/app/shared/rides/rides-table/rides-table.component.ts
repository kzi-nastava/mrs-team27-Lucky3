import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Ride } from '../../data/mock-data';

@Component({
  selector: 'app-rides-table',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './rides-table.component.html',
  styleUrl: './rides-table.component.css'
})
export class RidesTableComponent {
  @Input() rides: Ride[] = [];
  @Input() sortField: 'startDate' | 'endDate' | 'distance' | 'route' | 'passengers' = 'startDate';
  @Input() sortDirection: 'asc' | 'desc' = 'desc';
  
  @Output() sortChange = new EventEmitter<'startDate' | 'endDate' | 'distance' | 'route' | 'passengers'>();
  @Output() viewDetails = new EventEmitter<string>();

  handleSort(field: 'startDate' | 'endDate' | 'distance' | 'route' | 'passengers') {
    this.sortChange.emit(field);
  }

  onViewDetails(id: string) {
    this.viewDetails.emit(id);
  }

  formatDate(dateStr?: string): string {
    if (!dateStr) return '—';
    return new Date(dateStr).toLocaleDateString('en-GB', {
      day: '2-digit',
      month: '2-digit',
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

  getDuration(start?: string, end?: string): string {
    if (!start || !end) return '—';
    const startTime = new Date(start).getTime();
    const endTime = new Date(end).getTime();
    const diff = Math.round((endTime - startTime) / 60000);
    return `${diff} min`;
  }
}
