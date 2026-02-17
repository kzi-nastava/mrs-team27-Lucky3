// analytics.component.ts
import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { BaseChartDirective } from 'ng2-charts';
import { ChartData, ChartOptions, ChartType } from 'chart.js';

@Component({
  selector: 'app-analytics',
  standalone: true,
  imports: [CommonModule, BaseChartDirective],
  templateUrl: './analytics-component.html',
})
export class AnalyticsComponent {
  rideStats = {
    finished: 145,
    cancelled: 23,
    rejected: 12,
    inProgress: 8,
  };

  rideStatusChartType = 'doughnut' as const; // Remove ': ChartType'

  rideStatusChartData: ChartData<'doughnut', number[], string> = {
    labels: ['Finished', 'Cancelled', 'Rejected', 'In progress'],
    datasets: [
      {
        data: [145, 23, 12, 8],
        backgroundColor: ['#10b981', '#ef4444', '#f97316', '#eab308'],
        borderColor: '#111827', // gray-900, blends with dark UI
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
          color: '#d1d5db', // gray-300 for dark theme
          boxWidth: 12,
        },
      },
      tooltip: {
        bodyColor: '#e5e7eb',
        titleColor: '#f9fafb',
      },
    },
  };

  // Call this after fetching analytics for the selected date range
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
