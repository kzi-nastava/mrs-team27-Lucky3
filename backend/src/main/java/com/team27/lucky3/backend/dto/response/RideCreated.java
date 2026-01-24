package com.team27.lucky3.backend.dto.response;

import com.team27.lucky3.backend.dto.LocationDto;
import com.team27.lucky3.backend.entity.Ride;
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
    //driver details
    private Long driverId;
    private String driverName;
    private String driverSurname;
    private String driverEmail;
    private String driverProfilePictureUrl;

    //vehicle details
    private String vehicleModel;
    private String vehicleLicensePlate;
    private Boolean babyTransport;
    private Boolean petTransport;
    private VehicleType requestedVehicleType;

    //Route details
    private LocationDto departure;
    private LocationDto destination;
    private List<LocationDto> stops;        // optional (keep if you support multi-stops)
    private List<RoutePointResponse> routePoints;

    private List<String> passengersEmails;

    // Timing and cost details
    private LocalDateTime scheduledTime;    // null if "now"
    private Double distanceKm;
    private Integer estimatedTimeInMinutes;
    private Double estimatedCost;

    private String rejectionReason;         // null unless REJECTED

    public static RideCreated fromRide(Ride r){
        return new RideCreated(r.getId(),
                r.getStatus(),
                r.getDriver().getId(),
                r.getDriver().getName(),
                r.getDriver().getSurname(),
                r.getDriver().getEmail(),
                null,
                null,
                null,
                null,
                null,
                r.getRequestedVehicleType(),
                new LocationDto(r.getStartLocation().getAddress(), r.getStartLocation().getLatitude(), r.getStartLocation().getLongitude()),
                new LocationDto(r.getEndLocation().getAddress(), r.getEndLocation().getLatitude(), r.getEndLocation().getLongitude()),
                null, // stops
                null,
                r.getInvitedEmails(), // passengers emails
                r.getScheduledTime(),
                r.getDistance(),
                null, // estimated time
                r.getEstimatedCost(),
                r.getRejectionReason()
        );
    }
}
