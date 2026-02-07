package com.example.mobile.models;

import com.example.mobile.services.DriverService;

public class DriverResponse extends UserResponse {
    private VehicleInformation vehicle;
    private boolean isActive;
    private boolean isBlocked;
    private String active24h;

    // 1. Default Empty Constructor
    public DriverResponse() {
    }

    // 2. Full Constructor (for these specific fields)
    public DriverResponse(boolean isActive, boolean isBlocked, String active24h) {
        this.isActive = isActive;
        this.isBlocked = isBlocked;
        this.active24h = active24h;
    }

    // 3. Getters and Setters
    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public boolean isBlocked() {
        return isBlocked;
    }

    public void setBlocked(boolean blocked) {
        isBlocked = blocked;
    }

    public String getActive24h() {
        return active24h;
    }

    public void setActive24h(String active24h) {
        this.active24h = active24h;
    }

    public VehicleInformation getVehicle() {
        return vehicle;
    }
    public void setVehicle(VehicleInformation vehicle){
        this.vehicle = vehicle;
    }
}
