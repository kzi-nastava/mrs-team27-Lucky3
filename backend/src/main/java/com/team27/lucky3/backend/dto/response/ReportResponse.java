package com.team27.lucky3.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReportResponse {
    private List<DailyReport> dailyData;

    private Double cumulativeRides;
    private Double cumulativeKilometers;
    private Double cumulativeMoney;

    private Double averageRides;
    private Double averageKilometers;
    private Double averageMoney;

    private int pendingRides;
    private int activeRides;
    private int inProgressRides;
    private int finishedRides;
    private int rejectedRides;
    private int panicRides;
    private int cancelledRides;
}