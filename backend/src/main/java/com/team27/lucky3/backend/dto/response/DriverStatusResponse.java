package com.team27.lucky3.backend.dto.response;

public record DriverStatusResponse(
        Long driverId,
        boolean active,
        boolean inactiveRequested,
        boolean hasActiveRide,
        boolean hasUpcomingRides, // SCHEDULED or PENDING rides
        boolean workingHoursExceeded,
        String workedHoursToday, // e.g. "6h 30m"
        String statusMessage // Message to display to the driver
) {
    // Constructor for backward compatibility
    public DriverStatusResponse(Long driverId, boolean active, boolean inactiveRequested, boolean hasActiveRide) {
        this(driverId, active, inactiveRequested, hasActiveRide, false, false, null, null);
    }
    
    // Constructor without message
    public DriverStatusResponse(Long driverId, boolean active, boolean inactiveRequested, boolean hasActiveRide,
                                boolean hasUpcomingRides, boolean workingHoursExceeded, String workedHoursToday) {
        this(driverId, active, inactiveRequested, hasActiveRide, hasUpcomingRides, workingHoursExceeded, workedHoursToday, null);
    }
}
