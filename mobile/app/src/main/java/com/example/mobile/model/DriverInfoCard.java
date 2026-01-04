package com.example.mobile.model;

public class DriverInfoCard {

    private String name;
    private String surname;
    private String imageUrl;          // or local path / resource reference as String
    private String email;
    private String vehicleLicensePlate;
    private String vehicleModel;
    private float rating;            // 0..5, used by RatingBar
    private String status;           // e.g. "Active,Online" or single value
    private int totalRides;
    private double earnings;         // total earnings, e.g. 45120.0

    // Empty constructor (needed for some serializers, etc.)
    public DriverInfoCard() {
    }

    // Full constructor
    public DriverInfoCard(String name,
                          String surname,
                          String email,
                          String imageUrl,
                          String vehicleLicensePlate,
                          String vehicleModel,
                          float rating,
                          String status,
                          int totalRides,
                          double earnings) {
        this.name = name;
        this.surname = surname;
        this.email = email;
        this.imageUrl = imageUrl;
        this.vehicleLicensePlate = vehicleLicensePlate;
        this.vehicleModel = vehicleModel;
        this.rating = rating;
        this.status = status;
        this.totalRides = totalRides;
        this.earnings = earnings;
    }

    // Getters and setters

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

    public void setEmail(String email){
        this.email = email;
    }
    public String getEmail(){
        return email;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getVehicleLicensePlate() {
        return vehicleLicensePlate;
    }

    public void setVehicleLicensePlate(String vehicleLicensePlate) {
        this.vehicleLicensePlate = vehicleLicensePlate;
    }

    public String getVehicleModel() {
        return vehicleModel;
    }

    public void setVehicleModel(String vehicleModel) {
        this.vehicleModel = vehicleModel;
    }

    public float getRating() {
        return rating;
    }

    public void setRating(float rating) {
        this.rating = rating;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getTotalRides() {
        return totalRides;
    }

    public void setTotalRides(int totalRides) {
        this.totalRides = totalRides;
    }

    public double getEarnings() {
        return earnings;
    }

    public void setEarnings(double earnings) {
        this.earnings = earnings;
    }
}
