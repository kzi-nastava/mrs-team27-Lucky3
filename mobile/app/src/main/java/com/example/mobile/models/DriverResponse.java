package com.example.mobile.models;

import com.example.mobile.services.DriverService;
import com.google.gson.annotations.SerializedName;

public class DriverResponse extends UserResponse {
    private VehicleInformation vehicle;
    @SerializedName("active")  // Maps JSON "active" to Java "isActive"
    private boolean isActive;

    @SerializedName("blocked")  // Maps JSON "blocked" to Java "isBlocked"
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
