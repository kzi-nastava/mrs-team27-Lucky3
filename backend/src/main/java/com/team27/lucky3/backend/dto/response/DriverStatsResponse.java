package com.team27.lucky3.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DriverStatsResponse {
    private Long driverId;
    private Double totalEarnings;      // Total earnings from all completed rides
    private Integer completedRides;    // Number of completed rides
    private Double averageRating;      // Average driver rating (1-5)
    private Integer totalRatings;      // Number of ratings received
    private Double averageVehicleRating;  // Average vehicle rating (1-5)
    private Integer totalVehicleRatings;  // Number of vehicle ratings received
    private String onlineHoursToday;   // Formatted as "Xh Ym" - time driver was active in last 24h
}
