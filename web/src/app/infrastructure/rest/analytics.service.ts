import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ReportResponse } from './model/report-response.model';
import { AuthService } from '../auth/auth.service';

@Injectable({
  providedIn: 'root'
})
export class AnalyticsService {
  private readonly API_URL = 'http://localhost:8081/api/reports';

  constructor(private http: HttpClient, private authService: AuthService) {}

  /**
   * Fetches analytics report for a specific user
   * @param userId - The user ID
   * @param from - Start date in ISO 8601 format (e.g., '2026-02-01T00:00:00')
   * @param to - End date in ISO 8601 format (e.g., '2026-02-17T23:59:59')
   * @param type - Report type (e.g., 'daily', 'weekly', 'monthly')
   * @returns Observable of ReportResponse
   */
  getReportForUser(
    userId: number,
    from: Date | string,
    to: Date | string,
    type: string
  ): Observable<ReportResponse> {
    if (userId == -1 || userId == null) {
      userId = Number(this.authService.getUserId());
    }
    // Convert dates to ISO string format if Date objects are passed
    const fromDate = from instanceof Date ? from.toISOString() : from;
    const toDate = to instanceof Date ? to.toISOString() : to;

    const params = new HttpParams()
      .set('from', fromDate)
      .set('to', toDate)
      .set('type', type);

    return this.http.get<ReportResponse>(`${this.API_URL}/${userId}`, { params });
  }

  /**
   * Alternative method with individual date components for easier use
   */
  getReportForUserByDateRange(
    userId: number,
    fromDate: Date,
    toDate: Date,
    reportType: 'daily' | 'weekly' | 'monthly'
  ): Observable<ReportResponse> {
    return this.getReportForUser(userId, fromDate, toDate, reportType);
  }
}