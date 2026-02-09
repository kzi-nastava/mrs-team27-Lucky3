// models/DriverResponse.java
package com.example.mobile.models;

public class DriverProfileResponse extends UserResponse {
    private VehicleInformation vehicle;
    private boolean isActive;
    private boolean isBlocked;
    private String active24h;

    public DriverProfileResponse() {
        super();
    }

    public DriverProfileResponse(Long id, String name, String surname, String email,
                          String profilePictureUrl, String role, String phoneNumber,
                          String address, VehicleInformation vehicle, boolean isActive,
                          boolean isBlocked, String active24h) {
        super(id, name, surname, email, profilePictureUrl, role, phoneNumber, address);
        this.vehicle = vehicle;
        this.isActive = isActive;
        this.isBlocked = isBlocked;
        this.active24h = active24h;
    }

    // Getters and setters
    public VehicleInformation getVehicle() {
        return vehicle;
    }

    public void setVehicle(VehicleInformation vehicle) {
        this.vehicle = vehicle;
    }

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
}
