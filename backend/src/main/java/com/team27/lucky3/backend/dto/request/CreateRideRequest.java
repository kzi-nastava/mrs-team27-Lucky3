package com.team27.lucky3.backend.dto.request;

import com.team27.lucky3.backend.dto.LocationDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateRideRequest {
    @NotNull(message = "Start location is required")
    @Valid
    private LocationDto start;

    @NotNull(message = "Destination is required")
    @Valid
    private LocationDto destination;

    @Valid
    private List<LocationDto> stops;

    private List<String> passengerEmails;

    private LocalDateTime scheduledTime;

    @NotNull(message = "Ride requirements are required")
    @Valid
    private RideRequirements requirements;
}


