package com.example.mobile.models;

/**
 * Model for vehicle location response from the API.
 * Matches the backend VehicleLocationResponse DTO.
 */
public class VehicleLocationResponse {
    private Long id;
    private String vehicleType;
    private double latitude;
    private double longitude;
    private Long driverId;
    private boolean available;
    private boolean currentPanic;

    public VehicleLocationResponse() {}

    public VehicleLocationResponse(Long id, String vehicleType, double latitude, double longitude,
                                   Long driverId, boolean available, boolean currentPanic) {
        this.id = id;
        this.vehicleType = vehicleType;
        this.latitude = latitude;
        this.longitude = longitude;
        this.driverId = driverId;
        this.available = available;
        this.currentPanic = currentPanic;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getVehicleType() {
        return vehicleType;
    }

    public void setVehicleType(String vehicleType) {
        this.vehicleType = vehicleType;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public Long getDriverId() {
        return driverId;
    }

    public void setDriverId(Long driverId) {
        this.driverId = driverId;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public boolean isCurrentPanic() {
        return currentPanic;
    }

    public void setCurrentPanic(boolean currentPanic) {
        this.currentPanic = currentPanic;
    }
}
