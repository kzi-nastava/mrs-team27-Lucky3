package com.example.mobile.models;

public class FavoriteRouteRequest {
    private String start;
    private String destination;
    private String routeName;

    public FavoriteRouteRequest(String start, String destination, String routeName) {
        this.start = start;
        this.destination = destination;
        this.routeName = routeName;
    }

    // Getters and Setters
    public String getStart() { return start; }
    public void setStart(String start) { this.start = start; }

    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }

    public String getRouteName() { return routeName; }
    public void setRouteName(String routeName) { this.routeName = routeName; }
}
