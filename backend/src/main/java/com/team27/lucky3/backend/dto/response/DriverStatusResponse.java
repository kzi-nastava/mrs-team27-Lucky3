package com.team27.lucky3.backend.dto.response;

public record DriverStatusResponse(
        Long driverId,
        boolean active,
        boolean inactiveRequested,
        boolean hasActiveRide
) {}
