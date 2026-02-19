package com.example.mobile.models;

public class CreateDriverRequest {
    private String name;

    private String surname;

    private String email;

    private String address;

    private String phone;

    private VehicleInformation vehicle;

    // Empty constructor
    public CreateDriverRequest() {
    }

    // Full constructor
    public CreateDriverRequest(String name, String surname, String email, String address, String phone, VehicleInformation vehicle) {
        this.name = name;
        this.surname = surname;
        this.email = email;
        this.address = address;
        this.phone = phone;
        this.vehicle = vehicle;
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSurname() {
        return surname;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public VehicleInformation getVehicle() {
        return vehicle;
    }

    public void setVehicle(VehicleInformation vehicle) {
        this.vehicle = vehicle;
    }
}
