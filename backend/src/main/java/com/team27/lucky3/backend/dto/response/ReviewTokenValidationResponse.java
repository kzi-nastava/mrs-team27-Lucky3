package com.team27.lucky3.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for validating a review token.
 * Returns information about the ride and passenger to personalize the review page.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewTokenValidationResponse {
    private boolean valid;
    private Long rideId;
    private Long passengerId;
    private Long driverId;
    private String driverName;
    private String pickupAddress;
    private String dropoffAddress;
    /** Set when the reviewer is a linked (non-registered) passenger. */
    private String reviewerEmail;
}
