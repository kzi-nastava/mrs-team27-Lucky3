package com.example.mobile.models;

public class EndRideRequest {

    private Boolean passengersExited;
    private Boolean paid;

    public EndRideRequest() {
    }

    public EndRideRequest(Boolean passengersExited, Boolean paid) {
        this.passengersExited = passengersExited;
        this.paid = paid;
    }

    public Boolean getPassengersExited() {
        return passengersExited;
    }

    public void setPassengersExited(Boolean passengersExited) {
        this.passengersExited = passengersExited;
    }

    public Boolean getPaid() {
        return paid;
    }

    public void setPaid(Boolean paid) {
        this.paid = paid;
    }
}
