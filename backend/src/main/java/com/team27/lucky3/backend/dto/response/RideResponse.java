package com.team27.lucky3.backend.dto.response;

import com.team27.lucky3.backend.entity.enums.RideStatus;
import com.team27.lucky3.backend.entity.enums.VehicleType;
import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RideResponse {
    private Long id;
    private RideStatus status;
    private Long driverId;
    private List<Long> passengerIds;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private double totalCost;
    private double estimatedCost;
    private VehicleType vehicleType;
    private LocationResponse vehicleLocation;
    private List<RoutePointResponse> routePoints;
    private int etaMinutes;
    private double distanceKm;
    private List<String> inconsistencyReports;


    private String driverEmail;
    private String passengerEmail;
    private long estimatedTimeInMinutes;
    private String rideStatus; // String representation or alternative status
    private String rejectionReason;
    private boolean panicPressed;
    private String departure;
    private String destination;

    public void setDriverEmail(String driverEmail) {
        this.driverEmail = driverEmail;
    }
    public void setPassengerEmail(String passengerEmail) {
        this.passengerEmail = passengerEmail;
    }
    public void setEstimatedTimeInMinutes(long estimatedTimeInMinutes) {
        this.estimatedTimeInMinutes = estimatedTimeInMinutes;
    }
    public void setRideStatus(String rideStatus) {
        this.rideStatus = rideStatus;
    }
    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }
    public void setPanicPressed(boolean panicPressed) {
        this.panicPressed = panicPressed;
    }
    public void setDeparture(String departure) {
        this.departure = departure;
    }
    public void setDestination(String destination) {
        this.destination = destination;
    }
}
