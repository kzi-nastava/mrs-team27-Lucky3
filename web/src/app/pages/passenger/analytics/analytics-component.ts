import { Component, ChangeDetectorRef  } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { BaseChartDirective } from 'ng2-charts';
import { ChartData, ChartOptions } from 'chart.js';
import { ReportResponse } from '../../../infrastructure/rest/model/report-response.model';
import { AnalyticsService } from '../../../infrastructure/rest/analytics.service';

@Component({
  selector: 'app-analytics',
  standalone: true,
  imports: [CommonModule, BaseChartDirective, FormsModule],
  templateUrl: './analytics-component.html',
})
export class AnalyticsComponent {
  reportData: ReportResponse | null = null;
  
  startDate: string = '';
  endDate: string = '';
  
  totalMoneySpent: number = 0;
  totalKm: number = 0;
  totalRides: number = 0;
  averageMoney: number = 0;
  averageKm: number = 0;
  averageRides: number = 0;

  rideStats = {
    finished: 0,
    cancelled: 0,
    rejected: 0,
    inProgress: 0,
  };
  
  isLoading: boolean = false;
  errorMessage: string = '';

  rideStatusChartType = 'doughnut' as const;

  rideStatusChartData: ChartData<'doughnut', number[], string> = {
    labels: ['Finished', 'Cancelled', 'Rejected', 'In progress'],
    datasets: [
      {
        data: [0, 0, 0, 0],
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

  constructor(
    private analyticsService: AnalyticsService,
    private cdr: ChangeDetectorRef
  ) {}

  applyDateRange(): void {
    // Validate date inputs
    if (!this.startDate || !this.endDate) {
      this.errorMessage = 'Please select both start and end dates';
      return;
    }

    // Validate date range
    if (new Date(this.startDate) > new Date(this.endDate)) {
      this.errorMessage = 'Start date must be before end date';
      return;
    }

    this.errorMessage = '';
    this.isLoading = true;

    // Convert date strings to ISO DateTime format
    const fromDateTime = new Date(this.startDate + 'T00:00:00').toISOString();
    const toDateTime = new Date(this.endDate + 'T23:59:59').toISOString();

    console.log('Fetching analytics from', fromDateTime, 'to', toDateTime);

    this.analyticsService.getReportForUser(-1, fromDateTime, toDateTime, "")
      .subscribe({
        next: (response: ReportResponse) => {
          this.reportData = response;
          this.updateUIWithReportData(response);
          this.isLoading = false;
          this.cdr.detectChanges();
          console.log('Analytics data loaded:', response);
        },
        error: (error) => {
          console.error('Error fetching analytics:', error);
          this.errorMessage = 'Failed to load analytics data. Please try again.';
          this.isLoading = false;
        }
      });
  }

  private updateUIWithReportData(data: ReportResponse): void {
  // Update cumulative statistics
  this.totalMoneySpent = data.cumulativeMoney || 0;
  this.totalKm = data.cumulativeKilometers || 0;
  this.totalRides = data.cumulativeRides || 0;

  // Update average statistics
  this.averageMoney = data.averageMoney || 0;
  this.averageKm = data.averageKilometers || 0;
  this.averageRides = data.averageRides || 0;

  // Update ride statistics
  this.rideStats = {
    finished: data.finishedRides || 0,
    cancelled: data.cancelledRides || 0,
    rejected: data.rejectedRides || 0,
    inProgress: data.inProgressRides || 0,
  };

  // Update chart data
  this.rideStatusChartData.datasets[0].data = [
    this.rideStats.finished,
    this.rideStats.cancelled,
    this.rideStats.rejected,
    this.rideStats.inProgress,
  ];

    // Force chart update
    this.rideStatusChartData = { ...this.rideStatusChartData };
  }

  setRideStats(stats: typeof this.rideStats): void {
    this.rideStats = stats;
    this.rideStatusChartData.datasets[0].data = [
      stats.finished,
      stats.cancelled,
      stats.rejected,
      stats.inProgress,
    ];
  }
}
