package com.example.mobile.models;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

public class CreateRideRequest {
    private LocationDto start;
    private LocationDto destination;
    private List<LocationDto> stops;
    private List<String> passengerEmails;
    private LocalDateTime scheduledTime;
    private RideRequirements requirements;

    public CreateRideRequest(LocationDto start, LocationDto destination) {
        this.start = start;
        this.destination = destination;
        this.stops = new java.util.ArrayList<>();
        this.passengerEmails = new java.util.ArrayList<>();
        this.requirements = new RideRequirements("STANDARD", false, false);
        this.scheduledTime = null; // null means immediate ride
    }

    public LocationDto getStart() {
        return start;
    }

    public void setStart(LocationDto start) {
        this.start = start;
    }

    public LocationDto getDestination() {
        return destination;
    }

    public void setDestination(LocationDto destination) {
        this.destination = destination;
    }

    public List<LocationDto> getStops() {
        return stops;
    }

    public void setStops(List<LocationDto> stops) {
        this.stops = stops;
    }

    public List<String> getPassengerEmails() {
        return passengerEmails;
    }

    public void setPassengerEmails(List<String> passengerEmails) {
        this.passengerEmails = passengerEmails;
    }

    public LocalDateTime getScheduledTime() {
        return scheduledTime;
    }

    public void setScheduledTime(LocalDateTime scheduledTime) {
        this.scheduledTime = scheduledTime;
    }

    /**
     * Convenience method to set scheduled time from milliseconds timestamp
     */
    public void setScheduledTimeFromMillis(long millis) {
        this.scheduledTime = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(millis),
                ZoneId.systemDefault()
        );
    }

    public RideRequirements getRequirements() {
        return requirements;
    }

    public void setRequirements(RideRequirements requirements) {
        this.requirements = requirements;
    }
}
