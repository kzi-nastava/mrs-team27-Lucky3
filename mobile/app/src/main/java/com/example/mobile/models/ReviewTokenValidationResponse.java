package com.example.mobile.models;

public class ReviewTokenValidationResponse {
    private boolean valid;
    private Long rideId;
    private Long passengerId;
    private Long driverId;
    private String driverName;
    private String pickupAddress;
    private String dropoffAddress;
    private String reviewerEmail;

    public ReviewTokenValidationResponse() {}

    public boolean isValid() { return valid; }
    public void setValid(boolean valid) { this.valid = valid; }

    public Long getRideId() { return rideId; }
    public void setRideId(Long rideId) { this.rideId = rideId; }

    public Long getPassengerId() { return passengerId; }
    public void setPassengerId(Long passengerId) { this.passengerId = passengerId; }

    public Long getDriverId() { return driverId; }
    public void setDriverId(Long driverId) { this.driverId = driverId; }

    public String getDriverName() { return driverName; }
    public void setDriverName(String driverName) { this.driverName = driverName; }

    public String getPickupAddress() { return pickupAddress; }
    public void setPickupAddress(String pickupAddress) { this.pickupAddress = pickupAddress; }

    public String getDropoffAddress() { return dropoffAddress; }
    public void setDropoffAddress(String dropoffAddress) { this.dropoffAddress = dropoffAddress; }

    public String getReviewerEmail() { return reviewerEmail; }
    public void setReviewerEmail(String reviewerEmail) { this.reviewerEmail = reviewerEmail; }
}
