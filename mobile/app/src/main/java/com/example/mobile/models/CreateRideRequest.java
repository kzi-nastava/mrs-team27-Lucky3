package com.example.mobile.models;

import java.util.List;

public class CreateRideRequest {
    private LocationDto start;
    private LocationDto destination;
    private List<LocationDto> stops;
    private List<String> passengerEmails;
    private RideRequirements requirements;

    public CreateRideRequest(LocationDto start, LocationDto destination) {
        this.start = start;
        this.destination = destination;
        this.stops = new java.util.ArrayList<>();
        this.passengerEmails = new java.util.ArrayList<>();
        this.requirements = new RideRequirements("STANDARD", false, false);
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

    public RideRequirements getRequirements() {
        return requirements;
    }

    public void setRequirements(RideRequirements requirements) {
        this.requirements = requirements;
    }
}
