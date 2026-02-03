package com.team27.lucky3.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminStatsResponse {
    private Integer activeRidesCount;       // Number of rides with status PENDING, ACCEPTED, SCHEDULED, IN_PROGRESS
    private Double averageDriverRating;     // Average rating across all drivers
    private Integer driversOnlineCount;     // Number of drivers currently online
    private Integer totalPassengersInRides; // Total passengers currently in active rides
}
