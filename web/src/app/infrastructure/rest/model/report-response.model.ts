// Interface matching your Java ReportResponse
export interface ReportResponse {
  dailyData: DailyReport[];
  cumulativeRides: number;
  cumulativeKilometers: number;
  cumulativeMoney: number;
  averageRides: number;
  averageKilometers: number;
  averageMoney: number;
  pendingRides: number;
  activeRides: number;
  inProgressRides: number;
  finishedRides: number;
  rejectedRides: number;
  panicRides: number;
  cancelledRides: number;
}

// Interface matching your Java DailyReport
export interface DailyReport {
  date: string;
  rideCount: number;
  kilometers: number;
  money: number;
}