package com.example.mobile.models;

/**
 * DTO for admin dashboard statistics from GET /api/admin/stats
 */
public class AdminStatsResponse {
    private int activeRidesCount;
    private double averageDriverRating;
    private int driversOnlineCount;
    private int totalPassengersInRides;

    public int getActiveRidesCount() { return activeRidesCount; }
    public void setActiveRidesCount(int activeRidesCount) { this.activeRidesCount = activeRidesCount; }

    public double getAverageDriverRating() { return averageDriverRating; }
    public void setAverageDriverRating(double averageDriverRating) { this.averageDriverRating = averageDriverRating; }

    public int getDriversOnlineCount() { return driversOnlineCount; }
    public void setDriversOnlineCount(int driversOnlineCount) { this.driversOnlineCount = driversOnlineCount; }

    public int getTotalPassengersInRides() { return totalPassengersInRides; }
    public void setTotalPassengersInRides(int totalPassengersInRides) { this.totalPassengersInRides = totalPassengersInRides; }
}
