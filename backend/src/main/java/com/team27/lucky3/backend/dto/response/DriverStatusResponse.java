package com.team27.lucky3.backend.dto.response;

public record DriverStatusResponse(
        Long driverId,
        boolean active,
        boolean inactiveRequested,
        boolean hasActiveRide,
        boolean workingHoursExceeded,
        String workedHoursToday // e.g. "6h 30m"
) {
    // Constructor for backward compatibility
    public DriverStatusResponse(Long driverId, boolean active, boolean inactiveRequested, boolean hasActiveRide) {
        this(driverId, active, inactiveRequested, hasActiveRide, false, null);
    }
}
