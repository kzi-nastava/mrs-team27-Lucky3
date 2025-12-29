package com.team27.lucky3.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DriverReviewAggregateResponse {
    private Long driverId;
    private double averageDriverRating;
    private double averageVehicleRating;
    private int totalReviews;
}
