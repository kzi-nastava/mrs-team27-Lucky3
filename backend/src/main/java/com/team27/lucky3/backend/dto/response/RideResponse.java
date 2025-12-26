package com.team27.lucky3.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RideResponse {
    private Long id;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private double totalCost;
    private String driverEmail;
    private String passengerEmail;
    private long estimatedTimeInMinutes;
    private String rideStatus;
    private String rejectionReason;
    private boolean panicPressed;
    private String departure;
    private String destination;
}
