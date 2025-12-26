package com.team27.lucky3.backend.dto.response;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResponse {
    private Long id;
    private Long rideId;
    private Long passengerId;
    private int driverRating;
    private int vehicleRating;
    private String comment;
    private LocalDateTime createdAt;
}
