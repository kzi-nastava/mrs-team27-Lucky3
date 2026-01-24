package com.team27.lucky3.backend.dto.response;

import com.team27.lucky3.backend.dto.LocationDto;
import com.team27.lucky3.backend.entity.enums.RideStatus;
import com.team27.lucky3.backend.entity.enums.VehicleType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RideCreated{
    private Long id;
    private RideStatus status;              // ASSIGNED / SCHEDULED / REJECTED
    private UserResponse driver;

    private LocationDto departure;
    private LocationDto destination;
    private List<LocationDto> stops;        // optional (keep if you support multi-stops)

    //private List<String> passengersEmails;  // optional

    private LocalDateTime scheduledTime;    // null if "now"
    private Double distanceKm;
    private Integer estimatedTimeInMinutes;
    private Double estimatedCost;

    private String rejectionReason;         // null unless REJECTED
}
