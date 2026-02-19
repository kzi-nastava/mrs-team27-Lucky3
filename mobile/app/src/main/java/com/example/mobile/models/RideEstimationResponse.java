package com.example.mobile.models;

import java.util.List;

public class RideEstimationResponse {
    private int estimatedTimeInMinutes;
    private double estimatedCost;
    private int estimatedDriverArrivalInMinutes;
    private double estimatedDistance;
    private List<RoutePointResponse> routePoints;

    public int getEstimatedTimeInMinutes() {
        return estimatedTimeInMinutes;
    }

    public void setEstimatedTimeInMinutes(int estimatedTimeInMinutes) {
        this.estimatedTimeInMinutes = estimatedTimeInMinutes;
    }

    public double getEstimatedCost() {
        return estimatedCost;
    }

    public void setEstimatedCost(double estimatedCost) {
        this.estimatedCost = estimatedCost;
    }

    public int getEstimatedDriverArrivalInMinutes() {
        return estimatedDriverArrivalInMinutes;
    }

    public void setEstimatedDriverArrivalInMinutes(int estimatedDriverArrivalInMinutes) {
        this.estimatedDriverArrivalInMinutes = estimatedDriverArrivalInMinutes;
    }

    public double getEstimatedDistance() {
        return estimatedDistance;
    }

    public void setEstimatedDistance(double estimatedDistance) {
        this.estimatedDistance = estimatedDistance;
    }

    public List<RoutePointResponse> getRoutePoints() {
        return routePoints;
    }

    public void setRoutePoints(List<RoutePointResponse> routePoints) {
        this.routePoints = routePoints;
    }
}
