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
    private List<LocationResponse> locations;
    private int etaMinutes;
    private double distanceKm;
}
