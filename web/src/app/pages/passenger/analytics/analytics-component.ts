// analytics.component.ts
import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms'; // Add this import
import { BaseChartDirective } from 'ng2-charts';
import { ChartData, ChartOptions, ChartType } from 'chart.js';

@Component({
  selector: 'app-analytics',
  standalone: true,
  imports: [CommonModule, BaseChartDirective, FormsModule], // Add FormsModule here
  templateUrl: './analytics-component.html',
})
export class AnalyticsComponent {
  startDate: string = ''; // Change to string for date input binding
  endDate: string = '';   // Change to string for date input binding

  totalMoneySpent: number = 12450.50; // Add this
  totalKm: number = 3542.8;           // Add this

  rideStats = {
    finished: 145,
    cancelled: 23,
    rejected: 12,
    inProgress: 8,
  };

  rideStatusChartType = 'doughnut' as const;

  rideStatusChartData: ChartData<'doughnut', number[], string> = {
    labels: ['Finished', 'Cancelled', 'Rejected', 'In progress'],
    datasets: [
      {
        data: [145, 23, 12, 8],
        backgroundColor: ['#10b981', '#ef4444', '#f97316', '#eab308'],
        borderColor: '#111827',
        borderWidth: 2,
        hoverOffset: 6,
      },
    ],
  };

  rideStatusChartOptions: ChartOptions<'doughnut'> = {
    responsive: true,
    maintainAspectRatio: false,
    cutout: '65%',
    plugins: {
      legend: {
        position: 'bottom',
        labels: {
          color: '#d1d5db',
          boxWidth: 12,
        },
      },
      tooltip: {
        bodyColor: '#e5e7eb',
        titleColor: '#f9fafb',
      },
    },
  };

  applyDateRange(): void {
    console.log('Applying date range:', this.startDate, 'to', this.endDate);
    // Call your API service here to fetch analytics
  }

  setRideStats(stats: typeof this.rideStats) {
    this.rideStats = stats;
    this.rideStatusChartData.datasets[0].data = [
      stats.finished,
      stats.cancelled,
      stats.rejected,
      stats.inProgress,
    ];
  }
}
