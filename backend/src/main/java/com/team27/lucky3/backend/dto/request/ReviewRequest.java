package com.team27.lucky3.backend.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewRequest {
    @Min(value = 1, message = "Driver rating must be at least 1")
    @Max(value = 5, message = "Driver rating must be at most 5")
    private int driverRating;

    @Min(value = 1, message = "Vehicle rating must be at least 1")
    @Max(value = 5, message = "Vehicle rating must be at most 5")
    private int vehicleRating;

    @Size(max = 500, message = "Comment cannot exceed 500 characters")
    private String comment;

    @Min(value = 1, message = "Ride ID must be valid")
    private Long rideId;
}
