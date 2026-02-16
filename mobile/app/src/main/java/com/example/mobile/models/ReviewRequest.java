package com.example.mobile.models;

public class ReviewRequest {
    private int driverRating;
    private int vehicleRating;
    private String comment;
    private Long rideId;
    private String token;

    public ReviewRequest() {}

    /** Constructor for authenticated review (from in-app button). */
    public ReviewRequest(Long rideId, int driverRating, int vehicleRating, String comment) {
        this.rideId = rideId;
        this.driverRating = driverRating;
        this.vehicleRating = vehicleRating;
        this.comment = comment;
    }

    /** Constructor for token-based review (from email deep link). */
    public ReviewRequest(String token, int driverRating, int vehicleRating, String comment) {
        this.token = token;
        this.driverRating = driverRating;
        this.vehicleRating = vehicleRating;
        this.comment = comment;
    }

    public int getDriverRating() { return driverRating; }
    public void setDriverRating(int driverRating) { this.driverRating = driverRating; }

    public int getVehicleRating() { return vehicleRating; }
    public void setVehicleRating(int vehicleRating) { this.vehicleRating = vehicleRating; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public Long getRideId() { return rideId; }
    public void setRideId(Long rideId) { this.rideId = rideId; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
}
