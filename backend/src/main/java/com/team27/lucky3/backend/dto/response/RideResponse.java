package com.team27.lucky3.backend.dto.response;

import com.team27.lucky3.backend.dto.LocationDto;
import com.team27.lucky3.backend.entity.enums.RideStatus;
import com.team27.lucky3.backend.entity.enums.VehicleType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RideResponse {
    private Long id;
    private RideStatus status;
    private UserResponse driver;
    private List<UserResponse> passengers;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Double totalCost;
    private VehicleType vehicleType;
    private String model;
    private String licensePlates;
    private LocationDto vehicleLocation;
    private List<RoutePointResponse> routePoints;
    private Double distanceKm;
    private List<InconsistencyResponse> inconsistencyReports;
    private Integer estimatedTimeInMinutes;
    private String rejectionReason;
    private Boolean panicPressed;
    private String panicReason;
    private LocationDto departure;
    private LocationDto destination;
    private LocalDateTime scheduledTime;
    private Double estimatedCost;
    private Boolean petTransport;
    private Boolean babyTransport;
    private Boolean paid;
    private Boolean passengersExited;
    private List<LocationDto> stops;
    private Set<Integer> completedStopIndexes;
    private Double distanceTraveled; // Distance traveled so far (for cost calculation)
    private Double rateBaseFare; // Snapshot: base fare at ride creation
    private Double ratePricePerKm; // Snapshot: price per km at ride creation
    private List<ReviewResponse> reviews;
}
