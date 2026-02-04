package com.example.mobile.models;

/**
 * DTO for driver statistics from the backend.
 * Matches the web app's DriverStatsResponse model.
 */
public class DriverStatsResponse {
    
    private Long driverId;
    private Double totalEarnings;
    private Integer completedRides;
    private Double averageRating;
    private Integer totalRatings;
    private Double averageVehicleRating;
    private Integer totalVehicleRatings;
    private String onlineHoursToday; // Formatted as "Xh Ym"
    
    public DriverStatsResponse() {
    }
    
    public Long getDriverId() {
        return driverId;
    }
    
    public void setDriverId(Long driverId) {
        this.driverId = driverId;
    }
    
    public Double getTotalEarnings() {
        return totalEarnings != null ? totalEarnings : 0.0;
    }
    
    public void setTotalEarnings(Double totalEarnings) {
        this.totalEarnings = totalEarnings;
    }
    
    public Integer getCompletedRides() {
        return completedRides != null ? completedRides : 0;
    }
    
    public void setCompletedRides(Integer completedRides) {
        this.completedRides = completedRides;
    }
    
    public Double getAverageRating() {
        return averageRating != null ? averageRating : 0.0;
    }
    
    public void setAverageRating(Double averageRating) {
        this.averageRating = averageRating;
    }
    
    public Integer getTotalRatings() {
        return totalRatings != null ? totalRatings : 0;
    }
    
    public void setTotalRatings(Integer totalRatings) {
        this.totalRatings = totalRatings;
    }
    
    public Double getAverageVehicleRating() {
        return averageVehicleRating != null ? averageVehicleRating : 0.0;
    }
    
    public void setAverageVehicleRating(Double averageVehicleRating) {
        this.averageVehicleRating = averageVehicleRating;
    }
    
    public Integer getTotalVehicleRatings() {
        return totalVehicleRatings != null ? totalVehicleRatings : 0;
    }
    
    public void setTotalVehicleRatings(Integer totalVehicleRatings) {
        this.totalVehicleRatings = totalVehicleRatings;
    }
    
    public String getOnlineHoursToday() {
        return onlineHoursToday != null ? onlineHoursToday : "0h 0m";
    }
    
    public void setOnlineHoursToday(String onlineHoursToday) {
        this.onlineHoursToday = onlineHoursToday;
    }
}
