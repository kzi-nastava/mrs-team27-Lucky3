package com.example.mobile.models;

/**
 * DTO for driver status from the backend.
 * Matches the backend's DriverStatusResponse record.
 */
public class DriverStatusResponse {

    private Long driverId;
    private boolean active;
    private boolean inactiveRequested;
    private boolean hasActiveRide;
    private boolean hasUpcomingRides;
    private boolean workingHoursExceeded;
    private String workedHoursToday;  // Formatted as "Xh Ym"
    private String statusMessage;

    public DriverStatusResponse() {
    }

    public Long getDriverId() {
        return driverId;
    }

    public void setDriverId(Long driverId) {
        this.driverId = driverId;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isInactiveRequested() {
        return inactiveRequested;
    }

    public void setInactiveRequested(boolean inactiveRequested) {
        this.inactiveRequested = inactiveRequested;
    }

    public boolean isHasActiveRide() {
        return hasActiveRide;
    }

    public void setHasActiveRide(boolean hasActiveRide) {
        this.hasActiveRide = hasActiveRide;
    }

    public boolean isHasUpcomingRides() {
        return hasUpcomingRides;
    }

    public void setHasUpcomingRides(boolean hasUpcomingRides) {
        this.hasUpcomingRides = hasUpcomingRides;
    }

    public boolean isWorkingHoursExceeded() {
        return workingHoursExceeded;
    }

    public void setWorkingHoursExceeded(boolean workingHoursExceeded) {
        this.workingHoursExceeded = workingHoursExceeded;
    }

    public String getWorkedHoursToday() {
        return workedHoursToday != null ? workedHoursToday : "0h 0m";
    }

    public void setWorkedHoursToday(String workedHoursToday) {
        this.workedHoursToday = workedHoursToday;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }
}
