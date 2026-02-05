package com.example.mobile.models;

public class VehicleInformation {
    private String model;
    private VehicleType vehicleType;
    private String licenseNumber;
    private Integer passengerSeats;
    private Boolean babyTransport;
    private Boolean petTransport;
    private Long driverId;

    public VehicleInformation() {

    }

    public VehicleInformation(String model, VehicleType type, String licenseNumber,
                              int seats, boolean babyTransport, boolean petTransport,
                              long driverId) {
        this.model = model;
        this.vehicleType = type;
        this.licenseNumber = licenseNumber;
        this.passengerSeats = seats;
        this.babyTransport = babyTransport;
        this.petTransport = petTransport;
        this.driverId = driverId;
    }

    // Getters
    public String getModel() {
        return model;
    }

    public VehicleType getVehicleType() {
        return vehicleType;
    }

    public String getLicenseNumber() {
        return licenseNumber;
    }

    public Integer getPassengerSeats() {
        return passengerSeats;
    }

    public Boolean getBabyTransport() {
        return babyTransport;
    }

    public Boolean getPetTransport() {
        return petTransport;
    }

    public Long getDriverId() {
        return driverId;
    }

    // Setters
    public void setModel(String model) {
        this.model = model;
    }

    public void setVehicleType(VehicleType vehicleType) {
        this.vehicleType = vehicleType;
    }

    public void setLicenseNumber(String licenseNumber) {
        this.licenseNumber = licenseNumber;
    }

    public void setPassengerSeats(Integer passengerSeats) {
        this.passengerSeats = passengerSeats;
    }

    public void setBabyTransport(Boolean babyTransport) {
        this.babyTransport = babyTransport;
    }

    public void setPetTransport(Boolean petTransport) {
        this.petTransport = petTransport;
    }

    public void setDriverId(Long driverId) {
        this.driverId = driverId;
    }
}
