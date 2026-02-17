import { Component, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { BaseChartDirective } from 'ng2-charts';
import { ChartData, ChartOptions } from 'chart.js';
import { ReportResponse, DailyReport } from '../../../infrastructure/rest/model/report-response.model';
import { AnalyticsService } from '../../../infrastructure/rest/analytics.service';

@Component({
  selector: 'app-analytics',
  standalone: true,
  imports: [CommonModule, BaseChartDirective, FormsModule],
  templateUrl: './analytics-component.html',
})
export class AdminAnalyticsComponent {
  reportData: ReportResponse | null = null;
  
  // Report type selection
  reportType: 'specific' | 'PASSENGER' | 'DRIVER' = 'specific';
  userEmail: string = '';
  
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

  // Daily data for bar charts
  dailyData: DailyReport[] = [];

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

  onReportTypeChange(): void {
    // Clear error messages when report type changes
    this.errorMessage = '';    
    // Clear user email if switching away from specific user
    if (this.reportType !== 'specific') {
      this.userEmail = '';
    }
  }

  applyDateRange(): void {
    // Reset messages
    this.errorMessage = '';

    // Validate date inputs
    if (!this.startDate || !this.endDate) {
      this.errorMessage = 'Please select both start and end dates';
      this.cdr.detectChanges();
      return;
    }

    // Validate date range
    if (new Date(this.startDate) > new Date(this.endDate)) {
      this.errorMessage = 'Start date must be before end date';
      this.cdr.detectChanges();
      return;
    }

    // Validate user email for specific user reports
    if (this.reportType === 'specific' && !this.userEmail.trim()) {
      this.errorMessage = 'Please enter a user email address';
      this.cdr.detectChanges();
      return;
    }

    // Validate email format
    if (this.reportType === 'specific' && !this.isValidEmail(this.userEmail)) {
      this.errorMessage = 'Please enter a valid email address';
      this.cdr.detectChanges();
      return;
    }

    this.isLoading = true;
    this.cdr.detectChanges();

    const fromDateTime = new Date(this.startDate + 'T00:00:00').toISOString();
    const toDateTime = new Date(this.endDate + 'T23:59:59').toISOString();

    console.log('Fetching analytics:', {
      reportType: this.reportType,
      userEmail: this.userEmail,
      from: fromDateTime,
      to: toDateTime
    });

    // Call appropriate service method based on report type
    if (this.reportType === 'specific') {
      this.fetchSpecificUserReport(fromDateTime, toDateTime);
    } else {
      this.fetchGlobalReport(fromDateTime, toDateTime);
    }
  }

  private fetchSpecificUserReport(fromDateTime: string, toDateTime: string): void {
    this.analyticsService.getReportForUserByEmail(this.userEmail, fromDateTime, toDateTime)
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
          
          // Handle specific error cases
          if (error.status === 404) {
            this.errorMessage = `User with email "${this.userEmail}" not found`;
          } else if (error.status === 400) {
            this.errorMessage = 'Invalid request. Please check your input';
          } else {
            this.errorMessage = 'Failed to load analytics data. Please try again.';
          }
          
          this.isLoading = false;
          this.cdr.detectChanges();
        }
      });
  }

  private fetchGlobalReport(fromDateTime: string, toDateTime: string): void {
    this.analyticsService.getReportForUserType(fromDateTime, toDateTime, this.reportType)
      .subscribe({
        next: (response: ReportResponse) => {
          this.reportData = response;
          this.updateUIWithReportData(response);
          const userTypeLabel = this.reportType === 'PASSENGER' ? 'Passengers' : 'Drivers';
          this.isLoading = false;
          this.cdr.detectChanges();
          console.log('Analytics data loaded:', response);
        },
        error: (error) => {
          console.error('Error fetching analytics:', error);
          this.errorMessage = 'Failed to load global analytics data. Please try again.';
          this.isLoading = false;
          this.cdr.detectChanges();
        }
      });
  }

  private isValidEmail(email: string): boolean {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return emailRegex.test(email);
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

    // Update daily data for charts
    this.dailyData = data.dailyData || [];

    // Update chart data
    this.rideStatusChartData.datasets[0].data = [
      this.rideStats.finished,
      this.rideStats.cancelled,
      this.rideStats.rejected,
      this.rideStats.inProgress,
    ];

    this.rideStatusChartData = { ...this.rideStatusChartData };
    this.cdr.detectChanges();
  }

  // Calculate bar height percentage based on max value
  calculateBarHeight(value: number, dataArray: number[]): string {
    if (!dataArray || dataArray.length === 0) return '0%';
    const maxValue = Math.max(...dataArray);
    if (maxValue === 0) return '0%';
    return `${(value / maxValue) * 100}%`;
  }

  // Get ride counts from daily data
  getRideCounts(): number[] {
    return this.dailyData.map(day => day.rideCount || 0);
  }

  // Get money values from daily data
  getMoneyValues(): number[] {
    return this.dailyData.map(day => day.money || 0);
  }

  // Get kilometers from daily data
  getKilometers(): number[] {
    return this.dailyData.map(day => day.kilometers || 0);
  }

  // Format date for display (e.g., "Feb 15")
  formatDate(dateString: string): string {
    const date = new Date(dateString);
    return date.toLocaleDateString('en-US', { month: 'short', day: 'numeric' });
  }

  setRideStats(stats: typeof this.rideStats): void {
    this.rideStats = stats;
    this.rideStatusChartData.datasets[0].data = [
      stats.finished,
      stats.cancelled,
      stats.rejected,
      stats.inProgress,
    ];
    this.cdr.detectChanges();
  }
}
