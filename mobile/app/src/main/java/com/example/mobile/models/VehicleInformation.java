package com.example.mobile.models;

public class VehicleInformation {
    private String model;
    private VehicleType vehicleType; // Renamed from 'type' to be explicit
    private String licenseNumber; // Standardized
    private Integer passengerSeats; // Standardized
    private Boolean babyTransport;
    private Boolean petTransport;
    private Long driverId;
}
